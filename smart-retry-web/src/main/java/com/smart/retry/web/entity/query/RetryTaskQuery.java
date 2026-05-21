package com.smart.retry.web.entity.query;

import java.util.Date;
import java.util.List;

/**
 * 任务查询对象（管理模块专用）
 */
public class RetryTaskQuery {
    
    private Long id;
    private Integer status;
    private List<Integer> statusList;
    private List<Long> shardingKeyList;
    private String taskCode;
    private Integer intervalSecond;
    private Date delayTime;
    private Date nextPlanTime;
    private Integer retryNum;
    private Date minNextPlanTime;
    private Date maxNextPlanTime;
    private Integer minRetryNum;
    private Integer maxRetryNum;
    private Date deadTaskTime;
    private Integer originRetryNum;
    private Integer minOriginRetryNum;
    private Integer maxOriginRetryNum;
    private String creator;
    private String uniqueKey;
    private String executor;
    
    // 创建时间范围
    private Date minGmtCreate;
    private Date maxGmtCreate;
    
    // 分页参数
    private int offset;
    private int limit;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getTaskCode() {
        return taskCode;
    }

    public void setTaskCode(String taskCode) {
        this.taskCode = taskCode;
    }

    public Integer getIntervalSecond() {
        return intervalSecond;
    }

    public void setIntervalSecond(Integer intervalSecond) {
        this.intervalSecond = intervalSecond;
    }

    public Date getDelayTime() {
        return delayTime;
    }

    public void setDelayTime(Date delayTime) {
        this.delayTime = delayTime;
    }

    public Date getNextPlanTime() {
        return nextPlanTime;
    }

    public void setNextPlanTime(Date nextPlanTime) {
        this.nextPlanTime = nextPlanTime;
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

    public Date getDeadTaskTime() {
        return deadTaskTime;
    }

    public void setDeadTaskTime(Date deadTaskTime) {
        this.deadTaskTime = deadTaskTime;
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

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public String getUniqueKey() {
        return uniqueKey;
    }

    public void setUniqueKey(String uniqueKey) {
        this.uniqueKey = uniqueKey;
    }

    public String getExecutor() {
        return executor;
    }

    public void setExecutor(String executor) {
        this.executor = executor;
    }

    public Date getMinGmtCreate() {
        return minGmtCreate;
    }

    public void setMinGmtCreate(Date minGmtCreate) {
        this.minGmtCreate = minGmtCreate;
    }

    public Date getMaxGmtCreate() {
        return maxGmtCreate;
    }

    public void setMaxGmtCreate(Date maxGmtCreate) {
        this.maxGmtCreate = maxGmtCreate;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }
}
