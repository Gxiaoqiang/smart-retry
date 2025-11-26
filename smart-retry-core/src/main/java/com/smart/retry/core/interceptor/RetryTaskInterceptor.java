package com.smart.retry.core.interceptor;

import com.smart.retry.common.RetryConfiguration;
import com.smart.retry.common.RetryHandler;
import com.smart.retry.core.handler.DefaultRetryHandler;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.IntroductionInterceptor;

/**
 * @Author xiaoqiang
 * @Version RetryTaskInterceptor.java, v 0.1 2025年02月14日 19:04 xiaoqiang
 * @Description: TODO
 */
public class RetryTaskInterceptor implements IntroductionInterceptor {

    private RetryConfiguration retryConfiguration;

    public RetryTaskInterceptor(RetryConfiguration retryConfiguration) {
        this.retryConfiguration = retryConfiguration;
    }

    @Override
    public Object invoke(MethodInvocation methodInvocation) throws Throwable {
        RetryHandler retryHandler = new DefaultRetryHandler(retryConfiguration,methodInvocation);
        return retryHandler.retryHandler();

        //RetryConfiguration retryConfiguration = methodInvocation.getThis().getClass().getAnnotation(RetryConfiguration.class);
    }

    @Override
    public boolean implementsInterface(Class<?> aClass) {
        return false;
    }
}
