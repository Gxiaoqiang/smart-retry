package com.smart.retry.core.cache;

import com.google.common.collect.Maps;
import com.smart.retry.common.exception.RetryException;
import com.smart.retry.common.model.RetryTaskObject;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author xiaoqiang
 * @Version RetryCache.java, v 0.1 2025年02月14日 09:54 xiaoqiang
 * @Description: TODO
 */
public class RetryCache {

    private static final Map<String, RetryTaskObject> RETRY_CACHE = new ConcurrentHashMap<String, RetryTaskObject>();

    public static void put(String key, RetryTaskObject retryTaskObject) {
        String errFormat = "retry task {} already exists in cache";
        if(RETRY_CACHE.containsKey(key)) {
            throw new RetryException(String.format(errFormat, key));
        }
        RETRY_CACHE.put(key, retryTaskObject);
    }

    public static RetryTaskObject get(String key) {
        return RETRY_CACHE.get(key);
    }

    public static Map<String, RetryTaskObject> getAll() {
        return RETRY_CACHE;
    }

    public static void remove(String key) {
        RETRY_CACHE.remove(key);
    }

    public static int retryCount(String key) {
        return RETRY_CACHE.size();
    }

}
