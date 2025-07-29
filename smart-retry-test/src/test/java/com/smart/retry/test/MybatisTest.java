package com.smart.retry.test;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.smart.retry.common.RetryTaskCreator;
import com.smart.retry.common.model.RetryTask;
import com.smart.retry.common.model.RetryTaskBuilder;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @Author xiaoqiang
 * @Version MybatisTest.java, v 0.1 2025年02月19日 00:50 xiaoqiang
 * @Description: TODO
 */
public class MybatisTest extends AbstractTest {


    @Autowired
    private TestRetry testRetry;

    @Autowired
    private RetryTaskCreator retryTaskCreator;

    @Autowired
    private TestChain testChain;

    @Test
    public void testSleep() {
        //testChain.test_chain_start();
        try {
            TimeUnit.SECONDS.sleep(10000);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testException() throws InterruptedException {
        testRetry.testException();

        //TimeUnit.SECONDS.sleep(1000);
        //System.out.println(retryContainer);
    }

    @Test
    public void testParams() throws InterruptedException {
        testRetry.testParams("123", "456");

        //TimeUnit.SECONDS.sleep(1000);
        //System.out.println(retryContainer);
    }

    @Test
    public void test_TestClassWithList() throws InterruptedException {

        RetryTaskBuilder<List<TestModel>> retryTaskBuilder = RetryTaskBuilder.of()
                .withRetryNum(3)
                .withTaskCode("TestClassWithList")
                .withTaskDesc("Test")
                .withIntervalSecond(20)
                .withDelaySecond(2);
        retryTaskBuilder.withParam(Lists.newArrayList(new TestModel("123", "456",2),
                new TestModel("789", "456",3)));
        retryTaskCreator.createTask(retryTaskBuilder);

        TimeUnit.SECONDS.sleep(1000);

        //retryTaskCreator.createTask(retryTask);

        //retryTaskCreator.createTask(testRetry, "testException", new Object[]{}, new Class[]{});
    }


    @Test
    public void test_TestClassWithParam() throws InterruptedException {
        try {
            RetryTask retryTask = new RetryTask();
            retryTask.setRetryNum(3);
            retryTask.setIntervalSecond(20);
            retryTask.setDelaySecond(3);
            retryTask.setTaskCode("test_class_retry_task_code");
            retryTask.setParameters(JSONObject.toJSONString("params"));
            RetryTaskBuilder<TestModel> retryTaskBuilder = RetryTaskBuilder.of()
                    .withRetryNum(3)
                    .withTaskCode("TestClassWithParam")
                    .withTaskDesc("Test")
                    .withIntervalSecond(20)
                    .withDelaySecond(2)
                    .withParam(new TestModel("123", "456",2));
            retryTaskBuilder.withParam(new TestModel("123", "456",2));
            retryTaskCreator.createTask(retryTaskBuilder);
        }catch (Exception e){
            e.printStackTrace();
        }


        //retryTaskCreator.createTask(retryTask);

        //retryTaskCreator.createTask(testRetry, "testException", new Object[]{}, new Class[]{});
    }



    @Test
    public void testClass() throws InterruptedException {
        RetryTaskBuilder retryTaskBuilder = RetryTaskBuilder.of(
                "TestClassWithParam",null,3,20,"params"
        );
        retryTaskCreator.createTask(retryTaskBuilder);

        //retryTaskCreator.createTask(retryTask);

        //retryTaskCreator.createTask(testRetry, "testException", new Object[]{}, new Class[]{});
    }

        //TimeUnit.SECONDS.sleep(1000);

}
