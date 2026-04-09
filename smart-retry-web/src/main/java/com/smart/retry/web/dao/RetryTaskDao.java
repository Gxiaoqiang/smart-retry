package com.smart.retry.web.dao;

import com.smart.retry.web.entity.RetryTaskDO;
import com.smart.retry.web.entity.query.RetryTaskQuery;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 重试任务DAO（管理模块专用）
 */
public interface RetryTaskDao {

    /**
     * 插入任务
     */
    long insert(RetryTaskDO retryTaskDO);

    /**
     * 更新任务
     */
    int update(RetryTaskDO retryTaskDO);

    /**
     * 根据ID查询
     */
    RetryTaskDO selectById(Long id);

    /**
     * 删除任务
     */
    int deleteById(Long id);

    /**
     * 批量删除任务
     */
    int batchDeleteByIds(@Param("ids") List<Long> ids);

    /**
     * 查询任务列表
     */
    List<RetryTaskDO> selectByQuery(RetryTaskQuery query);

    /**
     * 统计任务数量
     */
    int countByQuery(RetryTaskQuery query);

    /**
     * 统计任务状态分布
     */
    List<Map<String, Object>> countTaskStatusDistribution();

    /**
     * 统计各任务类型积压量
     */
    List<Map<String, Object>> countTaskBacklogByType();

    /**
     * 获取死信任务趋势
     */
    List<Map<String, Object>> getDeadLetterTrend(@Param("hours") int hours);

    /**
     * 统计任务处理速率
     */
    Double getTaskProcessRate(@Param("minutes") int minutes);

    /**
     * 统计指定实例下不能删除的任务数量
     * 包括：待执行、执行中、失败但重试次数大于0的任务
     */
    int countUndeletableTasksByShardingKey(@Param("shardingKey") long shardingKey);

    /**
     * 删除指定实例下的所有任务
     */
    int deleteByShardingKey(@Param("shardingKey") long shardingKey);
}
