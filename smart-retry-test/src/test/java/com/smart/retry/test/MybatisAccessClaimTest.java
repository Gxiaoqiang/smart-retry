package com.smart.retry.test;

import com.smart.retry.mybatis.access.MybatisAccess;
import com.smart.retry.mybatis.entity.RetryTaskDO;
import com.smart.retry.mybatis.repo.RetryTaskRepo;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @Author xiaoqiang
 * @Version MybatisAccessClaimTest.java, v 0.1 2026年08月28日 xiaoqiang
 * @Description: 验证 MybatisAccess#claimRetryTask 的参数传播与返回值透传。
 *
 * <p>覆盖以下改动点：
 * <ul>
 *     <li>claimRetryTask 只设置 id/executor/nextPlanTime/shardingKey 四个字段到 RetryTaskDO；</li>
 *     <li>委托 RetryTaskRepo#claimRetryTask 并原样透传受影响行数（1 / 0）。</li>
 * </ul>
 */
public class MybatisAccessClaimTest {

    private MybatisAccess buildAccess(RetryTaskRepo repo) {
        return new MybatisAccess(repo);
    }

    @Test
    public void claimRetryTask_shouldDelegateAndReturnAffectedRows() {
        RetryTaskRepo repo = mock(RetryTaskRepo.class);
        when(repo.claimRetryTask(any(RetryTaskDO.class))).thenReturn(1);
        MybatisAccess access = buildAccess(repo);

        Long id = 123L;
        String executor = "192.168.1.10";
        Date nextPlanTime = new Date();
        Long shardingKey = 99L;

        int result = access.claimRetryTask(id, executor, nextPlanTime, shardingKey);

        assertEquals("认领成功应返回 1", 1, result);

        ArgumentCaptor<RetryTaskDO> captor = ArgumentCaptor.forClass(RetryTaskDO.class);
        verify(repo).claimRetryTask(captor.capture());

        RetryTaskDO passed = captor.getValue();
        assertEquals("id 应正确传播", id, passed.getId());
        assertEquals("executor 应正确传播", executor, passed.getExecutor());
        assertEquals("nextPlanTime 应正确传播", nextPlanTime, passed.getNextPlanTime());
        assertEquals("shardingKey 应正确传播", shardingKey.longValue(), passed.getShardingKey());
    }

    @Test
    public void claimRetryTask_whenRepoReturnsZero_shouldPropagateZero() {
        RetryTaskRepo repo = mock(RetryTaskRepo.class);
        when(repo.claimRetryTask(any(RetryTaskDO.class))).thenReturn(0);
        MybatisAccess access = buildAccess(repo);

        int result = access.claimRetryTask(1L, "192.168.1.10", new Date(), 99L);

        assertEquals("认领失败应返回 0", 0, result);
        verify(repo).claimRetryTask(any(RetryTaskDO.class));
    }
}
