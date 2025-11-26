package com.smart.retry.core.retry;

import com.smart.retry.common.RetryConfiguration;
import com.smart.retry.common.annotation.RetryOnMethod;
import com.smart.retry.common.constant.RetryTaskStatus;
import com.smart.retry.common.identifier.Identifier;
import com.smart.retry.common.model.RetryAttemptContext;
import com.smart.retry.common.model.RetryTask;
import com.smart.retry.common.retry.IRetryer;
import com.smart.retry.common.serializer.SmartSerializer;
import com.smart.retry.common.utils.IpUtils;
import com.smart.retry.core.ShardingContextHolder;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Date;

/**
 * @Author xiaoqiang
 * @Version RemoteRetryer.java, v 0.1 2025年02月14日 19:24 xiaoqiang
 * @Description: TODO
 */
public class RemoteRetryer implements IRetryer {
    private static final Logger log = LoggerFactory.getLogger(RemoteRetryer.class);

    private MethodInvocation methodInvocation;


    private RetryOnMethod retryable;

    private RetryAttemptContext retryAttemptContext;

    private RetryConfiguration retryConfiguration;

    public RemoteRetryer(RetryConfiguration retryConfiguration,MethodInvocation methodInvocation,
                         RetryOnMethod retryable,
                         RetryAttemptContext retryAttemptContext) {
        this.retryConfiguration = retryConfiguration;
        this.methodInvocation = methodInvocation;
        this.retryable = retryable;
        this.retryAttemptContext = retryAttemptContext;
    }

    //借助guava的开源组件进行重试
    @Override
    public Object retry() throws Throwable{
        registerRemoteRetyTask();
        if (retryAttemptContext.getThrowable() != null) {
            throw retryAttemptContext.getThrowable();
        }
        return retryAttemptContext.getResult();
    }


    /**
     * 远程重试，注册到服务中心
     *
     * @return
     */
    private void registerRemoteRetyTask() {



        RetryTask retryTask = new RetryTask();
        retryTask.setRetryNum(retryable.maxAttempt());
        retryTask.setDelaySecond(retryable.firstDelaySecond());
        retryTask.setOriginRetryNum(retryable.maxAttempt());
        retryTask.setCreator(IpUtils.getIp());
        retryTask.setStatus(RetryTaskStatus.WAITING.getCode());
        Method method = methodInvocation.getMethod();
        String taskCode = method.getDeclaringClass().getName() + "#" + method.getName();
        retryTask.setTaskCode(taskCode);
        retryTask.setParameters(getArgs());
        retryTask.setUniqueKey(getUniqueKey(taskCode, getArgs()));
        retryTask.setShardingKey(ShardingContextHolder.getRandomShardingIndex());
        retryTask.setNextPlanTimeStrategy(retryTask.getNextPlanTimeStrategy());
        retryTask.setIntervalSecond(retryable.intervalSecond());

        long firstNextExecuteTime = System.currentTimeMillis()+retryable.firstDelaySecond()*1000;
        retryTask.setNextPlanTime(new Date(firstNextExecuteTime));

        retryConfiguration.getRetryTaskAcess().saveRetryTask(retryTask);


    }

    /**
     * 序列化重试任务的参数
     *
     * @return
     */
    private String getArgs() {
        Method method = methodInvocation.getMethod();
        Object []args = methodInvocation.getArguments();
        SmartSerializer serializer = retryConfiguration.getSmartSerializer();

        return serializer.serializer(method, args);

    }


    /**
     * 获取uniqueKey
     *
     * @param taskCode
     * @param argsStr
     * @return
     */
    private String getUniqueKey( String taskCode, String argsStr) {

        Identifier identifier = retryConfiguration.getIdentifier();
        return identifier.identify(taskCode, argsStr);

    }
}
