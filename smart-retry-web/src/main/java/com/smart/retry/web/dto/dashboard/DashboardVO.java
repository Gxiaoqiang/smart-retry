package com.smart.retry.web.dto.dashboard;

import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * 仪表盘监控数据
 */
@Data
public class DashboardVO {
    
    /**
     * 活跃实例数量
     */
    private Integer activeInstanceCount;
    
    /**
     * 分片分布情况 key: instanceId, value: 分片数量
     */
    private Map<String, Integer> shardingDistribution;
    
    /**
     * 实例心跳延迟列表
     */
    private List<InstanceHeartbeatVO> instanceHeartbeats;
    
    /**
     * 任务状态分布 key: status, value: count
     */
    private Map<Integer, Long> taskStatusDistribution;
    
    /**
     * 各任务类型积压量 key: taskCode, value: count
     */
    private Map<String, Long> taskBacklogByType;
    
    /**
     * 死信任务趋势（最近24小时）
     */
    private List<DeadLetterTrendVO> deadLetterTrend;
    
    /**
     * 任务处理速率
     */
    private Double taskProcessRate;
}
