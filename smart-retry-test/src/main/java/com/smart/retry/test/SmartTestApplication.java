package com.smart.retry.test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @Author xiaoqiang
 * @Version SmartTestApplication.java, v 0.1 2025年02月18日 21:29 xiaoqiang
 * @Description: TODO
 */
@SpringBootApplication(scanBasePackages = {"com.smart.retry"})
@EnableAutoConfiguration
public class SmartTestApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartTestApplication.class, args);

    }
}
