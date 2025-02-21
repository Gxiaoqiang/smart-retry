package com.smart.retry.common;

/**
 * @Author xiaoqiang
 * @Version RetryHandler.java, v 0.1 2025年02月14日 19:05 xiaoqiang
 * @Description: TODO
 */
public interface RetryHandler {
    /**
     * 重试方法的处理
     * 1、如果方法上没有注解 有 {@link com.smart.retry.common.annotation.RetryOnMethod} ，说明不是重试的方法，则直接执行对应的方法
     * 2、如果方法有注解 {@link com.smart.retry.common.annotation.RetryOnMethod}，先执行方法，如果执行成功，不进行重试，如果抛出指定的异常
     * @return
     */
    Object retryHandler()throws Throwable;
}
