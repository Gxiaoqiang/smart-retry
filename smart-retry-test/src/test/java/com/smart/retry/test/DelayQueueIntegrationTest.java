package com.smart.retry.test;

import com.smart.retry.common.RetryTaskOperator;
import com.smart.retry.common.constant.NextPlanTimeStrategyEnum;
import com.smart.retry.common.model.RetryTaskBuilder;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.TimeUnit;

/**
 * DelayQueue 精准调度集成测试
 * 覆盖多任务并发、失败重试、异常处理、压力测试等场景
 *
 * @Author xiaoqiang
 * @Version DelayQueueIntegrationTest.java, v 0.1 2025年06月23日 xiaoqiang
 */
public class DelayQueueIntegrationTest extends AbstractTest {

    @Autowired
    private RetryTaskOperator<TestParam> retryTaskOperator;

    @Autowired
    private MultiTaskListener multiTaskListener;

    @Autowired
    private FailRetryListener failRetryListener;

    @Autowired
    private StressTestListener stressTestListener;

    @Autowired
    private TestRetryListener testRetryListener;

    // ======================== 多任务并发测试 ========================

    /**
     * 验证多个任务并发创建后，全部能按时执行完成
     */
    @Test
    public void testMultipleTasksConcurrent() throws Exception {
        int taskCount = 5;
        multiTaskListener.reset(taskCount);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < taskCount; i++) {
            RetryTaskBuilder<TestParam> builder = RetryTaskBuilder.of();
            builder.withTaskCode("test-multi-task");
            builder.withParam(new TestParam("concurrent-task-" + i));
            builder.withRetryNum(1);
            builder.withDelaySecond(3);
            builder.withIntervalSecond(10);
            builder.withNextPlanTimeStrategy(NextPlanTimeStrategyEnum.FIXED);

            retryTaskOperator.createTask(builder);
        }

        boolean allExecuted = multiTaskListener.awaitExecution(30, TimeUnit.SECONDS);
        long elapsed = System.currentTimeMillis() - startTime;

        Assert.assertTrue("所有 " + taskCount + " 个并发任务应全部执行完成", allExecuted);
        Assert.assertTrue("并发任务应在 20s 内全部完成，实际: " + elapsed + "ms", elapsed < 20000);
    }

    // ======================== 失败重试测试 ========================

    /**
     * 验证任务失败后能按配置的重试次数进行多次重试
     */
    @Test
    public void testTaskFailureAndRetry() throws Exception {
        int maxRetryNum = 3;
        failRetryListener.reset(maxRetryNum);

        RetryTaskBuilder<TestParam> builder = RetryTaskBuilder.of();
        builder.withTaskCode("test-fail-retry");
        builder.withParam(new TestParam("fail-retry-test"));
        builder.withRetryNum(maxRetryNum);
        builder.withDelaySecond(3);
        builder.withIntervalSecond(5);
        builder.withNextPlanTimeStrategy(NextPlanTimeStrategyEnum.FIXED);

        long startTime = System.currentTimeMillis();
        retryTaskOperator.createTask(builder);

        // 等待所有重试执行完毕
        boolean allAttemptsExecuted = failRetryListener.awaitExecution(60, TimeUnit.SECONDS);
        long elapsed = System.currentTimeMillis() - startTime;

        Assert.assertTrue("任务应执行 " + maxRetryNum + " 次重试", allAttemptsExecuted);
        Assert.assertEquals("应执行 " + maxRetryNum + " 次", maxRetryNum, failRetryListener.getExecuteCount());
        Assert.assertTrue("3次重试应在 50s 内完成，实际: " + elapsed + "ms", elapsed < 50000);
    }

    // ======================== 精准调度时间验证 ========================

    /**
     * 验证任务能在设定的 delay 时间内被精准调度触发
     * taskFindInterval=15s，生产者每 15s 扫描一次，
     * 最小 delay 为 1s，任务最晚在一次扫描间隔内被拾取，总耗时 <25s
     */
    @Test
    public void testPreciseSchedulingTiming() throws Exception {
        testRetryListener.reset();

        long startTime = System.currentTimeMillis();

        RetryTaskBuilder<TestParam> builder = RetryTaskBuilder.of();
        builder.withTaskCode("test-delay-queue");
        builder.withParam(new TestParam("timing-test"));
        builder.withRetryNum(1);
        builder.withDelaySecond(1);
        builder.withIntervalSecond(10);
        builder.withNextPlanTimeStrategy(NextPlanTimeStrategyEnum.FIXED);

        retryTaskOperator.createTask(builder);

        boolean executed = testRetryListener.awaitExecution(25, TimeUnit.SECONDS);
        long elapsed = System.currentTimeMillis() - startTime;

        Assert.assertTrue("任务应在 delay 时间内被触发", executed);
        // delay=1s + taskFindInterval=15s 扫描，应在 25s 内完成
        Assert.assertTrue("任务应在 25s 内完成（1s delay + taskFindInterval=15s 扫描），实际: " + elapsed + "ms",
            elapsed < 25000);
    }

    // ======================== 压力测试 ========================

    /**
     * 验证系统在大量任务同时提交时的处理能力
     */
    @Test
    public void testStressMultipleTasks() throws Exception {
        int taskCount = 20;
        stressTestListener.reset(taskCount);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < taskCount; i++) {
            RetryTaskBuilder<TestParam> builder = RetryTaskBuilder.of();
            builder.withTaskCode("test-stress");
            builder.withParam(new TestParam("stress-task-" + i));
            builder.withRetryNum(1);
            builder.withDelaySecond(2);
            builder.withIntervalSecond(10);
            builder.withNextPlanTimeStrategy(NextPlanTimeStrategyEnum.FIXED);

            retryTaskOperator.createTask(builder);
        }

        boolean allExecuted = stressTestListener.awaitExecution(60, TimeUnit.SECONDS);
        long elapsed = System.currentTimeMillis() - startTime;

        Assert.assertTrue("所有 " + taskCount + " 个压力测试任务应全部执行完成", allExecuted);
        Assert.assertEquals("应执行 " + taskCount + " 次", taskCount, stressTestListener.getSuccessCount());
    }

    // ======================== 顺序执行测试 ========================

    /**
     * 验证任务按顺序创建和执行，互不干扰
     * 需要多个扫描周期，使用足够大的等待时间
     */
    @Test
    public void testSequentialTaskExecution() throws Exception {
        // 第一个任务
        testRetryListener.reset();

        RetryTaskBuilder<TestParam> builder1 = RetryTaskBuilder.of();
        builder1.withTaskCode("test-delay-queue");
        builder1.withParam(new TestParam("sequential-1"));
        builder1.withRetryNum(1);
        builder1.withDelaySecond(1);
        builder1.withIntervalSecond(10);
        builder1.withNextPlanTimeStrategy(NextPlanTimeStrategyEnum.FIXED);

        long start1 = System.currentTimeMillis();
        retryTaskOperator.createTask(builder1);
        boolean executed1 = testRetryListener.awaitExecution(25, TimeUnit.SECONDS);
        long elapsed1 = System.currentTimeMillis() - start1;

        Assert.assertTrue("第一个任务应执行成功", executed1);

        // 第二个任务
        testRetryListener.reset();

        RetryTaskBuilder<TestParam> builder2 = RetryTaskBuilder.of();
        builder2.withTaskCode("test-delay-queue");
        builder2.withParam(new TestParam("sequential-2"));
        builder2.withRetryNum(1);
        builder2.withDelaySecond(1);
        builder2.withIntervalSecond(10);
        builder2.withNextPlanTimeStrategy(NextPlanTimeStrategyEnum.FIXED);

        long start2 = System.currentTimeMillis();
        retryTaskOperator.createTask(builder2);
        boolean executed2 = testRetryListener.awaitExecution(25, TimeUnit.SECONDS);
        long elapsed2 = System.currentTimeMillis() - start2;

        Assert.assertTrue("第二个任务应执行成功", executed2);
        Assert.assertTrue("每个顺序任务应在 25s 内完成（taskFindInterval=15s），实际: " + elapsed1 + "ms, " + elapsed2 + "ms",
            elapsed1 < 25000 && elapsed2 < 25000);
    }

    // ======================== 不同策略测试 ========================

    /**
     * 验证 FIXED、BACKOFF、INCREMENTING 三种重试策略的任务都能正常执行
     */
    @Test
    public void testDifferentStrategies() throws Exception {
        multiTaskListener.reset(3);

        // FIXED 策略
        RetryTaskBuilder<TestParam> fixedBuilder = RetryTaskBuilder.of();
        fixedBuilder.withTaskCode("test-multi-task");
        fixedBuilder.withParam(new TestParam("fixed"));
        fixedBuilder.withRetryNum(1);
        fixedBuilder.withDelaySecond(3);
        fixedBuilder.withIntervalSecond(10);
        fixedBuilder.withNextPlanTimeStrategy(NextPlanTimeStrategyEnum.FIXED);
        retryTaskOperator.createTask(fixedBuilder);

        // BACKOFF 策略
        RetryTaskBuilder<TestParam> backoffBuilder = RetryTaskBuilder.of();
        backoffBuilder.withTaskCode("test-multi-task");
        backoffBuilder.withParam(new TestParam("backoff"));
        backoffBuilder.withRetryNum(1);
        backoffBuilder.withDelaySecond(3);
        backoffBuilder.withIntervalSecond(10);
        backoffBuilder.withNextPlanTimeStrategy(NextPlanTimeStrategyEnum.BACKOFF);
        retryTaskOperator.createTask(backoffBuilder);

        // INCREMENTING 策略
        RetryTaskBuilder<TestParam> incrBuilder = RetryTaskBuilder.of();
        incrBuilder.withTaskCode("test-multi-task");
        incrBuilder.withParam(new TestParam("incrementing"));
        incrBuilder.withRetryNum(1);
        incrBuilder.withDelaySecond(3);
        incrBuilder.withIntervalSecond(10);
        incrBuilder.withNextPlanTimeStrategy(NextPlanTimeStrategyEnum.INCREMENTING);
        retryTaskOperator.createTask(incrBuilder);

        boolean allExecuted = multiTaskListener.awaitExecution(20, TimeUnit.SECONDS);

        Assert.assertTrue("三种策略的任务都应执行成功", allExecuted);
    }
}
