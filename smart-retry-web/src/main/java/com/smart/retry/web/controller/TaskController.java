package com.smart.retry.web.controller;

import com.smart.retry.web.dto.PageResult;
import com.smart.retry.web.dto.Result;
import com.smart.retry.web.dto.task.*;
import com.smart.retry.web.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 任务管理控制器
 */
@RestController
@RequestMapping("/api/task")
@RequiredArgsConstructor
public class TaskController {
    
    private final TaskService taskService;
    
    /**
     * 分页查询任务列表
     */
    @PostMapping("/query")
    public Result<PageResult<TaskVO>> queryTasks(@RequestBody TaskQueryRequest request) {
        PageResult<TaskVO> result = taskService.queryTasks(request);
        return Result.success(result);
    }
    
    /**
     * 创建任务
     */
    @PostMapping("/create")
    public Result<Long> createTask(@Valid @RequestBody TaskCreateRequest request) {
        Long taskId = taskService.createTask(request);
        return Result.success(taskId);
    }
    
    /**
     * 更新任务
     */
    @PutMapping("/update")
    public Result<Void> updateTask(@Valid @RequestBody TaskUpdateRequest request) {
        taskService.updateTask(request);
        return Result.success();
    }
    
    /**
     * 删除任务
     */
    @DeleteMapping("/delete/{id}")
    public Result<Void> deleteTask(@PathVariable("id") Long id) {
        taskService.deleteTask(id);
        return Result.success();
    }
    
    /**
     * 批量删除任务
     */
    @DeleteMapping("/batch-delete")
    public Result<Void> batchDeleteTasks(@RequestBody List<Long> ids) {
        taskService.batchDeleteTasks(ids);
        return Result.success();
    }
    
    /**
     * 获取分片选择列表
     */
    @GetMapping("/sharding-options")
    public Result<List<ShardingOptionVO>> getShardingOptions() {
        List<ShardingOptionVO> options = taskService.getShardingOptions();
        return Result.success(options);
    }
}
