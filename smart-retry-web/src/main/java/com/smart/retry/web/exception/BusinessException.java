package com.smart.retry.web.exception;

import lombok.Getter;

/**
 * 业务异常类
 */
@Getter
public class BusinessException extends RuntimeException {
    
    private final Integer code;
    
    public BusinessException(String message) {
        super(message);
        this.code = 500;
    }
    
    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }
    
    public BusinessException(String message, Throwable cause) {
        super(message, cause);
        this.code = 500;
    }
}
