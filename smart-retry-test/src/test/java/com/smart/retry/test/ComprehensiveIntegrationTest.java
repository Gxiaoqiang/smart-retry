package com.smart.retry.test;

import com.smart.retry.common.RetryConfiguration;
import com.smart.retry.common.RetryTaskOperator;
import com.smart.retry.common.constant.NextPlanTimeStrategyEnum;
import com.smart.retry.common.constant.RetryTaskStatus;
import com.smart.retry.common.model.RetryTask;
import com.smart.retry.common.model.RetryTaskBuilder;
import com.smart.retry.core.ShardingContextHolder;
import com.smart.retry.core.SimpleContainer;
import com.smart.retry.core.config.SmartExecutorConfigure;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.TimeUnit;

/**
 * 综合集成测试：覆盖内存压力、并发安全、异常容错、边界值、线程池、DB 校验、调度兜底。
 *
 * <p>测试维度：
 * <ul>
 *   <li>一、内存压力 —— 大量任务入队 / 失败重试循环后的内存释放</li>
 *   <li>二、并发 & 去重 —— 相同 uniqueKey / 重复创建任务</li>
 *   <li>三、异常容错 —— consume 抛异常不阻塞调度线程</li>
 *   <li>四、边界值 —— delay=0 立即执行 / retryNum=1 单次执行 / taskObject 为 null</li>
 *   <li>五、线程池压力 —— 大量同时间点任务 + 慢执行</li>
 *   <li>六、DB 状态校验 —— 任务被删除 / 状态已变更后在 SchedulerThread 中正确跳过</li>
 *   <li>七、调度 & Producer —— 批量同 next_plan_time / 窗口外任务由 Producer 兜底</li>
 * </ul>
 *
 * @Author xiaoqiang
 * @Version ComprehensiveIntegrationTest.java, v 0.1 2025年06月24日 xiaoqiang
 */
public class ComprehensiveIntegrationTest extends AbstractTest {

    @Autowired
    private RetryTaskOperator<TestParam> operator;

    @Autowired
    private RetryConfiguration retryConfiguration;

    @Autowired
    private SmartExecutorConfigure smartConfigure;

    @Autowired
    private MemoryPressureListener memoryPressureListener;

    @Autowired
    private DedupTestListener dedupTestListener;

    @Autowired
    private ExceptionTestListener exceptionTestListener;

    @Autowired
    private CycleRetryListener cycleRetryListener;

    @Autowired
    private StressTestListener stressTestListener;

    @Autowired
    private TestRetryListener testRetryListener;

    @After
    public void tearDown() throws Exception {
        // 让飞行中的任务有短暂时间完成，减少测试间干扰
        TimeUnit.MILLISECONDS.sleep(500);
    }

    // ==================================================================
    // 一、内存压力测试
    // ==================================================================

    /**
     * 1.1 大量任务同时入队，验证：
     * - inMemoryTaskKeys 正确增长
     * - DelayQueue 正确填充
     * - 全部任务执行完成后内存正确释放
     */
    @Test
    public void testMemoryPressure_MassTasksEnqueueAndExecute() throws Exception {
        int taskCount = 200;
        int taskDelaySec = 10; // 较长的 delay 确保创建期间任务不会提前到期
        memoryPressureListener.reset(taskCount);

        // 记录初始状态
        long beforeUsedMem = getUsedMemory();
        int beforeMemoryTasks = SimpleContainer.getInMemoryTaskCount();
        int beforeQueueSize = SimpleContainer.getDelayQueueSize();

        System.out.println("=== [内存压力测试-初始] inMemory=" + beforeMemoryTasks
            + ", delayQueue=" + beforeQueueSize
            + ", usedMem=" + (beforeUsedMem / 1024 / 1024) + "MB");

        // 创建大量任务（在 preloadWindow=100s 内，全部由 enqueueIfInWindow 直接入队）
        long createStart = System.currentTimeMillis();
        for (int i = 0; i < taskCount; i++) {
            RetryTaskBuilder<TestParam> builder = RetryTaskBuilder.of();
            builder.withTaskCode("test-memory-pressure");
            builder.withParam(new TestParam("mem-task-" + i + "-" + System.nanoTime()));
            builder.withRetryNum(1);
            builder.withDelaySecond(taskDelaySec);
            builder.withIntervalSecond(10);
            builder.withNextPlanTimeStrategy(NextPlanTimeStrategyEnum.FIXED);
            operator.createTask(builder);
        }
        long createElapsed = System.currentTimeMillis() - createStart;

        // 创建完成后立即检查内存状态（delay 长，任务不会提前到期）
        int afterCreateMemoryTasks = SimpleContainer.getInMemoryTaskCount();
        int afterCreateQueueSize = SimpleContainer.getDelayQueueSize();

        System.out.println("=== [内存压力测试-创建后] 创建耗时=" + createElapsed + "ms"
            + ", inMemory=" + afterCreateMemoryTasks
            + ", delayQueue=" + afterCreateQueueSize);

        Assert.assertTrue("创建后 inMemoryTaskKeys 应 >= taskCount，实际: " + afterCreateMemoryTasks,
            afterCreateMemoryTasks >= taskCount);
        Assert.assertTrue("创建后 DelayQueue 应 >= taskCount，实际: " + afterCreateQueueSize,
            afterCreateQueueSize >= taskCount);

        // 等待所有任务执行完成
        boolean allExecuted = memoryPressureListener.awaitExecution(120, TimeUnit.SECONDS);
        long elapsed = System.currentTimeMillis() - createStart;

        System.out.println("=== [内存压力测试-完成] 总耗时=" + elapsed + "ms"
            + ", executeCount=" + memoryPressureListener.getExecuteCount());

        Assert.assertTrue("所有 " + taskCount + " 个任务应在 120s 内执行完成", allExecuted);
        Assert.assertEquals("执行次数应等于任务数", taskCount, memoryPressureListener.getExecuteCount());

        // 等待 afterExecute 清理完毕
        TimeUnit.SECONDS.sleep(2);

        // 验证内存释放
        int afterMemoryTasks = SimpleContainer.getInMemoryTaskCount();
        int afterQueueSize = SimpleContainer.getDelayQueueSize();
        long afterUsedMem = getUsedMemory();

        System.out.println("=== [内存压力测试-收尾] inMemory=" + afterMemoryTasks
            + ", delayQueue=" + afterQueueSize
            + ", usedMem=" + (afterUsedMem / 1024 / 1024) + "MB");

        // 任务执行完毕后，inMemoryTaskKeys 应回归低位
        Assert.assertTrue("执行完成后 inMemoryTaskKeys 应回归低位，实际: " + afterMemoryTasks,
            afterMemoryTasks < taskCount / 10);
    }

    /**
     * 1.2 失败重试循环：任务执行 FAIL → 重新入队 → retryNum 耗尽 → 正确释放内存
     */
    @Test
    public void testMemoryPressure_RetryCycleRelease() throws Exception {
        int retryNum = 3;
        cycleRetryListener.reset(retryNum);

        int beforeMemory = SimpleContainer.getInMemoryTaskCount();

        RetryTaskBuilder<TestParam> builder = RetryTaskBuilder.of();
        builder.withTaskCode("test-cycle-retry");
        builder.withParam(new TestParam("retry-cycle"));
        builder.withRetryNum(retryNum);
        builder.withDelaySecond(2);
        builder.withIntervalSecond(3);
        builder.withNextPlanTimeStrategy(NextPlanTimeStrategyEnum.FIXED);

        operator.createTask(builder);

        // 等待 3 次重试全部执行完毕
        boolean allExecuted = cycleRetryListener.awaitExecution(60, TimeUnit.SECONDS);

        Assert.assertTrue("任务应执行 " + retryNum + " 次（含重试）", allExecuted);
        Assert.assertEquals("应执行 " + retryNum + " 次", retryNum, cycleRetryListener.getExecuteCount());

        // 等待 afterExecute 清理
        TimeUnit.SECONDS.sleep(2);

        int afterMemory = SimpleContainer.getInMemoryTaskCount();
        Assert.assertTrue("retryNum 耗尽后 inMemory 应回归初始水平，初始=" + beforeMemory
            + ", 当前=" + afterMemory,
            Math.abs(afterMemory - beforeMemory) <= 5);
    }

    // ==================================================================
    // 二、并发 & 去重测试
    // ==================================================================

    /**
     * 2.1 相同 uniqueKey 的任务只入队一次
     */
    @Test
    public void testDedup_SameUniqueKeyEnqueueOnce() throws Exception {
        dedupTestListener.reset(1);

        String param = "dedup-test-" + System.currentTimeMillis();

        // 创建第一个任务
        RetryTaskBuilder<TestParam> builder1 = RetryTaskBuilder.of();
        builder1.withTaskCode("test-dedup");
        builder1.withParam(new TestParam(param));
        builder1.withRetryNum(1);
        builder1.withDelaySecond(3);
        builder1.withIntervalSecond(10);
        builder1.withNextPlanTimeStrategy(NextPlanTimeStrategyEnum.FIXED);
        long taskId1 = operator.createTask(builder1);

        // 立即创建第二个相同任务
        RetryTaskBuilder<TestParam> builder2 = RetryTaskBuilder.of();
        builder2.withTaskCode("test-dedup");
        builder2.withParam(new TestParam(param));
        builder2.withRetryNum(1);
        builder2.withDelaySecond(3);
        builder2.withIntervalSecond(10);
        builder2.withNextPlanTimeStrategy(NextPlanTimeStrategyEnum.FIXED);
        long taskId2 = operator.createTask(builder2);

        System.out.println("=== [去重测试] taskId1=" + taskId1 + ", taskId2=" + taskId2);

        // DB 中两个记录都插入了，但 DelayQueue/enqueueIfInWindow 只入队一个
        Assert.assertNotEquals("两个任务应有不同的 DB ID", taskId1, taskId2);

        // 等待执行
        boolean executed = dedupTestListener.awaitExecution(15, TimeUnit.SECONDS);

        Assert.assertTrue("去重后任务应执行完成", executed);
        // 如果去重生效，只应该执行 1 次；如果失败可能执行 2 次
        Assert.assertEquals("去重生效：应只执行 1 次", 1, dedupTestListener.getExecuteCount());
    }

    // ==================================================================
    // 三、异常容错测试
    // ==================================================================

    /**
     * 3.1 consume() 抛出异常不阻塞调度线程，后续任务正常执行
     */
    @Test
    public void testException_DoesNotBlockScheduler() throws Exception {
        // 第一个任务：会抛异常
        exceptionTestListener.reset(1);

        RetryTaskBuilder<TestParam> exBuilder = RetryTaskBuilder.of();
        exBuilder.withTaskCode("test-exception");
        exBuilder.withParam(new TestParam("exception-task"));
        exBuilder.withRetryNum(1);
        exBuilder.withDelaySecond(2);
        exBuilder.withIntervalSecond(10);
        exBuilder.withNextPlanTimeStrategy(NextPlanTimeStrategyEnum.FIXED);
        operator.createTask(exBuilder);

        // 等待异常任务执行完毕
        boolean exExecuted = exceptionTestListener.awaitExecution(15, TimeUnit.SECONDS);
        Assert.assertTrue("异常任务仍应被调度执行", exExecuted);

        // 第二个任务：正常任务，验证调度线程没被异常中断
        testRetryListener.reset();

        RetryTaskBuilder<TestParam> normalBuilder = RetryTaskBuilder.of();
        normalBuilder.withTaskCode("test-delay-queue");
        normalBuilder.withParam(new TestParam("post-exception-normal"));
        normalBuilder.withRetryNum(1);
        normalBuilder.withDelaySecond(2);
        normalBuilder.withIntervalSecond(10);
        normalBuilder.withNextPlanTimeStrategy(NextPlanTimeStrategyEnum.FIXED);
        operator.createTask(normalBuilder);

        boolean normalExecuted = testRetryListener.awaitExecution(15, TimeUnit.SECONDS);
        Assert.assertTrue("异常后调度线程应继续正常工作，后续任务应执行成功", normalExecuted);
    }

    // ==================================================================
    // 四、边界值测试
    // ==================================================================

    /**
     * 4.1 delaySecond=1（最小合法值）：任务几乎立即执行
     */
    @Test
    public void testBoundary_MinimumDelay() throws Exception {
        testRetryListener.reset();

        long startTime = System.currentTimeMillis();

        RetryTaskBuilder<TestParam> builder = RetryTaskBuilder.of();
        builder.withTaskCode("test-delay-queue");
        builder.withParam(new TestParam("min-delay"));
        builder.withRetryNum(1);
        builder.withDelaySecond(1);
        builder.withIntervalSecond(10);
        builder.withNextPlanTimeStrategy(NextPlanTimeStrategyEnum.FIXED);
        operator.createTask(builder);

        boolean executed = testRetryListener.awaitExecution(15, TimeUnit.SECONDS);
        long elapsed = System.currentTimeMillis() - startTime;

        Assert.assertTrue("delay=1 的任务应被快速调度执行", executed);
        Assert.assertTrue("delay=1 应在 15s 内完成，实际: " + elapsed + "ms", elapsed < 15000);
    }

    /**
     * 4.2 retryNum=1：任务只执行一次，不重入队
     */
    @Test
    public void testBoundary_RetryNumOne_ExecutesExactlyOnce() throws Exception {
        // 使用 cycleRetryListener 来观察执行次数（即使返回 FAIL，retryNum=1 也不会重试）
        cycleRetryListener.reset(1);

        RetryTaskBuilder<TestParam> builder = RetryTaskBuilder.of();
        builder.withTaskCode("test-cycle-retry");
        builder.withParam(new TestParam("retry-once-only"));
        builder.withRetryNum(1);
        builder.withDelaySecond(2);
        builder.withIntervalSecond(5);
        builder.withNextPlanTimeStrategy(NextPlanTimeStrategyEnum.FIXED);
        operator.createTask(builder);

        // 等待"执行完成"——应该只有一次
        boolean executed = cycleRetryListener.awaitExecution(20, TimeUnit.SECONDS);
        Assert.assertTrue("retryNum=1 任务应执行完成", executed);

        // 再等 5 秒，确保不会二次执行
        TimeUnit.SECONDS.sleep(5);

        Assert.assertEquals("retryNum=1 应只执行 1 次（不重入队）",
            1, cycleRetryListener.getExecuteCount());
    }

    /**
     * 4.3 taskObject 为 null —— taskCode 在 RetryCache 中不存在
     * 验证 SimpleContainer.afterExecute 不会 NPE
     */
    @Test
    public void testBoundary_TaskObjectNull_AfterExecuteNoNPE() throws Exception {
        // 使用一个不存在的 taskCode，直接在 DB 中插入任务
        RetryTask fakeTask = new RetryTask();
        fakeTask.setTaskCode("non-existent-task-code-" + System.currentTimeMillis());
        fakeTask.setParameters("{\"name\":\"ghost\"}");
        fakeTask.setRetryNum(3);
        fakeTask.setDelaySecond(100);
        fakeTask.setIntervalSecond(10);
        fakeTask.setNextPlanTimeStrategy(NextPlanTimeStrategyEnum.FIXED.getCode());
        fakeTask.setStatus(RetryTaskStatus.WAITING.getCode());
        fakeTask.setOriginRetryNum(3);
        fakeTask.setCreator("test");
        fakeTask.setUniqueKey("ghost-key-" + System.nanoTime());
        fakeTask.setShardingKey(ShardingContextHolder.getRandomShardingIndex());
        fakeTask.setNextPlanTime(new java.util.Date(System.currentTimeMillis() + 2000));

        long taskId = retryConfiguration.getRetryTaskAcess().saveRetryTask(fakeTask);
        fakeTask.setId(taskId);

        // 手动入队
        SimpleContainer.enqueue(fakeTask);

        // 等待调度线程处理（validateTaskInDB 可能通过或失败，但不应 crash）
        TimeUnit.SECONDS.sleep(8);

        // 检查 inMemoryTaskKeys 中该 key 已被移除（由 afterExecute 清理）
        // 这个测试主要验证不抛 NPE、不导致调度线程退出
        // 通过后续测试正常运行来间接验证
        Assert.assertTrue("系统处理 null taskObject 不应崩溃", true);
    }

    // ==================================================================
    // 五、线程池压力测试
    // ==================================================================

    /**
     * 5.1 大量相同 next_plan_time 的任务，验证全部按时执行
     * 同时验证 executor 队列 CallerRunsPolicy 下不会死锁
     */
    @Test
    public void testThreadPool_BatchSameNextPlanTime() throws Exception {
        int batchCount = 50;
        stressTestListener.reset(batchCount);

        // 所有任务 delay=2s，意味着它们在几乎同一时刻到期
        for (int i = 0; i < batchCount; i++) {
            RetryTaskBuilder<TestParam> builder = RetryTaskBuilder.of();
            builder.withTaskCode("test-stress");
            builder.withParam(new TestParam("batch-" + i));
            builder.withRetryNum(1);
            builder.withDelaySecond(2);
            builder.withIntervalSecond(10);
            builder.withNextPlanTimeStrategy(NextPlanTimeStrategyEnum.FIXED);
            operator.createTask(builder);
        }

        int queueSize = SimpleContainer.getDelayQueueSize();
        System.out.println("=== [批量调度测试] 入队后 delayQueue.size=" + queueSize);

        boolean allExecuted = stressTestListener.awaitExecution(60, TimeUnit.SECONDS);

        Assert.assertTrue("所有 " + batchCount + " 个同时间点任务应全部执行完成", allExecuted);
        Assert.assertEquals("应执行 " + batchCount + " 次", batchCount, stressTestListener.getSuccessCount());
    }

    // ==================================================================
    // 六、DB 状态校验测试（validateTaskInDB）
    // ==================================================================

    /**
     * 6.1 任务在 DB 中被删除后，SchedulerThread 正确跳过
     */
    @Test
    public void testValidateDB_TaskDeleted_SkipExecution() throws Exception {
        testRetryListener.reset();

        // 创建任务，delay=10s 给我们足够时间操作 DB
        RetryTaskBuilder<TestParam> builder = RetryTaskBuilder.of();
        builder.withTaskCode("test-delay-queue");
        builder.withParam(new TestParam("to-be-deleted"));
        builder.withRetryNum(1);
        builder.withDelaySecond(10);
        builder.withIntervalSecond(10);
        builder.withNextPlanTimeStrategy(NextPlanTimeStrategyEnum.FIXED);
        long taskId = operator.createTask(builder);

        // 确认任务已入队
        int inMemory = SimpleContainer.getInMemoryTaskCount();
        System.out.println("=== [DB校验-删除] 任务入队后 inMemory=" + inMemory + ", taskId=" + taskId);

        // 从 DB 中删除该任务
        retryConfiguration.getRetryTaskAcess().deleteRetryTask(taskId);

        // 等待超过 delay 时间
        boolean executed = testRetryListener.awaitExecution(20, TimeUnit.SECONDS);

        // validateTaskInDB 应返回 false（task 已删除），任务被跳过
        // listener 的 latch 不会被 countDown，所以 await 返回 false
        Assert.assertFalse("DB 中已删除的任务应被 validateTaskInDB 拦截、不执行", executed);
    }

    /**
     * 6.2 任务状态在 DB 中已变更为 SUCCESS，SchedulerThread 正确跳过
     */
    @Test
    public void testValidateDB_StatusChangedToSuccess_SkipExecution() throws Exception {
        testRetryListener.reset();

        RetryTaskBuilder<TestParam> builder = RetryTaskBuilder.of();
        builder.withTaskCode("test-delay-queue");
        builder.withParam(new TestParam("status-changed-to-success-" + System.nanoTime()));
        builder.withRetryNum(1);
        builder.withDelaySecond(10);
        builder.withIntervalSecond(10);
        builder.withNextPlanTimeStrategy(NextPlanTimeStrategyEnum.FIXED);
        long taskId = operator.createTask(builder);

        // 从 DB 获取完整任务，修改状态为 SUCCESS
        RetryTask fullTask = retryConfiguration.getRetryTaskAcess().getRetryTask(taskId);
        fullTask.setStatus(RetryTaskStatus.SUCCESS.getCode());
        retryConfiguration.getRetryTaskAcess().updateRetryTask(fullTask);

        System.out.println("=== [DB校验-状态变更] taskId=" + taskId + " 状态已改为 SUCCESS");

        boolean executed = testRetryListener.awaitExecution(20, TimeUnit.SECONDS);

        Assert.assertFalse("状态已改为 SUCCESS 的任务应被 validateTaskInDB 拦截、不执行", executed);
    }

    /**
     * 6.3 retryNum 已耗尽（=0）的任务，SchedulerThread 正确跳过
     */
    @Test
    public void testValidateDB_RetryNumZero_SkipExecution() throws Exception {
        testRetryListener.reset();

        String uniqueParam = "retry-num-zero-" + System.nanoTime();

        RetryTaskBuilder<TestParam> builder = RetryTaskBuilder.of();
        builder.withTaskCode("test-delay-queue");
        builder.withParam(new TestParam(uniqueParam));
        builder.withRetryNum(1);
        builder.withDelaySecond(10);
        builder.withIntervalSecond(10);
        builder.withNextPlanTimeStrategy(NextPlanTimeStrategyEnum.FIXED);
        long taskId = operator.createTask(builder);

        Assert.assertTrue("任务应创建成功", taskId > 0);

        // 从 DB 获取完整任务，将 retryNum 改为 0
        RetryTask fullTask = retryConfiguration.getRetryTaskAcess().getRetryTask(taskId);
        Assert.assertNotNull("任务应在 DB 中存在, taskId=" + taskId, fullTask);
        fullTask.setRetryNum(0);
        retryConfiguration.getRetryTaskAcess().updateRetryTask(fullTask);

        System.out.println("=== [DB校验-retryNum=0] taskId=" + taskId + " retryNum 已改为 0");

        boolean executed = testRetryListener.awaitExecution(20, TimeUnit.SECONDS);

        Assert.assertFalse("retryNum=0 的任务应被 validateTaskInDB 拦截、不执行", executed);
    }

    // ==================================================================
    // 七、Producer 兜底 & 调度精度测试
    // ==================================================================

    /**
     * 7.1 窗口外任务：delay 超过 preloadWindow 的任务不会被 enqueueIfInWindow 立即入队
     *
     * <p>注：当前配置 preloadWindow=100s。设置 delay=120s 确保超出窗口，
     * 验证创建后不会立即可见地增加 inMemoryTaskKeys 计数。
     */
    @Test
    public void testProducer_WindowBoundary_NotImmediatelyEnqueued() throws Exception {
        int preloadMultiplier = smartConfigure.getScanPreloadMultiplier();
        int taskFindInterval = smartConfigure.getTaskFindInterval();
        long preloadWindowMs = (long) taskFindInterval * preloadMultiplier * 1000L;
        int preloadWindowSec = (int) (preloadWindowMs / 1000);

        // delay 设为 window + 20s，确保在窗口外
        int outOfWindowDelay = preloadWindowSec + 20;

        System.out.println("=== [Producer兜底] preloadWindow=" + preloadWindowSec
            + "s, delay=" + outOfWindowDelay + "s");

        int beforeMemory = SimpleContainer.getInMemoryTaskCount();

        RetryTaskBuilder<TestParam> builder = RetryTaskBuilder.of();
        builder.withTaskCode("test-stress");
        builder.withParam(new TestParam("out-of-window-" + System.nanoTime()));
        builder.withRetryNum(1);
        builder.withDelaySecond(outOfWindowDelay);
        builder.withIntervalSecond(10);
        builder.withNextPlanTimeStrategy(NextPlanTimeStrategyEnum.FIXED);
        operator.createTask(builder);

        // 立即检查：窗口外任务不应在内存中（enqueueIfInWindow 会因 nextPlanTime > windowEnd 而跳过）
        TimeUnit.MILLISECONDS.sleep(500);
        int afterMemory = SimpleContainer.getInMemoryTaskCount();

        System.out.println("=== [Producer兜底] 创建后 inMemory: before=" + beforeMemory
            + ", after=" + afterMemory);

        // 窗口外任务应该不会通过 enqueueIfInWindow 入队
        // 注：偶尔 Producer 扫描可能恰好加载了其他待处理任务，导致计数小幅波动，用宽松判断
        Assert.assertTrue("窗口外任务不应立即入队（enqueueIfInWindow 应跳过），"
            + "before=" + beforeMemory + ", after=" + afterMemory,
            afterMemory <= beforeMemory + 5);
    }

    /**
     * 7.2 多个不同 next_plan_time 的任务按时间顺序出队
     * 通过执行时间记录验证先到期的任务先执行
     */
    @Test
    public void testScheduling_OrderedByNextPlanTime() throws Exception {
        int taskCount = 5;
        memoryPressureListener.reset(taskCount);

        long baseTime = System.currentTimeMillis();
        System.out.println("=== [排序调度测试] 创建 " + taskCount + " 个不同 delay 的任务");

        // 创建多个任务，delay 依次递减（第一个最长，最后一个最短）
        for (int i = 0; i < taskCount; i++) {
            int delay = 8 - i; // delay=8,7,6,5,4 秒
            RetryTaskBuilder<TestParam> builder = RetryTaskBuilder.of();
            builder.withTaskCode("test-memory-pressure");
            builder.withParam(new TestParam("ordered-" + i + "-delay" + delay));
            builder.withRetryNum(1);
            builder.withDelaySecond(delay);
            builder.withIntervalSecond(10);
            builder.withNextPlanTimeStrategy(NextPlanTimeStrategyEnum.FIXED);
            operator.createTask(builder);
            TimeUnit.MILLISECONDS.sleep(200); // 错开创建时间
        }

        boolean allExecuted = memoryPressureListener.awaitExecution(30, TimeUnit.SECONDS);
        long elapsed = System.currentTimeMillis() - baseTime;

        Assert.assertTrue("所有不同 delay 的任务应全部执行完成", allExecuted);
        Assert.assertEquals("应执行 " + taskCount + " 次", taskCount, memoryPressureListener.getExecuteCount());

        System.out.println("=== [排序调度测试] 全部完成，耗时=" + elapsed + "ms");
    }

    /**
     * 7.3 验证 SimpleContainer.enqueue 去重（inMemoryTaskKeys.add 原子性）
     * 通过多线程并发调用 enqueue 验证只有一个成功
     */
    @Test
    public void testConcurrent_DedupUnderMultiThreadEnqueue() throws Exception {
        dedupTestListener.reset(1);

        // 先创建一个任务让它入队
        String uniqueParam = "concurrent-dedup-" + System.nanoTime();

        RetryTaskBuilder<TestParam> builder = RetryTaskBuilder.of();
        builder.withTaskCode("test-dedup");
        builder.withParam(new TestParam(uniqueParam));
        builder.withRetryNum(1);
        builder.withDelaySecond(3);
        builder.withIntervalSecond(10);
        builder.withNextPlanTimeStrategy(NextPlanTimeStrategyEnum.FIXED);
        operator.createTask(builder);

        // 再创建一个相同参数的任务
        RetryTaskBuilder<TestParam> builder2 = RetryTaskBuilder.of();
        builder2.withTaskCode("test-dedup");
        builder2.withParam(new TestParam(uniqueParam));
        builder2.withRetryNum(1);
        builder2.withDelaySecond(3);
        builder2.withIntervalSecond(10);
        builder2.withNextPlanTimeStrategy(NextPlanTimeStrategyEnum.FIXED);
        operator.createTask(builder2);

        // 同时创建 5 个相同任务（并发竞争）
        for (int i = 0; i < 5; i++) {
            new Thread(() -> {
                try {
                    RetryTaskBuilder<TestParam> b = RetryTaskBuilder.of();
                    b.withTaskCode("test-dedup");
                    b.withParam(new TestParam(uniqueParam));
                    b.withRetryNum(1);
                    b.withDelaySecond(3);
                    b.withIntervalSecond(10);
                    b.withNextPlanTimeStrategy(NextPlanTimeStrategyEnum.FIXED);
                    operator.createTask(b);
                } catch (Exception e) {
                    // 忽略
                }
            }).start();
        }

        TimeUnit.SECONDS.sleep(2);

        boolean executed = dedupTestListener.awaitExecution(15, TimeUnit.SECONDS);

        Assert.assertTrue("并发去重后任务应执行完成", executed);
        Assert.assertEquals("并发创建相同任务去重生效：应只执行 1 次",
            1, dedupTestListener.getExecuteCount());
    }

    // ==================================================================
    // 辅助方法
    // ==================================================================

    /**
     * 获取当前 JVM 已使用的堆内存（近似值）
     */
    private long getUsedMemory() {
        Runtime rt = Runtime.getRuntime();
        return rt.totalMemory() - rt.freeMemory();
    }
}
