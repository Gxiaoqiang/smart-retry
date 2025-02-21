package com.smart.retry.common;

import com.smart.retry.common.model.RetryTaskBuilder;

/**
 * @Author xiaoqiang
 * @Version RetryTaskCreator.java, v 0.1 2025年02月20日 17:37 xiaoqiang
 * @Description: TODO
 */
public interface RetryTaskCreator<T> {

    void createTask(RetryTaskBuilder<T> retryTaskBuilder);

}
