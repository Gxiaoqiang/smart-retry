package com.smart.retry.web.controller;

import com.smart.retry.web.dto.PageResult;
import com.smart.retry.web.dto.Result;
import com.smart.retry.web.dto.instance.InstanceQueryRequest;
import com.smart.retry.web.dto.instance.InstanceUpdateRequest;
import com.smart.retry.web.dto.instance.InstanceVO;
import com.smart.retry.web.service.InstanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 实例管理控制器
 */
@RestController
@RequestMapping("/api/instance")
@RequiredArgsConstructor
public class InstanceController {
    
    private final InstanceService instanceService;
    
    /**
     * 分页查询实例列表
     */
    @PostMapping("/query")
    public Result<PageResult<InstanceVO>> queryInstances(@RequestBody InstanceQueryRequest request) {
        PageResult<InstanceVO> result = instanceService.queryInstances(request);
        return Result.success(result);
    }
    
    /**
     * 更新实例信息
     */
    @PutMapping("/update")
    public Result<Void> updateInstance(@Valid @RequestBody InstanceUpdateRequest request) {
        instanceService.updateInstance(request);
        return Result.success();
    }
    
    /**
     * 删除实例
     */
    @DeleteMapping("/delete/{id}")
    public Result<Void> deleteInstance(@PathVariable("id") Long id) {
        instanceService.deleteInstance(id);
        return Result.success();
    }
}
