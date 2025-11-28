package com.smart.retry.core;

import com.smart.retry.common.RetryConfiguration;
import com.smart.retry.common.RetryTaskOperator;
import com.smart.retry.common.constant.RetryTaskStatus;
import com.smart.retry.common.exception.RetryException;
import com.smart.retry.common.model.RetryTask;
import com.smart.retry.common.model.RetryTaskBuilder;
import com.smart.retry.common.utils.GsonTool;
import com.smart.retry.common.utils.IpUtils;
import com.smart.retry.core.config.SmartExecutorConfigure;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.transaction.annotation.Propagation.REQUIRED;

/**
 * @Author xiaoqiang
 * @Version SimpleRetryTaskCreator.java, v 0.1 2025年02月20日 17:43 xiaoqiang
 * @Description: TODO
 */
public class SimpleRetryTaskOperator<T> implements RetryTaskOperator<T> {


    public static final Logger LOGGER = LoggerFactory.getLogger(SimpleRetryTaskOperator.class);
    private RetryConfiguration retryConfiguration;
    private SmartExecutorConfigure  smartExecutorConfigure;

    public SimpleRetryTaskOperator(RetryConfiguration retryConfiguration, SmartExecutorConfigure smartExecutorConfigure) {
        this.retryConfiguration = retryConfiguration;
        this.smartExecutorConfigure = smartExecutorConfigure;
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = REQUIRED)
    public long createTask(RetryTaskBuilder<T> retryTaskBuilder) {

        RetryTask retryTask = new RetryTask();
        BeanUtils.copyProperties(retryTaskBuilder, retryTask);

        retryTask.setNextPlanTimeStrategy(retryTaskBuilder.getNextPlanTimeStrategy().getCode());
        retryTask.setParameters(GsonTool.toJsonString(retryTaskBuilder.getParam()));

        retryTask.setIntervalSecond(retryTaskBuilder.getIntervalSecond());

        retryTask.setTaskCode(retryTaskBuilder.getTaskCode());
        retryTask.setStatus(RetryTaskStatus.WAITING.getCode());
        retryTask.setShardingKey(ShardingContextHolder.getRandomShardingIndex());
        retryTask.setCreator(IpUtils.getIp());

        retryTask.setOriginRetryNum(retryTask.getRetryNum());
        checkRetryCondition(retryTask);
        retryTask.setUniqueKey(retryConfiguration.getIdentifier().identify(retryTask.getTaskCode(), retryTask.getParameters()));

        return retryConfiguration.getRetryTaskAcess().saveRetryTask(retryTask);
    }

    @Override
    public void invokeTaskSync(long taskId) {
        RetryTask retryTask = getRetryTask(taskId);
        if (retryTask == null) return;
        SimpleContainer.invokeTaskSync(retryTask, retryConfiguration);
    }

    private RetryTask getRetryTask(long taskId) {
        RetryTask retryTask = retryConfiguration.getRetryTaskAcess().getRetryTask(taskId);
        if (retryTask == null) {
            LOGGER.warn("[RetryTaskRepoImpl#updateRetryTask]retryTask not exists, id:{}", taskId);
            return null;
        }
        Integer taskStatus = retryTask.getStatus();
        if (!taskStatus.equals( RetryTaskStatus.WAITING.getCode())) {
            LOGGER.warn("[RetryTaskRepoImpl#updateRetryTask]retryTask status is not WAITING, id:{}", taskId);
            return null;
        }
        return retryTask;
    }

    @Override
    public void invokeTaskAsync(long taskId) {
        RetryTask retryTask = getRetryTask(taskId);
        if (retryTask == null) return;
        SimpleContainer.invokeTaskAsync(retryTask, retryConfiguration,smartExecutorConfigure);
    }

    private void checkRetryCondition(RetryTask task) {
        // TODO: 2025年02月20日 17:44 xiaoqiang 这里需要实现具体的重试条件判断
        if (StringUtils.isEmpty(task.getTaskCode())) {
            throw new RetryException("task code is empty");
        }
        if (task.getRetryNum() == null) {
            throw new RetryException("task retryNum is null");
        }
        if (task.getRetryNum() <= 0) {
            throw new RetryException("task retryNum is less than 0");
        }
        if (task.getDelaySecond() == null) {
            throw new RetryException("task delaySecond is null");
        }
        if (task.getDelaySecond() <= 0) {
            throw new RetryException("task delayTime is less than 0");
        }
        if (task.getIntervalSecond() == null) {
            throw new RetryException("task intervalSecond is null");
        }
        if (task.getIntervalSecond() <= 0) {
            throw new RetryException("task intervalTime is less than 0");
        }


    }
}
