package com.smart.retry.web.service;

import com.smart.retry.web.dao.RetryShardingDao;
import com.smart.retry.web.dao.RetryTaskDao;
import com.smart.retry.web.dto.dashboard.DashboardVO;
import com.smart.retry.web.dto.dashboard.DeadLetterTrendVO;
import com.smart.retry.web.dto.dashboard.InstanceHeartbeatVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

/**
 * 仪表盘监控服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {
    
    private final RetryShardingDao retryShardingDao;
    private final RetryTaskDao retryTaskDao;
    
    /**
     * 获取仪表盘监控数据
     */
    public DashboardVO getDashboardData() {
        DashboardVO dashboard = new DashboardVO();
        
        // 1. 活跃实例数量
        dashboard.setActiveInstanceCount(retryShardingDao.countActiveInstances(10));
        
        // 2. 分片分布情况
        List<Map<String, Object>> shardingDist = retryShardingDao.getShardingDistribution();
        Map<String, Integer> shardingMap = new HashMap<>();
        for (Map<String, Object> item : shardingDist) {
            String instanceId = (String) item.get("instanceId");
            Number count = (Number) item.get("count");
            shardingMap.put(instanceId, count != null ? count.intValue() : 0);
        }
        dashboard.setShardingDistribution(shardingMap);
        
        // 3. 实例心跳信息
        List<Map<String, Object>> heartbeats = retryShardingDao.getInstanceHeartbeats();
        List<InstanceHeartbeatVO> heartbeatList = new ArrayList<>();
        for (Map<String, Object> item : heartbeats) {
            InstanceHeartbeatVO vo = new InstanceHeartbeatVO();
            vo.setInstanceId((String) item.get("instanceId"));
            // MyBatis返回的是LocalDateTime，直接设置
            Object lastHeartbeatObj = item.get("lastHeartbeat");
            if (lastHeartbeatObj instanceof LocalDateTime) {
                vo.setLastHeartbeat((LocalDateTime) lastHeartbeatObj);
            } else if (lastHeartbeatObj instanceof Date) {
                // 兼容旧版本，将Date转换为LocalDateTime
                vo.setLastHeartbeat(((Date) lastHeartbeatObj).toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime());
            }
            Number delay = (Number) item.get("heartbeatDelay");
            vo.setHeartbeatDelay(delay != null ? delay.longValue() : 0L);
            heartbeatList.add(vo);
        }
        dashboard.setInstanceHeartbeats(heartbeatList);
        
        // 4. 任务状态分布
        List<Map<String, Object>> statusDist = retryTaskDao.countTaskStatusDistribution();
        Map<Integer, Long> statusMap = new HashMap<>();
        for (Map<String, Object> item : statusDist) {
            Number status = (Number) item.get("status");
            Number count = (Number) item.get("count");
            statusMap.put(status != null ? status.intValue() : -1, 
                         count != null ? count.longValue() : 0L);
        }
        dashboard.setTaskStatusDistribution(statusMap);
        
        // 5. 各任务类型积压量
        List<Map<String, Object>> backlogByType = retryTaskDao.countTaskBacklogByType();
        Map<String, Long> backlogMap = new HashMap<>();
        for (Map<String, Object> item : backlogByType) {
            String taskCode = (String) item.get("taskCode");
            Number count = (Number) item.get("count");
            backlogMap.put(taskCode, count != null ? count.longValue() : 0L);
        }
        dashboard.setTaskBacklogByType(backlogMap);
        
        // 6. 死信任务趋势
        List<Map<String, Object>> deadLetterData = retryTaskDao.getDeadLetterTrend(24);
        List<DeadLetterTrendVO> trendList = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        for (Map<String, Object> item : deadLetterData) {
            DeadLetterTrendVO vo = new DeadLetterTrendVO();
            try {
                String timeStr = (String) item.get("timePoint");
                if (timeStr != null) {
                    vo.setTimePoint(sdf.parse(timeStr));
                }
            } catch (Exception e) {
                log.error("[DashboardService#getDashboardData]解析时间失败: {}", item.get("timePoint"), e);
            }
            Number count = (Number) item.get("count");
            vo.setCount(count != null ? count.longValue() : 0L);
            trendList.add(vo);
        }
        dashboard.setDeadLetterTrend(trendList);
        
        // 7. 任务处理速率
        Double rate = retryTaskDao.getTaskProcessRate(5);
        dashboard.setTaskProcessRate(rate != null ? rate : 0.0);
        
        return dashboard;
    }
}
