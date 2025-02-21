package com.smart.retry.common;

/**
 * @Author xiaoqiang
 * @Version RetryTaskHeart.java, v 0.1 2025年02月15日 22:16 xiaoqiang
 * @Description: TODO
 */
public interface RetryTaskHeart {

    /**
     * 初始化心跳
     */
    default void initHeart()     {
        // do nothing
    }

    /**
     * 心跳通知
     */
    default void heartBeat() {
        // do nothing
    }


    /**
     * 获取已经停止的分片信息
     */
    default void scrambleDeadSharding() {

    }
}
