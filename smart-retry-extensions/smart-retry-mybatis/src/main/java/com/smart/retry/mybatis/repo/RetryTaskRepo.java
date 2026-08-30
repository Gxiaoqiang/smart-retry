package com.smart.retry.mybatis.repo;

import com.smart.retry.mybatis.entity.RetryTaskDO;
import com.smart.retry.mybatis.entity.query.RetryTaskQuery;

import java.util.Date;
import java.util.List;

/**
 * @Author xiaoqiang
 * @Version RetryTaskRepo.java, v 0.1 2025年02月16日 21:03 xiaoqiang
 * @Description: TODO
 */
public interface RetryTaskRepo {

    long saveRetryTask(RetryTaskDO retryTask);



    int updateRetryTask(RetryTaskDO retryTask);

    /**
     * 原子认领任务（乐观锁 CAS）。
     *
     * <p><b>注意：与 {@link #updateRetryTask}（先 selectById 再 update）不同，
     * 本方法必须直接透传单条原子 UPDATE，禁止先查后改</b>，否则会在
     * "读取状态 -> 更新"之间重新引入 TOCTOU 竞态窗口，导致业务方法被重复执行。
     *
     * @param retryTask 仅需设置 id / executor / nextPlanTime / shardingKey 四个字段
     * @return 受影响行数：1=认领成功，0=已被他人认领/状态不满足
     */
    int claimRetryTask(RetryTaskDO retryTask);

    /**
     * 条件化写入终态（乐观锁 CAS 守卫）。
     *
     * <p><b>注意：与 {@link #updateRetryTask}（先 selectById 再 update）不同，
     * 本方法必须直接透传单条原子 UPDATE，禁止先查后改</b>，否则会重新引入 TOCTOU 竞态。
     * 守卫：DB {@code executor} == 传入值 且 {@code retry_num} == 传入值（认领后扣减值）。
     *
     * @param retryTask 需设置 id / status / executor / retryNum(扣减后) / nextPlanTime / attribute
     * @return 受影响行数：1=写入成功，0=租约已失效/状态漂移
     */
    int markRetryTaskTerminal(RetryTaskDO retryTask);

    /**
     * 条件化标记"未注册 taskCode"任务为失败（乐观锁 CAS 守卫）。
     *
     * <p>单条原子 UPDATE，禁止先查后改。守卫：status IN (0,3) 且 retry_num == 传入值（扣减前）。
     *
     * @param retryTask 需设置 id / executor / retryNum(扣减前) / attribute
     * @return 受影响行数：1=写入成功，0=任务已被认领/状态已变化
     */
    int markNullTaskObjectFail(RetryTaskDO retryTask);

    /**
     * 条件化复活死信任务（乐观锁 CAS 守卫）。
     *
     * <p>单条原子 UPDATE，禁止先查后改。守卫：status = 1 且 gmt_modified < deadTaskTime。
     *
     * @param id           任务 ID
     * @param deadTaskTime 超时判定时间点
     * @return 受影响行数：1=复活成功，0=任务已终态/已被其他实例复活
     */
    int reviveDeadRetryTask(Long id, Date deadTaskTime);

    RetryTaskDO getRetryTask(long id);

    List<RetryTaskDO> listAllWaitingRetryTask();

    List<RetryTaskDO> listAllWaitingRetryTask(Date maxNextPlanTime, int limit);

    List<RetryTaskDO> listAllDeadTask(Date deadTaskTime);


    int deleteRetryTask(long taskId);



    int deleteByGmtCreate(Date gmtCreate, int limitRows, int status);
}
