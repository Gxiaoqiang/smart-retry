package com.smart.retry.mybatis.repo;

import com.smart.retry.mybatis.entity.RetryShardingDO;

import java.util.List;

/**
 * @Author xiaoqiang
 * @Version RetryShardingRepo.java, v 0.1 2025年02月15日 23:55 xiaoqiang
 * @Description: TODO
 */
public interface RetryShardingRepo {

    long saveRetrySharding(RetryShardingDO retrySharding);

    /**
     * 更新最后心跳时间
     * @param instanceId
     * @return
     */
    int updateLastHeartbeat(String instanceId,int status);


    /**
     * 争抢不活跃分区
     * @param instanceId
     * @return
     */
    int scrambleDeadSharding(String instanceId,int status);

    /**
     * 根据实例id查询分区数据
     * @param instanceId
     * @return
     */
    List<RetryShardingDO> selectByInstanceId(String instanceId);
}
