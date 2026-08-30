package com.smart.retry.test;

import com.smart.retry.common.RetryTaskAccess;
import com.smart.retry.common.constant.RetryTaskStatus;
import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Date;

import static org.junit.Assert.assertEquals;

/**
 * @Author xiaoqiang
 * @Version ClaimTaskIT.java, v 0.1 2026年08月28日 xiaoqiang
 * @Description: 验证 claimRetryTask（DB 乐观锁 CAS）在真实 MySQL 下的行为。
 *
 * <p>覆盖以下场景：
 * <ul>
 *     <li><b>CAS 直测</b>：同一 WAITING 任务连续认领两次，第一次返回 1、第二次返回 0，
 *         DB 状态变为 RUNNING(1)、retry_num 减一、executor 写入；</li>
 *     <li><b>守卫用例</b>：SUCCESS 终态返回 0；retry_num=0 返回 0；sharding_key 不符返回 0。</li>
 * </ul>
 *
 * <p>依赖真实 MySQL（retry_task 库）。测试数据使用专用分片键 + task_code 前缀，
 * 且 next_plan_time 设在远期（365 天）避免 Producer 扫描干扰，@After 统一清理。
 */
public class ClaimTaskIT extends AbstractTest {

    private static final String TASK_CODE_PREFIX = "it-claim-";
    private static final long TEST_SHARDING = 987_654_000L;

    @Autowired
    private RetryTaskAccess retryTaskAccess;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @After
    public void cleanup() {
        jdbcTemplate.update("DELETE FROM retry_task WHERE task_code LIKE ?", TASK_CODE_PREFIX + "%");
    }

    /**
     * CAS 直测：同一 WAITING 任务连续认领两次，第一次 1、第二次 0；
     * DB 状态变为 RUNNING、retry_num 减一、executor 写入。
     */
    @Test
    public void claimRetryTask_waitingTask_firstWinsSecondLoses() {
        long id = insertTask(RetryTaskStatus.WAITING.getCode(), 3, TEST_SHARDING);

        Date nextPlanTime = new Date(System.currentTimeMillis() + 60_000);
        int first = retryTaskAccess.claimRetryTask(id, "ip-a", nextPlanTime, TEST_SHARDING);
        assertEquals("第一次认领应成功", 1, first);

        int second = retryTaskAccess.claimRetryTask(id, "ip-b", nextPlanTime, TEST_SHARDING);
        assertEquals("第二次认领应失败（状态已为 RUNNING）", 0, second);

        // 重读 DB 断言
        assertEquals("状态应为 RUNNING", 1, queryInt("SELECT status FROM retry_task WHERE id = ?", id));
        assertEquals("retry_num 应减一", 2, queryInt("SELECT retry_num FROM retry_task WHERE id = ?", id));
        assertEquals("executor 应写入第一次认领方", "ip-a",
                queryString("SELECT executor FROM retry_task WHERE id = ?", id));
    }

    /**
     * 守卫：SUCCESS 终态任务不可认领。
     */
    @Test
    public void claimRetryTask_successTask_shouldReturnZero() {
        long id = insertTask(RetryTaskStatus.SUCCESS.getCode(), 3, TEST_SHARDING);
        int claimed = retryTaskAccess.claimRetryTask(id, "ip", new Date(), TEST_SHARDING);
        assertEquals("SUCCESS 终态任务认领应失败", 0, claimed);
    }

    /**
     * 守卫：retry_num = 0 的任务不可认领（重试次数已耗尽）。
     */
    @Test
    public void claimRetryTask_exhaustedRetryNum_shouldReturnZero() {
        long id = insertTask(RetryTaskStatus.WAITING.getCode(), 0, TEST_SHARDING);
        int claimed = retryTaskAccess.claimRetryTask(id, "ip", new Date(), TEST_SHARDING);
        assertEquals("retry_num=0 的任务认领应失败", 0, claimed);
    }

    /**
     * 守卫：传入的 sharding_key 与任务实际分片不符时不可认领，且任务保持原状态。
     */
    @Test
    public void claimRetryTask_wrongShardingKey_shouldReturnZero() {
        long id = insertTask(RetryTaskStatus.WAITING.getCode(), 3, TEST_SHARDING);
        int claimed = retryTaskAccess.claimRetryTask(id, "ip", new Date(), TEST_SHARDING + 1);
        assertEquals("sharding_key 不符认领应失败", 0, claimed);

        // 任务应保持原状态（未被篡改）
        assertEquals("状态应保持 WAITING",
                RetryTaskStatus.WAITING.getCode().intValue(), queryInt("SELECT status FROM retry_task WHERE id = ?", id));
        assertEquals("retry_num 应保持 3", 3, queryInt("SELECT retry_num FROM retry_task WHERE id = ?", id));
    }

    private long insertTask(int status, int retryNum, long sharding) {
        String taskCode = TASK_CODE_PREFIX + status + "-" + retryNum + "-" + System.nanoTime();
        jdbcTemplate.update(
                "INSERT INTO retry_task (gmt_create, gmt_modified, sharding_key, task_code, status, "
                        + "next_plan_time, retry_num, unique_key, task_desc) "
                        + "VALUES (NOW(3), NOW(3), ?, ?, ?, "
                        + "DATE_ADD(NOW(3), INTERVAL 365 DAY), ?, ?, 'it-claim')",
                sharding, taskCode, status, retryNum, taskCode);
        Long id = jdbcTemplate.queryForObject("SELECT id FROM retry_task WHERE task_code = ?", Long.class, taskCode);
        return id == null ? -1 : id;
    }

    private int queryInt(String sql, Object... args) {
        Integer value = jdbcTemplate.queryForObject(sql, Integer.class, args);
        return value == null ? 0 : value;
    }

    private String queryString(String sql, Object... args) {
        return jdbcTemplate.queryForObject(sql, String.class, args);
    }
}
