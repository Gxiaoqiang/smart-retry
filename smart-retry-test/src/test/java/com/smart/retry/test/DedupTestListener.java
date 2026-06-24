package com.smart.retry.test;

import com.smart.retry.common.RetryLinstener;
import com.smart.retry.common.annotation.RetryOnClass;
import com.smart.retry.common.constant.ExecuteResultStatus;
import org.springframework.stereotype.Component;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 去重测试监听器：验证相同 uniqueKey 的任务不会被重复执行
 *
 * @Author xiaoqiang
 * @Version DedupTestListener.java, v 0.1 2025年06月24日 xiaoqiang
 */
@Component
@RetryOnClass(taskCode = "test-dedup")
public class DedupTestListener implements RetryLinstener<TestParam> {

    private volatile CountDownLatch latch = new CountDownLatch(1);

    private final AtomicInteger executeCount = new AtomicInteger(0);

    @Override
    public ExecuteResultStatus consume(TestParam param) {
        executeCount.incrementAndGet();
        latch.countDown();
        return ExecuteResultStatus.SUCCESS;
    }

    public int getExecuteCount() {
        return executeCount.get();
    }

    public boolean awaitExecution(long timeout, TimeUnit unit) throws InterruptedException {
        return latch.await(timeout, unit);
    }

    public void reset(int expectedCount) {
        latch = new CountDownLatch(expectedCount);
        executeCount.set(0);
    }
}
