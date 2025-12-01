package com.smart.retry.mybatis.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @Author xiaoqiang
 * @Version SmartConfigure.java, v 0.1 2025年02月21日 11:30 xiaoqiang
 * @Description: TODO
 */
@ConfigurationProperties(prefix = "spring.smart-retry.mybatis")
public class SmartConfigure {


    private String datasource = "dataSource";



    public String getDatasource() {
        return datasource;
    }

    public void setDatasource(String datasource) {
        this.datasource = datasource;
    }
}
