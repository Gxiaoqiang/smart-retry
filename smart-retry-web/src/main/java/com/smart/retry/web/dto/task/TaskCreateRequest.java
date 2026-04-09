package com.smart.retry.web.dto.task;

import com.smart.retry.web.dto.PageRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 任务创建请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TaskCreateRequest extends PageRequest {
    
    @NotBlank(message = "taskCode不能为空")
    private String taskCode;
    
    @NotBlank(message = "taskDesc不能为空")
    private String taskDesc;
    
    @NotNull(message = "retryNum不能为空")
    private Integer retryNum;
    
    private Integer delaySecond = 100;
    
    private Integer intervalSecond = 600;
    
    @NotBlank(message = "param不能为空")
    private String param;
    
    @NotNull(message = "shardingKey不能为空")
    private Long shardingKey;
    
    private Integer nextPlanTimeStrategy = 0;
}
