package com.smart.retry.common;

/**
 * @Author xiaoqiang
 * @Version SmartRtryFlag.java, v 0.1 2025年11月14日 21:19 xiaoqiang
 * @Description: TODO
 */
public class SmartRtryExit {

    private static volatile Boolean taskFlag = false;

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            taskFlag = true;
        }));
    }

    public static Boolean isExit() {
        return !taskFlag;
    }
}
