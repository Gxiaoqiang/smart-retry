package com.smart.retry.common;

import com.smart.retry.common.model.RetryTask;

/**
 * @Author xiaoqiang
 * @Version IExecutor.java, v 0.1 2025年02月11日 20:53 xiaoqiang
 * @Description: 执行器
 */
public interface IExecutor {

    /**
     * 任务发布
     * @param
     */
    void publishRetry();

    /**
     * 任务取消
     * @param retryTask
     */
    void killRetry(RetryTask retryTask);




}
