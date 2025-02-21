package com.smart.retry.common.model;

import com.smart.retry.common.annotation.RetryOnMethod;
import com.smart.retry.common.constant.RetryOccurType;

import java.io.Serializable;
import java.lang.reflect.Method;

/**
 * @Author xiaoqiang
 * @Version RetryAttemptContext.java, v 0.1 2025年02月15日 16:02 xiaoqiang
 * @Description: TODO
 */
public class RetryAttemptContext implements Serializable {

    private static final long serialVersionUID = 1L;
    /**
     * 重试方法
     */
    private Method method;

    /**
     * 方法执行的结果
     */
    private Object result;

    /**
     * 方法抛出的异常
     */
    private Throwable throwable;


    private RetryOnMethod retryable;


    private Class<? extends Throwable>[] includes;

    private Class<? extends Throwable>[] excludes;


    private RetryOccurType retryOccurType;


    private MethodChain currentMethodChain;

    public MethodChain getCurrentMethodChain() {
        return currentMethodChain;
    }

    public void setCurrentMethodChain(MethodChain currentMethodChain) {
        this.currentMethodChain = currentMethodChain;
    }

    public RetryOccurType getRetryOccurType() {
        return retryOccurType;
    }

    public void setRetryOccurType(RetryOccurType retryOccurType) {
        this.retryOccurType = retryOccurType;
    }

    public Class<? extends Throwable>[] getIncludes() {
        return includes;
    }

    public void setIncludes(Class<? extends Throwable>[] includes) {
        this.includes = includes;
    }

    public Class<? extends Throwable>[] getExcludes() {
        return excludes;
    }

    public void setExcludes(Class<? extends Throwable>[] excludes) {
        this.excludes = excludes;
    }

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    public Throwable getThrowable() {
        return throwable;
    }

    public void setThrowable(Throwable throwable) {
        this.throwable = throwable;
    }


    public RetryOnMethod getRetryable() {
        return retryable;
    }

    public void setRetryable(RetryOnMethod retryable) {
        this.retryable = retryable;
    }
}
