package com.smart.retry.mybatis.entity.query;

import com.alibaba.fastjson.annotation.JSONField;

import java.util.Date;
import java.util.List;

/**
 * @Author xiaoqiang
 * @Version RetryTaskQuery.java, v 0.1 2025年02月15日 21:19 xiaoqiang
 * @Description: TODO
 */
public class RetryTaskQuery {


    private Long id;

    private List<Long> idList;
    private String taskCode;

    private Integer status;

    private List<Integer> statusList;

    private List<Long> shardingKeyList;


    private String creator;

    private Integer intervalSecond;

    private Integer delayTime;

    public Integer getIntervalSecond() {
        return intervalSecond;
    }

    public void setIntervalSecond(Integer intervalSecond) {
        this.intervalSecond = intervalSecond;
    }

    public Integer getDelayTime() {
        return delayTime;
    }

    public void setDelayTime(Integer delayTime) {
        this.delayTime = delayTime;
    }

    private Integer retryNum;

    private Date minNextPlanTime;

    private Date maxNextPlanTime;

    private Integer minRetryNum;

    private Integer maxRetryNum;

    private Integer originRetryNum;

    private Integer minOriginRetryNum;
    private Integer maxOriginRetryNum;

    private String executor;

    private String uniqueKey;

    private Date nextPlanTime;

    public Date getNextPlanTime() {
        return nextPlanTime;
    }

    public void setNextPlanTime(Date nextPlanTime) {
        this.nextPlanTime = nextPlanTime;
    }

    private int limit = 100;
    private int offset = 0;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUniqueKey() {
        return uniqueKey;
    }

    public void setUniqueKey(String uniqueKey) {
        this.uniqueKey = uniqueKey;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public List<Long> getIdList() {
        return idList;
    }

    public void setIdList(List<Long> idList) {
        this.idList = idList;
    }

    public String getTaskCode() {
        return taskCode;
    }

    public void setTaskCode(String taskCode) {
        this.taskCode = taskCode;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public List<Integer> getStatusList() {
        return statusList;
    }

    public void setStatusList(List<Integer> statusList) {
        this.statusList = statusList;
    }

    public List<Long> getShardingKeyList() {
        return shardingKeyList;
    }

    public void setShardingKeyList(List<Long> shardingKeyList) {
        this.shardingKeyList = shardingKeyList;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public Integer getRetryNum() {
        return retryNum;
    }

    public void setRetryNum(Integer retryNum) {
        this.retryNum = retryNum;
    }

    public Date getMinNextPlanTime() {
        return minNextPlanTime;
    }

    public void setMinNextPlanTime(Date minNextPlanTime) {
        this.minNextPlanTime = minNextPlanTime;
    }

    public Date getMaxNextPlanTime() {
        return maxNextPlanTime;
    }

    public void setMaxNextPlanTime(Date maxNextPlanTime) {
        this.maxNextPlanTime = maxNextPlanTime;
    }

    public Integer getMinRetryNum() {
        return minRetryNum;
    }

    public void setMinRetryNum(Integer minRetryNum) {
        this.minRetryNum = minRetryNum;
    }

    public Integer getMaxRetryNum() {
        return maxRetryNum;
    }

    public void setMaxRetryNum(Integer maxRetryNum) {
        this.maxRetryNum = maxRetryNum;
    }

    public Integer getOriginRetryNum() {
        return originRetryNum;
    }

    public void setOriginRetryNum(Integer originRetryNum) {
        this.originRetryNum = originRetryNum;
    }

    public Integer getMinOriginRetryNum() {
        return minOriginRetryNum;
    }

    public void setMinOriginRetryNum(Integer minOriginRetryNum) {
        this.minOriginRetryNum = minOriginRetryNum;
    }

    public Integer getMaxOriginRetryNum() {
        return maxOriginRetryNum;
    }

    public void setMaxOriginRetryNum(Integer maxOriginRetryNum) {
        this.maxOriginRetryNum = maxOriginRetryNum;
    }

    public String getExecutor() {
        return executor;
    }

    public void setExecutor(String executor) {
        this.executor = executor;
    }
}
