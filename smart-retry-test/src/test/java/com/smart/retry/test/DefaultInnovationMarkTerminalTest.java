package com.smart.retry.test;

import com.smart.retry.common.RetryConfiguration;
import com.smart.retry.common.RetryLinstener;
import com.smart.retry.common.RetryTaskAccess;
import com.smart.retry.common.constant.ExecuteResultStatus;
import com.smart.retry.common.constant.NextPlanTimeStrategyEnum;
import com.smart.retry.common.constant.RetryTaskStatus;
import com.smart.retry.common.constant.RetryTaskTypeEnum;
import com.smart.retry.common.model.RetryTask;
import com.smart.retry.common.model.RetryTaskObject;
import com.smart.retry.common.notify.NotifyContext;
import com.smart.retry.common.notify.RetryTaskNotify;
import com.smart.retry.core.cache.RetryCache;
import com.smart.retry.core.innovation.DefaultInnovation;
import org.junit.After;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @Author xiaoqiang
 * @Version DefaultInnovationMarkTerminalTest.java, v 0.1 2026年08月29日 xiaoqiang
 * @Description: 验证 DefaultInnovation 终态写入（markRetryTaskTerminal）与通知的关系。
 *
 * <p>覆盖以下改动点：
 * <ul>
 *     <li>markRetryTaskTerminal 返回 1（租约有效）→ 通知被触发（oneTimeNotify + allRetryTaskFinishNotify）；</li>
 *     <li>markRetryTaskTerminal 返回 0（任务被复活/转交）→ 业务仍执行一次，但通知被抑制；</li>
 *     <li>taskCode 未注册（taskObject == null）→ 走 processNullTaskObject，调用 markNullTaskObjectFail
 *         且传入扣减前 retryNum，业务不被执行。</li>
 * </ul>
 */
public class DefaultInnovationMarkTerminalTest {

    /** 计数型通知实现，供 DefaultInnovation 反射实例化并统计调用。 */
    public static class CountingNotify implements RetryTaskNotify {
        static final AtomicInteger ONCE = new AtomicInteger();
        static final AtomicInteger FINISH = new AtomicInteger();

        @Override
        public void oneTimeNotify(NotifyContext context) {
            ONCE.incrementAndGet();
        }

        @Override
        public void allRetryTaskFinishNotify(NotifyContext context) {
            FINISH.incrementAndGet();
        }

        static void reset() {
            ONCE.set(0);
            FINISH.set(0);
        }
    }

    private static final String TASK_CODE = "it-mark-terminal-" + System.nanoTime();

    @After
    public void cleanup() {
        RetryCache.remove(TASK_CODE);
        CountingNotify.reset();
    }

    /**
     * 场景一：markTerminal 返回 1（仍持有租约）→ 通知正常触发。
     * 认领后 retryNum=0，应同时触发 oneTimeNotify 与 allRetryTaskFinishNotify。
     */
    @Test
    public void invoke_whenMarkTerminalSucceeds_shouldTriggerNotify() throws Throwable {
        RetryLinstener<TestParam> listener = mock(RetryLinstener.class);
        when(listener.consume(any())).thenReturn(ExecuteResultStatus.SUCCESS);

        RetryTaskObject taskObject = RetryTaskObject.of()
                .withTaskCode(TASK_CODE)
                .withBeanObj(listener)
                .withRetryType(RetryTaskTypeEnum.CLASS)
                .withRetryTaskNotify(new Class[]{CountingNotify.class});
        RetryCache.put(TASK_CODE, taskObject);

        RetryTaskAccess access = mock(RetryTaskAccess.class);
        when(access.claimRetryTask(anyLong(), anyString(), any(Date.class), anyLong())).thenReturn(1);
        when(access.markRetryTaskTerminal(anyLong(), anyInt(), anyString(), anyInt(), any(Date.class), anyString()))
                .thenReturn(1);

        RetryConfiguration config = mock(RetryConfiguration.class);
        when(config.getRetryTaskAcess()).thenReturn(access);

        RetryTask task = new RetryTask();
        task.setId(100L);
        task.setTaskCode(TASK_CODE);
        task.setStatus(RetryTaskStatus.WAITING.getCode());
        task.setRetryNum(1);
        task.setIntervalSecond(10);
        task.setNextPlanTimeStrategy(NextPlanTimeStrategyEnum.FIXED.getCode());
        task.setNextPlanTime(new Date());
        task.setShardingKey(1L);

        Object result = new DefaultInnovation(task, config).invoke();

        assertEquals("业务执行应返回 SUCCESS", ExecuteResultStatus.SUCCESS, result);
        assertEquals("终态写入成功应触发 oneTimeNotify", 1, CountingNotify.ONCE.get());
        assertEquals("认领后 retryNum=0 应触发 allRetryTaskFinishNotify", 1, CountingNotify.FINISH.get());
        assertEquals("内存任务终态应为 SUCCESS", RetryTaskStatus.SUCCESS.getCode(), task.getStatus());
    }

    /**
     * 场景二：markTerminal 返回 0（任务被复活/转交他人）→ 业务仍执行一次，但通知被抑制。
     */
    @Test
    public void invoke_whenMarkTerminalFails_shouldSuppressNotifyButExecuteBusiness() throws Throwable {
        RetryLinstener<TestParam> listener = mock(RetryLinstener.class);
        when(listener.consume(any())).thenReturn(ExecuteResultStatus.SUCCESS);

        RetryTaskObject taskObject = RetryTaskObject.of()
                .withTaskCode(TASK_CODE)
                .withBeanObj(listener)
                .withRetryType(RetryTaskTypeEnum.CLASS)
                .withRetryTaskNotify(new Class[]{CountingNotify.class});
        RetryCache.put(TASK_CODE, taskObject);

        RetryTaskAccess access = mock(RetryTaskAccess.class);
        when(access.claimRetryTask(anyLong(), anyString(), any(Date.class), anyLong())).thenReturn(1);
        when(access.markRetryTaskTerminal(anyLong(), anyInt(), anyString(), anyInt(), any(Date.class), anyString()))
                .thenReturn(0);

        RetryConfiguration config = mock(RetryConfiguration.class);
        when(config.getRetryTaskAcess()).thenReturn(access);

        RetryTask task = new RetryTask();
        task.setId(200L);
        task.setTaskCode(TASK_CODE);
        task.setStatus(RetryTaskStatus.WAITING.getCode());
        task.setRetryNum(3);
        task.setIntervalSecond(10);
        task.setNextPlanTimeStrategy(NextPlanTimeStrategyEnum.FIXED.getCode());
        task.setNextPlanTime(new Date());
        task.setShardingKey(1L);

        Object result = new DefaultInnovation(task, config).invoke();

        assertEquals("业务执行应返回 SUCCESS（不因终态写入失败而抛异常）", ExecuteResultStatus.SUCCESS, result);
        verify(listener).consume(any());
        assertEquals("终态写入失败不应触发 oneTimeNotify", 0, CountingNotify.ONCE.get());
        assertEquals("终态写入失败不应触发 allRetryTaskFinishNotify", 0, CountingNotify.FINISH.get());
    }

    /**
     * 场景三：taskCode 未注册 → processNullTaskObject，调用 markNullTaskObjectFail（传扣减前 retryNum），
     * 业务不被执行，invoke 返回 null。
     */
    @Test
    public void invoke_whenTaskObjectNull_shouldMarkFailViaNullTaskObjectGuard() throws Throwable {
        // 故意不注册 taskCode，使 RetryCache.get(taskCode) 返回 null
        RetryTaskAccess access = mock(RetryTaskAccess.class);
        when(access.markNullTaskObjectFail(anyLong(), anyString(), anyInt(), anyString())).thenReturn(1);

        RetryConfiguration config = mock(RetryConfiguration.class);
        when(config.getRetryTaskAcess()).thenReturn(access);

        RetryTask task = new RetryTask();
        task.setId(300L);
        task.setTaskCode(TASK_CODE);
        task.setStatus(RetryTaskStatus.WAITING.getCode());
        task.setRetryNum(3);
        task.setIntervalSecond(10);
        task.setNextPlanTimeStrategy(NextPlanTimeStrategyEnum.FIXED.getCode());
        task.setNextPlanTime(new Date());
        task.setShardingKey(1L);

        Object result = new DefaultInnovation(task, config).invoke();

        assertNull("未注册 taskCode 时 invoke 应返回 null", result);

        ArgumentCaptor<Long> idCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<String> executorCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Integer> retryNumCaptor = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<String> attrCaptor = ArgumentCaptor.forClass(String.class);
        verify(access).markNullTaskObjectFail(
                idCaptor.capture(), executorCaptor.capture(), retryNumCaptor.capture(), attrCaptor.capture());
        assertEquals("应传入任务 id", Long.valueOf(300L), idCaptor.getValue());
        assertEquals("应传入扣减前 retryNum", Integer.valueOf(3), retryNumCaptor.getValue());
        assertEquals("应传入失败原因", "taskObject is null", attrCaptor.getValue());
        assertNotNull("executor 应为本实例 IP", executorCaptor.getValue());

        assertEquals("内存任务状态应为 FAIL", RetryTaskStatus.FAIL.getCode(), task.getStatus());
        assertEquals("内存 retryNum 应扣减为 2", Integer.valueOf(2), task.getRetryNum());
        verify(access, never()).markRetryTaskTerminal(anyLong(), anyInt(), anyString(), anyInt(), any(Date.class), anyString());
    }
}
