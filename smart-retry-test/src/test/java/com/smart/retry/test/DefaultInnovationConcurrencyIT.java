package com.smart.retry.test;

import com.smart.retry.common.RetryConfiguration;
import com.smart.retry.common.constant.RetryTaskStatus;
import com.smart.retry.common.exception.RetryTaskClaimedException;
import com.smart.retry.common.model.RetryTask;
import com.smart.retry.core.innovation.DefaultInnovation;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @Author xiaoqiang
 * @Version DefaultInnovationConcurrencyIT.java, v 0.1 2026年08月28日 xiaoqiang
 * @Description: 双线程并发认领集成测试。
 *
 * <p>两个线程各从 DB 重新读取任务副本，直接调用
 * {@code new DefaultInnovation(task, retryConfiguration).invoke()}（绕过 ConsumerTask，
 * 从而绕过单 JVM 内存去重，使 TOCTOU 竞态真实暴露），验证：
 * <ul>
 *     <li>DB 乐观锁 CAS 保证恰一个线程认领成功、另一个抛 {@link RetryTaskClaimedException}；</li>
 *     <li>业务监听器恰好执行一次（非幂等场景下不允许重复执行）；</li>
 *     <li>任务终态为 SUCCESS(2)。</li>
 * </ul>
 *
 * <p>依赖真实 MySQL（retry_task 库）。测试任务使用专用分片键 + task_code 前缀，
 * next_plan_time 设在远期避免 Producer 扫描干扰，@Before/@After 统一清理。
 */
public class DefaultInnovationConcurrencyIT extends AbstractTest {

    /** 必须与 {@link ConcurrencyRetryListener} 的 @RetryOnClass taskCode 精确一致 */
    private static final String TASK_CODE = "it-concurrency-retry";
    private static final String TASK_CODE_PREFIX = "it-concurrency-";
    private static final long TEST_SHARDING = 987_654_111L;

    @Autowired
    private RetryConfiguration retryConfiguration;

    @Autowired
    private ConcurrencyRetryListener concurrencyRetryListener;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Before
    public void setUp() {
        jdbcTemplate.update("DELETE FROM retry_task WHERE task_code LIKE ?", TASK_CODE_PREFIX + "%");
        concurrencyRetryListener.reset();
    }

    @After
    public void cleanup() {
        jdbcTemplate.update("DELETE FROM retry_task WHERE task_code LIKE ?", TASK_CODE_PREFIX + "%");
        concurrencyRetryListener.reset();
    }

    @Test
    public void concurrentInvoke_shouldExecuteBusinessExactlyOnce() throws Exception {
        long id = insertTask();

        // 两个线程各从 DB 拿独立副本（模拟两个执行方各自读取）
        RetryTask taskA = retryConfiguration.getRetryTaskAcess().getRetryTask(id);
        RetryTask taskB = retryConfiguration.getRetryTaskAcess().getRetryTask(id);
        assertNotNull("任务 A 副本不应为空", taskA);
        assertNotNull("任务 B 副本不应为空", taskB);

        CyclicBarrier barrier = new CyclicBarrier(2);
        AtomicReference<Throwable> errorA = new AtomicReference<>();
        AtomicReference<Throwable> errorB = new AtomicReference<>();

        Thread t1 = new Thread(() -> invokeInThread(taskA, barrier, errorA), "claim-thread-a");
        Thread t2 = new Thread(() -> invokeInThread(taskB, barrier, errorB), "claim-thread-b");

        t1.start();
        t2.start();
        t1.join(30_000);
        t2.join(30_000);

        // 恰一个线程正常返回，另一个抛 RetryTaskClaimedException
        boolean aOk = errorA.get() == null;
        boolean bOk = errorB.get() == null;
        assertNotEquals("恰一个线程应认领成功", aOk, bOk);
        if (errorA.get() != null) {
            assertTrue("线程 A 应抛 RetryTaskClaimedException",
                    errorA.get() instanceof RetryTaskClaimedException);
        }
        if (errorB.get() != null) {
            assertTrue("线程 B 应抛 RetryTaskClaimedException",
                    errorB.get() instanceof RetryTaskClaimedException);
        }

        // 业务只执行一次
        assertEquals("业务应恰好执行一次", 1, concurrencyRetryListener.getExecuteCount());

        // 任务终态为 SUCCESS
        RetryTask dbTask = retryConfiguration.getRetryTaskAcess().getRetryTask(id);
        assertNotNull("终态任务应存在", dbTask);
        assertEquals("任务终态应为 SUCCESS", RetryTaskStatus.SUCCESS.getCode(), dbTask.getStatus());
    }

    private void invokeInThread(RetryTask task, CyclicBarrier barrier, AtomicReference<Throwable> errorRef) {
        try {
            barrier.await();
            new DefaultInnovation(task, retryConfiguration).invoke();
        } catch (Throwable t) {
            errorRef.set(t);
        }
    }

    private long insertTask() {
        String uniqueKey = TASK_CODE_PREFIX + System.nanoTime();
        jdbcTemplate.update(
                "INSERT INTO retry_task (gmt_create, gmt_modified, sharding_key, task_code, status, "
                        + "next_plan_time, interval_second, retry_num, parameters, unique_key, task_desc, "
                        + "next_plan_time_strategy) "
                        + "VALUES (NOW(3), NOW(3), ?, ?, 0, "
                        + "DATE_ADD(NOW(3), INTERVAL 365 DAY), 3600, 1, ?, ?, 'it-concurrency', 1)",
                TEST_SHARDING, TASK_CODE, "\"hello\"", uniqueKey);
        Long id = jdbcTemplate.queryForObject("SELECT id FROM retry_task WHERE task_code = ?", Long.class, TASK_CODE);
        return id == null ? -1 : id;
    }
}
