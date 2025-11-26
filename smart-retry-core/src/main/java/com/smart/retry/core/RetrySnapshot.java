package com.smart.retry.core;

import com.smart.retry.common.model.MethodChain;

import java.lang.reflect.Method;

/**
 * @Author xiaoqiang
 * @Version RetrySnapshot.java, v 0.1 2025年02月15日 16:32 xiaoqiang
 * @Description: TODO
 */
public class RetrySnapshot {

    private static final ThreadLocal<MethodChain> METHOD_CHAIN_THREAD_LOCAL = new ThreadLocal<>();



    public static void removeInterceptorChain(Method method) {

        if (method == null) {
            METHOD_CHAIN_THREAD_LOCAL.remove();
            return;
        }
        MethodChain methodChainModel = METHOD_CHAIN_THREAD_LOCAL.get();
        MethodChain preTail = methodChainModel;
        while (methodChainModel != null) {
            if (methodChainModel.getMethod().equals(method)) {
                break;
            }
            preTail = methodChainModel;
            methodChainModel = methodChainModel.getNext();
        }
        if (preTail != null) {
            preTail.setNext(null);
            //preTail.setTail(true);
        }
        if (methodChainModel.isHeader()) {
            METHOD_CHAIN_THREAD_LOCAL.remove();
        }
    }

    public static MethodChain getChainByMethod(Method method) {
        MethodChain methodChainModel = METHOD_CHAIN_THREAD_LOCAL.get();
        while (methodChainModel != null) {
            if (methodChainModel.getMethod().equals(method)) {
                break;
            }
            methodChainModel = methodChainModel.getNext();
        }
        return methodChainModel;
    }

    public static synchronized void setInterceptorChain(MethodChain chainModel) {

        MethodChain methodChainModel = METHOD_CHAIN_THREAD_LOCAL.get();
        if (methodChainModel == null) {
            chainModel.setHeader(true);
            chainModel.setTail(true);
            METHOD_CHAIN_THREAD_LOCAL.set(chainModel);
            return;
        }
        MethodChain tmpChain = methodChainModel;
        MethodChain preTmpChain = tmpChain;
        while (true) {
            preTmpChain = tmpChain;
            tmpChain = tmpChain.getNext();
            if (tmpChain == null) {
                break;
            }
        }
        preTmpChain.setHeader(false);
        preTmpChain.setTail(false);

        chainModel.setTail(true);
        chainModel.setHeader(false);
        //设置第一个为header数据
        methodChainModel.setHeader(true);
        preTmpChain.setNext(chainModel);

    }
}
