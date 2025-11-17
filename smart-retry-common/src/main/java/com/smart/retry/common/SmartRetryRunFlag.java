package com.smart.retry.common;

/**
 * @Author xiaoqiang
 * @Version SmartRetryRunFlag.java, v 0.1 2025年11月17日 17:25 xiaoqiang
 * @Description: TODO
 */
public class SmartRetryRunFlag {

    private static volatile Boolean flag = false;


    public static Boolean getFlag() {
        return flag;
    }

    public static void setFlag(Boolean flag) {
        SmartRetryRunFlag.flag = flag;
    }

}
