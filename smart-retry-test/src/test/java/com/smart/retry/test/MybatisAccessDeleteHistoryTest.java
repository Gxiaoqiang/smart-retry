package com.smart.retry.test;

import com.smart.retry.common.constant.RetryTaskStatus;
import com.smart.retry.mybatis.access.MybatisAccess;
import com.smart.retry.mybatis.repo.RetryTaskRepo;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @Author xiaoqiang
 * @Version MybatisAccessDeleteHistoryTest.java, v 0.1 2026年08月27日 00:00 xiaoqiang
 * @Description: 验证 ClearTask 历史清理入口 MybatisAccess#deleteHistoryRetryTask 的参数计算。
 *
 * <p>回归点（修复前缺陷）：
 * <ul>
 *     <li>int 溢出：{@code clearBeforeDays * 24 * 60 * 60 * 1000} 为 int 运算，当
 *         beforeDays &gt;= 26 时溢出成负数，导致清理时间被算成“未来约 19.7 天”，
 *         进而 SQL 的 {@code gmt_create < ?} 会命中几乎全部任务（含执行中的任务）。</li>
 *     <li>未过滤状态：清理 SQL 需要只删除 SUCCESS 终态，此方法必须把
 *         {@link RetryTaskStatus#SUCCESS} 的状态码传给 Repo。</li>
 * </ul>
 */
public class MybatisAccessDeleteHistoryTest {

    private static final int LIMIT_ROWS = 100;

    private MybatisAccess buildAccess(RetryTaskRepo repo) {
        return new MybatisAccess(repo);
    }

    @Test
    public void deleteHistoryRetryTask_30days_shouldPassTime30DaysAgoNotFuture() {
        RetryTaskRepo repo = mock(RetryTaskRepo.class);
        MybatisAccess access = buildAccess(repo);

        long beforeCall = System.currentTimeMillis();
        access.deleteHistoryRetryTask(30, LIMIT_ROWS);
        long afterCall = System.currentTimeMillis();

        ArgumentCaptor<Date> dateCaptor = ArgumentCaptor.forClass(Date.class);
        verify(repo).deleteByGmtCreate(dateCaptor.capture(), eq(LIMIT_ROWS), eq(RetryTaskStatus.SUCCESS.getCode()));

        Date passed = dateCaptor.getValue();
        long passedMillis = passed.getTime();

        // 修复前：溢出后传入的时间约等于“现在 + 19.7 天”（未来）。修复后必须严格早于调用时刻。
        assertTrue("clearBeforeDate must be in the past, but was " + passed, passedMillis < beforeCall);

        // 且应约等于“30 天前”（允许少量执行耗时误差）。
        long expected = (beforeCall + afterCall) / 2 - TimeUnit.DAYS.toMillis(30);
        long diff = Math.abs(passedMillis - expected);
        assertTrue("clearBeforeDate should be ~30 days ago, actual diff=" + diff + "ms", diff < 10_000);
    }

    @Test
    public void deleteHistoryRetryTask_26days_shouldStillBePast() {
        // 修复前：26 天即开始 int 溢出（26 * 24 * 60 * 60 * 1000 > Integer.MAX_VALUE）。
        RetryTaskRepo repo = mock(RetryTaskRepo.class);
        MybatisAccess access = buildAccess(repo);

        access.deleteHistoryRetryTask(26, LIMIT_ROWS);

        ArgumentCaptor<Date> dateCaptor = ArgumentCaptor.forClass(Date.class);
        verify(repo).deleteByGmtCreate(dateCaptor.capture(), eq(LIMIT_ROWS), eq(RetryTaskStatus.SUCCESS.getCode()));

        long now = System.currentTimeMillis();
        Date passed = dateCaptor.getValue();
        assertTrue("clearBeforeDate must be in the past, but was " + passed, passed.getTime() < now);

        long expected = now - TimeUnit.DAYS.toMillis(26);
        long diff = Math.abs(passed.getTime() - expected);
        assertTrue("clearBeforeDate should be ~26 days ago, actual diff=" + diff + "ms", diff < 10_000);
    }

    @Test
    public void deleteHistoryRetryTask_25days_shouldStillBePast() {
        // 回归：25 天是 int 运算不溢出的最大边界，不应被破坏。
        RetryTaskRepo repo = mock(RetryTaskRepo.class);
        MybatisAccess access = buildAccess(repo);

        access.deleteHistoryRetryTask(25, LIMIT_ROWS);

        ArgumentCaptor<Date> dateCaptor = ArgumentCaptor.forClass(Date.class);
        verify(repo).deleteByGmtCreate(dateCaptor.capture(), eq(LIMIT_ROWS), eq(RetryTaskStatus.SUCCESS.getCode()));

        long now = System.currentTimeMillis();
        Date passed = dateCaptor.getValue();
        assertTrue("clearBeforeDate must be in the past, but was " + passed, passed.getTime() < now);

        long expected = now - TimeUnit.DAYS.toMillis(25);
        long diff = Math.abs(passed.getTime() - expected);
        assertTrue("clearBeforeDate should be ~25 days ago, actual diff=" + diff + "ms", diff < 10_000);
    }

    @Test
    public void deleteHistoryRetryTask_1day_shouldStillBePast() {
        RetryTaskRepo repo = mock(RetryTaskRepo.class);
        MybatisAccess access = buildAccess(repo);

        access.deleteHistoryRetryTask(1, LIMIT_ROWS);

        ArgumentCaptor<Date> dateCaptor = ArgumentCaptor.forClass(Date.class);
        verify(repo).deleteByGmtCreate(dateCaptor.capture(), eq(LIMIT_ROWS), eq(RetryTaskStatus.SUCCESS.getCode()));

        long now = System.currentTimeMillis();
        Date passed = dateCaptor.getValue();
        assertTrue("clearBeforeDate must be in the past, but was " + passed, passed.getTime() < now);
    }

    @Test
    public void deleteHistoryRetryTask_shouldPassSuccessStatus() {
        RetryTaskRepo repo = mock(RetryTaskRepo.class);
        MybatisAccess access = buildAccess(repo);

        access.deleteHistoryRetryTask(30, LIMIT_ROWS);

        verify(repo).deleteByGmtCreate(
                any(Date.class),
                eq(LIMIT_ROWS),
                eq(RetryTaskStatus.SUCCESS.getCode()));
    }

    @Test
    public void deleteHistoryRetryTask_shouldPassLimitRows() {
        int limitRows = 500;
        RetryTaskRepo repo = mock(RetryTaskRepo.class);
        MybatisAccess access = buildAccess(repo);

        access.deleteHistoryRetryTask(30, limitRows);

        verify(repo).deleteByGmtCreate(
                any(Date.class),
                eq(limitRows),
                eq(RetryTaskStatus.SUCCESS.getCode()));
    }
}
