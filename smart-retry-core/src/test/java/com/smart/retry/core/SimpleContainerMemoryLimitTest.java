package com.smart.retry.core;

import com.smart.retry.common.model.RetryTask;
import com.smart.retry.core.config.SmartExecutorConfigure;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SimpleContainer 入队路径内存上限（maxInMemory）精确控制的单元测试。
 *
 * <p>不启动容器线程（SchedulerThread/Producer），直接验证：
 * <ul>
 *     <li>{@link SimpleContainer#enqueue(RetryTask)} 在未满/重复/已满时的返回语义；</li>
 *     <li>并发入队时内存任务数不超过 maxInMemory；</li>
 *     <li>{@link SimpleContainer#enqueueIfInWindow(RetryTask)} 的窗口判断与内存满拒绝。</li>
 * </ul>
 */
class SimpleContainerMemoryLimitTest {

    private static final int MAX = 100;

    @BeforeEach
    void initContainerAndCleanState() throws Exception {
        SmartExecutorConfigure configure = new SmartExecutorConfigure();
        configure.setMaxInMemory(MAX);

        // enqueue / enqueueIfInWindow 不依赖 retryConfiguration，传 null 即可初始化静态 smartConfigure
        new SimpleContainer(null, configure);

        clearRetryTaskCache();
        clearDelayQueue();
        setPreloadWindowMs(0L);
    }

    // ========== enqueue：内存上限精确控制 ==========

    @Test
    void enqueue_belowLimit_returnsTrue() throws Exception {
        assertTrue(SimpleContainer.enqueue(newTask("tc", "k1", -1000)));
        assertEquals(1, RetryTaskCache.size());
        assertEquals(1, delayQueueSize());
    }

    @Test
    void enqueue_duplicateKey_returnsFalseWithoutGrowing() throws Exception {
        RetryTask task = newTask("tc", "k1", -1000);
        assertTrue(SimpleContainer.enqueue(task));
        assertFalse(SimpleContainer.enqueue(newTask("tc", "k1", -1000)));
        assertEquals(1, RetryTaskCache.size());
        assertEquals(1, delayQueueSize());
    }

    @Test
    void enqueue_atLimit_returnsFalseAndKeepsLimit() throws Exception {
        fillToLimit();
        assertEquals(MAX, RetryTaskCache.size());

        // 第 MAX+1 个不同 key 应被拒绝，size 保持 MAX
        assertFalse(SimpleContainer.enqueue(newTask("tc", "overflow", -1000)));
        assertEquals(MAX, RetryTaskCache.size());
        assertEquals(MAX, delayQueueSize());
    }

    @Test
    void enqueue_concurrent_neverExceedsLimit() throws Exception {
        int threads = 300;
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger maxObservedSize = new AtomicInteger();
        CyclicBarrier barrier = new CyclicBarrier(threads);
        CountDownLatch done = new CountDownLatch(threads);
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            for (int i = 0; i < threads; i++) {
                final int idx = i;
                pool.submit(() -> {
                    try {
                        barrier.await();
                        if (SimpleContainer.enqueue(newTask("tc", "ck-" + idx, -1000))) {
                            successCount.incrementAndGet();
                        }
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

        assertEquals(MAX, successCount.get(),
                "并发下恰好只有 max 个任务能入队，实际成功数: " + successCount.get());
        assertEquals(MAX, RetryTaskCache.size(),
                "最终内存任务数应为 max，实际: " + RetryTaskCache.size());
        assertEquals(MAX, delayQueueSize(),
                "delayQueue 中任务数应与内存占位一致，实际: " + delayQueueSize());
        assertTrue(maxObservedSize.get() <= MAX,
                "任意观测时刻 size 不得超过 max，实际最大值: " + maxObservedSize.get());
    }

    // ========== enqueueIfInWindow：窗口判断 ==========

    @Test
    void enqueueIfInWindow_nullTask_noop() {
        SimpleContainer.enqueueIfInWindow(null);
        assertEquals(0, RetryTaskCache.size());
    }

    @Test
    void enqueueIfInWindow_nullNextPlanTime_noop() {
        RetryTask task = newTask("tc", "k1", -1000);
        task.setNextPlanTime(null);
        SimpleContainer.enqueueIfInWindow(task);
        assertEquals(0, RetryTaskCache.size());
    }

    @Test
    void enqueueIfInWindow_withinWindow_enqueues() {
        // preloadWindowMs=0，windowEnd=now；过去时间点的任务必然在窗口内
        SimpleContainer.enqueueIfInWindow(newTask("tc", "k1", -1000));
        assertEquals(1, RetryTaskCache.size());
    }

    @Test
    void enqueueIfInWindow_withinConfiguredWindow_enqueues() throws Exception {
        // 显式预加载窗口 60s：nextPlanTime=now+10s 应在窗口内
        setPreloadWindowMs(60_000L);
        SimpleContainer.enqueueIfInWindow(newTask("tc", "k1", 10_000));
        assertEquals(1, RetryTaskCache.size());
    }

    @Test
    void enqueueIfInWindow_outsideWindow_notEnqueued() {
        // preloadWindowMs=0，nextPlanTime=now+60s > windowEnd(now)，不入队，由 Producer 兜底
        SimpleContainer.enqueueIfInWindow(newTask("tc", "k1", 60_000));
        assertEquals(0, RetryTaskCache.size());
    }

    @Test
    void enqueueIfInWindow_memoryFull_notEnqueued() throws Exception {
        fillToLimit();
        assertEquals(MAX, RetryTaskCache.size());

        // 即使任务在窗口内，内存已满也应被拒绝（快速失败分支）
        SimpleContainer.enqueueIfInWindow(newTask("tc", "k-overflow", -1000));
        assertEquals(MAX, RetryTaskCache.size());
        assertEquals(MAX, delayQueueSize());
    }

    @Test
    void enqueueIfInWindow_afterUnmarkOfExisting_reenqueues() throws Exception {
        // 模拟"createTask 入队后手动触发释放占位再重新入队"的语义：
        // unmark 释放占位后，同一 key 可再次入队（内存占位不重复累计，size 恒为 1）
        RetryTask task = newTask("tc", "k1", -1000);
        assertTrue(SimpleContainer.enqueue(task));
        RetryTaskCache.unmark(SimpleContainer.getUniqueKey(task));

        assertTrue(SimpleContainer.enqueue(task));
        assertEquals(1, RetryTaskCache.size());
        // delayQueue 会残留重复 ScheduledTask（旧占位 + 新入队各一份）：
        // 这正是 releaseAutoEnqueueMark 注释描述的已知权衡——不删除残留任务，
        // 由 SchedulerThread 消费时通过 tryMark/validateTaskInDB/CAS 拦截跳过。
        assertEquals(2, delayQueueSize());
    }

    // ========== helpers ==========

    private RetryTask newTask(String taskCode, String uniqueKey, long nextPlanTimeOffsetMs) {
        RetryTask task = new RetryTask();
        task.setId((long) uniqueKey.hashCode());
        task.setTaskCode(taskCode);
        task.setUniqueKey(uniqueKey);
        task.setNextPlanTime(new Date(System.currentTimeMillis() + nextPlanTimeOffsetMs));
        return task;
    }

    private void fillToLimit() {
        for (int i = 0; i < MAX; i++) {
            assertTrue(SimpleContainer.enqueue(newTask("tc", "fill-" + i, -1000)),
                    "填满阶段第 " + i + " 个任务应入队成功");
        }
    }

    private void clearRetryTaskCache() throws Exception {
        Field field = RetryTaskCache.class.getDeclaredField("IN_MEMORY_TASKS");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        Set<String> set = (Set<String>) field.get(null);
        set.clear();
    }

    private void clearDelayQueue() throws Exception {
        Field field = SimpleContainer.class.getDeclaredField("delayQueue");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        DelayQueue<?> queue = (DelayQueue<?>) field.get(null);
        queue.clear();
    }

    private void setPreloadWindowMs(long ms) throws Exception {
        Field field = SimpleContainer.class.getDeclaredField("preloadWindowMs");
        field.setAccessible(true);
        field.setLong(null, ms);
    }

    private int delayQueueSize() throws Exception {
        Field field = SimpleContainer.class.getDeclaredField("delayQueue");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        DelayQueue<?> queue = (DelayQueue<?>) field.get(null);
        return queue.size();
    }
}
