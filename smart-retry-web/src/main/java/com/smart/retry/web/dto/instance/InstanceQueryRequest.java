package com.smart.retry.web.dto.instance;

import com.smart.retry.web.dto.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 实例查询请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class InstanceQueryRequest extends PageRequest {
    
    private String creatorId;
    
    private String instanceId;
}
