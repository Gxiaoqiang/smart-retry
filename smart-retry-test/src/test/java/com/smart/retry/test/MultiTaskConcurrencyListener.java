package com.smart.retry.test;

import com.smart.retry.common.RetryLinstener;
import com.smart.retry.common.annotation.RetryOnClass;
import com.smart.retry.common.constant.ExecuteResultStatus;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 多线程多任务并发测试专用监听器。
 *
 * <p>与 {@link ConcurrentMultiTaskIT} 配合：按消费参数（每个任务一个唯一参数）
 * 统计 {@code consume} 实际执行次数，用于验证：
 * <ul>
 *     <li>多线程并发执行多个不同任务时，每个任务恰好执行一次（无丢失、无重复）；</li>
 *     <li>多线程并发触发同一任务时，DB 乐观锁 CAS + 内存去重兜底，业务至多执行一次。</li>
 * </ul>
 *
 * @Author xiaoqiang
 * @Version MultiTaskConcurrencyListener.java, v 0.1 2026年08月28日 xiaoqiang
 */
@Component
@RetryOnClass(taskCode = ConcurrentMultiTaskIT.TASK_CODE)
public class MultiTaskConcurrencyListener implements RetryLinstener<String> {

    private final Map<String, AtomicInteger> countMap = new ConcurrentHashMap<>();

    @Override
    public ExecuteResultStatus consume(String param) {
        countMap.computeIfAbsent(param, k -> new AtomicInteger()).incrementAndGet();
        return ExecuteResultStatus.SUCCESS;
    }

    /**
     * 获取指定参数对应的消费次数。
     *
     * @param param 消费参数
     * @return 执行次数
     */
    public int getExecuteCount(String param) {
        AtomicInteger count = countMap.get(param);
        return count == null ? 0 : count.get();
    }

    /**
     * 获取全部消费总次数。
     *
     * @return 总执行次数
     */
    public int getTotalCount() {
        int total = 0;
        for (AtomicInteger count : countMap.values()) {
            total += count.get();
        }
        return total;
    }

    public void reset() {
        countMap.clear();
    }
}
