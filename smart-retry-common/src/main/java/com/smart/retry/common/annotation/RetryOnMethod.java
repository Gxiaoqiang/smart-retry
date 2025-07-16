package com.smart.retry.common.annotation;

import com.smart.retry.common.IRetryCallback;
import com.smart.retry.common.constant.NextPlanTimeStrategyEnum;
import com.smart.retry.common.constant.RetryOccurType;
import com.smart.retry.common.notify.RetryTaskNotify;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @Author xiaoqiang
 * @Version Retryable.java, v 0.1 2025年02月12日 11:11 xiaoqiang
 * @Description: TODO
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)

public @interface RetryOnMethod {


    /**
     * 指定的异常会进行重试，如果不配置 所有的异常会重试
     * 不能和{exclude} 有重叠的异常类型配置
     * @return exception types to retry
     */
    Class<? extends Throwable>[] include() default {};


    /**
     * 指定重试的异常，如果不配置，所有的异常都会重试，如果配置，在配置中的异常不会重试。
     * 不能和 {include} 有重叠的异常类型配置
     * @return exception types not to retry
     */
    Class<? extends Throwable>[] exclude() default {};

    /**
     * 最大的重试次数
     * @return
     */
    int maxAttempt() default 1;


    /**
     * 第一次延迟时间，如果小于0，会先
     * @return
     */
    int firstDelaySecond() default 10;

    /**
     * 间隔时间
     * @return
     */
    int intervalSecond() default 300;


    /**
     * 时间间隔策略
     * 固定间隔
     * 增长间隔
     * 斐波那契间隔
     * @return
     */
    NextPlanTimeStrategyEnum nextPlanTimeStragy() default NextPlanTimeStrategyEnum.FIXED;

    /**
     * 任务执行结束后发起回调
     * 如果是多个回调则 按照默认配置的顺序进行执行，各个回调之间不相互影响
     * @example A->B->C 三个回调，B抛出异常，不影响C的执行，回调的方法事务是独立的，回调的事务和父方法相互影响
     * @return
     */
    Class<? extends IRetryCallback>[] retryCallback() default {};


    RetryOccurType occurType() default RetryOccurType.EXCEPTION;

    Class<? extends RetryTaskNotify>[] retryTaskNotifies() default {};

}
