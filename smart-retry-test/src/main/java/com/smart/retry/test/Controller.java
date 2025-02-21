package com.smart.retry.test;

import com.smart.retry.common.RetryContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @Author xiaoqiang
 * @Version Controller.java, v 0.1 2025年02月19日 13:59 xiaoqiang
 * @Description: TODO
 */
@Service
public class Controller {

    @Autowired
    private RetryContainer retryContainer;



    public void test() {
        System.out.println("test");
    }
}
