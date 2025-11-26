package com.smart.retry.core.innovation;

import com.google.gson.reflect.TypeToken;
import com.smart.retry.common.RetryConfiguration;
import com.smart.retry.common.constant.ExecuteResultStatus;
import com.smart.retry.common.constant.RetryTaskStatus;
import com.smart.retry.common.constant.RetryTaskTypeEnum;
import com.smart.retry.common.exception.RetryException;
import com.smart.retry.common.innovation.SmartInnovation;
import com.smart.retry.common.model.RetryTask;
import com.smart.retry.common.model.RetryTaskObject;
import com.smart.retry.common.notify.NotifyContext;
import com.smart.retry.common.notify.RetryTaskNotify;
import com.smart.retry.common.utils.ExceptionUtils;
import com.smart.retry.common.utils.GsonTool;
import com.smart.retry.common.utils.IpUtils;
import com.smart.retry.core.cache.RetryCache;
import com.smart.retry.core.nextPlanTimeStrategy.*;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author xiaoqiang
 * @Version DefaultInnovation.java, v 0.1 2025年02月18日 13:41 xiaoqiang
 * @Description: TODO
 */
public class DefaultInnovation implements SmartInnovation {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultInnovation.class);

    private RetryTask retryTask;

    private RetryConfiguration retryConfiguration;


    private static final Map<Class<? extends RetryTaskNotify>, RetryTaskNotify> retryTaskNotifyMap = new ConcurrentHashMap<>();

    public DefaultInnovation(RetryTask retryTask, RetryConfiguration retryConfiguration) {
        this.retryTask = retryTask;
        this.retryConfiguration = retryConfiguration;
    }


    private void beforeProcessTask(RetryTask retryTask) {
        retryTask.setStatus(RetryTaskStatus.RUNNING.getCode());
        processNextExecuteTime(retryTask);
        Integer retryNum = retryTask.getRetryNum();
        if (retryNum >= 1) {
            retryTask.setRetryNum(retryNum - 1);
        }
        retryTask.setExecutor(IpUtils.getIp());
        retryConfiguration.getRetryTaskAcess().updateRetryTask(retryTask);
    }

    @Override
    public Object invoke() throws Throwable {

        String taskCode = retryTask.getTaskCode();

        RetryTaskObject taskObject = RetryCache.get(taskCode);
        if (taskObject == null) {
            LOGGER.error("[DefaultInnovation#invoke]taskObject is null, taskCode:{}", taskCode);
            return null;
        }
        Method method = taskObject.getMethod();

        Throwable throwable = null;


        ExecuteResultStatus executeResultStatus = ExecuteResultStatus.SUCCESS;
        NotifyContext notifyContext = new NotifyContext();
        notifyContext.setRetryTask(retryTask);
        try {
            //更新任务状态为执行中
            beforeProcessTask(retryTask);

            Object result = doInvoke(taskObject, retryTask, method);
            RetryTaskTypeEnum retryTaskTypeEnum = taskObject.getRetryType();

            if (result == null) {
                executeResultStatus = ExecuteResultStatus.SUCCESS;
            }
            if (result != null && !(result instanceof ExecuteResultStatus)) {
                if (retryTaskTypeEnum == RetryTaskTypeEnum.CLASS) {
                    executeResultStatus = ExecuteResultStatus.SUCCESS;
                }
                if (retryTaskTypeEnum == RetryTaskTypeEnum.METHOD && result instanceof RetryTaskTypeEnum) {
                    executeResultStatus = ExecuteResultStatus.SUCCESS;
                }
            }
            if (result != null && (result instanceof ExecuteResultStatus)) {
                executeResultStatus = (ExecuteResultStatus) result;
            }
            notifyContext.setResult(result);
            return result;
        } catch (Throwable ex) {
            throwable = ex;
            LOGGER.error("[DefaultInnovation#invoke]taskCode:{} invoke error", taskCode, ex);
            executeResultStatus = ExecuteResultStatus.FAIL;
            notifyContext.setThrowable(ex);
            throw ex;
        } finally {
            String exceptionMsg = ExceptionUtils.createConciseStackTraceMessage(throwable);
            if (exceptionMsg != null) {
                retryTask.setAttribute(exceptionMsg);
            }
            //更新任务状态为执行完成
            if (executeResultStatus == ExecuteResultStatus.SUCCESS) {
                retryTask.setStatus(RetryTaskStatus.SUCCESS.getCode());
            }
            if (executeResultStatus == ExecuteResultStatus.FAIL) {
                retryTask.setStatus(RetryTaskStatus.FAIL.getCode());
            }
            //System.out.println(GsonTool.toJsonString(retryTask));
            retryConfiguration.getRetryTaskAcess().updateRetryTask(retryTask);
            notify(taskObject, taskCode, notifyContext, executeResultStatus, throwable);
        }
    }


    private void processNextExecuteTime(RetryTask retryTask) {

        Date nextPlanTime = NextPlanTimeStrategyManager.nextTime(retryTask);
        retryTask.setNextPlanTime(nextPlanTime);

    }

    private Object doInvoke(RetryTaskObject taskObject, RetryTask retryTask, Method method) throws Throwable {
        RetryTaskTypeEnum retryTaskTypeEnum = taskObject.getRetryType();

        if (retryTaskTypeEnum == RetryTaskTypeEnum.CLASS) {
            Object parameterValue = null;
            if (retryTask.getParameters() == null) {
                parameterValue = null;
            }
            Type type = getRealType(taskObject);

            parameterValue = GsonTool.fromJson(retryTask.getParameters(), type);
            Object[] args = new Object[1];
            args[0] = parameterValue;
            return method.invoke(taskObject.getTargetObj(), args);
        }
        if (retryTaskTypeEnum == RetryTaskTypeEnum.METHOD) {
            Object[] args = retryConfiguration.getSmartSerializer().deSerializer(method, retryTask.getParameters());
            return method.invoke(taskObject.getTargetObj(), args);
        }
        throw new RetryException("retryTaskTypeEnum is not support");
    }


    /**
     * 获取真实的参数类型
     * @param taskObject
     * @return
     */
    private  Type getRealType(RetryTaskObject taskObject) {
        Type superClass = taskObject.getTargetObj().getClass().getGenericSuperclass();
        ParameterizedType parameterizedType = (ParameterizedType) superClass;
        Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
        Type type = TypeToken.get(actualTypeArguments[0]).getType();
        return type;
    }

    private void notify(RetryTaskObject taskObject, String taskCode, NotifyContext notifyContext, ExecuteResultStatus executeResultStatus, Throwable throwable) {
        Class<? extends RetryTaskNotify>[] clazzs = taskObject.getRetryTaskNotify();
        if (ArrayUtils.isEmpty(clazzs)) {
            return;
        }
        notifyContext.setExecutionStatus(executeResultStatus);
        notifyContext.setThrowable(throwable);
        //1.m每次执行结束
        Arrays.stream(clazzs).forEach(clazz -> {
            RetryTaskNotify notify = retryTaskNotifyMap.get(clazz);
            if (notify == null) {
                notify = generateNotify(taskCode, clazz);
                if (notify == null) {
                    return;
                }
            }
            try {
                notify.oneTimeNotify(notifyContext);
            } catch (Throwable ex) {
                LOGGER.warn("[DefaultInnovation#notify] oneTimeNotify invoke error,taskCode:{} ", taskCode, ex);
            }
            if (retryTask.getRetryNum() == 0) {
                try {
                    notify.allRetryTaskFinishNotify(notifyContext);
                } catch (Throwable ex) {
                    LOGGER.warn("[DefaultInnovation#notify]taskCode:{} finishTaskNotify invoke error,taskCode:{} ", taskCode, ex);
                }
            }
        });
    }

    private static RetryTaskNotify generateNotify(String taskCode, Class<? extends RetryTaskNotify> clazz) {
        RetryTaskNotify notify = null;
        try {
            notify = clazz.newInstance();
        } catch (Exception e) {
            LOGGER.warn("[DefaultInnovation#notify] notify instance error,taskCode:{} ", taskCode, e);
            return notify;
        }
        retryTaskNotifyMap.put(clazz, notify);
        return notify;
    }

}


