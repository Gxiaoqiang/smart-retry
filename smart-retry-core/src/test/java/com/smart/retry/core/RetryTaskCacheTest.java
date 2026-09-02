package com.smart.retry.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * RetryTaskCache 去重集合的单元测试。
 *
 * <p>重点覆盖 {@link RetryTaskCache#tryMarkIfBelowLimit(String, int)}：
 * <ul>
 *     <li>未达上限时可标记；</li>
 *     <li>已达上限（含边界 size==max）时拒绝且不改变集合大小；</li>
 *     <li>已存在 key 重复标记被拒；</li>
 *     <li>并发下集合大小不超过 max（size 检查与 add 同锁原子的精确性）。</li>
 * </ul>
 */
class RetryTaskCacheTest {

    @BeforeEach
    void cleanState() throws Exception {
        Field field = RetryTaskCache.class.getDeclaredField("IN_MEMORY_TASKS");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        Set<String> set = (Set<String>) field.get(null);
        set.clear();
    }

    @Test
    void tryMark_addsAndDedups() {
        assertTrue(RetryTaskCache.tryMark("k1"));
        assertEquals(1, RetryTaskCache.size());

        // 重复 key 返回 false，集合不增长
        assertFalse(RetryTaskCache.tryMark("k1"));
        assertEquals(1, RetryTaskCache.size());
    }

    @Test
    void unmark_removesKey() {
        RetryTaskCache.tryMark("k1");
        RetryTaskCache.unmark("k1");
        assertEquals(0, RetryTaskCache.size());

        // 释放后可再次成功标记
        assertTrue(RetryTaskCache.tryMark("k1"));
        assertEquals(1, RetryTaskCache.size());
    }

    @Test
    void tryMarkIfBelowLimit_belowLimit_succeeds() {
        assertTrue(RetryTaskCache.tryMarkIfBelowLimit("k1", 3));
        assertTrue(RetryTaskCache.tryMarkIfBelowLimit("k2", 3));
        assertTrue(RetryTaskCache.tryMarkIfBelowLimit("k3", 3));
        assertEquals(3, RetryTaskCache.size());
    }

    @Test
    void tryMarkIfBelowLimit_atLimit_rejectsAndKeepsSize() {
        RetryTaskCache.tryMark("k1");
        RetryTaskCache.tryMark("k2");
        RetryTaskCache.tryMark("k3");

        // size == max（3）时拒绝，且集合不变
        assertFalse(RetryTaskCache.tryMarkIfBelowLimit("k4", 3));
        assertEquals(3, RetryTaskCache.size());
    }

    @Test
    void tryMarkIfBelowLimit_aboveLimit_rejects() {
        RetryTaskCache.tryMark("k1");
        RetryTaskCache.tryMark("k2");
        RetryTaskCache.tryMark("k3");

        // 若 max 被调小到低于当前 size，同样拒绝
        assertFalse(RetryTaskCache.tryMarkIfBelowLimit("k4", 2));
        assertEquals(3, RetryTaskCache.size());
    }

    @Test
    void tryMarkIfBelowLimit_existingKey_rejectsEvenBelowLimit() {
        assertTrue(RetryTaskCache.tryMarkIfBelowLimit("k1", 5));
        assertFalse(RetryTaskCache.tryMarkIfBelowLimit("k1", 5));
        assertEquals(1, RetryTaskCache.size());
    }

    @Test
    void tryMarkIfBelowLimit_concurrent_neverExceedsLimit() throws Exception {
        int max = 10;
        int threads = 50;
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger maxObservedSize = new AtomicInteger();
        CyclicBarrier barrier = new CyclicBarrier(threads);
        CountDownLatch done = new CountDownLatch(threads);
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            for (int i = 0; i < threads; i++) {
                final String key = "concurrent-" + i;
                pool.submit(() -> {
                    try {
                        barrier.await();
                        if (RetryTaskCache.tryMarkIfBelowLimit(key, max)) {
                            successCount.incrementAndGet();
                        }
                        // 每次观测到的集合大小都必须不超过 max
                        maxObservedSize.accumulateAndGet(RetryTaskCache.size(), Math::max);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    } finally {
                        done.countDown();
                    }
                });
            }
            done.await();
        } finally {
            pool.shutdownNow();
        }

        assertEquals(max, successCount.get(),
                "并发下恰好只有 max 个不同 key 能成功标记，实际成功数: " + successCount.get());
        assertEquals(max, RetryTaskCache.size(),
                "最终集合大小应为 max，实际: " + RetryTaskCache.size());
        assertTrue(maxObservedSize.get() <= max,
                "任意观测时刻 size 不得超过 max，实际最大值: " + maxObservedSize.get());
    }
}
