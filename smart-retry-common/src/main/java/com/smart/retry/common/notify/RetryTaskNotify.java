package com.smart.retry.common.notify;

import com.smart.retry.common.constant.ExecuteResultStatus;
import com.smart.retry.common.model.RetryTask;

import java.util.List;

/**
 * @Author xiaoqiang
 * @Version RetryTaskNotify.java, v 0.1 2025年02月12日 16:04 xiaoqiang
 * @Description: 执行通知接口
 */
public interface RetryTaskNotify {

    List<String> supportRetryTask();

    /**
     * 执行一次结果通知
     *
     */
    void oneTimeNotify(NotifyContext context);


    /**
     * 任务执行完成通知
     */
    void finishTaskNotify(NotifyContext context);


}
