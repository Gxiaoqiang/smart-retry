package com.smart.retry.mybatis.repo.impl;

import com.google.common.collect.Lists;
import com.smart.retry.common.constant.RetryTaskStatus;
import com.smart.retry.common.utils.IpUtils;
import com.smart.retry.core.ShardingContextHolder;
import com.smart.retry.mybatis.dao.RetryTaskDao;
import com.smart.retry.mybatis.entity.RetryTaskDO;
import com.smart.retry.mybatis.entity.query.RetryTaskQuery;
import com.smart.retry.mybatis.repo.RetryTaskRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import java.util.Date;
import java.util.List;

/**
 * @Author xiaoqiang
 * @Version RetryTaskRepoImpl.java, v 0.1 2025年02月16日 21:09 xiaoqiang
 * @Description: TODO
 */
public class RetryTaskRepoImpl implements RetryTaskRepo {

    private final Logger logger = LoggerFactory.getLogger(RetryTaskRepoImpl.class);

    private RetryTaskDao retryTaskDao;

    public RetryTaskRepoImpl(RetryTaskDao retryTaskDao) {
        this.retryTaskDao = retryTaskDao;
    }

    @Override
    public long saveRetryTask(RetryTaskDO retryTask) {
        String uniqueKey = retryTask.getUniqueKey();
        RetryTaskQuery retryTaskQuery = new RetryTaskQuery();
        retryTaskQuery.setUniqueKey(uniqueKey);
        retryTask.setShardingKey(ShardingContextHolder.getRandomShardingIndex());
        retryTaskQuery.setTaskCode(retryTask.getTaskCode());
        retryTaskQuery.setStatusList(Lists.newArrayList(RetryTaskStatus.WAITING.getCode(),
                RetryTaskStatus.RUNNING.getCode()));
        List<RetryTaskDO> retryTaskList = retryTaskDao.selectByQuery(retryTaskQuery);
        if (retryTaskList.size() > 0) {
            logger.warn("[RetryTaskRepoImpl-saveRetryTask]uniqueKey:{} already exists, skip insert", uniqueKey);
            return -1;
        }
        //long nextTime = System.currentTimeMillis() + retryTask.getDelaySecond() * 1000;
        //retryTask.setNextPlanTime(new Date(nextTime));
        retryTask.setOriginRetryNum(retryTask.getRetryNum());
        retryTask.setCreator(IpUtils.getIp());
        retryTaskDao.insert(retryTask);
        return retryTask.getId();
    }

    @Override
    public int updateRetryTask(RetryTaskDO retryTask) {

        long taskId = retryTask.getId();
        RetryTaskDO oldTask = retryTaskDao.selectById(taskId);
        if (oldTask == null) {
            logger.warn("[RetryTaskRepoImpl-updateRetryTask]retryTask not exists, id:{}", taskId);
            return 0;
        }

        return retryTaskDao.update(retryTask);
    }

    @Override
    public int claimRetryTask(RetryTaskDO retryTask) {
        // 关键：单条原子 UPDATE 直接透传，禁止先 selectById 再 update，
        // 否则在"读取状态 -> 更新"之间会引入 TOCTOU 竞态窗口。
        return retryTaskDao.claimTask(retryTask);
    }

    @Override
    public int markRetryTaskTerminal(RetryTaskDO retryTask) {
        // 关键：单条原子 UPDATE 直接透传，禁止先查后改，
        // 守卫 executor + retry_num，防止 stale 副本覆盖复活状态/新租约。
        return retryTaskDao.markTerminal(retryTask);
    }

    @Override
    public int markNullTaskObjectFail(RetryTaskDO retryTask) {
        // 关键：单条原子 UPDATE 直接透传，禁止先查后改，
        // 守卫 status IN (0,3) + retry_num，防止覆盖他方已认领的 RUNNING。
        return retryTaskDao.markNullTaskObjectFail(retryTask);
    }

    @Override
    public int reviveDeadRetryTask(Long id, Date deadTaskTime) {
        // 关键：单条原子 UPDATE 直接透传，禁止先查后改，
        // 守卫 status = 1 + gmt_modified，防止复活已终态任务或覆盖刚写入的终态。
        return retryTaskDao.reviveTask(id, deadTaskTime);
    }

    @Override
    public RetryTaskDO getRetryTask(long id) {
        return retryTaskDao.selectById(id);
    }

    //获取所有执行中的任务，并且超过最大的执行时间
    @Override
    public List<RetryTaskDO> listAllDeadTask(Date deadTaskTime) {
        RetryTaskQuery query = new RetryTaskQuery();
        //如果获取不到分区，则返回空列表，不执行任何重试任务
        List<Long> shardingKeyList = ShardingContextHolder.shardingIndex();
        if (CollectionUtils.isEmpty(shardingKeyList)) {
            return Lists.newArrayList();
        }
        long currentTime = System.currentTimeMillis();
        //Date deadTaskTime = new Date(currentTime - maxExecuteTime * 1000);
        query.setDeadTaskTime(deadTaskTime);
        query.setShardingKeyList(shardingKeyList);
        query.setStatusList(Lists.newArrayList(RetryTaskStatus.RUNNING.getCode()));
        query.setOffset(0);
        query.setLimit(1000);

        return retryTaskDao.selectByQuery(query);


    }

    @Override
    public int deleteRetryTask(long taskId) {
        return retryTaskDao.deleteById(taskId);
    }

    @Override
    public List<RetryTaskDO> listAllWaitingRetryTask() {
        RetryTaskQuery query = new RetryTaskQuery();
        //如果获取不到分区，则返回空列表，不执行任何重试任务
        List<Long> shardingKeyList = ShardingContextHolder.shardingIndex();
        if (CollectionUtils.isEmpty(shardingKeyList)) {
            return Lists.newArrayList();
        }
        query.setShardingKeyList(ShardingContextHolder.shardingIndex());
        query.setStatusList(Lists.newArrayList(RetryTaskStatus.WAITING.getCode(), RetryTaskStatus.FAIL.getCode()));
        query.setMinRetryNum(1);
        query.setMaxNextPlanTime(new Date());
        //默认查询1000条数据
        query.setOffset(0);
        query.setLimit(500);
        return retryTaskDao.selectByQuery(query);
    }

    @Override
    public List<RetryTaskDO> listAllWaitingRetryTask(Date maxNextPlanTime, int limit) {
        RetryTaskQuery query = new RetryTaskQuery();
        //如果获取不到分区，则返回空列表，不执行任何重试任务
        List<Long> shardingKeyList = ShardingContextHolder.shardingIndex();
        if (CollectionUtils.isEmpty(shardingKeyList)) {
            return Lists.newArrayList();
        }
        query.setShardingKeyList(shardingKeyList);
        query.setStatusList(Lists.newArrayList(RetryTaskStatus.WAITING.getCode(), RetryTaskStatus.FAIL.getCode()));
        query.setMinRetryNum(1);
        query.setMaxNextPlanTime(maxNextPlanTime != null ? maxNextPlanTime : new Date());
        query.setOffset(0);
        query.setLimit(limit);
        return retryTaskDao.selectByQuery(query);
    }

    @Override
    public int deleteByGmtCreate(Date gmtCreate, int limitRows, int status) {
        int deleteCount = 0;
        while (true) {
            int deleteRows = retryTaskDao.deleteByGmtCreate(gmtCreate,
                    limitRows,
                    ShardingContextHolder.shardingIndex(),
                    status);
            deleteCount += deleteRows;
            if (deleteRows < limitRows) {
                break;
            }
        }
        return deleteCount;
    }
}
