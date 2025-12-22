package com.smart.retry.core;

import com.smart.retry.common.exception.RetryException;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * @Author xiaoqiang
 * @Version ShardingIndex.java, v 0.1 2025年02月15日 21:42 xiaoqiang
 * @Description: TODO
 */
public class ShardingContextHolder {


    private static Set<Long> shardingIndexSet = new TreeSet<>();
    //private static AtomicLong totalIndex = new AtomicLong(0);


    private static ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();

    private static ReentrantReadWriteLock.ReadLock readLock = readWriteLock.readLock();

    private static ReentrantReadWriteLock.WriteLock writeLock = readWriteLock.writeLock();


    public static List<Long> shardingIndex() {
        try {
            readLock.lock();
            return new ArrayList<>(shardingIndexSet);
        } finally {
            readLock.unlock();
        }
    }

    public static void initShardingIndex(List<Long> shardingIndexList) {
        try {
            writeLock.lock();
            shardingIndexSet.clear();

            shardingIndexSet.addAll(shardingIndexList.stream()
                    .distinct().collect(Collectors.toSet()));
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * 默认只取最小的一个分区
     * @return
     */
    public static Long getRandomShardingIndex() {

        try {
            readLock.lock();
            if (shardingIndexSet.isEmpty()) {
                throw new RetryException("sharding index is not init");
            }
            return shardingIndex().get(0);
        } finally {
            readLock.unlock();
        }
    }

}
