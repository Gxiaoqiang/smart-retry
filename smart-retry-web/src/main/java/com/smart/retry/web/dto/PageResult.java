package com.smart.retry.web.dto;

import lombok.Data;
import java.util.List;

/**
 * 分页响应结果
 */
@Data
public class PageResult<T> {
    
    private List<T> list;
    
    private Long total;
    
    private Integer pageNum;
    
    private Integer pageSize;
    
    private Integer totalPages;
    
    public PageResult(List<T> list, Long total, Integer pageNum, Integer pageSize) {
        this.list = list;
        this.total = total;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        this.totalPages = (int) Math.ceil((double) total / pageSize);
    }
}
