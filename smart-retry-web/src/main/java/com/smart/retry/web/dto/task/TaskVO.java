package com.smart.retry.web.dto.task;

import lombok.Data;
import java.util.Date;

/**
 * 任务信息VO
 */
@Data
public class TaskVO {
    
    private Long id;
    
    private Date gmtCreate;
    
    private Date gmtModified;
    
    private Long shardingKey;
    
    private String taskDesc;
    
    private String taskCode;
    
    private String parameters;
    
    private String attribute;
    
    private Integer status;
    
    private Integer intervalSecond;
    
    private Integer delaySecond;
    
    private Integer maxExecuteTime;
    
    private Date nextPlanTime;
    
    private Integer retryNum;
    
    private String creator;
    
    /**
     * 分片信息，格式：ip(分片id)
     */
    private String shardingInfo;
    
    private String executor;
    
    private Integer originRetryNum;
    
    private Long currentLogId;
    
    private String uniqueKey;
    
    private Integer nextPlanTimeStrategy;
}
