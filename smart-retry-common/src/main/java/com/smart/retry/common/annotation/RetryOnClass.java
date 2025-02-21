package com.smart.retry.common.annotation;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * @Author xiaoqiang
 * @Version RetryOnClass.java, v 0.1 2025年02月14日 09:52 xiaoqiang
 * @Description: TODO
 */
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@Component
public @interface RetryOnClass {

    /**
     * 任务名称
     *
     * @return
     */
    String taskCode();

    String taskDesc() default "";


}
