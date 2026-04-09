# 异常处理优化说明

## 优化概述

本次优化建立了统一的异常处理机制，替换了原有的散乱异常处理方式，使代码更加规范、可维护。

## 核心组件

### 1. BusinessException（业务异常类）

**位置**: `com.smart.retry.web.exception.BusinessException`

**特点**:
- 继承自 `RuntimeException`
- 支持自定义错误码和消息
- 用于业务逻辑中的异常情况

**使用示例**:
```java
// 默认500错误码
throw new BusinessException("实例不存在");

// 自定义400错误码
throw new BusinessException(400, "参数格式不正确");

// 包含原始异常
throw new BusinessException("操作失败", e);
```

### 2. GlobalExceptionHandler（全局异常处理器）

**位置**: `com.smart.retry.web.exception.GlobalExceptionHandler`

**功能**:
- 统一捕获和处理所有异常
- 自动转换为标准的 Result 响应格式
- 记录适当的日志级别

**处理的异常类型**:

| 异常类型 | HTTP状态码 | 说明 |
|---------|-----------|------|
| BusinessException | 自定义/500 | 业务异常 |
| MethodArgumentNotValidException | 400 | @Valid参数校验失败 |
| BindException | 400 | 参数绑定失败 |
| ConstraintViolationException | 400 | 约束违反 |
| IllegalArgumentException | 400 | 非法参数 |
| RuntimeException | 500 | 运行时异常 |
| Exception | 500 | 其他所有异常 |

## 优化前后对比

### 优化前（Controller层）

```java
@PostMapping("/create")
public Result<Long> createTask(@Valid @RequestBody TaskCreateRequest request) {
    try {
        Long taskId = taskService.createTask(request);
        return Result.success(taskId);
    } catch (IllegalArgumentException e) {
        return Result.error(400, e.getMessage());
    } catch (Exception e) {
        return Result.error("创建任务失败: " + e.getMessage());
    }
}
```

**问题**:
- ❌ 每个方法都需要try-catch
- ❌ 代码冗余，重复率高
- ❌ 异常处理逻辑分散
- ❌ 容易遗漏异常处理

### 优化后（Controller层）

```java
@PostMapping("/create")
public Result<Long> createTask(@Valid @RequestBody TaskCreateRequest request) {
    Long taskId = taskService.createTask(request);
    return Result.success(taskId);
}
```

**优势**:
- ✅ 代码简洁清晰
- ✅ 无样板代码
- ✅ 专注业务逻辑
- ✅ 统一异常处理

### Service层优化

**优化前**:
```java
if (!request.getInstanceId().matches("^\\d+\\.\\d+\\.\\d+\\.\\d+:\\d+$")) {
    throw new IllegalArgumentException("instanceId必须是ip:port格式");
}
```

**优化后**:
```java
if (!request.getInstanceId().matches("^\\d+\\.\\d+\\.\\d+\\.\\d+:\\d+$")) {
    throw new BusinessException(400, "instanceId必须是ip:port格式，例如：192.168.1.100:8080");
}
```

**改进点**:
- ✅ 使用BusinessException替代通用异常
- ✅ 提供更友好的错误提示（包含示例）
- ✅ 明确指定HTTP状态码

## 最佳实践

### 1. Controller层
- **不要**使用try-catch包裹业务调用
- **让**全局异常处理器统一处理
- **保持**代码简洁

```java
@RestController
@RequestMapping("/api/task")
public class TaskController {
    
    @PostMapping("/create")
    public Result<Long> createTask(@Valid @RequestBody TaskCreateRequest request) {
        // 直接调用，不捕获异常
        Long taskId = taskService.createTask(request);
        return Result.success(taskId);
    }
}
```

### 2. Service层
- **使用** BusinessException抛出业务异常
- **提供**清晰的错误消息
- **指定**合适的HTTP状态码

```java
@Service
public class TaskService {
    
    public void updateTask(TaskUpdateRequest request) {
        // 参数校验
        if (task.getStatus() != 0 && task.getStatus() != 3) {
            throw new BusinessException(400, "只能编辑待执行或失败的任务");
        }
        
        // 业务逻辑
        int result = retryTaskDao.update(taskDO);
        if (result == 0) {
            throw new BusinessException("更新失败，任务不存在");
        }
    }
}
```

### 3. 参数校验
- **使用** Jakarta Validation注解
- **提供**友好的校验消息
- **自动**被全局异常处理器捕获

```java
@Data
public class TaskCreateRequest {
    
    @NotBlank(message = "任务编码不能为空")
    private String taskCode;
    
    @NotBlank(message = "任务描述不能为空")
    private String taskDesc;
    
    @NotNull(message = "重试次数不能为空")
    @Min(value = 1, message = "重试次数必须大于0")
    private Integer retryNum;
}
```

## 异常处理流程

```
用户请求
   ↓
Controller接收
   ↓
@Valid参数校验
   ↓ (失败)
GlobalExceptionHandler捕获 → 返回400错误
   ↓ (成功)
Service业务处理
   ↓ (异常)
抛出BusinessException
   ↓
GlobalExceptionHandler捕获 → 返回对应错误
   ↓ (成功)
返回成功结果
```

## 日志级别

| 异常类型 | 日志级别 | 说明 |
|---------|---------|------|
| BusinessException | WARN | 业务异常，需要关注 |
| 参数校验异常 | WARN | 客户端参数错误 |
| RuntimeException | ERROR | 系统错误，需要排查 |
| Exception | ERROR | 未知错误，需要排查 |

## 扩展建议

### 1. 添加更多业务异常子类

```java
// 资源不存在异常
public class ResourceNotFoundException extends BusinessException {
    public ResourceNotFoundException(String resourceName) {
        super(404, resourceName + "不存在");
    }
}

// 权限异常
public class PermissionDeniedException extends BusinessException {
    public PermissionDeniedException() {
        super(403, "没有权限执行此操作");
    }
}
```

### 2. 国际化支持

```java
@ExceptionHandler(BusinessException.class)
public Result<Void> handleBusinessException(BusinessException e, Locale locale) {
    String message = messageSource.getMessage(e.getCode(), null, locale);
    return Result.error(e.getCode(), message);
}
```

### 3. 异常监控和告警

```java
@ExceptionHandler(Exception.class)
public Result<Void> handleException(Exception e) {
    // 发送告警通知
    alertService.sendAlert("系统异常", e.getMessage());
    
    // 记录详细日志
    log.error("未知异常", e);
    
    return Result.error("系统异常，请联系管理员");
}
```

## 总结

通过本次优化：
- ✅ 消除了Controller层的try-catch样板代码
- ✅ 统一了异常处理逻辑
- ✅ 提高了代码可读性和可维护性
- ✅ 提供了更友好的错误提示
- ✅ 建立了规范的异常处理体系
