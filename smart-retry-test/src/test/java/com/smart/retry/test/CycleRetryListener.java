package com.smart.retry.test;

import com.smart.retry.common.RetryLinstener;
import com.smart.retry.common.annotation.RetryOnClass;
import com.smart.retry.common.constant.ExecuteResultStatus;
import org.springframework.stereotype.Component;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 失败重试循环监听器：始终返回 FAIL，验证 retryNum 耗尽后正确释放内存
 *
 * @Author xiaoqiang
 * @Version CycleRetryListener.java, v 0.1 2025年06月24日 xiaoqiang
 */
@Component
@RetryOnClass(taskCode = "test-cycle-retry")
public class CycleRetryListener implements RetryLinstener<TestParam> {

    private volatile CountDownLatch latch = new CountDownLatch(1);

    private final AtomicInteger executeCount = new AtomicInteger(0);

    @Override
    public ExecuteResultStatus consume(TestParam param) {
        int count = executeCount.incrementAndGet();
        latch.countDown();
        return ExecuteResultStatus.FAIL;
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
