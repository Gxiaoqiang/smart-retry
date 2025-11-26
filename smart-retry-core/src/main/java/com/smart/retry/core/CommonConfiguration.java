package com.smart.retry.core;

import com.smart.retry.common.RetryConfiguration;
import com.smart.retry.common.RetryTaskAccess;
import com.smart.retry.common.RetryTaskCreator;
import com.smart.retry.common.identifier.Identifier;
import com.smart.retry.common.serializer.SmartSerializer;
import com.smart.retry.core.context.SmartContext;
import com.smart.retry.core.identifier.MD5Identifier;
import com.smart.retry.core.scanner.RetryScannerMannger;
import com.smart.retry.core.serializer.JsonSerializer;
import org.springframework.aop.Advisor;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Role;

/**
 * @Author xiaoqiang
 * @Version CommonConfiguration.java, v 0.1 2025年02月14日 21:54 xiaoqiang
 * @Description:
 */
public class CommonConfiguration{


    @Bean
    @ConditionalOnMissingBean(SmartContext.class)
    public SmartContext smartContext() {
        return new SmartContext();
    }
    @Bean
    @ConditionalOnMissingBean(RetryConfiguration.class)
    public RetryConfiguration getRetryConfiguration(RetryTaskAccess retryTaskAccess) {

        return new RetryConfiguration() {
            @Override
            public RetryTaskAccess getRetryTaskAcess() {
                return retryTaskAccess;
            }

            @Override
            public Identifier getIdentifier() {
                return new MD5Identifier();
            }

            @Override
            public SmartSerializer getSmartSerializer() {
                return new JsonSerializer();
            }

        };
    }

    @Bean
    @ConditionalOnMissingBean(RetryTaskAdvisor.class)
    @ConditionalOnBean(RetryConfiguration.class)
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public Advisor getRetryTaskAcessAdvisor(RetryConfiguration retryConfiguration) {
        return new RetryTaskAdvisor(retryConfiguration);
    }

    @Bean
    @ConditionalOnBean(RetryConfiguration.class)
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public RetryTaskCreator retryTaskCreator(RetryConfiguration retryConfiguration) {
        return new SimpleRetryTaskCreator(retryConfiguration);
    }
    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    @Primary
    @ConditionalOnMissingBean(RetryScannerMannger.class)
    public RetryScannerMannger retryScannerMannger() {
        RetryScannerMannger retryScannerMannger = new RetryScannerMannger();
        return retryScannerMannger;
    }
}
