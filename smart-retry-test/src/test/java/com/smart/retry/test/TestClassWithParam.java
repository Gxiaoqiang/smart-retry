package com.smart.retry.test;

import com.alibaba.fastjson.JSONObject;
import com.smart.retry.common.RetryLinstener;
import com.smart.retry.common.annotation.RetryOnClass;
import com.smart.retry.common.constant.ExecuteResultStatus;
import org.apache.commons.lang3.time.DateFormatUtils;

import javax.swing.text.DateFormatter;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.logging.SimpleFormatter;

/**
 * @Author xiaoqiang
 * @Version TestClassWithParam.java, v 0.1 2025年02月21日 12:24 xiaoqiang
 * @Description: TODO
 */
@RetryOnClass(taskCode = "TestClassWithParam")
public class TestClassWithParam implements RetryLinstener<TestModel> {

    @Override
    public ExecuteResultStatus consume(TestModel param) {



        System.out.println(DateFormatUtils.format(new Date(),"yyyy-MM-dd HH:mm:ss") +
                " TestClassWithParam consume param: " + JSONObject.toJSONString(param));
        return ExecuteResultStatus.FAIL;
    }
}


