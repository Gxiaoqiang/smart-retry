package com.smart.retry.core;

import com.smart.retry.common.RetryConfiguration;
import com.smart.retry.common.annotation.RetryOnMethod;
import com.smart.retry.core.interceptor.RetryTaskInterceptor;
import org.aopalliance.aop.Advice;
import org.springframework.aop.ClassFilter;
import org.springframework.aop.IntroductionAdvisor;
import org.springframework.aop.MethodMatcher;
import org.springframework.aop.Pointcut;
import org.springframework.aop.support.AbstractPointcutAdvisor;
import org.springframework.aop.support.ComposablePointcut;
import org.springframework.aop.support.StaticMethodMatcherPointcut;
import org.springframework.aop.support.annotation.AnnotationClassFilter;
import org.springframework.aop.support.annotation.AnnotationMethodMatcher;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @Author xiaoqiang
 * @Version RetryTaskAdvisor.java, v 0.1 2025年02月14日 19:30 xiaoqiang
 * @Description: TODO
 */
public class RetryTaskAdvisor extends AbstractPointcutAdvisor implements IntroductionAdvisor, BeanFactoryAware, InitializingBean {

    private RetryConfiguration retryConfiguration;

    private Advice advice;

    private Pointcut pointcut;

    private BeanFactory beanFactory;

    public RetryTaskAdvisor(RetryConfiguration retryConfiguration) {
        this.retryConfiguration = retryConfiguration;
    }

    @Override
    public ClassFilter getClassFilter() {
        return this.pointcut.getClassFilter();
    }

    @Override
    public void validateInterfaces() throws IllegalArgumentException {
    }

    @Override
    public Class<?>[] getInterfaces() {
        return new Class[0];
    }

    @Override
    public Pointcut getPointcut() {
        return pointcut;
    }

    @Override
    public Advice getAdvice() {
        return advice;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    @Override
    public void afterPropertiesSet() {

        Set<Class<? extends Annotation>> retryableAnnotationTypes = new LinkedHashSet<Class<? extends Annotation>>(1);
        retryableAnnotationTypes.add(RetryOnMethod.class);
        this.pointcut = buildPointcut(retryableAnnotationTypes);
        this.advice = buildAdvice();
        if (this.advice instanceof BeanFactoryAware) {
            ((BeanFactoryAware)this.advice).setBeanFactory(this.beanFactory);
        }
    }

    protected Advice buildAdvice() {
        RetryTaskInterceptor interceptor = new RetryTaskInterceptor(retryConfiguration);
        return interceptor;
    }

    protected Pointcut buildPointcut(Set<Class<? extends Annotation>> retryAnnotationTypes) {
        ComposablePointcut result = null;
        for (Class<? extends Annotation> retryAnnotationType : retryAnnotationTypes) {
            Pointcut filter = new AnnotationClassOrMethodPointcut(retryAnnotationType);
            if (result == null) {
                result = new ComposablePointcut(filter);
            } else {
                result.union(filter);
            }
        }
        return result;
    }

    private final class AnnotationClassOrMethodPointcut extends StaticMethodMatcherPointcut {

        private final MethodMatcher methodResolver;

        AnnotationClassOrMethodPointcut(Class<? extends Annotation> annotationType) {
            this.methodResolver = new AnnotationMethodMatcher(annotationType);
            setClassFilter(new AnnotationClassOrMethodFilter(annotationType));
        }

        @Override
        public boolean matches(Method method, Class<?> targetClass) {
            return getClassFilter().matches(targetClass) || this.methodResolver.matches(method, targetClass);
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof AnnotationClassOrMethodPointcut)) {
                return false;
            }
            AnnotationClassOrMethodPointcut otherAdvisor = (AnnotationClassOrMethodPointcut)other;
            return ObjectUtils.nullSafeEquals(this.methodResolver, otherAdvisor.methodResolver);
        }

    }

    private final class AnnotationClassOrMethodFilter extends AnnotationClassFilter {

        private final AnnotationMethodsResolver methodResolver;

        AnnotationClassOrMethodFilter(Class<? extends Annotation> annotationType) {
            super(annotationType, true);
            this.methodResolver = new AnnotationMethodsResolver(annotationType);
        }

        @Override
        public boolean matches(Class<?> clazz) {
            return super.matches(clazz) || this.methodResolver.hasAnnotatedMethods(clazz);
        }

    }

    private static class AnnotationMethodsResolver {

        private Class<? extends Annotation> annotationType;

        public AnnotationMethodsResolver(Class<? extends Annotation> annotationType) {
            this.annotationType = annotationType;
        }

        public boolean hasAnnotatedMethods(Class<?> clazz) {
            final AtomicBoolean found = new AtomicBoolean(false);
            ReflectionUtils.doWithMethods(clazz, new ReflectionUtils.MethodCallback() {
                @Override
                public void doWith(Method method) throws IllegalArgumentException, IllegalAccessException {
                    if (found.get()) {
                        return;
                    }
                    Annotation annotation = AnnotationUtils.findAnnotation(method, AnnotationMethodsResolver.this.annotationType);
                    if (annotation != null) {
                        found.set(true);
                    }
                }
            });
            return found.get();
        }

    }
}
