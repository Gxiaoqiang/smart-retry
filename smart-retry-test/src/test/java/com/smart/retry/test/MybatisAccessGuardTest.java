package com.smart.retry.test;

import com.smart.retry.mybatis.access.MybatisAccess;
import com.smart.retry.mybatis.entity.RetryTaskDO;
import com.smart.retry.mybatis.repo.RetryTaskRepo;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @Author xiaoqiang
 * @Version MybatisAccessGuardTest.java, v 0.1 2026年08月29日 xiaoqiang
 * @Description: 验证 MybatisAccess 三个条件化守卫方法的参数传播与返回值透传。
 *
 * <p>覆盖以下改动点：
 * <ul>
 *     <li>markRetryTaskTerminal：委托 Repo 并透传受影响行数，字段（id/status/executor/retryNum/nextPlanTime/attribute）正确传播；</li>
 *     <li>markNullTaskObjectFail：字段（id/executor/retryNum/attribute）正确传播；</li>
 *     <li>reviveDeadRetryTask：id 与 deadTaskTime 正确传播。</li>
 * </ul>
 */
public class MybatisAccessGuardTest {

    private MybatisAccess buildAccess(RetryTaskRepo repo) {
        return new MybatisAccess(repo);
    }

    @Test
    public void markRetryTaskTerminal_shouldDelegateAndReturnAffectedRows() {
        RetryTaskRepo repo = mock(RetryTaskRepo.class);
        when(repo.markRetryTaskTerminal(any(RetryTaskDO.class))).thenReturn(1);
        MybatisAccess access = buildAccess(repo);

        Date nextPlanTime = new Date();
        int result = access.markRetryTaskTerminal(123L, 2, "ip-a", 0, nextPlanTime, "err");

        assertEquals("终态写入成功应返回 1", 1, result);

        ArgumentCaptor<RetryTaskDO> captor = ArgumentCaptor.forClass(RetryTaskDO.class);
        verify(repo).markRetryTaskTerminal(captor.capture());

        RetryTaskDO passed = captor.getValue();
        assertEquals("id 应正确传播", Long.valueOf(123L), passed.getId());
        assertEquals("status 应正确传播", 2, passed.getStatus());
        assertEquals("executor 应正确传播", "ip-a", passed.getExecutor());
        assertEquals("retryNum 应正确传播", 0, passed.getRetryNum());
        assertEquals("nextPlanTime 应正确传播", nextPlanTime, passed.getNextPlanTime());
        assertEquals("attribute 应正确传播", "err", passed.getAttribute());
    }

    @Test
    public void markRetryTaskTerminal_whenRepoReturnsZero_shouldPropagateZero() {
        RetryTaskRepo repo = mock(RetryTaskRepo.class);
        when(repo.markRetryTaskTerminal(any(RetryTaskDO.class))).thenReturn(0);
        MybatisAccess access = buildAccess(repo);

        int result = access.markRetryTaskTerminal(1L, 3, "ip-a", 1, new Date(), null);

        assertEquals("守卫未通过应返回 0", 0, result);
        verify(repo).markRetryTaskTerminal(any(RetryTaskDO.class));
    }

    @Test
    public void markNullTaskObjectFail_shouldDelegateAndReturnAffectedRows() {
        RetryTaskRepo repo = mock(RetryTaskRepo.class);
        when(repo.markNullTaskObjectFail(any(RetryTaskDO.class))).thenReturn(1);
        MybatisAccess access = buildAccess(repo);

        int result = access.markNullTaskObjectFail(456L, "ip-a", 3, "taskObject is null");

        assertEquals("失败标记写入成功应返回 1", 1, result);

        ArgumentCaptor<RetryTaskDO> captor = ArgumentCaptor.forClass(RetryTaskDO.class);
        verify(repo).markNullTaskObjectFail(captor.capture());

        RetryTaskDO passed = captor.getValue();
        assertEquals("id 应正确传播", Long.valueOf(456L), passed.getId());
        assertEquals("executor 应正确传播", "ip-a", passed.getExecutor());
        assertEquals("retryNum（扣减前）应正确传播", 3, passed.getRetryNum());
        assertEquals("attribute 应正确传播", "taskObject is null", passed.getAttribute());
    }

    @Test
    public void reviveDeadRetryTask_shouldDelegateAndReturnAffectedRows() {
        RetryTaskRepo repo = mock(RetryTaskRepo.class);
        when(repo.reviveDeadRetryTask(any(Long.class), any(Date.class))).thenReturn(1);
        MybatisAccess access = buildAccess(repo);

        Date deadTaskTime = new Date();
        int result = access.reviveDeadRetryTask(789L, deadTaskTime);

        assertEquals("复活成功应返回 1", 1, result);

        ArgumentCaptor<Long> idCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Date> timeCaptor = ArgumentCaptor.forClass(Date.class);
        verify(repo).reviveDeadRetryTask(idCaptor.capture(), timeCaptor.capture());

        assertEquals("id 应正确传播", Long.valueOf(789L), idCaptor.getValue());
        assertEquals("deadTaskTime 应正确传播", deadTaskTime, timeCaptor.getValue());
    }

    @Test
    public void reviveDeadRetryTask_whenRepoReturnsZero_shouldPropagateZero() {
        RetryTaskRepo repo = mock(RetryTaskRepo.class);
        when(repo.reviveDeadRetryTask(any(Long.class), any(Date.class))).thenReturn(0);
        MybatisAccess access = buildAccess(repo);

        int result = access.reviveDeadRetryTask(1L, new Date());

        assertEquals("守卫未通过应返回 0", 0, result);
        verify(repo).reviveDeadRetryTask(any(Long.class), any(Date.class));
    }

    @Test
    public void markRetryTaskTerminal_nullAttribute_shouldStillPropagateOtherFields() {
        RetryTaskRepo repo = mock(RetryTaskRepo.class);
        when(repo.markRetryTaskTerminal(any(RetryTaskDO.class))).thenReturn(1);
        MybatisAccess access = buildAccess(repo);

        Date nextPlanTime = new Date();
        int result = access.markRetryTaskTerminal(999L, 2, "ip-a", 1, nextPlanTime, null);

        assertEquals("应返回 1", 1, result);

        ArgumentCaptor<RetryTaskDO> captor = ArgumentCaptor.forClass(RetryTaskDO.class);
        verify(repo).markRetryTaskTerminal(captor.capture());
        RetryTaskDO passed = captor.getValue();
        assertEquals("id 应正确传播", Long.valueOf(999L), passed.getId());
        assertNotNull("nextPlanTime 不应为 null", passed.getNextPlanTime());
        assertEquals("attribute 为 null 应透传", null, passed.getAttribute());
    }
}
