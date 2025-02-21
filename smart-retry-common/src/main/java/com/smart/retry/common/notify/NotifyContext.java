package com.smart.retry.common.notify;

import com.smart.retry.common.constant.ExecuteResultStatus;
import com.smart.retry.common.model.RetryTask;

/**
 * @Author xiaoqiang
 * @Version NotifyContext.java, v 0.1 2025年02月18日 17:28 xiaoqiang
 * @Description: TODO
 */
public class NotifyContext {

    private RetryTask retryTask;


    private Object []args;

    private ExecuteResultStatus executionStatus;


    private Throwable throwable;

    private Object result;
    public Object getResult() {
        return result;
    }
    public void setResult(Object result) {
        this.result = result;
    }

    public RetryTask getRetryTask() {
        return retryTask;
    }

    public void setRetryTask(RetryTask retryTask) {
        this.retryTask = retryTask;
    }

    public Object[] getArgs() {
        return args;
    }

    public void setArgs(Object[] args) {
        this.args = args;
    }

    public ExecuteResultStatus getExecutionStatus() {
        return executionStatus;
    }

    public void setExecutionStatus(ExecuteResultStatus executionStatus) {
        this.executionStatus = executionStatus;
    }

    public Throwable getThrowable() {
        return throwable;
    }

    public void setThrowable(Throwable throwable) {
        this.throwable = throwable;
    }
}
