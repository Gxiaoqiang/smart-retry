package com.smart.retry.common.notify;

/**
 * @Author xiaoqiang
 * @Version RetryTaskNotify.java, v 0.1 2025年02月12日 16:04 xiaoqiang
 * @Description: 执行通知接口
 */
public interface RetryTaskNotify {


    /**
     * 每执行一次任务后通知
     *
     */
    void oneTimeNotify(NotifyContext context);


    /**
     * 任务执行次数达到设置的最大次数后通知
     */
    void allRetryTaskFinishNotify(NotifyContext context);


}
