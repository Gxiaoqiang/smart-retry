package com.smart.retry.common;

/**
 * @Author xiaoqiang
 * @Version RetryCondition.java, v 0.1 2025年02月15日 17:10 xiaoqiang
 * @Description: TODO
 */
public interface RetryCondition {
    /**
     * 判断能否重试
     * @return
     */
    boolean needRetry();
}
