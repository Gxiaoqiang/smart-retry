package com.smart.retry.test;

import com.alibaba.fastjson.JSONObject;
import com.smart.retry.common.RetryLinstener;
import com.smart.retry.common.annotation.RetryOnClass;
import com.smart.retry.common.constant.ExecuteResultStatus;

/**
 * @Author xiaoqiang
 * @Version TestTransaction.java, v 0.1 2026年02月06日 10:06 xiaoqiang
 * @Description: TODO
 */
import com.smart.retry.common.RetryLinstener;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RetryOnClass(taskCode = "TestTransaction", taskDesc = "测试事务")
public class TestTransaction implements RetryLinstener<List<TestModel>> {

    @Override
    @Transactional
    public ExecuteResultStatus consume(List<TestModel> param) {

        System.out.println("TestTransaction consume param: " + JSONObject.toJSONString(param));
        return ExecuteResultStatus.SUCCESS;
    }
}
