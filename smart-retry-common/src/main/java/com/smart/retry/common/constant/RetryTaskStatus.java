package com.smart.retry.common.constant;

/**
 * @Author xiaoqiang
 * @Version RetryTaskStatusEnum.java, v 0.1 2025年02月11日 20:06 xiaoqiang
 * @Description: 任务执行时间
 */
public enum RetryTaskStatus {


    WAITING(0, "待执行"),
    RUNNING(1, "执行中"),
    SUCCESS(2, "成功"),
    FAIL(3, "失败");

    private Integer code;

    private String desc;
    RetryTaskStatus(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public Integer getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}
