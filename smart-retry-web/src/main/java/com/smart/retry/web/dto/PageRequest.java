package com.smart.retry.web.dto;

import lombok.Data;

/**
 * 分页请求参数
 */
@Data
public class PageRequest {
    
    private Integer pageNum = 1;
    
    private Integer pageSize = 10;
    
    public Integer getOffset() {
        return (pageNum - 1) * pageSize;
    }
}
