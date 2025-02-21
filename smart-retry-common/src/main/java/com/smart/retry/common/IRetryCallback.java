package com.smart.retry.common;


import com.smart.retry.common.constant.ExecuteResultStatus;

/**
 *
 * @Author gao.gwq
 * @Version IRetryCallback.java, v 0.1 2023年08月03日 23:31 gao.gwq
 * @Description:
 * 重试任务执行结束后回调方法,
 */
public interface IRetryCallback {

    /**
     *
     * @param resultStatus 本次重试执行的结果状态，成功、失败
     * @param result 本次重试执行的结果，如果没有返回则是null
     * @param args 本次重试的入参
     * 回调方法是独立的事务
     */
    void executeCallback(ExecuteResultStatus resultStatus, Object result, Object args);
}
