package com.smart.retry.test;

import com.alibaba.fastjson.JSONObject;
import com.smart.retry.common.RetryLinstener;
import com.smart.retry.common.annotation.RetryOnClass;
import com.smart.retry.common.constant.ExecuteResultStatus;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 多任务并发测试监听器
 *
 * @Author xiaoqiang
 * @Version MultiTaskListener.java, v 0.1 2025年06月23日 xiaoqiang
 */
@Component
@RetryOnClass(taskCode = "test-multi-task")
public class MultiTaskListener implements RetryLinstener<TestParam> {

    private volatile CountDownLatch latch = new CountDownLatch(1);

    private static AtomicInteger DEFAULT_COUNT = new AtomicInteger(0);

    @Override
    public ExecuteResultStatus consume(TestParam param) {


        int count = DEFAULT_COUNT.incrementAndGet();
        System.out.println( DateFormatUtils.format(new Date(), "yyyy-MM-dd HH:mm:ss.SSS")+ " "+(count)+" " + "多任务并发测试监听器执行"+ JSONObject.toJSONString(param));
       // latch.countDown();

        int index = param.getIndex();

        try {
            TimeUnit.MILLISECONDS.sleep(7000);
        }catch (Exception e){
            e.printStackTrace();
        }
        /*if(index % 8 == 0){
            throw new RuntimeException("test exception");
        }*/
        return ExecuteResultStatus.FAIL;
    }

    public boolean awaitExecution(long timeout, TimeUnit unit) throws InterruptedException {
        return latch.await(timeout, unit);
    }

    public void reset(int count) {
        latch = new CountDownLatch(count);
    }
}
