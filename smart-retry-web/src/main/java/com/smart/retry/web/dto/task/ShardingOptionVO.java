package com.smart.retry.web.dto.task;

import lombok.Data;

/**
 * 分片选择项VO
 */
@Data
public class ShardingOptionVO {
    
    private Long shardingKey;
    
    private String instanceId;
    
    private String displayText;
}
