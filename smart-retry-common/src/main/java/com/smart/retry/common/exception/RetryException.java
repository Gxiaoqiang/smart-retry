package com.smart.retry.common.exception;

/**
 * @Author xiaoqiang
 * @Version RetryException.java, v 0.1 2025年02月14日 13:03 xiaoqiang
 * @Description: TODO
 */
public class RetryException extends RuntimeException {
    public RetryException(String message) {
        super(message);
    }
    public RetryException(String message, Throwable cause) {
        super(message, cause);
    }
    public RetryException(Throwable cause) {
        super(cause);
    }
    public RetryException() {
        super();
    }
}
