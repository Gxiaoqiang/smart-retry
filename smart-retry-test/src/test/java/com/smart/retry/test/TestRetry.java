package com.smart.retry.test;

import com.smart.retry.common.annotation.RetryOnMethod;
import org.springframework.stereotype.Service;

/**
 * @Author xiaoqiang
 * @Version TestException.java, v 0.1 2025年02月19日 21:04 xiaoqiang
 * @Description: TODO
 */
@Service
public class TestRetry {

    @RetryOnMethod
    public void testException() {

        int i = 1/0;
        System.out.println("testException");
    }

    @RetryOnMethod(firstDelaySecond = 3)
    public void testParams(String param1, String param2) {

        System.out.println("param1:" + param1 + ", param2:" + param2);
        int i = 1/0;
        System.out.println("testException");
    }
}
