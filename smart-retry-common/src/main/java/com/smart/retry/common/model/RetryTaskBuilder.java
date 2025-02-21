package com.smart.retry.common.model;

import java.io.Serializable;

/**
 * @Author xiaoqiang
 * @Version RetryTaskContext.java, v 0.1 2025年02月12日 16:33 xiaoqiang
 * @Description: 任务执行上下文，用于记录任务执行过程中的相关信息
 * 为什么不用泛型，用户在使用的时候，所记录的参数
 */
public class RetryTaskBuilder<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 重试重试任务的名称，同一个项目下，名称是唯一的
     */
    private String taskCode;

    /**
     * 重试任务的描述
     */
    private String taskDesc;

    private int  delaySecond = 5;


    /**
     * 执行时间间隔 秒
     */
    private Integer intervalSecond;

    /**
     * 重试次数>1 小于100
     */
    private Integer retryNum;



    private T param;


    /**public RetryTaskBuilder(String taskCode,
                            String taskDesc,
                            Integer intervalSecond,
                            Integer retryNum,
                            T param){
        this.taskCode = taskCode;
        this.taskDesc = taskDesc;
        this.intervalSecond = intervalSecond;
        this.retryNum = retryNum;
        this.param = param;
    }**/

    public static <T>  RetryTaskBuilder<T> of(String taskCode, String taskDesc, Integer intervalSecond, Integer retryNum, T param){
        return new RetryTaskBuilder<T>()
               .withTaskCode(taskCode)
               .withTaskDesc(taskDesc)
               .withIntervalSecond(intervalSecond)
               .withRetryNum(retryNum)
               .withParam(param);
    }


    public static <T>  RetryTaskBuilder<T> of(){
        return new RetryTaskBuilder<T>();
    }

    public RetryTaskBuilder withTaskCode(String taskCode) {
        this.taskCode = taskCode;
        return this;
    }
    public RetryTaskBuilder withTaskDesc(String taskDesc) {
        this.taskDesc = taskDesc;
        return this;
    }
    public RetryTaskBuilder withIntervalSecond(Integer intervalSecond) {
        this.intervalSecond = intervalSecond;
        return this;
    }

    public RetryTaskBuilder withRetryNum(Integer retryNum) {
        this.retryNum = retryNum;
        return this;
    }
    public RetryTaskBuilder withParam(T param) {
        this.param = param;
        return this;
    }

    public RetryTaskBuilder withDelaySecond(int delaySecond) {
        this.delaySecond = delaySecond;
        return this;
    }

    public String getTaskCode() {
        return taskCode;
    }

    public String getTaskDesc() {
        return taskDesc;
    }

    public int getDelaySecond() {
        return delaySecond;
    }

    public Integer getIntervalSecond() {
        return intervalSecond;
    }

    public Integer getRetryNum() {
        return retryNum;
    }

    public T getParam() {
        return param;
    }
}
