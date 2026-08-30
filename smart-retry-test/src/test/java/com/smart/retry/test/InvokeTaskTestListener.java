package com.smart.retry.test;

import com.smart.retry.common.RetryLinstener;
import com.smart.retry.common.annotation.RetryOnClass;
import com.smart.retry.common.constant.ExecuteResultStatus;
import org.springframework.stereotype.Component;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 用于测试 invokeTaskOnceSync / invokeTaskAsync 的监听器。
 * 支持配置每次执行返回 SUCCESS 还是 FAIL，以及跟踪执行次数。
 *
 * @Author xiaoqiang
 * @Version InvokeTaskTestListener.java, v 0.1 2025年07月05日 xiaoqiang
 */
@Component
@RetryOnClass(taskCode = "test-invoke-task")
public class InvokeTaskTestListener implements RetryLinstener<TestParam> {

    private volatile CountDownLatch latch = new CountDownLatch(1);

    private final AtomicInteger executeCount = new AtomicInteger(0);

    /**
     * 每次执行返回的结果：true=SUCCESS, false=FAIL
     */
    private volatile boolean shouldSucceed = true;

    /**
     * 前 N 次返回 FAIL，之后返回 SUCCESS
     * <=0 表示不使用此模式
     */
    private volatile int failFirstNTimes = 0;

    @Override
    public ExecuteResultStatus consume(TestParam param) {
        int count = executeCount.incrementAndGet();
        latch.countDown();

        if (failFirstNTimes > 0 && count <= failFirstNTimes) {
            return ExecuteResultStatus.FAIL;
        }

        return shouldSucceed ? ExecuteResultStatus.SUCCESS : ExecuteResultStatus.FAIL;
    }

    // ==================== 辅助方法 ====================

    public int getExecuteCount() {
        return executeCount.get();
    }

    public boolean awaitExecution(long timeout, TimeUnit unit) throws InterruptedException {
        return latch.await(timeout, unit);
    }

    public void reset() {
        reset(1);
    }

    public void reset(int expectedCount) {
        latch = new CountDownLatch(expectedCount);
        executeCount.set(0);
        shouldSucceed = true;
        failFirstNTimes = 0;
    }

    public void setShouldSucceed(boolean shouldSucceed) {
        this.shouldSucceed = shouldSucceed;
    }

    /**
     * 设置前 N 次返回 FAIL，之后返回 SUCCESS。
     * 用于测试 sync 循环重试场景。
     */
    public void setFailFirstNTimes(int n) {
        this.failFirstNTimes = n;
    }
}
