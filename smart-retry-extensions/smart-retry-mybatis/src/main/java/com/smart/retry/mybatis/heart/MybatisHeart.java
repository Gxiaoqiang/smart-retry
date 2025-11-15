package com.smart.retry.mybatis.heart;

import com.google.common.collect.Lists;
import com.smart.retry.common.RetryTaskHeart;
import com.smart.retry.common.SmartRtryExit;
import com.smart.retry.common.utils.IpUtils;
import com.smart.retry.core.ShardingContextHolder;
import com.smart.retry.mybatis.entity.RetryShardingDO;
import com.smart.retry.mybatis.repo.RetryShardingRepo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @Author xiaoqiang
 * @Version MybatisHeart.java, v 0.1 2025年02月15日 22:19 xiaoqiang
 * @Description: TODO
 */
public class MybatisHeart implements RetryTaskHeart {

    private static final Logger LOGGER = LoggerFactory.getLogger(MybatisHeart.class);

    private RetryShardingRepo retryShardingRepo;


    private static volatile Boolean flag = true;

    public MybatisHeart(RetryShardingRepo retryShardingRepo) {
        this.retryShardingRepo = retryShardingRepo;
    }


    public void destroy() {
        flag = false;
    }

    static {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                flag = false;
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
        if(CollectionUtils.isEmpty(retryShardingDOS)){
            RetryShardingDO retryShardingDO = new RetryShardingDO();
            retryShardingDO.setIntanceId(instanceId);
            retryShardingDO.setLastHeartbeat(new Date());
            retryShardingRepo.saveRetrySharding(retryShardingDO);
            retryShardingDOS = retryShardingRepo.selectByInstanceId(instanceId);
        }
        List<Long> existShardingIds = retryShardingDOS.stream()
                .map(RetryShardingDO::getId)
                .collect(Collectors.toList());
        ShardingContextHolder.initShardingIndex(existShardingIds);
    }


    class HeartbeatTask implements Runnable {
        @Override
        public void run() {
            String instanceId = IpUtils.getIp();

            while (SmartRtryExit.isExit()){
                try {
                    TimeUnit.SECONDS.sleep(3);
                    int heartBeatCount = retryShardingRepo.updateLastHeartbeat(instanceId, 1);
                    if(LOGGER.isDebugEnabled()){
                        LOGGER.debug("[MybatisHeart#heartBeat] heart beat success, instanceId:{}, heartBeatCount:{}", instanceId, heartBeatCount);
                    }

                } catch (Exception e) {
                    LOGGER.error("[MybatisHeart#heartBeat] heart beat error，instanceId:{}", instanceId, e);
                }
            }
        }
    }

    class ScrambleDeadShardingTask implements Runnable {
        @Override
        public void run() {
            String instanceId = IpUtils.getIp();

            while (SmartRtryExit.isExit()){
                try {
                    TimeUnit.SECONDS.sleep(5);
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
            }
        }
    }
    @Override
    public void heartBeat() {
        Thread heartbeatThread = new Thread(new HeartbeatTask());
        heartbeatThread.setDaemon(true);
        heartbeatThread.start();

    }

    /**
     *
     */
    @Override
    public void scrambleDeadSharding() {
        Thread scrambleDeadShardingThread = new Thread(new ScrambleDeadShardingTask());
        scrambleDeadShardingThread.setDaemon(true);
        scrambleDeadShardingThread.start();
    }
}
