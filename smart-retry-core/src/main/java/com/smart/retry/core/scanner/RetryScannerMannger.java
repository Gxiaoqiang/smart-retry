package com.smart.retry.core.scanner;

import com.smart.retry.common.SmartRetryRunFlag;
import com.smart.retry.common.scanner.RetryScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.event.EventListener;

/**
 * @Author xiaoqiang
 * @Version RetryScanner.java, v 0.1 2025年02月14日 18:52 xiaoqiang
 * @Description: TODO
 */
public class RetryScannerMannger implements RetryScanner, ApplicationContextAware {

    private ApplicationContext context;
    private volatile boolean flag = false;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.context = applicationContext;
    }

    @Override
    public void scan(ApplicationContext applicationContext) {
        RetryScanner methodScanner = new RetryMethodScanner();
        methodScanner.scan(applicationContext);

        RetryScanner classScanner = new RetryClassScanner();
        classScanner.scan(applicationContext);
    }

    @EventListener(ApplicationReadyEvent.class)
    public synchronized void onApplicationReady() {
        if (flag) {
            return;
        }
        scan(context);
        flag = true;
        SmartRetryRunFlag.setFlag(true);
    }
}
