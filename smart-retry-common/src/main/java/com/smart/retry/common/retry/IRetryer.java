package com.smart.retry.common.retry;

/**
 * @Author xiaoqiang
 * @Version IRetryer.java, v 0.1 2025年02月12日 16:27 xiaoqiang
 * @Description: TODO
 */
public interface IRetryer<R> {

    R retry()throws Throwable;
}
