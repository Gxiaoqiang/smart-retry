package com.smart.retry.mybatis.entity;

import com.smart.retry.common.model.BaseEntity;

import java.util.Date;

/**
 * @Author xiaoqiang
 * @Version RetryTaskDO.java, v 0.1 2025年02月15日 21:19 xiaoqiang
 * @Description: TODO
 */
public class RetryTaskDO extends BaseEntity {

    /**
     * 重试任务的描述
     */
    private String taskDesc;

    /**
     * 重试任务的bean 名称
     */
    private String taskCode;


    /**
     * 参数
     */
    private String parameters;


    private long shardingKey;

    private String attribute;

    /**
     *
     * 任务状态 '最终执行状态 0:待执行,1:执行中,-1:执行失败,2:执行成功'
     */
    private int status;

    private int intervalSecond;

    private int delaySecond;

    private Date nextPlanTime;

    private int retryNum;



    private int originRetryNum;

    private String creator;

    private String executor;

    private Long currentLogId;

    private String uniqueKey;

    private int nextPlanTimeStrategy;

    public int getNextPlanTimeStrategy() {
        return nextPlanTimeStrategy;
    }

    public void setNextPlanTimeStrategy(int nextPlanTimeStrategy) {
        this.nextPlanTimeStrategy = nextPlanTimeStrategy;
    }

    public String getExecutor() {
        return executor;
    }

    public void setExecutor(String executor) {
        this.executor = executor;
    }

    public String getUniqueKey() {
        return uniqueKey;
    }

    public void setUniqueKey(String uniqueKey) {
        this.uniqueKey = uniqueKey;
    }

    public String getTaskDesc() {
        return taskDesc;
    }

    public void setTaskDesc(String taskDesc) {
        this.taskDesc = taskDesc;
    }

    public String getTaskCode() {
        return taskCode;
    }

    public void setTaskCode(String taskCode) {
        this.taskCode = taskCode;
    }

    public String getParameters() {
        return parameters;
    }

    public void setParameters(String parameters) {
        this.parameters = parameters;
    }

    public long getShardingKey() {
        return shardingKey;
    }

    public void setShardingKey(long shardingKey) {
        this.shardingKey = shardingKey;
    }


    public String getAttribute() {
        return attribute;
    }

    public void setAttribute(String attribute) {
        this.attribute = attribute;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getIntervalSecond() {
        return intervalSecond;
    }

    public void setIntervalSecond(int intervalSecond) {
        this.intervalSecond = intervalSecond;
    }

    public int getDelaySecond() {
        return delaySecond;
    }

    public void setDelaySecond(int delaySecond) {
        this.delaySecond = delaySecond;
    }

    public Date getNextPlanTime() {
        return nextPlanTime;
    }

    public void setNextPlanTime(Date nextPlanTime) {
        this.nextPlanTime = nextPlanTime;
    }

    public int getRetryNum() {
        return retryNum;
    }

    public void setRetryNum(int retryNum) {
        this.retryNum = retryNum;
    }


    public int getOriginRetryNum() {
        return originRetryNum;
    }

    public void setOriginRetryNum(int originRetryNum) {
        this.originRetryNum = originRetryNum;
    }


    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }


    public Long getCurrentLogId() {
        return currentLogId;
    }

    public void setCurrentLogId(Long currentLogId) {
        this.currentLogId = currentLogId;
    }
}
