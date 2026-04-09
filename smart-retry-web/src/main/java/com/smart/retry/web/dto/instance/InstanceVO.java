package com.smart.retry.web.dto.instance;

import lombok.Data;
import java.util.Date;

/**
 * 实例信息VO
 */
@Data
public class InstanceVO {
    
    private Long id;
    
    private Date gmtCreate;
    
    private Integer status;
    
    private String creatorId;
    
    private String instanceId;
    
    private Date lastHeartbeat;
}
