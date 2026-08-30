package com.smart.retry.test;

import com.smart.retry.common.RetryLinstener;
import com.smart.retry.common.annotation.RetryOnClass;
import com.smart.retry.common.constant.ExecuteResultStatus;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 并发认领测试专用监听器。
 *
 * <p>与 {@link DefaultInnovationConcurrencyIT} 配合：统计 {@code consume} 实际执行次数，
 * 用于验证 DB 乐观锁 CAS 保证业务方法至多被执行一次。
 *
 * @Author xiaoqiang
 * @Version ConcurrencyRetryListener.java, v 0.1 2026年08月28日 xiaoqiang
 */
@Component
@RetryOnClass(taskCode = "it-concurrency-retry")
public class ConcurrencyRetryListener implements RetryLinstener<String> {

    private final AtomicInteger executeCount = new AtomicInteger(0);

    @Override
    public ExecuteResultStatus consume(String param) {
        executeCount.incrementAndGet();
        return ExecuteResultStatus.SUCCESS;
    }

    public int getExecuteCount() {
        return executeCount.get();
    }

    public void reset() {
        executeCount.set(0);
    }
}
