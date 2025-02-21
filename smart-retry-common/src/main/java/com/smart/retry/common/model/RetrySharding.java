package com.smart.retry.common.model;

import java.util.Date;

/**
 * @Author xiaoqiang
 * @Version RetryShardingMetata.java, v 0.1 2025年02月11日 18:16 xiaoqiang
 * @Description:  重试分片实体类
 */
public class RetrySharding extends BaseEntity{

    private Long shardingKey;

    private String instanceId;

    private Date lastHeartbeat;

    /**
     * 分片状态 0 未被占用，1 被占用
     */
    private int status;

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }


    public Long getShardingKey() {
        return shardingKey;
    }

    public void setShardingKey(Long shardingKey) {
        this.shardingKey = shardingKey;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public Date getLastHeartbeat() {
        return lastHeartbeat;
    }

    public void setLastHeartbeat(Date lastHeartbeat) {
        this.lastHeartbeat = lastHeartbeat;
    }
}
