package com.smart.retry.core;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 任务实例内存去重缓存。
 *
 * <p>合并了旧版 {@code retryTasks} (ConcurrentHashMap) 与 {@code SimpleContainer.inMemoryTaskKeys} (Set)，
 * 统一为基于 {@link ConcurrentHashMap#newKeySet()} 的去重集合，key = taskCode + "-" + uniqueKey。
 *
 * <p>用于替代旧版两套独立去重机制，减少内存冗余和语义不一致。
 *
 * @Author xiaoqiang
 * @Version RetryTaskCache.java, v 0.2 2025年06月26日 xiaoqiang
 */
public class RetryTaskCache {

    /** 正在处理中的任务实例集合，key = taskCode + "-" + uniqueKey */
    private static final Set<String> IN_MEMORY_TASKS = new HashSet<>();

    /**
     * 尝试将任务标记为"内存中"（首次入队/去重）。
     * @param taskKey taskCode + "-" + uniqueKey
     * @return true=标记成功（任务不存在），false=已在内存中
     */
    public synchronized static boolean tryMark(String taskKey) {
        return IN_MEMORY_TASKS.add(taskKey);
    }

    /**
     * 移除任务的"内存中"标记。
     */
    public synchronized   static void unmark(String taskKey) {
        IN_MEMORY_TASKS.remove(taskKey);
    }

    /**
     * 当前内存中的任务实例总数。
     */
    public synchronized static int size() {
        return IN_MEMORY_TASKS.size();
    }

}
