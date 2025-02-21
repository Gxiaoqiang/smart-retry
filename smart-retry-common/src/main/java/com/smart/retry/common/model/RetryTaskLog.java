package com.smart.retry.common.model;

import java.util.Date;

/**
 * @Author xiaoqiang
 * @Version RetryTaskLog.java, v 0.1 2025年02月11日 19:57 xiaoqiang
 * @Description: TODO
 */
public class RetryTaskLog extends BaseEntity {


    /**
     * 需要执行的任务名称
     * <p>
     * This field corresponds to the database column retry_task_log.task_name
     */
    private String taskCode;

    /**
     * 任务描述
     * <p>
     * This field corresponds to the database column retry_task_log.task_id
     */
    private Long taskId;

    /**
     * 最终执行状态 1 执行中,-1:执行失败,2:执行成功
     * <p>
     * This field corresponds to the database column retry_task_log.status
     */
    private Integer status;

    /**
     * 本身预计执行时间
     * <p>
     * This field corresponds to the database column retry_task_log.pre_start_time
     */
    private Date preStartTime;

    /**
     * 执行开始时间
     * <p>
     * This field corresponds to the database column retry_task_log.start_time
     */
    private Date startTime;

    /**
     * 执行结束时间
     * <p>
     * This field corresponds to the database column retry_task_log.end_time
     */
    private Date endTime;

    /**
     * 执行机器ip
     * <p>
     * This field corresponds to the database column retry_task_log.execute_ip
     */
    private String executeIp;

    /**
     * 执行轨迹id
     * <p>
     * This field corresponds to the database column retry_task_log.trace_id
     */
    private String traceId;

    private String msgId;

    private Integer maxExecuteTime;

    public Integer getMaxExecuteTime() {
        return maxExecuteTime;
    }

    public void setMaxExecuteTime(Integer maxExecuteTime) {
        this.maxExecuteTime = maxExecuteTime;
    }

    public String getMsgId() {
        return msgId;
    }

    public void setMsgId(String msgId) {
        this.msgId = msgId;
    }

    /**
     * 错误原因
     * <p>
     * This field corresponds to the database column retry_task_log.error_message
     */
    private String errorMessage;


    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }


    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getTaskCode() {
        return taskCode;
    }

    public void setTaskCode(String taskCode) {
        this.taskCode = taskCode;
    }

    public Date getPreStartTime() {
        return preStartTime;
    }

    public void setPreStartTime(Date preStartTime) {
        this.preStartTime = preStartTime;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public String getExecuteIp() {
        return executeIp;
    }

    public void setExecuteIp(String executeIp) {
        this.executeIp = executeIp == null ? null : executeIp.trim();
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId == null ? null : traceId.trim();
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage == null ? null : errorMessage.trim();
    }

    /**
     * This method corresponds to the database table retry_task_log
     *
     * @return String
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", taskCode=").append(taskCode);
        sb.append(", taskId=").append(taskId);
        sb.append(", status=").append(status);
        sb.append(", preStartTime=").append(preStartTime);
        sb.append(", startTime=").append(startTime);
        sb.append(", endTime=").append(endTime);
        sb.append(", executeIp=").append(executeIp);
        sb.append(", traceId=").append(traceId);
        sb.append(", errorMessage=").append(errorMessage);
        sb.append("]");
        return sb.toString();
    }
}
