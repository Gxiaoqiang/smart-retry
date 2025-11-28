package com.smart.retry.core;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author xiaoqiang
 * @Version RetryTaskManager.java, v 0.1 2025年02月20日 09:29 xiaoqiang
 * @Description: 内存校验，防止重复执行
 */
public class RetryTaskCache {

     static final ConcurrentHashMap<String, Boolean> retryTasks = new ConcurrentHashMap<>();


    public static Boolean isTaskExists(String taskId) {

        return retryTasks.containsKey(taskId);
    }

    public static void removeTaskFlag(String taskId) {
        retryTasks.remove(taskId);
    }
    public static void addTaskFlag(String taskId) {
        retryTasks.put(taskId, true);
    }
}
