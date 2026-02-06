package com.smart.retry.test;

import com.alibaba.fastjson.JSONObject;
import com.smart.retry.common.RetryLinstener;
import com.smart.retry.common.annotation.RetryOnClass;
import com.smart.retry.common.constant.ExecuteResultStatus;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @Author xiaoqiang
 * @Version TestClassWithList.java, v 0.1 2025年02月21日 15:17 xiaoqiang
 * @Description: TODO
 */
@RetryOnClass(taskCode = "TestClassWithList",retryTaskNotifies = {NotifyTest.class})
public class TestClassWithList implements RetryLinstener<List<TestModel>> {


    private static int count = 0;


    @Override
    public ExecuteResultStatus consume(List<TestModel> param) {

        /*try {
            TimeUnit.SECONDS.sleep(15);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }*/

        System.out.println(System.currentTimeMillis()/1000+":"+(count++)+":"+JSONObject.toJSONString(param));

        return ExecuteResultStatus.FAIL;
    }
}
