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
     * 获取所有死任务 执行时间超过设置的最大的执行时间
     * @return
     */
    List<RetryTask> listDeadTask(int maxExecuteTime);
    /**
     * 获取所有待重试任务
     * @return
     */
    List<RetryTask> listRetryTask();


    RetryTask getRetryTask(long taskId);

    /**
     * 保存重试任务
     * @param retryTask
     * @return
     */
    long saveRetryTask(RetryTask retryTask);

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

    /**
     * 删除历史的重试任务
     * @param clearBeforeDays 多少天之前的任务
     * @param limitRows 每次限制删除的条数
     * @return
     */

    int  deleteHistoryRetryTask(int clearBeforeDays, int limitRows);

}
