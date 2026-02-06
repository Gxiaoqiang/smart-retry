package com.smart.retry.test;

import com.alibaba.fastjson.JSONObject;
import com.smart.retry.common.RetryLinstener;
import com.smart.retry.common.annotation.RetryOnClass;
import com.smart.retry.common.constant.ExecuteResultStatus;

import java.util.concurrent.TimeUnit;

/**
 * @Author xiaoqiang
 * @Version TestClassWithParam.java, v 0.1 2025年02月21日 12:24 xiaoqiang
 * @Description: TODO
 */
@RetryOnClass(taskCode = "TestClassWithParam")
public class TestClassWithParam implements RetryLinstener<TestModel> {

    @Override
    public ExecuteResultStatus consume(TestModel param) {


        System.out.println("TestClassWithParam consume param: " + JSONObject.toJSONString(param));
        return null;
    }
}


