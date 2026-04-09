package com.smart.retry.web.dao;

import com.smart.retry.web.entity.RetryShardingDO;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 重试分片DAO（管理模块专用）
 */
public interface RetryShardingDao {

    /**
     * 查询所有分片数据（分页）
     */
    List<RetryShardingDO> selectAllWithPage(@Param("offset") int offset, 
                                            @Param("limit") int limit,
                                            @Param("creatorId") String creatorId,
                                            @Param("instanceId") String instanceId);

    /**
     * 统计分片总数
     */
    long countAll(@Param("creatorId") String creatorId, @Param("instanceId") String instanceId);

    /**
     * 更新实例ID
     */
    int updateInstanceId(@Param("id") Long id, @Param("instanceId") String instanceId);

    /**
     * 根据ID查询分片
     */
    RetryShardingDO selectById(Long id);

    /**
     * 统计活跃实例数量
     */
    int countActiveInstances(@Param("timeoutSeconds") int timeoutSeconds);

    /**
     * 获取分片分布情况
     */
    List<Map<String, Object>> getShardingDistribution();

    /**
     * 获取实例心跳信息
     */
    List<Map<String, Object>> getInstanceHeartbeats();

    /**
     * 删除实例
     */
    int deleteById(Long id);
}
