package com.smart.retry.mybatis.start.test;

import com.smart.retry.mybatis.MybatisAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @Author xiaoqiang
 * @Version ConfigureTest.java, v 0.1 2025年02月18日 21:05 xiaoqiang
 * @Description: TODO
 */
public class ConfigureTestService {

    @Autowired
    private MybatisAutoConfiguration mybatisAutoConfiguration ;

    @Test
    public void test(){
        System.out.println(System.getProperty("user.dir"));
    }
}
