package com.smart.retry.test;

import com.smart.retry.common.RetryConfiguration;
import com.smart.retry.common.RetryTaskOperator;
import com.smart.retry.common.constant.NextPlanTimeStrategyEnum;
import com.smart.retry.common.constant.RetryTaskStatus;
import com.smart.retry.common.model.RetryTask;
import com.smart.retry.core.ShardingContextHolder;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 多线程多任务并发集成测试。
 *
 * <p>覆盖本次 DB 乐观锁 CAS 认领改动在<b>并发多任务</b>场景下的正确性：
 * <ul>
 *     <li><b>场景一</b>：多线程并发执行<b>多个不同任务</b>，每个任务恰好执行一次、
 *         终态为 SUCCESS、全程无异常（验证并发多任务下无丢失、无重复、无串扰）；</li>
 *     <li><b>场景二</b>：多线程并发触发<b>同一任务</b>（走完整 operator 链路：
 *         {@code getTriggerableTask → 内存 tryMark 去重 → ConsumerTask → CAS 认领}），
 *         业务恰好执行一次，终态为 SUCCESS（验证内存去重 + DB CAS 双层兜底）。</li>
 * </ul>
 *
 * <p>任务通过 {@code saveRetryTask} 直接插入 DB 且 {@code next_plan_time} 设在远期
 * （365 天），绕过 {@code createTask} 自动入队并避免 Producer 扫描干扰，
 * 只有手动 {@code invokeTaskOnceSync} 才会执行，保证断言确定性。
 *
 * <p>依赖真实 MySQL（retry_task 库）。测试数据使用专用 task_code，@Before/@After 统一清理。
 *
 * @Author xiaoqiang
 * @Version ConcurrentMultiTaskIT.java, v 0.1 2026年08月28日 xiaoqiang
 */
public class ConcurrentMultiTaskIT extends AbstractTest {

    /** 必须与 {@link MultiTaskConcurrencyListener} 的 @RetryOnClass taskCode 精确一致 */
    static final String TASK_CODE = "it-multitask-concurrency";

    private static final long DAY_MILLIS = 365L * 24 * 60 * 60 * 1000;

    @Autowired
    private RetryTaskOperator<String> retryTaskOperator;

    @Autowired
    private MultiTaskConcurrencyListener multiTaskConcurrencyListener;

    @Autowired
    private RetryConfiguration retryConfiguration;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Before
    public void setUp() {
        jdbcTemplate.update("DELETE FROM retry_task WHERE task_code = ?", TASK_CODE);
        multiTaskConcurrencyListener.reset();
    }

    @After
    public void tearDown() {
        jdbcTemplate.update("DELETE FROM retry_task WHERE task_code = ?", TASK_CODE);
        multiTaskConcurrencyListener.reset();
    }

    /**
     * 场景一：多线程并发执行多个不同任务，每个恰好执行一次。
     */
    @Test
    public void concurrentInvoke_distinctTasks_eachExactlyOnce() throws Exception {
        int taskCount = 10;
        int threadCount = 4;
        List<Long> taskIds = insertDistinctTasks(taskCount);

        CyclicBarrier barrier = new CyclicBarrier(threadCount);
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        AtomicInteger taskIndex = new AtomicInteger(0);
        CountDownLatch done = new CountDownLatch(taskCount);
        AtomicInteger errors = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            pool.submit(() -> {
                awaitBarrier(barrier);
                while (true) {
                    int idx = taskIndex.getAndIncrement();
                    if (idx >= taskCount) {
                        break;
                    }
                    try {
                        retryTaskOperator.invokeTaskOnceSync(taskIds.get(idx));
                    } catch (Exception e) {
                        errors.incrementAndGet();
                    } finally {
                        done.countDown();
                    }
                }
            });
        }
        Assert.assertTrue("10 个任务应在 30 秒内全部触发完成", done.await(30, TimeUnit.SECONDS));
        pool.shutdown();

        // 并发多任务执行不应抛异常
        Assert.assertEquals("并发执行不应抛异常", 0, errors.get());

        // 每个任务恰好执行一次（按参数区分，无重复执行）
        Assert.assertEquals("总执行次数应等于任务数", taskCount,
                multiTaskConcurrencyListener.getTotalCount());
        for (int i = 0; i < taskCount; i++) {
            String param = "param-" + i;
            Assert.assertEquals("任务 " + param + " 应恰好执行一次",
                    1, multiTaskConcurrencyListener.getExecuteCount(param));
        }

        // 终态全部为 SUCCESS
        for (Long id : taskIds) {
            RetryTask task = retryConfiguration.getRetryTaskAcess().getRetryTask(id);
            Assert.assertEquals("任务 " + id + " 终态应为 SUCCESS",
                    RetryTaskStatus.SUCCESS.getCode(), task.getStatus());
        }
    }

    /**
     * 场景二：多线程并发触发同一任务，业务恰好执行一次（内存去重 + CAS 兜底）。
     */
    @Test
    public void concurrentInvoke_sameTask_exactlyOnce() throws Exception {
        int threadCount = 8;
        long taskId = insertTask("same", 1);

        CyclicBarrier barrier = new CyclicBarrier(threadCount);
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch done = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            pool.submit(() -> {
                awaitBarrier(barrier);
                try {
                    retryTaskOperator.invokeTaskOnceSync(taskId);
                } catch (Exception ignored) {
                    // 认领失败 / 已执行等并发分支不应导致测试失败，以最终执行次数为准
                } finally {
                    done.countDown();
                }
            });
        }
        Assert.assertTrue("8 个线程应在 30 秒内全部触发完成", done.await(30, TimeUnit.SECONDS));
        pool.shutdown();

        Assert.assertEquals("同一任务并发触发应恰好执行一次",
                1, multiTaskConcurrencyListener.getExecuteCount("same"));

        RetryTask task = retryConfiguration.getRetryTaskAcess().getRetryTask(taskId);
        Assert.assertNotNull("任务应存在", task);
        Assert.assertEquals("终态应为 SUCCESS", RetryTaskStatus.SUCCESS.getCode(), task.getStatus());
    }

    private void awaitBarrier(CyclicBarrier barrier) {
        try {
            barrier.await();
        } catch (Exception e) {
            Thread.currentThread().interrupt();
        }
    }

    private List<Long> insertDistinctTasks(int count) {
        List<Long> ids = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            ids.add(insertTask("param-" + i, 1));
        }
        return ids;
    }

    /**
     * 直接插入一个远期任务（绕过 createTask 自动入队，next_plan_time 远期避免 Producer 扫描），
     * 只有手动 invokeTaskOnceSync 才会执行，保证断言确定性。
     */
    private long insertTask(String param, int retryNum) {
        RetryTask task = new RetryTask();
        task.setTaskCode(TASK_CODE);
        task.setParameters("\"" + param + "\"");   // JSON 字符串，listener consume(String) 反序列化
        task.setRetryNum(retryNum);
        task.setDelaySecond(60);
        task.setIntervalSecond(60);
        task.setNextPlanTimeStrategy(NextPlanTimeStrategyEnum.FIXED.getCode());
        task.setStatus(RetryTaskStatus.WAITING.getCode());
        task.setOriginRetryNum(retryNum);
        task.setCreator("test");
        task.setUniqueKey(TASK_CODE + "-" + param + "-" + System.nanoTime());
        task.setShardingKey(ShardingContextHolder.getRandomShardingIndex());
        task.setNextPlanTime(new Date(System.currentTimeMillis() + DAY_MILLIS));
        return retryConfiguration.getRetryTaskAcess().saveRetryTask(task);
    }
}
