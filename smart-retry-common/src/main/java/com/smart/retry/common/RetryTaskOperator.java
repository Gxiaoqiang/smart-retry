package com.smart.retry.common;

import com.smart.retry.common.model.RetryTaskBuilder;
import com.smart.retry.common.model.TaskExecutionResult;

/**
 * 重试任务操作接口，提供任务的创建、异步触发和同步触发能力。
 *
 * <p>使用方式：
 * <pre>{@code
 * // 1. 创建任务（参与调用方事务，与业务同事务持久化）
 * long taskId = operator.createTask(builder);
 *
 * // 2. 异步触发（提交到线程池，立即返回）
 * operator.invokeTaskAsync(taskId);
 *
 * // 3. 同步触发一次（当前线程阻塞执行，仅执行一次）
 * TaskExecutionResult result = operator.invokeTaskOnceSync(taskId);
 * //    result != null 表示真实执行了一次；失败后由调度器异步推进后续重试
 * }</pre>
 *
 * <p><b>事务说明：</b>
 * {@code invokeTaskOnceSync} 内部的数据库操作
 * 会参与调用方事务。如需事务隔离，请在调用前确保 {@link #createTask(RetryTaskBuilder)}
 * 的事务已提交，或使用 {@code @Transactional(propagation = REQUIRES_NEW)} 包裹触发方法。
 *
 * @Author xiaoqiang
 * @Version RetryTaskCreator.java, v 0.1 2025年02月20日 17:37 xiaoqiang
 * @Description: 重试任务操作接口
 */
public interface RetryTaskOperator<T> {

    /**
     * 创建重试任务并持久化到数据库。
     * 任务在调用方事务内创建（REQUIRED 传播），与业务操作保持同事务一致性。
     *
     * @param retryTaskBuilder 任务构建器，包含 taskCode、参数、重试次数、策略等
     * @return 任务 ID
     */
    long createTask(RetryTaskBuilder<T> retryTaskBuilder);

    /**
     * 触发任务，异步执行。
     * 任务提交到线程池后立即返回，不阻塞调用方线程。
     *
     * @param taskId 任务 ID
     */
    void invokeTaskAsync(long taskId);

    /**
     * 触发任务，同步执行一次。
     * 在当前线程中直接执行，阻塞直到本次执行完成。
     * 仅执行一次，执行失败后的后续重试由调度器（DelayQueue/Producer）异步推进。
     *
     * @param taskId 任务 ID
     * @return 本次执行结果（含业务返回值）；null 表示本次未执行
     *         （任务不存在/状态不允许/重试次数耗尽/被去重拦截）
     */
    TaskExecutionResult invokeTaskOnceSync(long taskId);
}
