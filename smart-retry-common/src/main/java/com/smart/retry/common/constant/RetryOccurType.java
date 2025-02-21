package com.smart.retry.common.constant;

/**
 * 方法重试复发的类型
 *
 * @Author gao.gwq
 * @Version RetryOccurType.java, v 0.1 2023年09月14日 15:53 gao.gwq
 * @Description: TODO
 */
public enum RetryOccurType {

    EXCEPTION("EXCEPTION", "异常"),
    RESULT("RESULT", "结果");

    private String code;

    private String desc;

    RetryOccurType(String code, String desc) {
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
