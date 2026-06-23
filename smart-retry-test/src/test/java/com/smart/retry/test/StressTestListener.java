package com.smart.retry.test;

import com.smart.retry.common.RetryLinstener;
import com.smart.retry.common.annotation.RetryOnClass;
import com.smart.retry.common.constant.ExecuteResultStatus;
import org.springframework.stereotype.Component;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 压力测试监听器
 *
 * @Author xiaoqiang
 * @Version StressTestListener.java, v 0.1 2025年06月23日 xiaoqiang
 */
@Component
@RetryOnClass(taskCode = "test-stress")
public class StressTestListener implements RetryLinstener<TestParam> {

    private volatile CountDownLatch latch = new CountDownLatch(1);

    private final AtomicInteger successCount = new AtomicInteger(0);

    @Override
    public ExecuteResultStatus consume(TestParam param) {
        successCount.incrementAndGet();
        latch.countDown();
        return ExecuteResultStatus.SUCCESS;
    }

    public int getSuccessCount() {
        return successCount.get();
    }

    public boolean awaitExecution(long timeout, TimeUnit unit) throws InterruptedException {
        return latch.await(timeout, unit);
    }

    public void reset(int expectedCount) {
        latch = new CountDownLatch(expectedCount);
        successCount.set(0);
    }
}
