package com.smart.retry.common;

import com.smart.retry.common.model.RetryTask;

import java.util.List;

/**
 * @Author xiaoqiang
 * @Version RetryTaskAcess.java, v 0.1 2025年02月12日 15:34 xiaoqiang
 * @Description: TODO
 */
public interface RetryTaskAccess {

    /**
     * 获取所有待重试任务
     * @return
     */
    List<RetryTask> listRetryTask();

    /**
     * 保存重试任务
     * @param retryTask
     * @return
     */
    void saveRetryTask(RetryTask retryTask);

    /**
     * 更新重试任务
     * @param retryTask
     */
    void updateRetryTask(RetryTask retryTask);


    /**
     * 删除重试任务
     * @param taskId
     */
    void deleteRetryTask(long taskId);


    /**
     * 停止重试任务
     * @param taskId
     */
    void stopRetryTask(long taskId);

}
