package com.smart.retry.test;

import com.smart.retry.common.RetryTaskAccess;
import com.smart.retry.common.constant.RetryTaskStatus;
import com.smart.retry.mybatis.dao.RetryTaskDao;
import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

/**
 * @Author xiaoqiang
 * @Version DeleteHistoryRetryTaskIT.java, v 0.1 2026年08月27日 00:00 xiaoqiang
 * @Description: 验证历史清理 SQL（deleteByGmtCreate）的真实行为：
 *
 * <ul>
 *     <li>只删除 SUCCESS 终态任务，WAITING / RUNNING / FAIL 等活跃任务必须保留；</li>
 *     <li>只删除“创建时间早于清理时间点”的任务（时间边界）；</li>
 *     <li>只删除当前分片内的任务（分片隔离）；</li>
 *     <li>单次删除条数受 limitRows 限制。</li>
 * </ul>
 *
 * <p>依赖真实 MySQL（retry_task 库）。测试数据使用专用分片键 + task_code 前缀，
 * 不会触碰真实任务数据，@After 统一清理。
 */
public class DeleteHistoryRetryTaskIT extends AbstractTest {

    private static final String TASK_CODE_PREFIX = "it-clear-del-";
    private static final long TEST_SHARDING = 987_654_321L;
    private static final long OTHER_SHARDING = 987_654_322L;

    @Autowired
    private RetryTaskDao retryTaskDao;

    @Autowired
    private RetryTaskAccess retryTaskAccess;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @After
    public void cleanup() {
        jdbcTemplate.update("DELETE FROM retry_task WHERE task_code LIKE ?", TASK_CODE_PREFIX + "%");
    }

    /**
     * 核心场景：同一分片下，40 天前的 4 种状态任务 + 时间边界 + 分片边界。
     * 只有“40 天前 + SUCCESS + 本分片”的那条应被删除。
     */
    @Test
    public void deleteByGmtCreate_shouldOnlyDeleteSuccessOlderThanBeforeDaysInSameSharding() {
        long waitingId = insertTask("waiting", RetryTaskStatus.WAITING.getCode(), 40, TEST_SHARDING);
        long runningId = insertTask("running", RetryTaskStatus.RUNNING.getCode(), 40, TEST_SHARDING);
        long failId = insertTask("fail", RetryTaskStatus.FAIL.getCode(), 40, TEST_SHARDING);
        long oldSuccessId = insertTask("success", RetryTaskStatus.SUCCESS.getCode(), 40, TEST_SHARDING);
        long recentSuccessId = insertTask("recent-success", RetryTaskStatus.SUCCESS.getCode(), 10, TEST_SHARDING);
        long otherShardingSuccessId = insertTask("other-sharding-success", RetryTaskStatus.SUCCESS.getCode(), 40, OTHER_SHARDING);

        Date clearBeforeDate = new Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30));
        int deleted = retryTaskDao.deleteByGmtCreate(clearBeforeDate, 100,
                Collections.singletonList(TEST_SHARDING), RetryTaskStatus.SUCCESS.getCode());

        // 只删除了 1 条：40 天前 + SUCCESS + 本分片
        assertEquals("only the old SUCCESS task in the same sharding should be deleted", 1, deleted);
        assertEquals(1, countById(waitingId));
        assertEquals(1, countById(runningId));
        assertEquals(1, countById(failId));
        assertEquals(0, countById(oldSuccessId));
        // 时间边界：10 天前创建的 SUCCESS 不在清理范围
        assertEquals(1, countById(recentSuccessId));
        // 分片边界：其他分片的 SUCCESS 不删
        assertEquals(1, countById(otherShardingSuccessId));
    }

    /**
     * limitRows 边界：5 条符合条件的 SUCCESS，limit=3，应恰好删除 3 条。
     */
    @Test
    public void deleteByGmtCreate_shouldRespectLimitRows() {
        int limit = 3;
        long[] ids = new long[5];
        for (int i = 0; i < ids.length; i++) {
            ids[i] = insertTask("batch-" + i, RetryTaskStatus.SUCCESS.getCode(), 40, TEST_SHARDING);
        }

        Date clearBeforeDate = new Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30));
        int deleted = retryTaskDao.deleteByGmtCreate(clearBeforeDate, limit,
                Collections.singletonList(TEST_SHARDING), RetryTaskStatus.SUCCESS.getCode());

        assertEquals(limit, deleted);
        for (int i = 0; i < limit; i++) {
            assertEquals(0, countById(ids[i]));
        }
        for (int i = limit; i < ids.length; i++) {
            assertEquals(1, countById(ids[i]));
        }
    }

    /**
     * 全链路：通过 RetryTaskAccess#deleteHistoryRetryTask（ClearTask 实际入口）触发清理，
     * 验证时间计算 + SUCCESS 状态码传递在真实 DB 下生效。
     */
    @Test
    public void deleteHistoryRetryTask_endToEnd_shouldOnlyDeleteOldSuccessInOwnSharding() {
        List<Long> originalSharding = com.smart.retry.core.ShardingContextHolder.shardingIndex();
        com.smart.retry.core.ShardingContextHolder.initShardingIndex(Collections.singletonList(TEST_SHARDING));
        try {
            long runningId = insertTask("e2e-running", RetryTaskStatus.RUNNING.getCode(), 40, TEST_SHARDING);
            long successId = insertTask("e2e-success", RetryTaskStatus.SUCCESS.getCode(), 40, TEST_SHARDING);
            long otherShardingId = insertTask("e2e-other", RetryTaskStatus.SUCCESS.getCode(), 40, OTHER_SHARDING);

            int deleted = retryTaskAccess.deleteHistoryRetryTask(30, 100);

            assertEquals(1, deleted);
            assertEquals(1, countById(runningId));
            assertEquals(0, countById(successId));
            assertEquals(1, countById(otherShardingId));
        } finally {
            // 恢复调用前的分片，避免影响容器调度线程
            com.smart.retry.core.ShardingContextHolder.initShardingIndex(originalSharding);
        }
    }

    private long insertTask(String suffix, int status, int daysAgo, long sharding) {
        String taskCode = TASK_CODE_PREFIX + suffix + "-" + System.nanoTime();
        jdbcTemplate.update(
                "INSERT INTO retry_task (gmt_create, gmt_modified, sharding_key, task_code, status, "
                        + "next_plan_time, retry_num, unique_key, task_desc) "
                        + "VALUES (DATE_SUB(NOW(3), INTERVAL ? DAY), NOW(3), ?, ?, ?, "
                        + "DATE_ADD(NOW(3), INTERVAL 365 DAY), 0, ?, 'it-clear-del')",
                daysAgo, sharding, taskCode, status, taskCode);
        Long id = jdbcTemplate.queryForObject("SELECT id FROM retry_task WHERE task_code = ?", Long.class, taskCode);
        return id == null ? -1 : id;
    }

    private int countById(long id) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM retry_task WHERE id = ?", Integer.class, id);
        return count == null ? 0 : count;
    }
}
