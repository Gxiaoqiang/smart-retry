package com.smart.retry.test;

import com.smart.retry.common.annotation.RetryOnMethod;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @Author xiaoqiang
 * @Version TestChain.java, v 0.1 2025年02月21日 16:28 xiaoqiang
 * @Description: TODO
 */
@Service
public class TestChain {

    @Autowired
    private TestRetry testRetry;

    @RetryOnMethod(maxAttempt=3,firstDelaySecond=1,intervalSecond = 20)
    public void test_chain_start(){
        System.out.println("test_chain_start");
        testRetry.testParams("test_chain_start","test_chain_start");
    }
}
