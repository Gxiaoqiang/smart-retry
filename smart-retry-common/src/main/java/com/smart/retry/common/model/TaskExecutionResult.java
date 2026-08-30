package com.smart.retry.common.model;

import com.smart.retry.common.constant.ExecuteResultStatus;

/**
 * 单次同步执行的结果。
 *
 * <p>{@code invokeTaskOnceSync} 执行完毕后返回本对象，
 * {@code status} 为本次执行状态（SUCCESS/FAIL），
 * {@code businessResult} 为业务方法/监听器返回值（可能为 null，如 void 方法或 consume 返回 null）。
 *
 * @param status        本次执行状态
 * @param businessResult 业务方法/监听器返回值，可为 null
 */
public record TaskExecutionResult(ExecuteResultStatus status, Object businessResult) {
}
