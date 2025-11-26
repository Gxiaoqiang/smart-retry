package com.smart.retry.core.handler;

import com.smart.retry.common.RetryCondition;
import com.smart.retry.common.RetryConfiguration;
import com.smart.retry.common.RetryHandler;
import com.smart.retry.common.annotation.RetryOnMethod;
import com.smart.retry.common.model.MethodChain;
import com.smart.retry.common.model.RetryAttemptContext;
import com.smart.retry.common.retry.IRetryer;
import com.smart.retry.core.DefaultRetryCondition;
import com.smart.retry.core.RetrySnapshot;
import com.smart.retry.core.retry.RemoteRetryer;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.ProxyMethodInvocation;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.lang.reflect.Method;

/**
 * @Author xiaoqiang
 * @Version DefaultRetryHandler.java, v 0.1 2025年02月14日 19:10 xiaoqiang
 * @Description: TODO
 */
public class DefaultRetryHandler implements RetryHandler {


    private MethodInvocation methodInvocation;

    private RetryConfiguration retryConfiguration;

    private RetryOnMethod retryable;

    public DefaultRetryHandler(RetryConfiguration retryConfiguration, MethodInvocation methodInvocation) {
        this.retryConfiguration = retryConfiguration;
        this.methodInvocation = methodInvocation;
    }

    @Override
    public Object retryHandler() throws Throwable {
        Method method = methodInvocation.getMethod();
        RetryOnMethod retryable = AnnotatedElementUtils.findMergedAnnotation(method, RetryOnMethod.class);
        //1、如果没有重注解，则直接执行方法
        if (retryable == null) {
            return methodInvocation.proceed();
        }
        this.retryable = retryable;
        //2、执行重试
        return retry();
    }

    private Object retry() throws Throwable {
        MethodChain methodChainModel = new MethodChain();
        methodChainModel.setMethod(methodInvocation.getMethod());

        RetryAttemptContext retryAttemptContext = initRetryAttemptContext();
        retryAttemptContext.setCurrentMethodChain(methodChainModel);
        Object result = null;
        try {
            RetrySnapshot.setInterceptorChain(methodChainModel);
            if(TransactionSynchronizationManager.isSynchronizationActive()){
                result = ((ProxyMethodInvocation)methodInvocation).invocableClone().proceed();

            }else{
                result = methodInvocation.proceed();
            }
            retryAttemptContext.setResult(result);
        } catch (Throwable ex) {
            methodChainModel.setThrowable(ex);
            retryAttemptContext.setThrowable(ex);
            //return processRetry( ex, methodChainModel);
        } /*finally {
            RetryInterceptorSnapot.removeInterceptorChain(methodChainModel.getMethod());
        }*/

        try {
            return processRetry(retryAttemptContext);
        } finally {
            RetrySnapshot.removeInterceptorChain(methodChainModel.getMethod());
        }
    }

    private Object processRetry(RetryAttemptContext retryAttemptContext) throws Throwable {
        //1、如果是被远程调用调用发起的重试，不会进行重试操作
        RetryCondition retryCondition = new DefaultRetryCondition(retryAttemptContext);
        boolean flag = retryCondition.needRetry();
        //如果需要重试，则执行重试
        if(flag){
            doRetry(retryAttemptContext);
        }
        //如果异常不为null直接抛出异常
        if (retryAttemptContext.getThrowable() != null) {
            throw retryAttemptContext.getThrowable();
        }
        //返回结果
        return retryAttemptContext.getResult();
    }

    /**
     * 初始化重试判断的上下文信息
     *
     * @return
     */
    private RetryAttemptContext initRetryAttemptContext() {
        RetryAttemptContext retryAttemptContext = new RetryAttemptContext();
        retryAttemptContext.setMethod(methodInvocation.getMethod());
        //retryAttemptContext.setRetryOccurType(retryable.retryOccurType());
        retryAttemptContext.setExcludes(retryable.exclude());
        retryAttemptContext.setIncludes(retryable.include());
        retryAttemptContext.setRetryOccurType(retryable.occurType());
        return retryAttemptContext;
    }

    /**
     * 1 、如果firstDelayTime  <= 0 立即进行本地重试
     *
     * @return
     */
    private void doRetry(RetryAttemptContext retryAttemptContext) throws Throwable {
        IRetryer<Object> retryer = new RemoteRetryer(retryConfiguration,methodInvocation, retryable, retryAttemptContext);
        retryer.retry();
    }
}
