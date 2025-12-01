package com.smart.retry.mybatis.dao;

import com.smart.retry.mybatis.entity.RetryShardingDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @Author xiaoqiang
 * @Version RetryShardingDao.java, v 0.1 2025年02月15日 23:56 xiaoqiang
 * @Description: TODO
 */
public interface RetryShardingDao {

    /**
     * 插入分区数据
     * @param retryShardingDO
     * @return
     */
    long insert(RetryShardingDO  retryShardingDO);


    /**
     * 更新最后心跳时间
     * @param instanceId
     * @return
     */
    int updateLastHeartbeat(@Param("instanceId") String instanceId, @Param("status") int status);


    /**
     * 争抢不活跃分区
     * @param instanceId
     * @return
     */
    int scrambleDeadSharding(@Param("instanceId") String instanceId,
                             @Param("status") int status,@Param("timeout") int timeout);

    /**
     * 根据实例id查询分区数据
     * @param instanceId
     * @return
     */
    List<RetryShardingDO> selectByInstanceId(String instanceId);
}
