package com.smart.retry.mybatis.repo.impl;

import com.alibaba.fastjson.JSONObject;
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
    public void saveRetryTask(RetryTaskDO retryTask) {
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
            return;
        }
        long nextTime = System.currentTimeMillis() + retryTask.getDelaySecond() * 1000;
        retryTask.setNextPlanTime(new Date(nextTime));
        retryTask.setOriginRetryNum(retryTask.getRetryNum());
        retryTask.setCreator(IpUtils.getIp());
        retryTaskDao.insert(retryTask);

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
        //1.如果是执行中更新，则直接更新
        /*if (retryTask.getStatus() == RetryTaskStatus.RUNNING.getCode().intValue()) {
            return retryTaskDao.update(retryTask);
        }
        //2.如果是成功或者失败，则更新下次执行时间
        //3.如果是失败，如果还有重试次数，则更新为等待状态。如果已经达到最大重试次数，则更新为失败状态。
        if (retryTask.getStatus() == RetryTaskStatus.SUCCESS.getCode().intValue()
                ||retryTask.getStatus() == RetryTaskStatus.FAIL.getCode().intValue()) {
            long nextTime = oldTask.getNextPlanTime().getTime() + oldTask.getIntervalSecond() * 1000;
            retryTask.setNextPlanTime(new Date(nextTime));
            if (retryTask.getRetryNum() >= 1) {
                retryTask.setRetryNum(retryTask.getRetryNum() - 1);
            }
            return retryTaskDao.update(retryTask);
        }

        logger.warn("[RetryTaskRepoImpl-updateRetryTask]retryTask status not support, retryTask:{}", JSONObject.toJSONString(retryTask));
        return -1;*/
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
        query.setStatusList(Lists.newArrayList(RetryTaskStatus.RUNNING.getCode()));
        query.setOffset(0);
        query.setLimit(1000);

        return retryTaskDao.selectByQuery(query);


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
        query.setLimit(1000);
        return retryTaskDao.selectByQuery(query);
    }
}
