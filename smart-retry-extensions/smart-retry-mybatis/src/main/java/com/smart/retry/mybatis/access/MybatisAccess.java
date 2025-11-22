package com.smart.retry.mybatis.access;

import com.smart.retry.common.RetryTaskAccess;
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

        Date deadTaskTime = new Date(currentTime - maxExecuteTime * 1000);

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
    public void saveRetryTask(RetryTask retryTask) {
        RetryTaskDO retryTaskDO = new RetryTaskDO();
        BeanUtils.copyProperties(retryTask, retryTaskDO);
        retryTaskRepo.saveRetryTask(retryTaskDO);
    }

    @Override
    public void updateRetryTask(RetryTask retryTask) {
        RetryTaskDO retryTaskDO = new RetryTaskDO();
        BeanUtils.copyProperties(retryTask, retryTaskDO);
        retryTaskRepo.updateRetryTask(retryTaskDO);

    }

    @Override
    public void deleteRetryTask(long taskId) {
        //retryTaskRepo.deleteRetryTask(taskId);
    }

    @Override
    public void stopRetryTask(long taskId) {

    }

    @Override
    public int deleteHistoryRetryTask(int clearBeforeDays, int limitRows) {
        Date clearBeforeDate = new Date(System.currentTimeMillis() - clearBeforeDays * 24 * 60 * 60 * 1000);

        return retryTaskRepo.deleteByGmtCreate(clearBeforeDate, limitRows);

    }
}
