package com.smart.retry.test;

import com.smart.retry.common.RetryTaskOperator;
import com.smart.retry.common.constant.NextPlanTimeStrategyEnum;
import com.smart.retry.common.model.RetryTaskBuilder;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.TimeUnit;

/**
 * 验证 DelayQueue 精准调度：任务创建后能在秒级时间内被触发
 *
 * @Author xiaoqiang
 * @Version DelayQueueSchedulingTest.java, v 0.1 2025年06月20日 xiaoqiang
 */
public class DelayQueueSchedulingTest extends AbstractTest {

    @Autowired
    private RetryTaskOperator<TestParam> retryTaskOperator;

    @Autowired
    private TestRetryListener testRetryListener;

    @Test
    public void testTaskTriggeredWithinShortDelay() throws Exception {
        // 重置 latch
        testRetryListener.reset();

        // 准备参数
        TestParam param = new TestParam("delay-queue-test");

        // 创建任务：delaySecond=5s（当前时间+5s后执行）
        RetryTaskBuilder<TestParam> builder = RetryTaskBuilder.of();
        builder.withTaskCode("test-delay-queue");
        builder.withParam(param);
        builder.withRetryNum(1);
        builder.withDelaySecond(5);
        builder.withIntervalSecond(10);
        builder.withNextPlanTimeStrategy(NextPlanTimeStrategyEnum.FIXED);

        long startTime = System.currentTimeMillis();
        retryTaskOperator.createTask(builder);

        // 等待任务执行完成（最多等待 15s）
        boolean executed = testRetryListener.awaitExecution(15, TimeUnit.SECONDS);

        long elapsed = System.currentTimeMillis() - startTime;

        Assert.assertTrue("任务应在 5s delay + 调度延迟内被触发", executed);
        Assert.assertTrue("任务应在 10s 内完成（5s delay + 最多几秒调度延迟），实际: " + elapsed + "ms",
            elapsed < 10000);
    }
}
