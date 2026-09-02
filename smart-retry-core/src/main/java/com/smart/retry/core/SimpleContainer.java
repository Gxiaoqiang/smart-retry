package com.smart.retry.core;

import com.smart.retry.common.RetryConfiguration;
import com.smart.retry.common.RetryContainer;
import com.smart.retry.common.SmartRetryExit;
import com.smart.retry.common.SmartRetryRunFlag;
import com.smart.retry.common.constant.ExecuteResultStatus;
import com.smart.retry.common.constant.RetryTaskStatus;
import com.smart.retry.common.model.RetryTask;
import com.smart.retry.common.exception.RetryTaskClaimedException;
import com.smart.retry.common.model.TaskExecutionResult;
import com.smart.retry.core.cache.RetryCache;
import com.smart.retry.core.config.SmartExecutorConfigure;
import com.smart.retry.core.innovation.DefaultInnovation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.*;

/**
 * @Author xiaoqiang
 * @Version SimpleContainer.java, v 0.1 2025年02月18日 00:24 xiaoqiang
 * @Description: TODO
 */
public class SimpleContainer implements RetryContainer {

    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(SimpleContainer.class);

    // ========== DelayQueue 精准调度相关字段 ==========

    /**
     * 内存精准调度队列
     */
    private static final DelayQueue<ScheduledTask> delayQueue = new DelayQueue<>();

    /**
     * 调度线程
     */
    private static Thread schedulerThread;

    /**
     * 预加载窗口毫秒数
     */
    private static volatile long preloadWindowMs;

    private static RetryConfiguration retryConfiguration;

    private static SmartExecutorConfigure smartConfigure;

    private static ThreadPoolExecutor consumerExecutor;

    private static BlockingQueue<Runnable> consumerQueue;

    private static ThreadPoolTaskScheduler taskScheduler;

    public SimpleContainer(RetryConfiguration retryConfiguration, SmartExecutorConfigure smartExecutorConfigure) {
        this.retryConfiguration = retryConfiguration;
        this.smartConfigure = smartExecutorConfigure;
    }


    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (schedulerThread != null) {
                schedulerThread.interrupt();
            }
            if (consumerExecutor != null) {
                consumerExecutor.shutdown();
            }
            if (taskScheduler != null) {
                taskScheduler.shutdown();
            }
        }));
    }


    @Override
    public void start() {
        initTaskExecutor(smartConfigure);

        // 初始化预加载窗口
        preloadWindowMs = (long) smartConfigure.getTaskFindInterval() * smartConfigure.getScanPreloadMultiplier() * 1000L;

        // Producer 兜底扫描线程（低频，仅加载到 DelayQueue）
        Thread producerTask = new Thread(new ProducerTask(), "smart-retry-producer");
        producerTask.start();

        // SchedulerThread 调度线程（从 DelayQueue 消费，精准触发）
        schedulerThread = new Thread(new SchedulerThread(), "smart-retry-scheduler");
        schedulerThread.start();

        if (smartConfigure.getDeadTask().getDeadTaskCheck()) {
            Thread deadLetterTask = new Thread(new DeadLetterTask());
            deadLetterTask.setDaemon(true);
            deadLetterTask.start();
        }

        if (smartConfigure.getClearTask().getEnabled()) {
            initTaskScheduler();
            CronTrigger trigger = new CronTrigger(smartConfigure.getClearTask().getCron());
            taskScheduler.schedule(new ClearTask(), trigger);
        }
    }


    private synchronized static void initTaskExecutor(SmartExecutorConfigure smartConfigure) {

        if (consumerExecutor != null) {
            return;
        }

        int corePoolSize = smartConfigure.getExecutor().getCorePoolSize();
        int maxPoolSize = smartConfigure.getExecutor().getMaxPoolSize();
        int queueSize = smartConfigure.getMaxInMemory();
        String name = smartConfigure.getExecutor().getName();
        consumerQueue = new ArrayBlockingQueue<>(queueSize+100);


        consumerExecutor = new ThreadPoolExecutor(corePoolSize,
                maxPoolSize,
                1L, TimeUnit.SECONDS,
                consumerQueue,
                new ThreadFactory() {
                    @Override
                    public Thread newThread(Runnable r) {
                        return new Thread(r, name);
                    }
                },
                //采用拒绝策略为callerRunsPolicy，即当线程池队列满时，直接在调用者线程中运行被拒绝的任务
                new ThreadPoolExecutor.CallerRunsPolicy());
    }

    private void initTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("clear-task-scheduler-");
        scheduler.initialize();
        taskScheduler = scheduler;
    }


    @Override
    public void destroy() {

    }

    static String getUniqueKey(RetryTask retryTask) {
        return retryTask.getTaskCode() + "-" + retryTask.getUniqueKey();
    }

    /**
     * DelayQueue 元素，按 next_plan_time 排序
     */
    static class ScheduledTask implements Delayed {
        private final RetryTask task;
        private final long executeTimeMillis;

        ScheduledTask(RetryTask task) {
            this.task = task;
            this.executeTimeMillis = task.getNextPlanTime().getTime();
        }

        RetryTask getTask() {
            return task;
        }

        @Override
        public long getDelay(TimeUnit unit) {
            return unit.convert(
                    executeTimeMillis - System.currentTimeMillis(),
                    TimeUnit.MILLISECONDS);
        }

        @Override
        public int compareTo(Delayed o) {
            return Long.compare(this.executeTimeMillis,
                    ((ScheduledTask) o).executeTimeMillis);
        }
    }

    /**
     * 将任务加入 DelayQueue，自动去重。
     * 内存上限（maxInMemory）在此处精确校验：size 检查与去重标记在同一把锁内原子完成。
     *
     * @param task 重试任务
     * @return true=入队成功，false=未入队（已在内存中或已达内存上限）
     */
    public synchronized static boolean enqueue(RetryTask task) {
        String key = getUniqueKey(task);
        // 内存上限精确控制 + 去重：两者在同一把锁内原子完成，并发下内存任务数不会超过 maxInMemory
        if (!RetryTaskCache.tryMarkIfBelowLimit(key, smartConfigure.getMaxInMemory())) {
            return false;
        }
        delayQueue.put(new ScheduledTask(task));
        return true;
    }

    /**
     * 静态方法：任务写入 DB 后调用，窗口内则入队
     * 供 RemoteRetryer 和 SimpleRetryTaskOperator 使用
     *
     * @param task 重试任务
     */
    public static void enqueueIfInWindow(RetryTask task) {
        if (task == null || task.getNextPlanTime() == null) {
            return;
        }

        // 内存上限快速失败：超过 maxInMemory 时拒绝入队，任务留在 DB 由 Producer 兜底扫描。
        // enqueue() 内还会在 synchronized 锁中做精确校验，保证并发下不超限。
        int currentSize = RetryTaskCache.size();
        int maxInMemory = smartConfigure.getMaxInMemory();
        if (currentSize >= maxInMemory) {
            LOGGER.warn("[SimpleContainer#enqueueIfInWindow] 内存任务数已达到上限，"
                            + "任务留在 DB 等待 Producer 兜底扫描。"
                            + "taskId={}, currentSize={}, maxInMemory={}",
                    task.getId(), currentSize, maxInMemory);
            return;
        }

        long effectiveWindowMs = preloadWindowMs;

        long nextPlanTime = task.getNextPlanTime().getTime();
        long windowEnd = System.currentTimeMillis() + effectiveWindowMs;
        if (nextPlanTime <= windowEnd) {
            enqueue(task);
            return;
        }
        if (smartConfigure.shouldLogInfo()) {
            LOGGER.info("[SimpleContainer#enqueueIfInWindow] 任务不在预加载窗口内，等待 Producer 兜底。"
                            + "taskId={}, nextPlanTime={}, windowEnd={}, preloadWindowMs={}",
                    task.getId(), task.getNextPlanTime(), windowEnd, effectiveWindowMs);
        }
    }

    /**
     * 事务提交后入队（若当前在活跃事务中）；否则立即入队。
     *
     * <p>用于"业务数据 + 重试任务同事务持久化"的创建入口（如 createTask）：
     * 内存入队属于非事务副作用，若在事务内直接入队，调用方事务回滚时会产生
     * <ul>
     *     <li><b>幽灵任务</b>：DB 无记录但 delayQueue 已入队，消费时认领失败被跳过，产生无谓调度；</li>
     *     <li><b>脏去重 key</b>：{@link RetryTaskCache#tryMark(String)} 预标记的 key 无事务回调释放，
     *     永久残留在内存去重集合，后续同 uniqueKey 的 createTask 被拦截无法入队，只能靠 Producer 兜底。</li>
     * </ul>
     * 通过 {@link TransactionSynchronization#afterCommit()} 将入队推迟到提交后，回滚时自然跳过。
     * 无活跃事务（代理未生效 / 直接调用）时立即入队，保持兼容。
     *
     * @param task 已写入 DB 并回填 id 的重试任务
     */
    public static void enqueueAfterCommit(RetryTask task) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    enqueueIfInWindow(task);
                }
            });
        } else {
            enqueueIfInWindow(task);
        }
    }

    /**
     * 任务执行完毕后的回调。
     * 注意：此时 DB 已更新 (status/retryNum/nextPlanTime)
     *
     * <p>关键设计：不在方法开头移除 inMemoryTaskKeys，而是保留 key 作为"占位锁"，
     * 阻止 Producer 在竞态窗口中重复加载同一任务。只有在确定不再重入队时才移除 key。
     *
     * <p>单次执行模型：失败且未到终态的任务放入 DelayQueue，
     * 后续重试由 SchedulerThread/Producer 异步调度推进，不再同步循环。
     *
     * @param task 已执行完毕的任务
     * @return true=已重新入队等待异步重试，false=已到达终态（unmark）
     */
    static boolean afterExecute(RetryTask task) {

        String key = getUniqueKey(task);
        Integer status = task.getStatus();
        // 成功或重试次数耗尽 → 移除占位，结束
        if (RetryTaskStatus.SUCCESS.getCode().equals(status)) {
            RetryTaskCache.unmark(key);
            return false;
        }
        Integer retryNum = task.getRetryNum();
        if (retryNum <= 0) {
            RetryTaskCache.unmark(key);
            return false;
        }
        // 如果taskCode在RetryCache中不存在，说明无法执行，不重新入队
        // 由Producer兜底扫描后续处理
        if (RetryCache.get(task.getTaskCode()) == null) {
            RetryTaskCache.unmark(key);
            return false;
        }
        Date nextPlanTime = task.getNextPlanTime();
        // 使用与 enqueueIfInWindow 一致的防御逻辑
        boolean inWindow = isInWindow(nextPlanTime);
        if (!inWindow) {
            RetryTaskCache.unmark(key);
            return false;
        }

        // 失败且未到终态：保留占位锁，放入 delayQueue 等待异步调度重试
        delayQueue.put(new ScheduledTask(task));
        return true;
    }

    private static boolean isInWindow(Date nextPlanTime) {
        long effectiveWindowMs = preloadWindowMs;
        // 防御：容器未启动时 preloadWindowMs 为 0，回退到配置值计算
        if (effectiveWindowMs <= 0) {
            effectiveWindowMs = (long) smartConfigure.getTaskFindInterval()
                    * smartConfigure.getScanPreloadMultiplier() * 1000L;
        }
        boolean inWindow = nextPlanTime.getTime()
                <= System.currentTimeMillis() + effectiveWindowMs;
        return inWindow;
    }

    /**
     * 执行前校验 DB 状态，防止无效执行
     *
     * @param task 待执行任务
     * @return true=可以执行，false=跳过该任务
     */
    static boolean validateTaskInDB(RetryTask task) {
        try {
            RetryTask dbTask = retryConfiguration.getRetryTaskAcess().getRetryTask(task.getId());
            if (dbTask == null) {
                return false;
            }
            Integer status = dbTask.getStatus();
            if (!RetryTaskStatus.WAITING.getCode().equals(status)
                    && !RetryTaskStatus.FAIL.getCode().equals(status)) {
                return false;
            }
            if (dbTask.getRetryNum() == null || dbTask.getRetryNum() <= 0) {
                return false;
            }
            List<Long> shardingIndexList = ShardingContextHolder.shardingIndex();
            if (shardingIndexList == null || !shardingIndexList.contains(dbTask.getShardingKey())) {
                return false;
            }
            return true;
        } catch (Exception e) {
            LOGGER.warn("[validateTaskInDB#validateTaskInDB] check failed for task:{}", task.getId(), e);
            return false;
        }
    }

    /**
     * 调度线程：从 DelayQueue 中 take() 到期任务，仅做内存级分片校验后分发。
     *
     * <p>不做 DB 查询，保持调度路径轻量。完整 DB 校验下沉到 ConsumerTask 工作线程中。
     */
    class SchedulerThread implements Runnable {
        @Override
        public void run() {
            while (SmartRetryExit.isExit()) {
                try {
                    ScheduledTask scheduled = delayQueue.take();  // 阻塞取第一个
                    List<ScheduledTask> batch = new ArrayList<>(101);
                    batch.add(scheduled);
                    delayQueue.drainTo(batch, 100);  // 非阻塞取更多到期任务

                    for (ScheduledTask scheduledTask : batch) {
                        RetryTask task = scheduledTask.getTask();

                        // 仅做内存级分片检查，避免 DB 查询阻塞调度线程
                        if (!checkShardingInMemory(task)) {
                            RetryTaskCache.unmark(getUniqueKey(task));
                            continue;
                        }

                        CompletableFuture.runAsync(
                                new ConsumerTask(task, retryConfiguration),
                                consumerExecutor
                        );
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    LOGGER.error("[SchedulerThread] error", e);
                }
            }
        }
    }

    /**
     * 内存级分片归属检查：当前实例是否负责该任务的分片。
     *
     * <p>仅使用 ShardingContextHolder 的内存数据，不查询 DB，
     * 保证调度路径零 DB 开销。
     *
     * @param task 待执行任务
     * @return true=当前实例负责该分片
     */
    private static boolean checkShardingInMemory(RetryTask task) {
        List<Long> shardingIndexList = ShardingContextHolder.shardingIndex();
        // shardingIndex() 永远返回非 null 的 List，为空表示分片未初始化
        if (shardingIndexList.isEmpty()) {
            // 分片未初始化，放行（由 ConsumerTask 的 validateTaskInDB 兜底）
            return true;
        }
        return shardingIndexList.contains(task.getShardingKey());
    }

    class ClearTask implements Runnable {

        @Override
        public void run() {
            try {
                int deleteCount = retryConfiguration.getRetryTaskAcess().deleteHistoryRetryTask(smartConfigure.getClearTask().getBeforeDays(), smartConfigure.getClearTask().getLimitRows());
                if (smartConfigure.shouldLogInfo()) {
                    LOGGER.info("[ClearTask#run] delete expired retry task count {},expire days {},limit rows {} ", deleteCount, smartConfigure.getClearTask().getBeforeDays(), smartConfigure.getClearTask().getLimitRows());
                }
            } catch (Exception e) {
                LOGGER.error("[ClearTask#run] error ", e);
            }
        }


    }

    /**
     * 死信处理任务
     * 死信任务：当任务已经变更状态为执行中，20分钟后，任务状态没有变更，则认为任务执行失败，进行死信处理
     * 处理逻辑是：将任务状态设置为失败，并记录失败原因，通知相关人员进行处理
     */
    class DeadLetterTask implements Runnable {
        @Override
        public void run() {

            while (true) {
                if (SmartRetryExit.isExit()) {
                    return;
                }
                try {
                    TimeUnit.SECONDS.sleep(15);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (!smartConfigure.getDeadTask().getDeadTaskCheck()) {
                    continue;
                }
                try {

                    List<RetryTask> allRetryTask = retryConfiguration.getRetryTaskAcess().listDeadTask(smartConfigure.getDeadTask().getTaskMaxExecuteTimeout());
                    if (CollectionUtils.isEmpty(allRetryTask)) {
                        continue;
                    }
                    //将任务重新设置为待执行状态,
                    // TODO 考虑超时的任务是否需要在内存中做线程的中断
                    // 统一超时判定时间点，避免循环处理期间时间漂移
                    Date deadTaskTime = new Date(System.currentTimeMillis()
                            - smartConfigure.getDeadTask().getTaskMaxExecuteTimeout() * 1000L);
                    for (RetryTask retryTask : allRetryTask) {

                        // 条件化复活（乐观锁 CAS 守卫）：仅当任务仍为 RUNNING(1) 且
                        // gmt_modified < deadTaskTime（确认超时）时才被复活，
                        // 防止复活已终态任务，或覆盖认领方在超时窗口内刚写入的终态。
                        int revived = retryConfiguration.getRetryTaskAcess()
                                .reviveDeadRetryTask(retryTask.getId(), deadTaskTime);
                        if (revived != 1) {
                            // 已被处理（认领方已写终态 / 已被其他实例复活），跳过
                            if (smartConfigure.shouldLogInfo()) {
                                LOGGER.info("[DeadLetterTask] revive skipped, task:{}", retryTask.getId());
                            }
                        }

                    }

                } catch (Exception e) {
                    LOGGER.error("[SimpleContainer#DeadLetterTask]run error,error msg {} ", e.getMessage(), e);

                }
            }

        }
    }

    /**
     * 兜底扫描线程：低频扫描 DB，将遗漏任务加入 DelayQueue
     * 不再直接提交任务到 executor
     */
    class ProducerTask implements Runnable {
        private long sleepBaseTimeMilliseconds;

        ProducerTask() {
            this.sleepBaseTimeMilliseconds = smartConfigure.getTaskFindInterval() * 1000L;
        }

        @Override
        public void run() {

            LOGGER.info("[ProducerTask#run] start run producer task,sleepBaseTimeMilliseconds {}", sleepBaseTimeMilliseconds);
            while (SmartRetryExit.isExit()) {
                if (!SmartRetryRunFlag.getFlag()) {
                    sleepOneInterval();
                    continue;
                }

                try {
                    int currentSize = RetryTaskCache.size();
                    int availableSlots = smartConfigure.getMaxInMemory() - currentSize;
                    if (availableSlots <= 0) {
                        LOGGER.warn("[ProducerTask#run] 内存任务数达到上限 {}, 跳过本轮扫描,当前任务{}",
                                smartConfigure.getMaxInMemory(), currentSize);
                        sleepOneInterval();
                        continue;
                    }

                    // 防御：容器未完全初始化时 preloadWindowMs 可能为 0，使用配置值兜底
                    long effectiveWindowMs = preloadWindowMs;
                    if (effectiveWindowMs <= 0) {
                        effectiveWindowMs = (long) smartConfigure.getTaskFindInterval()
                                * smartConfigure.getScanPreloadMultiplier() * 1000L;
                    }
                    Date maxNextPlanTime = new Date(
                            System.currentTimeMillis() + effectiveWindowMs);
                    List<RetryTask> allRetryTask = retryConfiguration
                            .getRetryTaskAcess()
                            .listRetryTask(maxNextPlanTime, Math.min(availableSlots, 500));

                    if (CollectionUtils.isEmpty(allRetryTask)) {
                        sleepOneInterval();
                        continue;
                    }

                    int enqueued = 0;
                    for (RetryTask retryTask : allRetryTask) {
                        if (enqueue(retryTask)) {
                            enqueued++;
                        }
                    }

                    if (smartConfigure.shouldLogInfo() && enqueued > 0) {
                        LOGGER.info("[ProducerTask#run] 兜底扫描加载 {} 个任务到 DelayQueue, 内存中任务数: {}",
                                enqueued, RetryTaskCache.size());
                    }
                    sleepOneInterval();
                } catch (Exception e) {
                    LOGGER.error("[ProducerTask#run] producer task exception errMsg,{}", e.getMessage(), e);
                }
            }
        }

        private void sleepOneInterval() {
            try {
                TimeUnit.MILLISECONDS.sleep(sleepBaseTimeMilliseconds);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    // ========== 监控方法（测试 & 运维） ==========


    // ========== 原有方法 ==========

    private static void doProduceTask(RetryTask retryTask, RetryConfiguration retryConfiguration) {
        //任务存在则不处理，避免重复处理
        if (checkTaskExists(retryTask)) {
            if (smartConfigure.shouldLogInfo()) {
                LOGGER.info("[SimpleContainer#doProduceTask]task exists,taskId:{}", retryTask.getId());
            }
            return;
        }

        CompletableFuture<Void> future = CompletableFuture.runAsync(new ConsumerTask(retryTask, retryConfiguration), consumerExecutor);
    }

    private static void initTaskConsumerExecutor(SmartExecutorConfigure smartConfigure) {
        if (consumerExecutor != null) {
            return;
        }
        initTaskExecutor(smartConfigure);
    }

    static void invokeTaskAsync(RetryTask retryTask,
                                RetryConfiguration retryConfiguration,
                                SmartExecutorConfigure smartConfigure) {
        initTaskConsumerExecutor(smartConfigure);
        // 释放 createTask 中 enqueueIfInWindow 预标记的去重 key
        releaseAutoEnqueueMark(retryTask);
        doProduceTask(retryTask, retryConfiguration);
    }

    /**
     * 同步执行一次任务（不循环重试）。
     * 在当前线程直接执行，仅执行一次。失败后的继续重试由 Producer/调度器异步推进。
     *
     * @param retryTask          待执行的任务
     * @param retryConfiguration 重试配置
     * @return 本次执行结果；null=未执行（被去重拦截 / getTriggerableTask 拒绝）
     */
    static TaskExecutionResult invokeTaskOnceSync(RetryTask retryTask,
                                                  RetryConfiguration retryConfiguration) {
        // 释放 createTask 中 enqueueIfInWindow 预标记的去重 key，
        // 确保手动触发能获取执行权（避免被 auto-enqueue 的 tryMark 拦截）
        releaseAutoEnqueueMark(retryTask);

        if (checkTaskExists(retryTask)) {
            if (smartConfigure.shouldLogInfo()) {
                LOGGER.info("[SimpleContainer#invokeTaskOnceSync] task already executing, taskId:{}",
                        retryTask.getId());
            }
            return null;
        }
        // 仅执行一次，后续重试由 afterExecute 放入 delayQueue 异步推进
        ConsumerTask task = new ConsumerTask(retryTask, retryConfiguration);
        task.run();
        return task.getResult();
    }

    /**
     * 释放 createTask 中 enqueueIfInWindow 预标记的去重 key。
     * <p>createTask 通过 enqueueIfInWindow → enqueue → tryMark 预先标记任务，
     * 导致紧跟其后的手动触发（invokeTaskOnceSync/Async）被去重拦截。
     * 手动触发前先释放该标记，让 checkTaskExists 能够重新获取执行权。
     *
     * <p>注意：不删除 delayQueue 中的 ScheduledTask（太昂贵），
     * SchedulerThread 取出后会因 tryMark 失败或 validateTaskInDB 失败而跳过。
     */
    private static void releaseAutoEnqueueMark(RetryTask retryTask) {
        String key = getUniqueKey(retryTask);
        RetryTaskCache.unmark(key);
    }

    private static boolean checkTaskExists(RetryTask retryTask) {
        String uniqueKey = getUniqueKey(retryTask);
        // tryMarkInMemory 返回 true = 标记成功（任务不存在），false = 已存在
        return !RetryTaskCache.tryMark(uniqueKey);
    }

    /**
     * 单次执行任务消费者。
     *
     * <p>只执行一轮：DB 校验 → 反射调用 → 回调 {@link SimpleContainer#afterExecute}。
     * 执行失败且未到终态时，由 {@code afterExecute} 将任务重新放入 DelayQueue，
     * 后续重试交给 SchedulerThread/Producer 异步调度，不再在当前线程循环。
     *
     * <p>同步/异步的区别仅在调用方线程（当前线程 {@code run()} vs 提交线程池），
     * 本类不感知调用方，始终保持"单次执行"语义。
     *
     * @Author xiaoqiang
     * @Version ConsumerTask.java, v 0.1 2025年08月27日 xiaoqiang
     * @Description: TODO
     */
    public static class ConsumerTask implements Runnable {

        private static final Logger LOGGER = LoggerFactory.getLogger(ConsumerTask.class);

        private final RetryTask retryTask;
        private final RetryConfiguration retryConfiguration;

        /** run() 执行完毕后的结果；未执行（被校验/去重拒绝）时为 null */
        private volatile TaskExecutionResult result;

        public ConsumerTask(RetryTask retryTask, RetryConfiguration retryConfiguration) {
            this.retryTask = retryTask;
            this.retryConfiguration = retryConfiguration;
        }

        @Override
        public void run() {
            // 1. DB 校验：只放行 WAITING/FAIL + retryNum>0 + 分片归属
            if (!validateTaskInDB(retryTask)) {
                RetryTaskCache.unmark(getUniqueKey(retryTask));
                this.result = null;
                return;
            }
            // 2. 反射调用：捕获业务返回值；异常时业务结果置 null
            Object businessResult = null;
            try {
                businessResult = new DefaultInnovation(retryTask, retryConfiguration).invoke();
            } catch (RetryTaskClaimedException e) {
                // 认领竞争失败：任务已由其他执行方接管（同 JVM 残留 delayQueue/手动触发，
                // 或跨实例的 Producer），本实例不执行业务。
                // 不调用 afterExecute，避免给胜者再制造一次冗余调度。
                LOGGER.info("[ConsumerTask#run] retry task already claimed by other executor, skip, taskId:{}",
                        retryTask.getId());
                RetryTaskCache.unmark(getUniqueKey(retryTask));
                this.result = null;
                return;
            } catch (Throwable e) {
                LOGGER.error("[ConsumerTask#run] consumer error, taskId:{}", retryTask.getId(), e);
            }
            // 3. 终态→unmark；非终态→入队 delayQueue（保留占位锁）由调度器异步重试
            afterExecute(retryTask);
            // 4. 记录本次结果（状态以 DefaultInnovation 写入内存 task 的 status 为准）
            this.result = new TaskExecutionResult(resolveStatus(retryTask), businessResult);
        }

        /**
         * 获取本次执行结果。仅在 run() 执行完毕后有效；未执行时返回 null。
         *
         * @return 本次执行结果（status + businessResult），未执行时为 null
         */
        public TaskExecutionResult getResult() {
            return result;
        }

        private static ExecuteResultStatus resolveStatus(RetryTask task) {
            return RetryTaskStatus.SUCCESS.getCode().equals(task.getStatus())
                    ? ExecuteResultStatus.SUCCESS
                    : ExecuteResultStatus.FAIL;
        }
    }
}
