package com.smart.retry.test;

import com.smart.retry.common.RetryLinstener;
import com.smart.retry.common.annotation.RetryOnClass;
import com.smart.retry.common.constant.ExecuteResultStatus;
import org.springframework.stereotype.Component;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 异常测试监听器：始终抛出异常，验证调度线程不被中断
 *
 * @Author xiaoqiang
 * @Version ExceptionTestListener.java, v 0.1 2025年06月24日 xiaoqiang
 */
@Component
@RetryOnClass(taskCode = "test-exception")
public class ExceptionTestListener implements RetryLinstener<TestParam> {

    private volatile CountDownLatch latch = new CountDownLatch(1);

    private final AtomicInteger executeCount = new AtomicInteger(0);

    @Override
    public ExecuteResultStatus consume(TestParam param) {
        executeCount.incrementAndGet();
        latch.countDown();
        // 抛出异常，验证 ConsumerTask 中的 catch Throwable 能兜底
        throw new RuntimeException("模拟 consume 执行异常");
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
