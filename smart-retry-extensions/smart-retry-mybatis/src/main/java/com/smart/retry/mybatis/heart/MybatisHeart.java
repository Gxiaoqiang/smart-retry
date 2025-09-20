package com.smart.retry.mybatis.heart;

import com.google.common.collect.Lists;
import com.smart.retry.common.RetryTaskHeart;
import com.smart.retry.common.utils.IpUtils;
import com.smart.retry.core.ShardingContextHolder;
import com.smart.retry.mybatis.entity.RetryShardingDO;
import com.smart.retry.mybatis.repo.RetryShardingRepo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import java.util.Date;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @Author xiaoqiang
 * @Version MybatisHeart.java, v 0.1 2025年02月15日 22:19 xiaoqiang
 * @Description: TODO
 */
public class MybatisHeart implements RetryTaskHeart {

    private static final Logger LOGGER = LoggerFactory.getLogger(MybatisHeart.class);

    private RetryShardingRepo retryShardingRepo; ;

    private static ScheduledExecutorService heartbeatExecutorService;

    private static ScheduledExecutorService scheduledExecutorService;

    public MybatisHeart(RetryShardingRepo retryShardingRepo) {
        this.retryShardingRepo = retryShardingRepo;
    }

    static {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                if(heartbeatExecutorService!= null){
                    heartbeatExecutorService.shutdown();
                }
                if(scheduledExecutorService!= null){
                    scheduledExecutorService.shutdown();
                }
            }
        });
    }
    /**
     * 初始化心跳
     */
    @Override
    public void initHeart() {
        String instanceId = IpUtils.getIp();
        //1.初始化时先更新当前实例的心跳
        retryShardingRepo.updateLastHeartbeat(instanceId, 1);

        //2.初始化sharding
        List<RetryShardingDO> retryShardingDOS = retryShardingRepo.selectByInstanceId(instanceId);
        List<Long> shardingIds = Lists.newArrayList();
        if(CollectionUtils.isEmpty(retryShardingDOS)){
            RetryShardingDO retryShardingDO = new RetryShardingDO();
            retryShardingDO.setIntanceId(instanceId);
            retryShardingDO.setLastHeartbeat(new Date());
            long shardingId = retryShardingRepo.saveRetrySharding(retryShardingDO);
            shardingIds.add(shardingId);
        }else{
            retryShardingDOS.forEach(retryShardingDO -> {
                shardingIds.add(retryShardingDO.getId());
            });
        }
        ShardingContextHolder.initShardingIndex(shardingIds);
    }

    @Override
    public void heartBeat() {
        heartbeatExecutorService = new ScheduledThreadPoolExecutor(1)   ;
        String instanceId = IpUtils.getIp();

        // 每隔5秒执行一次心跳
        heartbeatExecutorService.scheduleAtFixedRate(() -> {
            try {
               int heartBeatCount = retryShardingRepo.updateLastHeartbeat(instanceId, 1);
                LOGGER.info("[MybatisHeart#heartBeat] heart beat success, instanceId:{}, heartBeatCount:{}", instanceId, heartBeatCount);
            } catch (Exception e) {
                LOGGER.error("[MybatisHeart#heartBeat] heart beat error，instanceId:{}", instanceId, e);
            }
        }, 0, 5, java.util.concurrent.TimeUnit.SECONDS);



    }

    /**
     *
     */
    @Override
    public void scrambleDeadSharding() {
        scheduledExecutorService = new ScheduledThreadPoolExecutor(1)   ;
        String instanceId = IpUtils.getIp();


        scheduledExecutorService.scheduleAtFixedRate(() -> {
            try {
                int shardingCount = retryShardingRepo.scrambleDeadSharding(instanceId,1);
                List<RetryShardingDO> retryShardingDOS = retryShardingRepo.selectByInstanceId(instanceId);
                if(CollectionUtils.isEmpty(retryShardingDOS)){
                    initHeart();
                    return;
                }
                List<Long> shardingIds = Lists.newArrayList();
                retryShardingDOS.forEach(retryShardingDO -> {
                    shardingIds.add(retryShardingDO.getId());
                });
                ShardingContextHolder.initShardingIndex(shardingIds);
                if(LOGGER.isDebugEnabled()){
                    LOGGER.debug("[MybatisHeart#scrambleDeadSharding] scrambleDeadSharding success, instanceId:{}, shardingCount:{}", instanceId, shardingCount);
                }
            } catch (Exception e) {
                LOGGER.error("[MybatisHeart#scrambleDeadSharding] scrambleDeadSharding error，instanceId:{}", instanceId, e);
            }
        }, 1, 5, TimeUnit.SECONDS);



    }
}
