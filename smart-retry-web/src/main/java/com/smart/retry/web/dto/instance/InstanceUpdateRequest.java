package com.smart.retry.web.dto.instance;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 实例更新请求
 */
@Data
public class InstanceUpdateRequest {
    
    private Long id;
    
    @NotBlank(message = "instanceId不能为空")
    @Pattern(regexp = "^\\d+\\.\\d+\\.\\d+\\.\\d+:\\d+$", message = "instanceId必须是ip:port格式")
    private String instanceId;
}
