package com.smart.retry.common.constant;

/**
 * @Author xiaoqiang
 * @Version RetryTaskTypeEnum.java, v 0.1 2025年02月14日 09:13 xiaoqiang
 * @Description: TODO
 */
public enum RetryTaskTypeEnum {

    CLASS("CLASS","类级别"),
    METHOD("METHOD","方法级别"),;

    private String code;

    private String desc;

    RetryTaskTypeEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}
