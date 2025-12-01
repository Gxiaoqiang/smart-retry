package com.smart.retry.mybatis.repo.impl;

import com.smart.retry.mybatis.dao.RetryShardingDao;
import com.smart.retry.mybatis.entity.RetryShardingDO;
import com.smart.retry.mybatis.repo.RetryShardingRepo;

import java.util.Collections;
import java.util.List;

/**
 * @Author xiaoqiang
 * @Version RetryShardingRepoImpl.java, v 0.1 2025年02月16日 10:48 xiaoqiang
 * @Description: TODO
 */
public class RetryShardingRepoImpl implements RetryShardingRepo {

    private RetryShardingDao retryShardingDao;

    public RetryShardingRepoImpl(RetryShardingDao retryShardingDao) {
        this.retryShardingDao = retryShardingDao;
    }

    @Override
    public long saveRetrySharding(RetryShardingDO retrySharding) {
        return retryShardingDao.insert(retrySharding);
    }

    @Override
    public int updateLastHeartbeat(String instanceId, int status) {
        return retryShardingDao.updateLastHeartbeat(instanceId, status);
    }

    @Override
    public int scrambleDeadSharding(String instanceId, int status,int timeout) {



        return retryShardingDao.scrambleDeadSharding(instanceId, status,timeout);
    }

    @Override
    public List<RetryShardingDO> selectByInstanceId(String instanceId) {
        return retryShardingDao.selectByInstanceId(instanceId);
    }
}
