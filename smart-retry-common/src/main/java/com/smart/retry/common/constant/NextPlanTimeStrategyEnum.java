package com.smart.retry.common.constant;

/**
 * @Author xiaoqiang
 * @Version RetryTypeEnum.java, v 0.1 2025年07月15日 17:09 xiaoqiang
 * @Description: TODO
 */
public enum NextPlanTimeStrategyEnum {

    FIXED(1,"固定间隔"),
    INCREMENTING(2,"递增"),

    FIBONACCI(3,"斐波那契");
    private int code;
    private String desc;

    NextPlanTimeStrategyEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static NextPlanTimeStrategyEnum getByCode(int code) {
        for (NextPlanTimeStrategyEnum retryTypeEnum : NextPlanTimeStrategyEnum.values()) {
            if (retryTypeEnum.getCode() == code) {
                return retryTypeEnum;
            }
        }
        return null;
    }
}
