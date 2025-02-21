package com.smart.retry.common.scanner;

import org.springframework.context.ApplicationContext;

/**
 * @Author xiaoqiang
 * @Version RetryInfoScanner.java, v 0.1 2025年02月13日 19:57 xiaoqiang
 * @Description: 扫描重试信息并进行注册
 */
public interface RetryScanner {
    void scan(ApplicationContext applicationContext);
}
