package com.smart.retry.core.spi;

import com.smart.retry.common.notify.RetryTaskNotify;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @Author xiaoqiang
 * @Version NotifySpiDiscover.java, v 0.1 2025年02月18日 16:15 xiaoqiang
 * @Description: TODO
 */
public class NotifyDiscover implements ApplicationContextAware, InitializingBean {

    private ApplicationContext applicationContext;

    private static final List<RetryTaskNotify> notifyList  = new ArrayList<>();

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    private static void registerNotify(RetryTaskNotify notify) {
        notifyList.add(notify);
    }

    public static List<RetryTaskNotify> getNotifyList() {
        return notifyList;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Collection<RetryTaskNotify> retryTaskNotifies = applicationContext.getBeansOfType(RetryTaskNotify.class).values();
        for (RetryTaskNotify retryTaskNotify : retryTaskNotifies) {
            registerNotify(retryTaskNotify);
        }
    }





}
