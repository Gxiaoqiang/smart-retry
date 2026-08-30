package com.smart.retry.mybatis.dao;

import com.smart.retry.mybatis.entity.RetryTaskDO;
import com.smart.retry.mybatis.entity.query.RetryTaskQuery;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * @Author xiaoqiang
 * @Version RetryTaskDao.java, v 0.1 2025年02月16日 20:11 xiaoqiang
 * @Description: TODO
 */
public interface RetryTaskDao {

    long insert(RetryTaskDO retryTaskDO);


    int update(RetryTaskDO retryTaskDO);

    /**
     * 原子认领任务（乐观锁 CAS）。
     * 单参数，XML 直接引用 {@code #{id}} / {@code #{executor}} / {@code #{nextPlanTime}} / {@code #{shardingKey}}。
     *
     * @param retryTaskDO 仅需设置 id / executor / nextPlanTime / shardingKey 四个字段
     * @return 受影响行数：1=认领成功，0=已被他人认领/状态不满足
     */
    int claimTask(RetryTaskDO retryTaskDO);

    /**
     * 条件化写入终态（乐观锁 CAS 守卫）。
     * 单参数，XML 引用 {@code #{id}} / {@code #{status}} / {@code #{executor}} / {@code #{retryNum}} / {@code #{nextPlanTime}} / {@code #{attribute}}。
     * 守卫：DB executor == 传入值 且 retry_num == 传入值。
     *
     * @param retryTaskDO 需设置 id / status / executor / retryNum(扣减后) / nextPlanTime / attribute
     * @return 受影响行数：1=写入成功，0=租约已失效/状态漂移
     */
    int markTerminal(RetryTaskDO retryTaskDO);

    /**
     * 条件化标记"未注册 taskCode"任务为失败（乐观锁 CAS 守卫）。
     * 单参数，XML 引用 {@code #{id}} / {@code #{executor}} / {@code #{retryNum}} / {@code #{attribute}}。
     * 守卫：status IN (0,3) 且 retry_num == 传入值（扣减前）。
     *
     * @param retryTaskDO 需设置 id / executor / retryNum(扣减前) / attribute
     * @return 受影响行数：1=写入成功，0=任务已被认领/状态已变化
     */
    int markNullTaskObjectFail(RetryTaskDO retryTaskDO);

    /**
     * 条件化复活死信任务（乐观锁 CAS 守卫）。
     * 守卫：status = 1 且 gmt_modified < #{deadTaskTime}。
     *
     * @param id           任务 ID
     * @param deadTaskTime 超时判定时间点
     * @return 受影响行数：1=复活成功，0=任务已终态/已被其他实例复活
     */
    int reviveTask(@Param("id") Long id, @Param("deadTaskTime") Date deadTaskTime);


    int countByQuery(RetryTaskQuery retryTaskQuery);


    List<RetryTaskDO> selectByQuery(RetryTaskQuery retryTaskQuery);


    RetryTaskDO selectById(Long id);


    int deleteById(Long id);

    int deleteByGmtCreate(@Param("gmtCreate") Date  gmtCreate,
                          @Param("limitRows") int limitRows,
                          @Param("shardingKeyList") List<Long> shardingKeyList,
                          @Param("status") int status);

    /**
     * 批量删除任务
     */
    int batchDeleteByIds(@Param("ids") List<Long> ids);
}
