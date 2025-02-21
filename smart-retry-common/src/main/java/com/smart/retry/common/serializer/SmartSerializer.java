package com.smart.retry.common.serializer;

import java.lang.reflect.Method;

/**
 * @Author xiaoqiang
 * @Version SmartSerializer.java, v 0.1 2025年02月11日 20:32 xiaoqiang
 * @Description: TODO
 */
public interface SmartSerializer {
    /**
     * 序列化,将方法的参数序列化为字符串，
     * 存储的格式是 Map<参数名称,参数对象 序列化>
     *     最后将Map 进行序列化
     * @param method
     * @param args
     * @return
     */
    String serializer(Method method, Object[] args);

    /**
     * 将方法的参数反序列化为参数对象，
     * @param method
     * @param serivlizerVal
     * @return
     */
    Object[] deSerializer(Method method,String serivlizerVal);

}
