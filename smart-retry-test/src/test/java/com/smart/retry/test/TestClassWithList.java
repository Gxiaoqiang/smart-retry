package com.smart.retry.test;

import com.alibaba.fastjson.JSONObject;
import com.smart.retry.common.RetryLinstener;
import com.smart.retry.common.annotation.RetryOnClass;
import com.smart.retry.common.constant.ExecuteResultStatus;

import java.util.List;

/**
 * @Author xiaoqiang
 * @Version TestClassWithList.java, v 0.1 2025年02月21日 15:17 xiaoqiang
 * @Description: TODO
 */
@RetryOnClass(taskCode = "TestClassWithList")
public class TestClassWithList extends RetryLinstener<List<TestModel>> {


    @Override
    public ExecuteResultStatus consume(List<TestModel> param) {

        System.out.println(JSONObject.toJSONString(param));
        return null;
    }
}
