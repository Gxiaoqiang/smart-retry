package com.smart.retry.core.scanner;

import com.google.common.collect.Maps;
import com.smart.retry.common.annotation.RetryOnMethod;
import com.smart.retry.common.constant.RetryTaskTypeEnum;
import com.smart.retry.common.model.RetryTaskObject;
import com.smart.retry.common.scanner.RetryScanner;
import com.smart.retry.core.cache.RetryCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * @Author xiaoqiang
 * @Version MethodScanner.java, v 0.1 2025年02月14日 09:49 xiaoqiang
 * @Description: TODO
 */
public class RetryMethodScanner implements RetryScanner {


    private static final Logger logger = LoggerFactory.getLogger(RetryMethodScanner.class);



    @Override
    public void scan(ApplicationContext applicationContext) {
        String[] allBeanNames = applicationContext.getBeanDefinitionNames();
        for (String beanName : allBeanNames) {
            Object bean = applicationContext.getBean(beanName);

            //1、查找标有注解@see RetryableOnMethod 的方法
            resolveMethodAnnotation(bean, applicationContext);
        }

    }



    private void resolveMethodAnnotation(Object bean,ApplicationContext applicationContext) {
        Map<Method, RetryOnMethod> methodTMap = MethodIntrospector.selectMethods(bean.getClass(),
                new MethodIntrospector.MetadataLookup<RetryOnMethod>() {
                    @Override
                    public RetryOnMethod inspect(Method method) {
                        return AnnotationUtils.findAnnotation(method, RetryOnMethod.class);
                    }
                });
        if (methodTMap == null || methodTMap.isEmpty()) {
            return;
        }


        methodTMap.forEach((method, retryOnMethod) -> {
            String taskCode = method.getDeclaringClass().getName() + "#" + method.getName();
            boolean hasTransactional = method.isAnnotationPresent(Transactional.class) ||
                    method.getDeclaringClass().isAnnotationPresent(Transactional.class);
            Object proxy = bean;
            if (hasTransactional) {
                Class<?> beanType = AopUtils.getTargetClass(bean); // 处理代理类获取真实类型
                proxy = applicationContext.getBean(beanType);
                logger.warn("{} has transactional, please check", taskCode);
            }

            RetryTaskObject retryTaskObject =
                    RetryTaskObject.of().withRetryCallback(retryOnMethod.retryCallback())
                            .withTaskCode(taskCode)
                            .withMethod(method)
                            .withExcludes(retryOnMethod.exclude())
                            .withBeanObj(proxy)
                            .withRetryTaskNotify(retryOnMethod.retryTaskNotifies())
                            .withParams(method.getParameters())
                            .withRetryType(RetryTaskTypeEnum.METHOD);
            RetryCache.put(taskCode, retryTaskObject);
        });
    }

}
