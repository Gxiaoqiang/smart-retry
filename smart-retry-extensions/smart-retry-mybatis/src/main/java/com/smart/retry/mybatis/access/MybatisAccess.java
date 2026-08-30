package com.smart.retry.mybatis.access;

import com.smart.retry.common.RetryTaskAccess;
import com.smart.retry.common.constant.RetryTaskStatus;
import com.smart.retry.common.model.RetryTask;
import com.smart.retry.mybatis.entity.RetryTaskDO;
import com.smart.retry.mybatis.repo.RetryTaskRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @Author xiaoqiang
 * @Version MybatisAccess.java, v 0.1 2025年02月15日 22:13 xiaoqiang
 * @Description: TODO
 */
public class MybatisAccess implements RetryTaskAccess {


    private static final Logger LOGGER = LoggerFactory.getLogger(MybatisAccess.class);


    private RetryTaskRepo retryTaskRepo;

    public MybatisAccess(RetryTaskRepo retryTaskRepo) {
        this.retryTaskRepo = retryTaskRepo;
    }


    @Override
    public List<RetryTask> listDeadTask(int maxExecuteTime) {
        long currentTime = System.currentTimeMillis();

        Date deadTaskTime = new Date(currentTime - maxExecuteTime * 1000L);

        List<RetryTaskDO> retryTaskDOS = retryTaskRepo.listAllDeadTask(deadTaskTime);
        if (CollectionUtils.isEmpty(retryTaskDOS)) {
            return Collections.emptyList();
        }
        List<RetryTask> retryTasks = new ArrayList<>(retryTaskDOS.size());
        for (RetryTaskDO retryTask : retryTaskDOS) {
            RetryTask retryTaskDo = new RetryTask();
            BeanUtils.copyProperties(retryTask, retryTaskDo);
            retryTasks.add(retryTaskDo);
        }
        return retryTasks;
    }


    public RetryTask getRetryTask(long taskId) {
        RetryTaskDO retryTask = retryTaskRepo.getRetryTask(taskId);
        if (retryTask == null) {
            return null;
        }
        RetryTask retryTaskDo = new RetryTask();
        BeanUtils.copyProperties(retryTask, retryTaskDo);
        return retryTaskDo;
    }
    @Override
    public List<RetryTask> listRetryTask() {
        List<RetryTaskDO> retryTaskDOS = retryTaskRepo.listAllWaitingRetryTask();
        if (CollectionUtils.isEmpty(retryTaskDOS)) {
            return Collections.emptyList();
        }
        List<RetryTask> retryTasks = new ArrayList<>(retryTaskDOS.size());
        for (RetryTaskDO retryTask : retryTaskDOS) {
            RetryTask retryTaskDo = new RetryTask();
            BeanUtils.copyProperties(retryTask, retryTaskDo);
            retryTasks.add(retryTaskDo);
        }
        return retryTasks;
    }

    @Override
    public List<RetryTask> listRetryTask(Date maxNextPlanTime, int limit) {
        List<RetryTaskDO> retryTaskDOS = retryTaskRepo.listAllWaitingRetryTask(maxNextPlanTime, limit);
        if (CollectionUtils.isEmpty(retryTaskDOS)) {
            return Collections.emptyList();
        }
        List<RetryTask> retryTasks = new ArrayList<>(retryTaskDOS.size());
        for (RetryTaskDO retryTask : retryTaskDOS) {
            RetryTask retryTaskDo = new RetryTask();
            BeanUtils.copyProperties(retryTask, retryTaskDo);
            retryTasks.add(retryTaskDo);
        }
        return retryTasks;
    }

    @Override
    public long saveRetryTask(RetryTask retryTask) {

        long nextTime = System.currentTimeMillis() + retryTask.getDelaySecond() * 1000;
        retryTask.setNextPlanTime(new Date(nextTime));

        RetryTaskDO retryTaskDO = new RetryTaskDO();
        BeanUtils.copyProperties(retryTask, retryTaskDO);
       return retryTaskRepo.saveRetryTask(retryTaskDO);
    }

    @Override
    public void updateRetryTask(RetryTask retryTask) {
        RetryTaskDO retryTaskDO = new RetryTaskDO();
        BeanUtils.copyProperties(retryTask, retryTaskDO);
        retryTaskRepo.updateRetryTask(retryTaskDO);

    }

    @Override
    public int claimRetryTask(Long id, String executor, Date nextPlanTime, Long shardingKey) {
        // 仅设置认领所需字段，委托 Repo 执行单条原子 UPDATE（乐观锁 CAS）
        RetryTaskDO retryTaskDO = new RetryTaskDO();
        retryTaskDO.setId(id);
        retryTaskDO.setExecutor(executor);
        retryTaskDO.setNextPlanTime(nextPlanTime);
        retryTaskDO.setShardingKey(shardingKey);
        return retryTaskRepo.claimRetryTask(retryTaskDO);
    }

    @Override
    public int markRetryTaskTerminal(Long id, int status, String executor, int retryNum, Date nextPlanTime, String attribute) {
        // 仅设置终态写入所需字段，委托 Repo 执行单条原子 UPDATE（乐观锁 CAS 守卫）
        RetryTaskDO retryTaskDO = new RetryTaskDO();
        retryTaskDO.setId(id);
        retryTaskDO.setStatus(status);
        retryTaskDO.setExecutor(executor);
        retryTaskDO.setRetryNum(retryNum);
        retryTaskDO.setNextPlanTime(nextPlanTime);
        retryTaskDO.setAttribute(attribute);
        return retryTaskRepo.markRetryTaskTerminal(retryTaskDO);
    }

    @Override
    public int markNullTaskObjectFail(Long id, String executor, int retryNum, String attribute) {
        // 仅设置"未注册 taskCode"失败标记所需字段，委托 Repo 执行单条原子 UPDATE（乐观锁 CAS 守卫）
        RetryTaskDO retryTaskDO = new RetryTaskDO();
        retryTaskDO.setId(id);
        retryTaskDO.setExecutor(executor);
        retryTaskDO.setRetryNum(retryNum);
        retryTaskDO.setAttribute(attribute);
        return retryTaskRepo.markNullTaskObjectFail(retryTaskDO);
    }

    @Override
    public int reviveDeadRetryTask(Long id, Date deadTaskTime) {
        // 单条原子 UPDATE（乐观锁 CAS 守卫）：仅复活 RUNNING 且确认超时的任务
        return retryTaskRepo.reviveDeadRetryTask(id, deadTaskTime);
    }

    @Override
    public void deleteRetryTask(long taskId) {
        retryTaskRepo.deleteRetryTask(taskId);
    }

    @Override
    public void stopRetryTask(long taskId) {

    }

    @Override
    public int deleteHistoryRetryTask(int clearBeforeDays, int limitRows) {
        Date clearBeforeDate = new Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(clearBeforeDays));

        return retryTaskRepo.deleteByGmtCreate(clearBeforeDate, limitRows, RetryTaskStatus.SUCCESS.getCode());

    }
}
