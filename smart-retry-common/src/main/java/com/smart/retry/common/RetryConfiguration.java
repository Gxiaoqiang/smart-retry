package com.smart.retry.common;

import com.smart.retry.common.identifier.Identifier;
import com.smart.retry.common.innovation.SmartInnovation;
import com.smart.retry.common.serializer.SmartSerializer;

/**
 * @Author xiaoqiang
 * @Version RetryConfiguration.java, v 0.1 2025年02月14日 21:15 xiaoqiang
 * @Description: TODO
 */
public interface RetryConfiguration {
    /**
     * 获取重试任务的访问器
     * @return
     */
    RetryTaskAccess getRetryTaskAcess();
    /**
     * 获取重试任务的标识符
     * @return
     *
     */
    Identifier getIdentifier();


    /**
     * 获取序列化器
     *
     * @return
     */
    SmartSerializer getSmartSerializer();







}
