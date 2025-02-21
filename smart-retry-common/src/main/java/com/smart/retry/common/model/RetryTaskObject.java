package com.smart.retry.common.model;

import com.smart.retry.common.IRetryCallback;
import com.smart.retry.common.constant.RetryTaskTypeEnum;
import com.sun.corba.se.spi.protocol.RetryType;

import java.io.Serializable;
import java.lang.reflect.Method;

/**
 * @Author xiaoqiang
 * @Version RetryTaskInfo.java, v 0.1 2025年02月13日 19:26 xiaoqiang
 * @Description: 重试任务的信息
 */
public class RetryTaskObject implements Serializable {

    private static final long serialVersionUID = 1L;


    /**
     * 重试重试任务的名称，同一个项目下，名称是唯一的
     */
    private String taskCode;

    /**
     * 对象
     */
    private Object targetObj;

    /**
     * 代理的触发的方法
     */
    private Method method;

    /**
     * 未被代理的方法
     */
    private Method originalMethod;

    /**
     * 重试方法的参数列表
     */
    private Object[] params;

    /**
     *
     */
    private Class<? extends Throwable>[] includes;

    private Class<? extends Throwable>[] excludes;

    private Class<? extends IRetryCallback>[] retryCallback;


    private RetryTaskTypeEnum retryType;


    private RetryTaskObject() {

    }

    public static RetryTaskObject of(){
        return new RetryTaskObject();
    }
    public RetryTaskObject withTaskCode(String taskCode) {
        this.taskCode = taskCode;
        return this;
    }
    public RetryTaskObject withBeanObj(Object beanObj) {
        this.targetObj = beanObj;
        return this;
    }
    public RetryTaskObject withMethod(Method method) {
        this.method = method;
        return this;
    }
    public RetryTaskObject withOriginalMethod(Method originalMethod) {
        this.originalMethod = originalMethod;
        return this;
    }
    public RetryTaskObject withParams(Object[] params) {
        this.params = params;
        return this;
    }
    public RetryTaskObject withIncludes(Class<? extends Throwable>[] includes) {
        this.includes = includes;
        return this;
    }
    public RetryTaskObject withExcludes(Class<? extends Throwable>[] excludes) {
        this.excludes = excludes;
        return this;
    }
    public RetryTaskObject withRetryCallback(Class<? extends IRetryCallback>[] retryCallback) {
        this.retryCallback = retryCallback;
        return this;
    }

    public RetryTaskObject withRetryType(RetryTaskTypeEnum retryType) {
        this.retryType = retryType;
        return this;
    }


    public Object getTargetObj() {
        return targetObj;
    }



    public Method getMethod() {
        return method;
    }


    public Method getOriginalMethod() {
        return originalMethod;
    }


    public Object[] getParams() {
        return params;
    }



    public Class<? extends Throwable>[] getIncludes() {
        return includes;
    }



    public Class<? extends Throwable>[] getExcludes() {
        return excludes;
    }

    public void setExcludes(Class<? extends Throwable>[] excludes) {
        this.excludes = excludes;
    }

    public Class<? extends IRetryCallback>[] getRetryCallback() {
        return retryCallback;
    }

    public String getTaskCode() {
        return taskCode;
    }

    public RetryTaskTypeEnum getRetryType() {
        return retryType;
    }
}
