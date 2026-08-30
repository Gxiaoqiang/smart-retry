package com.smart.retry.common.exception;

/**
 * @Author xiaoqiang
 * @Version RetryTaskClaimedException.java, v 0.1 2026年08月28日 xiaoqiang
 * @Description: 重试任务已被其他执行方认领（DB CAS 竞争失败）时抛出。
 *
 * <p>当多个执行方（同 JVM 的 delayQueue + 手动触发、或多实例的 Producer）
 * 同时尝试认领同一条任务时，数据库乐观锁（{@code status IN (0,3) AND retry_num >= 1}）
 * 保证至多一个调用方受影响行数为 1，其余调用方认领失败并抛出本异常，
 * 从而避免业务方法被重复执行。
 */
public class RetryTaskClaimedException extends RetryException {

    public RetryTaskClaimedException(Long taskId) {
        super("retry task has already been claimed by another executor, taskId:" + taskId);
    }

    public RetryTaskClaimedException(String message) {
        super(message);
    }
}
