package com.smart.retry.test;

import com.smart.retry.common.annotation.RetryOnMethod;

/**
 * @Author xiaoqiang
 * @Version TestWithParams.java, v 0.1 2025年02月19日 23:22 xiaoqiang
 * @Description: TODO
 */
public class TestWithParams {

    @RetryOnMethod
    public void TestWithParams() {

        int i = 1/0;
        System.out.println("testException");
    }
}
