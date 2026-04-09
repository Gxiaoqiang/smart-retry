package com.smart.retry.web.service;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import com.smart.retry.common.constant.RetryTaskStatus;
import com.smart.retry.web.dao.RetryShardingDao;
import com.smart.retry.web.dao.RetryTaskDao;
import com.smart.retry.web.entity.RetryShardingDO;
import com.smart.retry.web.entity.RetryTaskDO;
import com.smart.retry.web.entity.query.RetryTaskQuery;
import com.smart.retry.web.dto.PageResult;
import com.smart.retry.web.dto.task.*;
import com.smart.retry.web.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 任务管理服务
 */
@Service
@RequiredArgsConstructor
public class TaskService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TaskService.class);
    
    private final RetryTaskDao retryTaskDao;
    private final RetryShardingDao retryShardingDao;
    
    private static final Gson GSON = new Gson();
    
    /**
     * 分页查询任务列表
     */
    public PageResult<TaskVO> queryTasks(TaskQueryRequest request) {
        // 构建查询条件
        RetryTaskQuery query = new RetryTaskQuery();
        query.setOffset(request.getOffset());
        query.setLimit(request.getPageSize());
        query.setId(request.getId());
        query.setTaskCode(request.getTaskCode());
        query.setStatus(request.getStatus());
        query.setShardingKeyList(request.getShardingKey() != null ? 
                Collections.singletonList(request.getShardingKey()) : null);
        
        // 查询总数
        int total = retryTaskDao.countByQuery(query);
        
        if (total == 0) {
            return new PageResult<>(new ArrayList<>(), 0L, request.getPageNum(), request.getPageSize());
        }
        
        // 查询列表
        List<RetryTaskDO> doList = retryTaskDao.selectByQuery(query);
        
        // 批量查询分片信息，避免 N+1 查询
        Map<Long, RetryShardingDO> shardingMap = new HashMap<>();
        for (RetryTaskDO taskDO : doList) {
            long shardingKey = taskDO.getShardingKey();
            if (!shardingMap.containsKey(shardingKey)) {
                RetryShardingDO sharding = retryShardingDao.selectById(shardingKey);
                if (sharding != null) {
                    shardingMap.put(shardingKey, sharding);
                }
            }
        }
        
        List<TaskVO> voList = new ArrayList<>();
        for (RetryTaskDO taskDO : doList) {
            TaskVO vo = new TaskVO();
            BeanUtils.copyProperties(taskDO, vo);
            
            // 设置分片信息：ip(分片id)
            long shardingKey = taskDO.getShardingKey();
            RetryShardingDO sharding = shardingMap.get(shardingKey);
            if (sharding != null && sharding.getInstanceId() != null) {
                vo.setShardingInfo(sharding.getInstanceId() + "(" + sharding.getId() + ")");
            } else {
                vo.setShardingInfo(taskDO.getShardingKey()+"");
            }
            
            voList.add(vo);
        }
        
        return new PageResult<>(voList, (long) total, request.getPageNum(), request.getPageSize());
    }
    
    /**
     * 创建任务
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createTask(TaskCreateRequest request) {
        // 校验JSON格式
        validateJson(request.getParam());
        
        // 创建任务对象
        RetryTaskDO taskDO = new RetryTaskDO();
        taskDO.setTaskCode(request.getTaskCode());
        taskDO.setTaskDesc(request.getTaskDesc());
        taskDO.setRetryNum(request.getRetryNum());
        taskDO.setOriginRetryNum(request.getRetryNum()); // 默认和retryNum一样
        taskDO.setDelaySecond(request.getDelaySecond());
        taskDO.setIntervalSecond(request.getIntervalSecond());
        taskDO.setParameters(request.getParam());
        taskDO.setShardingKey(request.getShardingKey());
        taskDO.setNextPlanTimeStrategy(request.getNextPlanTimeStrategy());
        taskDO.setStatus(RetryTaskStatus.WAITING.getCode());
        taskDO.setCreator("custom"); // 默认人为创建
        
        // 计算下次执行时间：当前时间 + delaySecond
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.SECOND, request.getDelaySecond());
        taskDO.setNextPlanTime(calendar.getTime());
        
        // 插入数据库
        retryTaskDao.insert(taskDO);
        
        log.info("[TaskService#createTask]创建任务成功，id: {}, taskCode: {}", taskDO.getId(), taskDO.getTaskCode());
        return taskDO.getId();
    }
    
    /**
     * 更新任务
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateTask(TaskUpdateRequest request) {
        // 查询当前任务
        RetryTaskDO taskDO = retryTaskDao.selectById(request.getId());
        if (taskDO == null) {
            throw new RuntimeException("任务不存在");
        }
        
        // 检查任务状态，执行中的任务不能编辑
        if (RetryTaskStatus.RUNNING.getCode().equals(taskDO.getStatus())) {
            throw new RuntimeException("执行中的任务无法编辑");
        }
        
        // 只允许编辑 nextPlanTime, retryNum, param, status
        if (request.getNextPlanTime() != null) {
            taskDO.setNextPlanTime(request.getNextPlanTime());
        }
        
        if (request.getRetryNum() != null) {
            taskDO.setRetryNum(request.getRetryNum());
        }
        
        if (request.getParam() != null) {
            // 校验JSON格式
            validateJson(request.getParam());
            taskDO.setParameters(request.getParam());
        }
        
        // 处理状态更新：只有失败(3)或成功(2)的任务可以调整为待执行(0)
        if (request.getStatus() != null) {
            Integer currentStatus = taskDO.getStatus();
            Integer newStatus = request.getStatus();
            
            // 执行中状态不能被设置
            if (RetryTaskStatus.RUNNING.getCode().equals(newStatus)) {
                throw new RuntimeException("不能将任务状态设置为执行中");
            }
            
            // 只有失败或成功的任务可以重置为待执行
            if (RetryTaskStatus.WAITING.getCode().equals(newStatus)) {
                if (!RetryTaskStatus.FAIL.getCode().equals(currentStatus) 
                    && !RetryTaskStatus.SUCCESS.getCode().equals(currentStatus)) {
                    throw new RuntimeException("只有失败或成功的任务才能重置为待执行");
                }
            }
            
            taskDO.setStatus(newStatus);
        }
        
        // 再次检查任务状态（防止并发问题）
        RetryTaskDO currentTask = retryTaskDao.selectById(request.getId());
        if (RetryTaskStatus.RUNNING.getCode().equals(currentTask.getStatus())) {
            throw new RuntimeException("任务正在执行中，无法保存");
        }
        
        retryTaskDao.update(taskDO);
        log.info("[TaskService#updateTask]更新任务成功，id: {}", request.getId());
    }
    
    /**
     * 删除任务
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteTask(Long id) {
        RetryTaskDO taskDO = retryTaskDao.selectById(id);
        if (taskDO == null) {
            throw new RuntimeException("任务不存在");
        }
        
        // 执行中的任务不能删除
        if (RetryTaskStatus.RUNNING.getCode().equals(taskDO.getStatus())) {
            throw new RuntimeException("执行中的任务无法删除");
        }
        
        retryTaskDao.deleteById(id);
        log.info("[TaskService#deleteTask]删除任务成功，id: {}", id);
    }
    
    /**
     * 批量删除任务
     */
    @Transactional(rollbackFor = Exception.class)
    public void batchDeleteTasks(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        
        // 检查是否有执行中的任务
        for (Long id : ids) {
            RetryTaskDO taskDO = retryTaskDao.selectById(id);
            if (taskDO != null && RetryTaskStatus.RUNNING.getCode().equals(taskDO.getStatus())) {
                throw new RuntimeException("任务ID " + id + " 正在执行中，无法删除");
            }
        }
        
        retryTaskDao.batchDeleteByIds(ids);
        log.info("[TaskService#batchDeleteTasks]批量删除任务成功，数量: {}", ids.size());
    }
    
    /**
     * 获取分片选择列表
     */
    public List<ShardingOptionVO> getShardingOptions() {
        // 查询所有分片
        List<RetryShardingDO> shardingList = retryShardingDao.selectAllWithPage(0, 1000, null, null);
        
        // 按instanceId分组，每个instanceId取id最小的shardingKey
        Map<String, RetryShardingDO> instanceMap = new HashMap<>();
        for (RetryShardingDO sharding : shardingList) {
            String instanceId = sharding.getInstanceId();
            if (instanceId != null && !instanceId.isEmpty()) {
                if (!instanceMap.containsKey(instanceId)) {
                    instanceMap.put(instanceId, sharding);
                } else {
                    // 比较id，保留较小的
                    if (sharding.getId() < instanceMap.get(instanceId).getId()) {
                        instanceMap.put(instanceId, sharding);
                    }
                }
            }
        }
        
        // 转换为VO
        List<ShardingOptionVO> options = new ArrayList<>();
        for (Map.Entry<String, RetryShardingDO> entry : instanceMap.entrySet()) {
            ShardingOptionVO vo = new ShardingOptionVO();
            vo.setInstanceId(entry.getKey());
            vo.setShardingKey(entry.getValue().getId()); // 使用分片的id作为shardingKey
            vo.setDisplayText(entry.getKey());
            options.add(vo);
        }
        
        // 按id排序
        options.sort(Comparator.comparing(ShardingOptionVO::getShardingKey));
        
        return options;
    }
    
    /**
     * 校验JSON格式
     */
    private void validateJson(String json) {
        try {
            JsonParser.parseString(json);
        } catch (Exception e) {
            throw new BusinessException(400, "参数不是有效的JSON格式: " + e.getMessage());
        }
    }
}
