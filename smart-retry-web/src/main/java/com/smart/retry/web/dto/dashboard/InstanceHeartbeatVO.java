package com.smart.retry.web.dto.dashboard;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 实例心跳信息
 */
@Data
public class InstanceHeartbeatVO {
    
    private String instanceId;
    
    private LocalDateTime lastHeartbeat;
    
    /**
     * 心跳延迟（秒）
     */
    private Long heartbeatDelay;
}
