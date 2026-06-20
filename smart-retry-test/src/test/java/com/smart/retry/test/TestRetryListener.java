package com.smart.retry.test;

import com.smart.retry.common.RetryLinstener;
import com.smart.retry.common.annotation.RetryOnClass;
import com.smart.retry.common.constant.ExecuteResultStatus;
import org.springframework.stereotype.Component;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 精准调度测试的监听器
 *
 * @Author xiaoqiang
 * @Version TestRetryListener.java, v 0.1 2025年06月20日 xiaoqiang
 */
@Component
@RetryOnClass(taskCode = "test-delay-queue")
public class TestRetryListener implements RetryLinstener<TestParam> {

    private volatile CountDownLatch latch = new CountDownLatch(1);

    @Override
    public ExecuteResultStatus consume(TestParam param) {
        latch.countDown();
        return ExecuteResultStatus.SUCCESS;
    }

    public boolean awaitExecution(long timeout, TimeUnit unit) throws InterruptedException {
        return latch.await(timeout, unit);
    }

    public void reset() {
        latch = new CountDownLatch(1);
    }
}
