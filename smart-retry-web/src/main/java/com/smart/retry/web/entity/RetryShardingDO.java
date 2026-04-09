package com.smart.retry.web.entity;

import com.smart.retry.common.model.BaseEntity;

import java.util.Date;

/**
 * 重试分片实体类（管理模块专用）
 */
public class RetryShardingDO extends BaseEntity {

    private String creatorId;

    private String instanceId;

    private Date lastHeartbeat;

    private int status;

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

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(String creatorId) {
        this.creatorId = creatorId;
    }
}
