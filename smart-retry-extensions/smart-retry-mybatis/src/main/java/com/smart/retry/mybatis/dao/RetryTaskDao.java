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


    int countByQuery(RetryTaskQuery retryTaskQuery);


    List<RetryTaskDO> selectByQuery(RetryTaskQuery retryTaskQuery);


    RetryTaskDO selectById(Long id);


    int deleteById(Long id);

    int deleteByGmtCreate(@Param("gmtCreate") Date  gmtCreate,
                          @Param("limitRows") int limitRows,
    @Param("shardingKeyList")List<Long> shardingKeyList);

}
