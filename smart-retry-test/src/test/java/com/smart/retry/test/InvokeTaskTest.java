package com.smart.retry.test;

import com.smart.retry.common.RetryConfiguration;
import com.smart.retry.common.RetryTaskOperator;
import com.smart.retry.common.constant.ExecuteResultStatus;
import com.smart.retry.common.constant.NextPlanTimeStrategyEnum;
import com.smart.retry.common.constant.RetryTaskStatus;
import com.smart.retry.common.model.RetryTask;
import com.smart.retry.common.model.RetryTaskBuilder;
import com.smart.retry.common.model.TaskExecutionResult;
import com.smart.retry.core.ShardingContextHolder;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.TimeUnit;

/**
 * invokeTaskOnceSync / invokeTaskAsync 功能测试。
 *
 * <p>覆盖以下改动点:
 * <ul>
 *   <li>getTriggerableTask: WAITING + FAIL 状态允许触发</li>
 *   <li>invokeTaskOnceSync: 单次同步执行，返回 {@link TaskExecutionResult}</li>
 *   <li>invokeTaskAsync: 异步执行</li>
 *   <li>ConsumerTask: 收敛为单次执行，后续重试由 delayQueue 异步推进</li>
 * </ul>
 *
 * @Author xiaoqiang
 * @Version InvokeTaskTest.java, v 0.2 2025年08月27日 xiaoqiang
 */
public class InvokeTaskTest extends AbstractTest {

    @Autowired
    private RetryTaskOperator<TestParam> retryTaskOperator;

    @Autowired
    private InvokeTaskTestListener invokeTaskTestListener;

    @Autowired
    private RetryConfiguration retryConfiguration;

    private static final String TASK_CODE = "test-invoke-task";

    @Before
    public void setUp() {
        invokeTaskTestListener.reset(1);
    }

    @After
    public void tearDown() {
        invokeTaskTestListener.reset(1);
    }

    // ===================================================================
    // 一、getTriggerableTask 状态校验测试
    // ===================================================================

    /**
     * 1.1 WAITING 状态的任务可以被 invokeTaskOnceSync 触发，返回 SUCCESS
     */
    @Test
    public void testTriggerWaitingTask_OnceSync() throws Exception {
        RetryTaskBuilder<TestParam> builder = createTaskBuilder(3, 2, 10);
        long taskId = retryTaskOperator.createTask(builder);

        // 验证任务创建成功且状态为 WAITING
        RetryTask task = retryConfiguration.getRetryTaskAcess().getRetryTask(taskId);
        Assert.assertNotNull("任务应创建成功", task);
        Assert.assertEquals("状态应为 WAITING", RetryTaskStatus.WAITING.getCode(), task.getStatus());

        // 单次同步执行：立即返回本次结果
        TaskExecutionResult result = retryTaskOperator.invokeTaskOnceSync(taskId);
        Assert.assertNotNull("单次执行应返回结果", result);
        Assert.assertEquals("首次执行应成功", ExecuteResultStatus.SUCCESS, result.status());

        Assert.assertEquals("应执行1次", 1, invokeTaskTestListener.getExecuteCount());

        // 验证 DB 状态为 SUCCESS
        task = retryConfiguration.getRetryTaskAcess().getRetryTask(taskId);
        Assert.assertEquals("任务状态应为 SUCCESS",
                RetryTaskStatus.SUCCESS.getCode(), task.getStatus());
    }

    /**
     * 1.2 FAIL 状态的任务可以被 invokeTaskOnceSync 触发（修复后允许）
     */
    @Test
    public void testTriggerFailTask_OnceSync() throws Exception {
        // 设置首次执行失败
        invokeTaskTestListener.setShouldSucceed(false);
        // 使用较大 delay 让 createTask 自动入队的调度任务落在测试窗口之外，避免干扰
        RetryTaskBuilder<TestParam> builder = createTaskBuilder(3, 60, 10);
        long taskId = retryTaskOperator.createTask(builder);

        // 第一次执行：返回 FAIL
        TaskExecutionResult first = retryTaskOperator.invokeTaskOnceSync(taskId);
        Assert.assertNotNull("首次执行应返回结果", first);
        Assert.assertEquals("首次执行应失败", ExecuteResultStatus.FAIL, first.status());

        // 验证任务状态变为 FAIL
        RetryTask task = retryConfiguration.getRetryTaskAcess().getRetryTask(taskId);
        Assert.assertEquals("第一次执行后状态应为 FAIL", RetryTaskStatus.FAIL.getCode(), task.getStatus());

        // 重新设置监听器为成功
        invokeTaskTestListener.setShouldSucceed(true);

        // 第二次执行：从 FAIL 状态触发（修复前会被拒绝）
        TaskExecutionResult second = retryTaskOperator.invokeTaskOnceSync(taskId);
        Assert.assertNotNull("FAIL 状态的任务应能被触发执行", second);
        Assert.assertEquals("第二次执行应成功", ExecuteResultStatus.SUCCESS, second.status());

        Assert.assertEquals("总共应执行2次", 2, invokeTaskTestListener.getExecuteCount());
    }

    /**
     * 1.3 SUCCESS 状态的任务不能被触发
     */
    @Test
    public void testTriggerSuccessTask_Rejected() throws Exception {
        RetryTaskBuilder<TestParam> builder = createTaskBuilder(3, 2, 10);
        long taskId = retryTaskOperator.createTask(builder);

        // 第一次执行成功
        TaskExecutionResult first = retryTaskOperator.invokeTaskOnceSync(taskId);
        Assert.assertNotNull(first);
        Assert.assertEquals(ExecuteResultStatus.SUCCESS, first.status());

        // 验证状态为 SUCCESS
        RetryTask task = retryConfiguration.getRetryTaskAcess().getRetryTask(taskId);
        Assert.assertEquals("状态应为 SUCCESS", RetryTaskStatus.SUCCESS.getCode(), task.getStatus());

        // 重置计数器，尝试再次触发：getTriggerableTask 应拒绝 SUCCESS 状态
        invokeTaskTestListener.reset(1);

        TaskExecutionResult second = retryTaskOperator.invokeTaskOnceSync(taskId);
        Assert.assertNull("SUCCESS 状态的任务触发应返回 null", second);
        Assert.assertEquals("SUCCESS 状态的任务不应再被执行", 0, invokeTaskTestListener.getExecuteCount());
    }

    /**
     * 1.4 retryNum 耗尽的任务不能被触发
     */
    @Test
    public void testTriggerExhaustedRetryNum_Rejected() throws Exception {
        // retryNum=1 的任务，执行一次后次数归零
        RetryTaskBuilder<TestParam> builder = createTaskBuilder(1, 2, 10);
        long taskId = retryTaskOperator.createTask(builder);

        // 执行一次（retryNum: 1→0）
        TaskExecutionResult first = retryTaskOperator.invokeTaskOnceSync(taskId);
        Assert.assertNotNull(first);
        Assert.assertEquals(ExecuteResultStatus.SUCCESS, first.status());

        RetryTask task = retryConfiguration.getRetryTaskAcess().getRetryTask(taskId);
        Assert.assertEquals("retryNum 应为0", Integer.valueOf(0), task.getRetryNum());

        // 尝试再次触发：getTriggerableTask 应拒绝
        invokeTaskTestListener.reset(1);
        TaskExecutionResult second = retryTaskOperator.invokeTaskOnceSync(taskId);
        Assert.assertNull("retryNum=0 的任务触发应返回 null", second);
        Assert.assertEquals("retryNum=0 的任务不应再执行", 0, invokeTaskTestListener.getExecuteCount());
    }

    // ===================================================================
    // 二、invokeTaskAsync 异步执行测试
    // ===================================================================

    /**
     * 2.1 异步执行：任务提交后立即返回，异步完成执行
     */
    @Test
    public void testInvokeAsync_TaskExecutesAsynchronously() throws Exception {
        RetryTaskBuilder<TestParam> builder = createTaskBuilder(3, 2, 10);
        long taskId = retryTaskOperator.createTask(builder);

        long startTime = System.currentTimeMillis();
        retryTaskOperator.invokeTaskAsync(taskId);
        long elapsed = System.currentTimeMillis() - startTime;

        // invokeTaskAsync 应立即返回（不阻塞）
        Assert.assertTrue("invokeTaskAsync 应立即返回（<3秒）", elapsed < 3000);

        // 任务应在合理时间内异步执行
        boolean executed = invokeTaskTestListener.awaitExecution(15, TimeUnit.SECONDS);
        Assert.assertTrue("异步任务应在15秒内执行", executed);
        Assert.assertEquals("应执行1次", 1, invokeTaskTestListener.getExecuteCount());
    }

    /**
     * 2.2 FAIL 状态任务也可以异步触发
     */
    @Test
    public void testInvokeAsync_FailTaskCanBeTriggered() throws Exception {
        invokeTaskTestListener.setShouldSucceed(false);
        // 使用较大 delay 让 createTask 自动入队的调度任务落在测试窗口之外
        RetryTaskBuilder<TestParam> builder = createTaskBuilder(3, 60, 10);
        long taskId = retryTaskOperator.createTask(builder);

        // 第一次执行失败
        TaskExecutionResult first = retryTaskOperator.invokeTaskOnceSync(taskId);
        Assert.assertNotNull(first);
        Assert.assertEquals(ExecuteResultStatus.FAIL, first.status());

        // 验证 FAIL 状态
        RetryTask task = retryConfiguration.getRetryTaskAcess().getRetryTask(taskId);
        Assert.assertEquals("状态应为 FAIL", RetryTaskStatus.FAIL.getCode(), task.getStatus());

        // 异步触发 FAIL 任务
        invokeTaskTestListener.setShouldSucceed(true);
        invokeTaskTestListener.reset(1);

        retryTaskOperator.invokeTaskAsync(taskId);
        boolean executed = invokeTaskTestListener.awaitExecution(15, TimeUnit.SECONDS);
        Assert.assertTrue("FAIL 状态的异步任务应能执行", executed);
    }

    // ===================================================================
    // 三、单次执行 + delayQueue 异步重试全链路
    // ===================================================================

    /**
     * 4.1 单次执行 + delayQueue 异步重试全链路。
     *
     * <p>invokeTaskOnceSync 只执行一轮并返回 FAIL；
     * 失败后的剩余重试由 afterExecute 放入 DelayQueue，调度器异步推进，
     * 直到 retryNum 耗尽（终态）。
     *
     * <p>说明：此处直接插入任务而非走 createTask，是为了绕过 createTask 的自动入队，
     * 避免"手动触发 + 自动调度"产生重复执行窗口（既有问题），保证断言确定性。
     */
    @Test
    public void testSingleExecution_AsyncRetryUntilTerminal() throws Exception {
        // 注意顺序：reset() 会重置 shouldSucceed 为 true，必须先 reset 再设置 false
        invokeTaskTestListener.reset(3);  // retryNum=3，预期 3 次执行
        invokeTaskTestListener.setShouldSucceed(false);

        long taskId = insertRetryTask(3, 2, 2);

        // 单次同步执行：立即返回 FAIL
        TaskExecutionResult result = retryTaskOperator.invokeTaskOnceSync(taskId);
        Assert.assertNotNull("单次执行应返回结果", result);
        Assert.assertEquals("首次执行应为 FAIL", ExecuteResultStatus.FAIL, result.status());

        // 等待 delayQueue 驱动的剩余重试执行完毕（共 3 次）
        boolean executed = invokeTaskTestListener.awaitExecution(30, TimeUnit.SECONDS);
        Assert.assertTrue("任务应在30秒内完成全部重试", executed);
        Assert.assertEquals("应执行3次（retryNum=3）", 3, invokeTaskTestListener.getExecuteCount());

        // 终态：retryNum 耗尽
        RetryTask task = retryConfiguration.getRetryTaskAcess().getRetryTask(taskId);
        Assert.assertEquals("retryNum 应为0", Integer.valueOf(0), task.getRetryNum());
    }

    /**
     * 4.2 异步模式下 afterExecute 保持原有行为（不持久化 WAITING，放入 delayQueue）
     */
    @Test
    public void testAfterExecute_AsyncMode_KeepsOriginalBehavior() throws Exception {
        // 注意顺序：reset() 会重置 shouldSucceed 为 true，必须先 reset 再设置 false
        invokeTaskTestListener.reset(1);
        invokeTaskTestListener.setShouldSucceed(false);

        // 使用较大 delay，让 createTask 自动入队及失败重试的调度时间落在测试窗口之外，
        // 避免 FAIL 任务在后续测试执行期间被调度器重复执行，干扰其他测试的计数断言
        RetryTaskBuilder<TestParam> builder = createTaskBuilder(3, 60, 10);
        long taskId = retryTaskOperator.createTask(builder);

        // 异步执行
        retryTaskOperator.invokeTaskAsync(taskId);
        invokeTaskTestListener.awaitExecution(15, TimeUnit.SECONDS);

        // 给 DB 写入留出余量，避免读取到 RUNNING 中间态
        Thread.sleep(500);

        // 异步模式下 afterExecute 不写 DB（delayQueue.put）
        // DB 状态由 DefaultInnovation 的 finally 写入（FAIL）
        RetryTask task = retryConfiguration.getRetryTaskAcess().getRetryTask(taskId);
        Assert.assertEquals("异步模式失败后状态应为 FAIL",
                RetryTaskStatus.FAIL.getCode(), task.getStatus());
    }

    // ===================================================================
    // 四、边界条件测试
    // ===================================================================

    /**
     * 5.1 不存在的 taskId 应静默返回
     */
    @Test
    public void testNonexistentTaskId_SilentReturn() throws Exception {
        long fakeTaskId = 99999999L;

        // invokeTaskOnceSync 应返回 null
        TaskExecutionResult result = retryTaskOperator.invokeTaskOnceSync(fakeTaskId);
        Assert.assertNull("不存在的任务应返回 null", result);

        // invokeTaskAsync 应静默返回
        retryTaskOperator.invokeTaskAsync(fakeTaskId);

        // 不应有任务被执行
        Assert.assertEquals("不应执行任何任务", 0, invokeTaskTestListener.getExecuteCount());
    }

    /**
     * 5.2 RUNNING 状态的任务不能被触发（防止重复执行）
     */
    @Test
    public void testTriggerRunningTask_Rejected() throws Exception {
        RetryTaskBuilder<TestParam> builder = createTaskBuilder(3, 2, 10);
        long taskId = retryTaskOperator.createTask(builder);

        // 手动设置状态为 RUNNING（模拟其他线程正在执行）
        RetryTask task = retryConfiguration.getRetryTaskAcess().getRetryTask(taskId);
        task.setStatus(RetryTaskStatus.RUNNING.getCode());
        retryConfiguration.getRetryTaskAcess().updateRetryTask(task);

        // 尝试触发
        TaskExecutionResult result = retryTaskOperator.invokeTaskOnceSync(taskId);
        Assert.assertNull("RUNNING 状态的任务触发应返回 null", result);
        Assert.assertEquals("RUNNING 状态的任务不应执行", 0, invokeTaskTestListener.getExecuteCount());
    }

    /**
     * 5.3 taskCode 未注册（RetryCache 中不存在）时的处理。
     * getTriggerableTask 不检查 RetryCache，所以会通过；
     * ConsumerTask 执行时发现 taskObject==null，更新状态为 FAIL，不抛异常。
     */
    @Test
    public void testUnregisteredTaskCode_HandledGracefully() throws Exception {
        // 获取有效的分片 key（如果分片未初始化，使用 0）
        Long shardingKey;
        try {
            shardingKey = com.smart.retry.core.ShardingContextHolder.getRandomShardingIndex();
        } catch (Exception e) {
            shardingKey = 0L;
        }

        // 直接通过 DB 插入一个 taskCode 不存在的任务
        RetryTask fakeTask = new RetryTask();
        fakeTask.setTaskCode("non-existent-code-" + System.currentTimeMillis());
        fakeTask.setParameters("{\"name\":\"test\"}");
        fakeTask.setRetryNum(1);
        fakeTask.setDelaySecond(2);
        fakeTask.setIntervalSecond(5);
        fakeTask.setNextPlanTimeStrategy(NextPlanTimeStrategyEnum.FIXED.getCode());
        fakeTask.setStatus(RetryTaskStatus.WAITING.getCode());
        fakeTask.setOriginRetryNum(1);
        fakeTask.setCreator("test");
        fakeTask.setUniqueKey("unregistered-" + System.nanoTime());
        fakeTask.setShardingKey(shardingKey);
        fakeTask.setNextPlanTime(new java.util.Date(System.currentTimeMillis() + 1000));

        long taskId = retryConfiguration.getRetryTaskAcess().saveRetryTask(fakeTask);

        // invokeTaskOnceSync：不应崩溃，返回 FAIL 结果
        TaskExecutionResult result = retryTaskOperator.invokeTaskOnceSync(taskId);
        Assert.assertNotNull("未注册任务也应返回结果", result);
        Assert.assertEquals("未注册 taskCode 的任务应标记为 FAIL",
                ExecuteResultStatus.FAIL, result.status());

        // 验证任务状态已被更新为 FAIL（因为 taskObject 为 null）
        RetryTask task = retryConfiguration.getRetryTaskAcess().getRetryTask(taskId);
        Assert.assertNotNull("任务应存在", task);
        Assert.assertEquals("未注册 taskCode 的任务应标记为 FAIL",
                RetryTaskStatus.FAIL.getCode(), task.getStatus());
    }

    // ===================================================================
    // 辅助方法
    // ===================================================================

    private RetryTaskBuilder<TestParam> createTaskBuilder(int retryNum, int delaySecond, int intervalSecond) {
        TestParam param = new TestParam();
        // 每次调用生成唯一参数，确保 uniqueKey 唯一。
        // 若多个任务共享同一 uniqueKey，saveRetryTask 的去重逻辑
        // （同 uniqueKey + taskCode 且状态为 WAITING/RUNNING 时返回 -1）会拦截插入，
        // 导致 createTask 返回 -1、getRetryTask(-1) 查不到任务。
        param.setValue("test-value-" + System.nanoTime());
        param.setIndex(1);

        return RetryTaskBuilder.<TestParam>of()
                .withTaskCode(TASK_CODE)
                .withTaskDesc("InvokeTaskTest")
                .withRetryNum(retryNum)
                .withDelaySecond(delaySecond)
                .withIntervalSecond(intervalSecond)
                .withNextPlanTimeStrategy(NextPlanTimeStrategyEnum.FIXED)
                .withParam(param);
    }

    /**
     * 直接插入一个重试任务（绕过 createTask 的自动入队），返回任务 ID。
     */
    private long insertRetryTask(int retryNum, int delaySecond, int intervalSecond) {
        RetryTask task = new RetryTask();
        task.setTaskCode(TASK_CODE);
        task.setParameters("{\"value\":\"test-value\",\"index\":1}");
        task.setRetryNum(retryNum);
        task.setDelaySecond(delaySecond);
        task.setIntervalSecond(intervalSecond);
        task.setNextPlanTimeStrategy(NextPlanTimeStrategyEnum.FIXED.getCode());
        task.setStatus(RetryTaskStatus.WAITING.getCode());
        task.setOriginRetryNum(retryNum);
        task.setCreator("test");
        task.setUniqueKey("once-sync-chain-" + System.nanoTime());
        task.setShardingKey(ShardingContextHolder.getRandomShardingIndex());
        return retryConfiguration.getRetryTaskAcess().saveRetryTask(task);
    }
}
