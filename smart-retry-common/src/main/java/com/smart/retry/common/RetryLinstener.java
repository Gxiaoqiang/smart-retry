package com.smart.retry.common;

import com.smart.retry.common.constant.ExecuteResultStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @Author xiaoqiang
 * @Version RetryLinstener.java, v 0.1 2025年02月14日 10:30 xiaoqiang
 * @Description: TODO
 */
public abstract  class RetryLinstener<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RetryLinstener.class);

    final public ExecuteResultStatus consumeProcess(T param) {
        try {
            beforeConsume(param);
        } catch (Exception ex) {
            LOGGER.error("retry-task listener beforeConsume {}", ex.getMessage(), ex);
        }

        ExecuteResultStatus consumeStatus = null;
        try {
            consumeStatus = consume(param);
            return consumeStatus;
        } finally {
            afterConsume(consumeStatus, param);
        }
    }

    public abstract ExecuteResultStatus consume(T param);

    public void beforeConsume(T context) {

    }

    public void afterConsume(ExecuteResultStatus consumeStatus, T param) {

    }
}
