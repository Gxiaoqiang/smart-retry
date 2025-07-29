package com.smart.retry.test;

import com.smart.retry.common.RetryLinstener;
import com.smart.retry.common.annotation.RetryOnClass;
import com.smart.retry.common.constant.ExecuteResultStatus;

/**
 * @Author xiaoqiang
 * @Version TestClassRetry.java, v 0.1 2025年02月20日 20:40 xiaoqiang
 * @Description: TODO
 */

@RetryOnClass(taskCode = "test_class_retry_task_code",retryTaskNotifies = {NotifyTest.class})
public class TestClassRetry extends RetryLinstener<String> {
    @Override
    public ExecuteResultStatus consume(String param) {



        System.out.println(param);
        return null;
    }
}
