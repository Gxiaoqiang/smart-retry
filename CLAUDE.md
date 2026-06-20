# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

smart-retry 是一个基于 Spring AOP 的轻量级重试框架，将重试任务与业务事务绑定，通过数据库持久化保证最终一致性。支持多实例自动接管、死信检测、历史清理等功能。

- **GroupId**: `com.smart.retry`
- **版本**: `${smart.retry.version}` (当前 1.0.1)
- **Java**: 17
- **Spring Boot**: 3.3.5
- **持久层**: MyBatis（支持 MySQL、PostgreSQL、Oracle）

## 常用命令

```bash
# 编译整个项目
mvn clean compile

# 运行全部测试
mvn test

# 运行单个测试类
mvn -pl smart-retry-test test -Dtest=MybatisTest

# 跳过测试打包
mvn clean package -DskipTests

# 启动 Web 管理后台（开发调试）
mvn -pl smart-retry-web spring-boot:run

# 启动 smart-retry-ui 前端开发服务器
cd smart-retry-web/src/main/resources/smart-retry-ui && npm run dev
```

## 模块架构

```
smart-retry (父 POM)
├── smart-retry-common          # 公共接口、注解、DTO、常量、SPI
├── smart-retry-core            # 核心引擎：调度容器、AOP 拦截、执行引擎
├── smart-retry-extensions/
│   └── smart-retry-mybatis     # MyBatis 扩展：DAO、实体、仓库实现
├── smart-retry-starters/
│   └── smart-retry-mybatis-start  # Spring Boot Starter（自动配置入口）
├── smart-retry-test            # 集成测试
└── smart-retry-web             # 管理后台（Spring Boot + Vue3 + Element Plus）
```

### 模块依赖方向

```
smart-retry-common  ←  smart-retry-core  ←  smart-retry-extensions/smart-retry-mybatis
                                                                  ↑
                                      smart-retry-starters/smart-retry-mybatis-start
                                                                  ↑
                                          smart-retry-web / smart-retry-test
```

## 核心架构

### 两种使用模式

1. **`@RetryOnClass`（监听器模式）**：实现 `RetryLinstener<T>` 接口，用 `@RetryOnClass(taskCode="xxx")` 注解标注。通过 `RetryTaskOperator.createTask()` 创建任务，系统异步调度执行 `consume()` 方法。

2. **`@RetryOnMethod`（方法注解模式）**：在 Spring Bean 的 public 方法上加 `@RetryOnMethod`，方法抛出异常时自动在事务内注册重试任务到数据库，后续异步重试执行该方法。

### 启动流程（`RetryScannerMannger` → `ApplicationReadyEvent`）

1. `RetryMethodScanner.scan()` 扫描所有带 `@RetryOnMethod` 的方法，注册到 `RetryCache`
2. `RetryClassScanner.scan()` 扫描所有带 `@RetryOnClass` 的 Bean，注册到 `RetryCache`
3. 设置 `SmartRetryRunFlag.flag = true`，调度器开始拉取任务

### 调度模型（生产者-消费者）

- **ProducerTask**（`SimpleContainer` 内部类）：按 `taskFindInterval` 间隔轮询 DB，获取待重试任务，提交到 `ArrayBlockingQueue`+`ThreadPoolExecutor`
- **ConsumerTask**：从队列取出任务，通过 `DefaultInnovation.invoke()` 反射调用目标方法/监听器
- 队列满时 Producer 自适应退避（指数增长睡眠，上限 20 × baseSleep）
- 任务去重通过 `RetryTaskCache.retryTasks`（ConcurrentHashMap，key = taskCode + uniqueKey）

### AOP 拦截链（`@RetryOnMethod`）

`RetryTaskAdvisor`（IntroductionAdvisor）→ `RetryTaskInterceptor` → `DefaultRetryHandler.retryHandler()`：
- 执行原方法，捕获异常
- `DefaultRetryCondition.needRetry()` 判断是否需要重试（基于 include/exclude 异常类型 + occurType）
- 需要重试时 `RemoteRetryer.retry()` 将任务序列化写入 `retry_task` 表（与业务在同一事务内）

### 任务执行引擎（`DefaultInnovation`）

1. `beforeProcessTask()`：更新任务状态为 RUNNING，计算 `next_plan_time`，扣减 retryNum
2. `doInvoke()`：根据 `RetryTaskTypeEnum` 决定调用方式
   - CLASS 模式：反序列化参数 → 调用 `RetryLinstener.consume()`
   - METHOD 模式：反射调用原方法
3. 根据结果更新任务状态（SUCCESS/FAIL）
4. 执行通知回调（`oneTimeNotify` + `allRetryTaskFinishNotify`）

### 下次执行时间策略（策略模式）

`NextPlanTimeStrategy` 接口，4 种实现：
- `FixedNextPlanTimeStrategy` — 固定间隔
- `IncrementingNextPlanTimeStrategy` — 线性递增（intervalSecond × n）
- `FibonacciNextPlanTimeStrategy` — 斐波那契增长
- `BackOffNextPlanTimeStrategy` — 指数退避（2ⁿ × interval）

### 分片与故障转移

- 每个实例在 `retry_sharding` 表中注册分片记录
- `MybatisHeart.heartBeat()`：定时更新心跳时间
- `MybatisHeart.scrambleDeadSharding()`：定期扫描心跳超时的实例，接管其分片
- `ShardingContextHolder`：ThreadLocal 持有当前实例的分片 ID 列表
- 查询任务时按 sharding_key 过滤，每个实例只处理自己分片内的任务

### 定时清理

- `ClearTask`：通过 `ThreadPoolTaskScheduler` + Cron 定时删除超过 `beforeDays` 天的历史任务
- `DeadLetterTask`：扫描执行时间超过 `taskMaxExecuteTimeout` 的任务，重置为 WAITING 状态

## 配置属性

配置前缀：`spring.smart-retry`（`SmartExecutorConfigure`）

| 属性 | 默认值 | 说明 |
|------|--------|------|
| `logger` | false | 是否打印 INFO 日志 |
| `task-find-interval` | 20 | 任务扫描间隔（秒） |
| `health.interval` | 3 | 心跳间隔（秒） |
| `health.timeout` | 240 | 心跳超时（秒） |
| `health.scan-interval` | 5 | 死分片扫描间隔（秒） |
| `dead-task.dead-task-check` | false | 是否开启死信检测 |
| `dead-task.task-max-execute-timeout` | 1800 | 任务执行超时（秒） |
| `clear-task.enabled` | false | 是否开启历史清理 |
| `clear-task.cron` | 0 0 3 * * ? | 清理 Cron 表达式 |
| `clear-task.before-days` | 30 | 清理 N 天前的数据 |
| `executor.core-pool-size` | CPU+1 | 线程池核心线程数 |
| `executor.max-pool-size` | CPU×2 | 线程池最大线程数 |
| `executor.queue-capacity` | 3000 | 队列容量 |

MyBatis 数据源配置前缀：`spring.smart-retry.mybatis`（`SmartConfigure`）

| 属性 | 默认值 | 说明 |
|------|--------|------|
| `enabled` | true | 是否启用 |
| `datasource` | dataSource | Spring 容器中数据源 Bean 名称 |

## 关键数据库表

- **`retry_sharding`**：分片元数据表（instance_id、last_heartbeat），用于多实例协调
- **`retry_task`**：重试任务表（task_code、parameters、status、retry_num、next_plan_time、sharding_key、unique_key）

建表脚本位于 `doc/`：
- `smart_retry_mysql.sql`
- `smart_retry_pg.sql`
- `smart_retry_oracle.sql`

## SPI 接口

在 `smart-retry-common` 中定义的核心 SPI：
- `RetryTaskAccess` — 任务持久化访问
- `RetryTaskOperator` — 用户 API（创建/触发任务）
- `RetryLinstener` — 监听器模式消费接口
- `RetryConfiguration` — 配置门面（整合 Access、Identifier、Serializer）
- `RetryContainer` — 容器生命周期（start/destroy）
- `RetryTaskHeart` — 心跳管理
- `SmartSerializer` — 序列化策略
- `Identifier` — 唯一标识生成策略
- `ISharding` — 分片策略接口（预留）