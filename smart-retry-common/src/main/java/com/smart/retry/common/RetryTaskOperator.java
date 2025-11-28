package com.smart.retry.common;

import com.smart.retry.common.model.RetryTaskBuilder;

/**
 * @Author xiaoqiang
 * @Version RetryTaskCreator.java, v 0.1 2025年02月20日 17:37 xiaoqiang
 * @Description: TODO
 */
public interface RetryTaskOperator<T> {

    long createTask(RetryTaskBuilder<T> retryTaskBuilder);


    /**
     * 触发任务, 异步执行
     * 任务会放到队列中执行
     * @param taskId
     */
    void invokeTaskAsync(long taskId);


    /**
     * 触发任务, 同步执行，阻塞当前线程
     * 任务会放到队列中执行
     * @param taskId
     */
    void invokeTaskSync(long taskId);
}
