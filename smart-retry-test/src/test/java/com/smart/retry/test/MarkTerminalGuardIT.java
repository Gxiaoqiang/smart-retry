package com.smart.retry.test;

import com.smart.retry.common.RetryTaskAccess;
import com.smart.retry.common.constant.RetryTaskStatus;
import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * @Author xiaoqiang
 * @Version MarkTerminalGuardIT.java, v 0.1 2026年08月29日 xiaoqiang
 * @Description: 验证 markRetryTaskTerminal 与 reviveDeadRetryTask 的乐观锁 CAS 守卫在真实 MySQL 下的行为。
 *
 * <p>覆盖以下场景：
 * <ul>
 *     <li><b>markTerminal 正常</b>：executor 与 retry_num（认领后扣减值）匹配 → 写入终态；</li>
 *     <li><b>markTerminal 守卫</b>：executor 不匹配返回 0；retry_num 不匹配返回 0，DB 保持 RUNNING；</li>
 *     <li><b>revive 正常</b>：RUNNING 且 gmt_modified 超时 → 复活为 WAITING + retry_num+1 + executor 清空；</li>
 *     <li><b>revive 守卫</b>：SUCCESS 终态返回 0；RUNNING 未超时返回 0，DB 保持不变。</li>
 * </ul>
 *
 * <p>依赖真实 MySQL（retry_task 库）。测试数据使用专用分片键 + task_code 前缀，
 * next_plan_time 设在远期避免 Producer 扫描干扰，@After 统一清理。
 */
public class MarkTerminalGuardIT extends AbstractTest {

    private static final String TASK_CODE_PREFIX = "it-markterminal-";
    private static final long TEST_SHARDING = 987_654_222L;

    @Autowired
    private RetryTaskAccess retryTaskAccess;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @After
    public void cleanup() {
        jdbcTemplate.update("DELETE FROM retry_task WHERE task_code LIKE ?", TASK_CODE_PREFIX + "%");
    }

    // ========== markTerminal ==========

    @Test
    public void markTerminal_whenExecutorAndRetryNumMatch_shouldWriteTerminal() {
        long id = insertTask(RetryTaskStatus.WAITING.getCode(), 3, TEST_SHARDING);

        // 先认领：retry_num 3 → 2，executor=ip-a
        assertEquals("认领应成功", 1,
                retryTaskAccess.claimRetryTask(id, "ip-a", new Date(), TEST_SHARDING));

        int marked = retryTaskAccess.markRetryTaskTerminal(
                id, RetryTaskStatus.SUCCESS.getCode(), "ip-a", 2, new Date(), null);
        assertEquals("租约匹配应写入终态", 1, marked);

        assertEquals("终态应为 SUCCESS", 2, queryInt("SELECT status FROM retry_task WHERE id = ?", id));
        assertEquals("retry_num 应保持认领后值 2", 2, queryInt("SELECT retry_num FROM retry_task WHERE id = ?", id));
    }

    @Test
    public void markTerminal_whenExecutorMismatch_shouldReturnZeroAndKeepRunning() {
        long id = insertTask(RetryTaskStatus.WAITING.getCode(), 3, TEST_SHARDING);
        assertEquals("认领应成功", 1,
                retryTaskAccess.claimRetryTask(id, "ip-a", new Date(), TEST_SHARDING));

        int marked = retryTaskAccess.markRetryTaskTerminal(
                id, RetryTaskStatus.SUCCESS.getCode(), "ip-other", 2, new Date(), null);
        assertEquals("executor 不匹配（新租约持有者不同）应返回 0", 0, marked);

        assertEquals("任务应保持 RUNNING", 1, queryInt("SELECT status FROM retry_task WHERE id = ?", id));
        assertEquals("executor 应保持认领方 ip-a", "ip-a",
                queryString("SELECT executor FROM retry_task WHERE id = ?", id));
    }

    @Test
    public void markTerminal_whenRetryNumMismatch_shouldReturnZeroAndKeepRunning() {
        long id = insertTask(RetryTaskStatus.WAITING.getCode(), 3, TEST_SHARDING);
        assertEquals("认领应成功", 1,
                retryTaskAccess.claimRetryTask(id, "ip-a", new Date(), TEST_SHARDING));

        int marked = retryTaskAccess.markRetryTaskTerminal(
                id, RetryTaskStatus.SUCCESS.getCode(), "ip-a", 5, new Date(), null);
        assertEquals("retry_num 与内存副本不符（DeadLetterTask 已 +1 等漂移）应返回 0", 0, marked);

        assertEquals("任务应保持 RUNNING", 1, queryInt("SELECT status FROM retry_task WHERE id = ?", id));
        assertEquals("retry_num 应保持认领后值 2", 2, queryInt("SELECT retry_num FROM retry_task WHERE id = ?", id));
    }

    // ========== reviveDeadRetryTask ==========

    @Test
    public void revive_whenRunningAndTimeout_shouldRevive() {
        long id = insertTask(RetryTaskStatus.RUNNING.getCode(), 3, TEST_SHARDING);
        // 模拟超时：gmt_modified 置为 1 小时前
        jdbcTemplate.update("UPDATE retry_task SET gmt_modified = DATE_SUB(NOW(3), INTERVAL 1 HOUR) WHERE id = ?", id);

        int revived = retryTaskAccess.reviveDeadRetryTask(id, new Date());
        assertEquals("RUNNING 且超时应复活", 1, revived);

        assertEquals("复活后状态应为 WAITING", 0, queryInt("SELECT status FROM retry_task WHERE id = ?", id));
        assertEquals("复活后 retry_num 应 +1 为 4", 4, queryInt("SELECT retry_num FROM retry_task WHERE id = ?", id));
        assertNull("复活后 executor 应清空", queryStringOrNull("SELECT executor FROM retry_task WHERE id = ?", id));
    }

    @Test
    public void revive_whenSuccess_shouldReturnZero() {
        long id = insertTask(RetryTaskStatus.SUCCESS.getCode(), 3, TEST_SHARDING);
        jdbcTemplate.update("UPDATE retry_task SET gmt_modified = DATE_SUB(NOW(3), INTERVAL 1 HOUR) WHERE id = ?", id);

        int revived = retryTaskAccess.reviveDeadRetryTask(id, new Date());
        assertEquals("SUCCESS 终态不可复活", 0, revived);

        assertEquals("任务应保持 SUCCESS", 2, queryInt("SELECT status FROM retry_task WHERE id = ?", id));
    }

    @Test
    public void revive_whenRunningButNotTimeout_shouldReturnZero() {
        long id = insertTask(RetryTaskStatus.RUNNING.getCode(), 3, TEST_SHARDING);
        // gmt_modified 保持 NOW（未超时），deadTaskTime 设为 10 分钟前
        Date deadTaskTime = new Date(System.currentTimeMillis() - 10 * 60 * 1000);

        int revived = retryTaskAccess.reviveDeadRetryTask(id, deadTaskTime);
        assertEquals("RUNNING 但未超时不可复活", 0, revived);

        assertEquals("任务应保持 RUNNING", 1, queryInt("SELECT status FROM retry_task WHERE id = ?", id));
        assertEquals("retry_num 应保持不变 3", 3, queryInt("SELECT retry_num FROM retry_task WHERE id = ?", id));
    }

    // ========== helpers ==========

    private long insertTask(int status, int retryNum, long sharding) {
        String taskCode = TASK_CODE_PREFIX + status + "-" + retryNum + "-" + System.nanoTime();
        jdbcTemplate.update(
                "INSERT INTO retry_task (gmt_create, gmt_modified, sharding_key, task_code, status, "
                        + "next_plan_time, retry_num, unique_key, task_desc) "
                        + "VALUES (NOW(3), NOW(3), ?, ?, ?, "
                        + "DATE_ADD(NOW(3), INTERVAL 365 DAY), ?, ?, 'it-markterminal')",
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

    private String queryStringOrNull(String sql, Object... args) {
        return jdbcTemplate.queryForObject(sql, String.class, args);
    }
}
