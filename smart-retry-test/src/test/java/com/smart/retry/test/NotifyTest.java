package com.smart.retry.test;

import com.smart.retry.common.notify.NotifyContext;
import com.smart.retry.common.notify.RetryTaskNotify;

import java.util.Collections;
import java.util.List;

/**
 * @Author xiaoqiang
 * @Version NotifyTest.java, v 0.1 2025年02月28日 12:25 xiaoqiang
 * @Description: TODO
 */
public class NotifyTest implements RetryTaskNotify {


    @Override
    public void oneTimeNotify(NotifyContext context) {

        System.out.println("oneTimeNotify");
    }

    @Override
    public void finishTaskNotify(NotifyContext context) {

        System.out.println("finishTaskNotify");
    }
}
