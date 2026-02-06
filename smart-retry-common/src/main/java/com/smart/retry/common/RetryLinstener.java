package com.smart.retry.common;

import com.smart.retry.common.constant.ExecuteResultStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @Author xiaoqiang
 * @Version RetryLinstener.java, v 0.1 2025年02月14日 10:30 xiaoqiang
 * @Description: TODO
 */
public interface RetryLinstener<T> {



    /**
     * 消费
     * 返回或者SUCCESS或者null表示消费成功，
     * 抛异常或者FAIL表示消费失败
     * @param param
     * @return
     */
     ExecuteResultStatus consume(T param);

    default void beforeConsume(T context) {

    }

    default void  afterConsume(ExecuteResultStatus consumeStatus, T param) {

    }
}
