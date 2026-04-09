package com.smart.retry;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Smart Retry Web 启动类
 */
@SpringBootApplication(scanBasePackages = {"com.smart.retry"})
@MapperScan({"com.smart.retry.web.dao", "com.smart.retry.mybatis.dao"})
public class Main {
    
    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(Main.class, args);
        printBanner();
    }
    
    /**
     * 打印 smart-retry ASCII 艺术字
     */
    private static void printBanner() {
        System.out.println("\n" +
                "------------------------------------------------------------------\n" +
                "  Smart Retry Framework Started Successfully!\n" +
                "------------------------------------------------------------------\n");
    }
}