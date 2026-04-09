package com.smart.retry.web.dto.dashboard;

import lombok.Data;
import java.util.Date;

/**
 * 死信任务趋势数据
 */
@Data
public class DeadLetterTrendVO {
    
    /**
     * 时间点
     */
    private Date timePoint;
    
    /**
     * 死信任务数量
     */
    private Long count;
}
