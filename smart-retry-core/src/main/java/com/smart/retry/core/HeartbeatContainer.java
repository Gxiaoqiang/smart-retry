package com.smart.retry.core;

import com.smart.retry.common.RetryContainer;
import com.smart.retry.common.RetryTaskHeart;
/**
 * @Author xiaoqiang
 * @Version HeartbeatContainer.java, v 0.1 2025年02月16日 10:50 xiaoqiang
 * @Description: TODO
 */
public class HeartbeatContainer implements RetryContainer {


    private RetryTaskHeart heart;


    public HeartbeatContainer(RetryTaskHeart heart) {
        this.heart = heart;
    }

    @Override
    public void start() {
        heart.initHeart();
        heart.heartBeat();
        heart.scrambleDeadSharding();
    }

    @Override
    public void destroy() {

    }
}
