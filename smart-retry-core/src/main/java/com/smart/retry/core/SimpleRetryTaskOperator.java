package com.smart.retry.core;

import com.smart.retry.common.RetryConfiguration;
import com.smart.retry.common.RetryTaskOperator;
import com.smart.retry.common.constant.RetryTaskStatus;
import com.smart.retry.common.exception.RetryException;
import com.smart.retry.common.model.RetryTask;
import com.smart.retry.common.model.RetryTaskBuilder;
import com.smart.retry.common.model.TaskExecutionResult;
import com.smart.retry.common.serializer.SmartSerializer;
import com.smart.retry.common.utils.GsonTool;
import com.smart.retry.common.utils.IpUtils;
import com.smart.retry.core.cache.RetryCache;
import com.smart.retry.core.config.SmartExecutorConfigure;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.lang.reflect.Method;

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

        long taskId = retryConfiguration.getRetryTaskAcess().saveRetryTask(retryTask);

        retryTask.setId(taskId);
        // 将任务加入 DelayQueue 精准调度（窗口内才入队）
        SimpleContainer.enqueueIfInWindow(retryTask);

        return taskId;
    }

    @Override
    public TaskExecutionResult invokeTaskOnceSync(long taskId) {
        warnIfInTransaction("invokeTaskOnceSync");
        RetryTask retryTask = getTriggerableTask(taskId);
        if (retryTask == null) return null;
        return SimpleContainer.invokeTaskOnceSync(retryTask, retryConfiguration);
    }

    @Override
    public void invokeTaskAsync(long taskId) {
        warnIfInTransaction("invokeTaskAsync");
        RetryTask retryTask = getTriggerableTask(taskId);
        if (retryTask == null) return;
        SimpleContainer.invokeTaskAsync(retryTask, retryConfiguration, smartExecutorConfigure);
    }

    /**
     * 获取可触发执行的任务。
     * <p>与 ConsumerTask#validateTaskInDB（允许 WAITING+FAIL）保持一致，
     * 手动触发允许 FAIL 状态，支持运维人员对失败任务进行补偿重试。
     *
     * @param taskId 任务 ID
     * @return 可触发的任务，不存在/状态不允许/重试次数耗尽时返回 null
     */
    private RetryTask getTriggerableTask(long taskId) {
        RetryTask retryTask = retryConfiguration.getRetryTaskAcess().getRetryTask(taskId);
        if (retryTask == null) {
            LOGGER.warn("[SimpleRetryTaskOperator#getTriggerableTask] task not found, id:{}", taskId);
            return null;
        }
        Integer taskStatus = retryTask.getStatus();
        // 与 ConsumerTask.validateTaskInDB 保持一致：允许 WAITING 和 FAIL
        if (!RetryTaskStatus.WAITING.getCode().equals(taskStatus)
                && !RetryTaskStatus.FAIL.getCode().equals(taskStatus)) {
            LOGGER.warn("[SimpleRetryTaskOperator#getTriggerableTask] task status not allowed, id:{}, status:{}",
                    taskId, taskStatus);
            return null;
        }
        if (retryTask.getRetryNum() == null || retryTask.getRetryNum() <= 0) {
            LOGGER.warn("[SimpleRetryTaskOperator#getTriggerableTask] retryNum exhausted, id:{}, retryNum:{}",
                    taskId, retryTask.getRetryNum());
            return null;
        }
        // FAIL 状态下重置为 WAITING，确保 ConsumerTask.validateTaskInDB 能通过
        // 仅修改内存对象，beforeProcessTask 会立即写入 RUNNING 状态
        if (RetryTaskStatus.FAIL.getCode().equals(taskStatus)) {
            retryTask.setStatus(RetryTaskStatus.WAITING.getCode());
        }
        return retryTask;
    }

    /**
     * 如果在活跃事务中调用，打印警告日志。
     * invokeTask 内部的 DB 操作会参与外层事务，事务回滚会导致状态不一致。
     */
    private void warnIfInTransaction(String methodName) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            LOGGER.warn("[SimpleRetryTaskOperator#{}] 在活跃事务中调用，任务状态更新将参与外层事务。"
                    + "建议在事务提交后调用。", methodName);
        }
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
