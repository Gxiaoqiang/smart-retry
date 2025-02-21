package com.smart.retry.mybatis.repo;

import com.smart.retry.mybatis.entity.RetryTaskDO;
import com.smart.retry.mybatis.entity.query.RetryTaskQuery;

import java.util.List;

/**
 * @Author xiaoqiang
 * @Version RetryTaskRepo.java, v 0.1 2025年02月16日 21:03 xiaoqiang
 * @Description: TODO
 */
public interface RetryTaskRepo {

    void saveRetryTask(RetryTaskDO retryTask);



    int updateRetryTask(RetryTaskDO retryTask);

    RetryTaskDO getRetryTask(long id);

    List<RetryTaskDO> listAllWaitingRetryTask();

}
