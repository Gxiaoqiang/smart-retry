package com.smart.retry.test;

import com.smart.retry.common.RetryConfiguration;
import com.smart.retry.common.RetryLinstener;
import com.smart.retry.common.RetryTaskAccess;
import com.smart.retry.common.constant.NextPlanTimeStrategyEnum;
import com.smart.retry.common.constant.RetryTaskStatus;
import com.smart.retry.common.constant.RetryTaskTypeEnum;
import com.smart.retry.common.exception.RetryTaskClaimedException;
import com.smart.retry.common.model.RetryTask;
import com.smart.retry.common.model.RetryTaskObject;
import com.smart.retry.core.cache.RetryCache;
import com.smart.retry.core.innovation.DefaultInnovation;
import org.junit.After;
import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @Author xiaoqiang
 * @Version DefaultInnovationClaimLostTest.java, v 0.1 2026年08月28日 xiaoqiang
 * @Description: 验证 DefaultInnovation 认领失败（CAS 返回 0）时的行为。
 *
 * <p>覆盖以下改动点：
 * <ul>
 *     <li>claimRetryTask 返回 0 时抛 {@link RetryTaskClaimedException}；</li>
 *     <li>业务 listener.consume 绝不被执行；</li>
 *     <li>认领失败发生在任何内存变更之前，任务对象仍保持 WAITING、retryNum 不变、executor 为空。</li>
 * </ul>
 */
public class DefaultInnovationClaimLostTest {

    private static final String TASK_CODE = "it-claim-lost-" + System.nanoTime();

    @After
    public void cleanup() {
        RetryCache.remove(TASK_CODE);
    }

    @Test
    public void invoke_whenClaimFails_shouldThrowClaimedExceptionAndSkipBusiness() throws Throwable {
        // 1. 注册 taskObject（唯一 taskCode，cleanup 中 remove）
        RetryLinstener<TestParam> listener = mock(RetryLinstener.class);
        RetryTaskObject taskObject = RetryTaskObject.of()
                .withTaskCode(TASK_CODE)
                .withBeanObj(listener)
                .withRetryType(RetryTaskTypeEnum.CLASS);
        RetryCache.put(TASK_CODE, taskObject);

        // 2. mock access.claimRetryTask -> 0（认领竞争失败）
        RetryTaskAccess access = mock(RetryTaskAccess.class);
        when(access.claimRetryTask(anyLong(), anyString(), any(Date.class), anyLong())).thenReturn(0);

        RetryConfiguration config = mock(RetryConfiguration.class);
        when(config.getRetryTaskAcess()).thenReturn(access);

        // 3. 构造 WAITING 任务（需满足 processNextExecuteTime 所需字段）
        RetryTask task = new RetryTask();
        task.setId(100L);
        task.setTaskCode(TASK_CODE);
        task.setStatus(RetryTaskStatus.WAITING.getCode());
        task.setRetryNum(3);
        task.setIntervalSecond(10);
        task.setNextPlanTimeStrategy(NextPlanTimeStrategyEnum.FIXED.getCode());
        task.setNextPlanTime(new Date());
        task.setShardingKey(1L);

        // 4. 认领失败应抛出 RetryTaskClaimedException，且不执行业务
        assertThrows(RetryTaskClaimedException.class,
                () -> new DefaultInnovation(task, config).invoke());

        // 5. 业务 consume 绝不被执行
        verify(listener, never()).consume(any());

        // 6. 内存任务对象未被变更（认领失败在任何内存变更之前）
        assertEquals("状态应保持 WAITING", RetryTaskStatus.WAITING.getCode(), task.getStatus());
        assertEquals("retryNum 应保持不变", Integer.valueOf(3), task.getRetryNum());
        assertNull("executor 不应被写入", task.getExecutor());
    }
}
