package com.smart.retry.common.model;

import java.lang.reflect.Method;

/**
 *
 * 记录调用链信息 只有方法上添加了重试的注解才会记录
 * 1、存放在threadLocal中，只有最外层(order == 0)的才会removeThreadLocal
 *
 * @example
 *    A-->B-->C 比如类似这样的重试调用关系
 *
 *  如果是CMethod 发生异常，被aMethod和bMethod 拦截住，则aMethod和bMethod 不会进行重试，只对cMethod进行重试
 *
 * @Author gao.gwq
 * @Version MethodChainModel.java, v 0.1 2023年08月29日 16:16 gao.gwq
 */
public class MethodChain {

    /**
     * 重试调用调用链方法
     */
    private Method method;


    /**
     *
     */
    private Throwable throwable;

    /**
     * 是否重试
     */
    private boolean isRetry = false;


    private boolean header;


    private boolean tail;

    public boolean isHeader() {
        return header;
    }

    public void setHeader(boolean header) {
        this.header = header;
    }

    public boolean isTail() {
        return tail;
    }

    public void setTail(boolean tail) {
        this.tail = tail;
    }

    private MethodChain next;

    public MethodChain getNext() {
        return next;
    }

    public void setNext(MethodChain next) {
        this.next = next;
    }

    public boolean isRetry() {
        return isRetry;
    }

    public void setRetry(boolean retry) {
        isRetry = retry;
    }

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }


    public Throwable getThrowable() {
        return throwable;
    }

    public void setThrowable(Throwable throwable) {
        this.throwable = throwable;
    }
}
