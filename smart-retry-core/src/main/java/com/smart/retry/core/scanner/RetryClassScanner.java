package com.smart.retry.core.scanner;

import com.smart.retry.common.RetryLinstener;
import com.smart.retry.common.annotation.RetryOnClass;
import com.smart.retry.common.constant.RetryTaskTypeEnum;
import com.smart.retry.common.model.RetryTaskObject;
import com.smart.retry.common.scanner.RetryScanner;
import com.smart.retry.core.cache.RetryCache;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;

import java.lang.reflect.Method;

/**
 * @Author xiaoqiang
 * @Version RetryClassScanner.java, v 0.1 2025年02月14日 17:23 xiaoqiang
 * @Description: TODO
 */
public class RetryClassScanner  implements RetryScanner {



    @Override
    public void scan(ApplicationContext applicationContext) {
        String[] beanNames = applicationContext.getBeanNamesForAnnotation(RetryOnClass.class);
        for (String beanName : beanNames) {
            Object bean = applicationContext.getBean(beanName);
            resolveClassAnnotation(bean);
        }
    }

    private void resolveClassAnnotation(Object bean) {
        if (!(bean instanceof RetryLinstener)) {
            return;
        }
        if (bean.getClass() == RetryLinstener.class) {
            return;
        }
        RetryOnClass retryableOnClass = AnnotationUtils.findAnnotation(bean.getClass(), RetryOnClass.class);
        if (retryableOnClass == null) {
            return;
        }
        RetryTaskObject retryTaskObject =
                RetryTaskObject.of().withBeanObj(bean)
                        .withRetryTaskNotify(retryableOnClass.retryTaskNotifies())
                        .withRetryType(RetryTaskTypeEnum.CLASS);
        Method[] methods = bean.getClass().getMethods();
        for (Method method : methods) {
            String methodName = method.getName();
            if (StringUtils.equals(methodName, "consumeProcess")) {
                retryTaskObject.withMethod(method);
                break;
            }
        }
        String taskCode = retryableOnClass.taskCode();
        RetryCache.put(taskCode, retryTaskObject);
    }
}
