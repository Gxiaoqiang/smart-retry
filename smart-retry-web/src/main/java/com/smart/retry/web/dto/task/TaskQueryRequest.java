package com.smart.retry.web.dto.task;

import com.smart.retry.web.dto.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 任务查询请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TaskQueryRequest extends PageRequest {
    
    private Long id;
    
    private String taskCode;
    
    private String taskDesc;
    
    private Integer status;
    
    private Long shardingKey;
    
    private String creator;
    
    /**
     * 创建时间范围
     */
    private Date gmtCreateStart;
    private Date gmtCreateEnd;
}
