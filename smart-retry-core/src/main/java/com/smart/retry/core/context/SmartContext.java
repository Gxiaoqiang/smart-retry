package com.smart.retry.core.context;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * @Author xiaoqiang
 * @Version SmartContext.java, v 0.1 2025年11月17日 17:09 xiaoqiang
 * @Description: TODO
 */
public class SmartContext implements ApplicationContextAware {

    private static ApplicationContext applicationContext;



    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public  ApplicationContext getApplicationContext() {
        return applicationContext;
    }
}
