package com.smart.retry.core;

import com.smart.retry.common.RetryConfiguration;
import com.smart.retry.common.RetryContainer;
import com.smart.retry.common.SmartRetryExit;
import com.smart.retry.common.SmartRetryRunFlag;
import com.smart.retry.common.constant.RetryTaskStatus;
import com.smart.retry.common.innovation.SmartInnovation;
import com.smart.retry.common.model.RetryTask;
import com.smart.retry.common.utils.GsonTool;
import com.smart.retry.core.config.SmartExecutorConfigure;
import com.smart.retry.core.innovation.DefaultInnovation;
import org.slf4j.Logger;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.util.CollectionUtils;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

/**
 * @Author xiaoqiang
 * @Version SimpleContainer.java, v 0.1 2025年02月18日 00:24 xiaoqiang
 * @Description: TODO
 */
public class SimpleContainer implements RetryContainer {

    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(SimpleContainer.class);

    // ========== DelayQueue 精准调度相关字段 ==========

    /** 内存精准调度队列 */
    private static final DelayQueue<ScheduledTask> delayQueue = new DelayQueue<>();

    /** 内存中去重集合，key = taskCode + "-" + uniqueKey */
    private static final Set<String> inMemoryTaskKeys = ConcurrentHashMap.newKeySet();

    /** 调度线程 */
    private static Thread schedulerThread;

    /** 预加载窗口毫秒数 */
    private static long preloadWindowMs;

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
        preloadWindowMs = (long) smartConfigure.getTaskFindInterval()
            * smartConfigure.getScanPreloadMultiplier() * 1000L;

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
        int queueSize = smartConfigure.getExecutor().getQueueCapacity();
        String name = smartConfigure.getExecutor().getName();
        consumerQueue = new ArrayBlockingQueue<>(queueSize);


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

    private static String getUniqueKey(RetryTask retryTask) {
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
     * 将任务加入 DelayQueue，自动去重
     *
     * @param task 重试任务
     * @return true=入队成功，false=已在内存中
     */
    static boolean enqueue(RetryTask task) {
        String key = getUniqueKey(task);
        if (!inMemoryTaskKeys.add(key)) {
            return false;
        }
        delayQueue.put(new ScheduledTask(task));
        return true;
    }

    /**
     * 任务执行完毕后的回调
     * 注意：此时 DB 已更新 (status/retryNum/nextPlanTime)
     *
     * @param task 已执行完毕的任务
     */
    static void afterExecute(RetryTask task) {
        String key = getUniqueKey(task);
        inMemoryTaskKeys.remove(key);

        Integer status = task.getStatus();
        Integer retryNum = task.getRetryNum();

        // 成功或重试次数耗尽，结束
        if (RetryTaskStatus.SUCCESS.getCode().equals(status) || retryNum == null || retryNum <= 0) {
            return;
        }

        // 失败且还有重试次数
        Date nextPlanTime = task.getNextPlanTime();
        if (nextPlanTime == null) {
            return;
        }
        boolean inWindow = nextPlanTime.getTime()
                           <= System.currentTimeMillis() + preloadWindowMs;

        if (inWindow) {
            enqueue(task);
        }
        // else: 窗口外，由 Producer 后续扫描加载
    }

    /**
     * 执行前校验 DB 状态，防止无效执行
     *
     * @param task 待执行任务
     * @return true=可以执行，false=跳过该任务
     */
    private static boolean validateTaskInDB(RetryTask task) {
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
            LOGGER.warn("[validateTaskInDB] check failed for task:{}", task.getId(), e);
            return true;
        }
    }

    /**
     * 调度线程：从 DelayQueue 中 take() 到期任务，校验后提交执行
     */
    class SchedulerThread implements Runnable {
        @Override
        public void run() {
            while (SmartRetryExit.isExit()) {
                try {
                    ScheduledTask scheduled = delayQueue.take();
                    RetryTask task = scheduled.getTask();

                    if (!validateTaskInDB(task)) {
                        inMemoryTaskKeys.remove(getUniqueKey(task));
                        continue;
                    }

                    CompletableFuture.runAsync(
                        new ConsumerTask(task, retryConfiguration),
                        consumerExecutor
                    );
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    LOGGER.error("[SchedulerThread] error", e);
                }
            }
        }
    }

    static class ConsumerTask implements Runnable {

        private RetryTask retryTask;
        private RetryConfiguration retryConfiguration;

        public ConsumerTask(RetryTask retryTask, RetryConfiguration retryConfiguration) {
            this.retryTask = retryTask;
            this.retryConfiguration = retryConfiguration;
        }

        @Override
        public void run() {
            String uniqueKey = getUniqueKey(retryTask);

            try {

                SmartInnovation innovation = new DefaultInnovation(retryTask, retryConfiguration);
                innovation.invoke();
            } catch (Throwable e) {
                LOGGER.error("[ConsumerTask-run error,retryTask:{} ", GsonTool.toJsonString(retryTask), e);
            } finally {
                RetryTaskCache.removeTaskFlag(uniqueKey);
                afterExecute(retryTask);
            }

        }
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
                    for (RetryTask retryTask : allRetryTask) {

                        RetryTask retryTaskDo = new RetryTask();
                        retryTaskDo.setId(retryTask.getId());
                        retryTaskDo.setStatus(RetryTaskStatus.WAITING.getCode());
                        retryTaskDo.setRetryNum(retryTask.getRetryNum() + 1);
                        retryConfiguration.getRetryTaskAcess().updateRetryTask(retryTaskDo);

                    }
                    //List<RetryTask> deadLetterTasks = retryTaskRepo.listDeadTask(smartConfigure.getDeadTask().getMaxExecuteTime());
                    //List<RetryTask> deadLetterTasks = retryTaskRepo.findDeadLetterTasks();
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
            while (SmartRetryExit.isExit()) {

                if (!SmartRetryRunFlag.getFlag()) {
                    sleepOneInterval();
                    continue;
                }

                try {
                    int currentSize = inMemoryTaskKeys.size();
                    int availableSlots = smartConfigure.getMaxInMemory() - currentSize;
                    if (availableSlots <= 0) {
                        LOGGER.warn("[ProducerTask] 内存任务数达到上限 {}, 跳过本轮扫描",
                            smartConfigure.getMaxInMemory());
                        sleepOneInterval();
                        continue;
                    }

                    Date maxNextPlanTime = new Date(
                        System.currentTimeMillis() + preloadWindowMs);
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
                        LOGGER.info("[ProducerTask] 兜底扫描加载 {} 个任务到 DelayQueue, 内存中任务数: {}",
                            enqueued, inMemoryTaskKeys.size());
                    }

                    sleepOneInterval();
                } catch (Exception e) {
                    LOGGER.error("[ProducerTask] error", e);
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

    private static void doProduceTask(RetryTask retryTask, RetryConfiguration retryConfiguration) {
        //任务存在则不处理，避免重复处理
        if (checkTaskExists(retryTask)) {
            if(smartConfigure.shouldLogInfo()){
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
        doProduceTask(retryTask, retryConfiguration);
    }

    static void invokeTaskSync(RetryTask retryTask,
                           RetryConfiguration retryConfiguration) {
        //任务存在则不处理，避免重复处理
        if (checkTaskExists(retryTask)){
            if(smartConfigure.shouldLogInfo()){
                LOGGER.info("[SimpleContainer#invokeTaskSync]task exists,taskId:{}", retryTask.getId());
            }
            return;
        }
        new ConsumerTask(retryTask, retryConfiguration).run();
    }

    private static boolean checkTaskExists(RetryTask retryTask) {
        String uniqueKey = getUniqueKey(retryTask);
        Boolean exists = RetryTaskCache.retryTasks.putIfAbsent(uniqueKey, true);
        //插入成功，则任务不存在
        if(exists == null){
            return false;
        }
        //插入失败，则任务已经存在
        return true;
    }

}
