# 内存 DelayQueue 精准调度设计方案

## 元信息

- **日期**: 2026-06-20
- **状态**: 待评审
- **目标**: 将任务调度延迟从最高 20 秒降低到秒级（1-3 秒内精准触发）

---

## 1. 问题背景

当前 smart-retry 通过 `ProducerTask` 定时轮询数据库（`taskFindInterval`，默认 20 秒）来发现和执行任务。任务到达 `next_plan_time` 后，必须等待下一次轮询才能被发现，最坏情况下延迟接近 20 秒。

### 延迟根因

| 因素 | 详情 |
|------|------|
| 轮询间隔过大 | `taskFindInterval` 默认 20 秒，每次查库后强制 sleep |
| 即便有任务也 sleep | 刚拉取到任务并提交后，Producer 仍 sleep 整段间隔 |
| 单线程 Producer | 拉取和提交串行执行 |
| SQL 时间过滤 | `next_plan_time <= now()` 过滤，任务刚到期时需等下一轮 |

---

## 2. 设计方案

### 2.1 核心思路

**从"拉"（poll）转向"推"（schedule）**：用 `java.util.concurrent.DelayQueue` 在内存中维护即将执行的任务，任务到期时 `take()` 立即返回，实现秒级精准触发。DB 轮询降级为低频兜底，仅用于故障恢复。

### 2.2 核心架构

```
                          +-----------------------------+
                          |       SimpleContainer        |
                          |                             |
  createTask() ---------->|  enqueue(task)  <-- RemoteRetryer (AOP)
  (CLASS模式)              |       |                     |
                          |       v                     |
  Producer 兜底扫描 ------>|  +--------------+          |
  (每 taskFindInterval 秒) |  |  DelayQueue  |          |
  next_plan_time <=        |  |  (精准调度)   |          |
  now() + preloadWindow    |  +------+-------+          |
  -> 补充遗漏/故障恢复      |         | take() 到期触发   |
                          |         v                  |
                          |  +--------------+          |
                          |  | 检查DB状态    |<- 安全阀  |
                          |  | 防止无效执行   |          |
                          |  +------+-------+          |
                          |         v                  |
                          |  +--------------+          |
                          |  | executor 执行  |          |
                          |  | DefaultInnov   |          |
                          |  +------+-------+          |
                          |         | finally          |
                          |         | 更新DB状态/次数   |
                          |         v                  |
                          |  +----------------------+  |
                          |  | SUCCESS      -> remove |  |
                          |  | FAIL + 窗口内  -> 入队  |  |
                          |  | FAIL + 窗口外  -> 移除  |  |
                          |  +----------------------+  |
                          +-----------------------------+
```

### 2.3 关键设计决策

#### 2.3.1 preloadWindow 与 taskFindInterval 的关系

`preloadWindow` 不独立设置，复用现有 `taskFindInterval` 配置：

```java
// taskFindInterval = 20;  // 现有配置，默认 20s
//   语义从"主轮询间隔"变为"兜底扫描间隔"
preloadMultiplier = 2;     // 预加载倍数，默认 2
preloadWindow = taskFindInterval * preloadMultiplier;  // = 40s
```

保证 `preloadWindow >= taskFindInterval`，扫描窗口之间永远有重叠，无覆盖缺口。

#### 2.3.2 Producer 扫描范围

```sql
SELECT * FROM retry_task
WHERE next_plan_time <= now() + preloadWindow  -- 预加载未来 40s 的任务
  AND status IN (WAITING, FAIL)
  AND retry_num >= 1
  AND sharding_key IN (当前实例分片)
ORDER BY id
LIMIT maxInMemory - currentSize
```

#### 2.3.3 内存容量控制

两层限流：

| 机制 | 说明 |
|------|------|
| 时间窗口 | 只加载 `next_plan_time <= now() + preloadWindow` 的任务 |
| 数量上限 | `maxInMemory`（默认 5000），达到上限后 Producer 停止加载新任务，打印 WARN |

任务执行后自动出队，内存中同时存在的任务 ≈ 未来 40s 内应执行的任务，数量天然可控。

`createTask()` 和 `enqueue()` 始终允许（不阻塞业务），不受 `maxInMemory` 硬限制。

#### 2.3.4 执行后 DB 同步更新

每次任务执行完成后，`DefaultInnovation.invoke()` 中的现有逻辑**保持不变**，同步更新数据库：

```
try:
    beforeProcessTask():
        -> DB UPDATE: status = RUNNING, retry_num = retry_num - 1,
                      next_plan_time = 计算新值, executor = 本机IP
    doInvoke():
        -> 反射执行业务方法
finally:
    -> DB UPDATE: status = SUCCESS 或 FAIL
    -> 执行通知回调 (oneTimeNotify / allRetryTaskFinishNotify)
```

**以上 DB 更新步骤完全不动**，与现有逻辑一致。DelayQueue 调度只是在 `finally` 之后增加入队/出队决策，不影响 DB 同步。

#### 2.3.5 执行后入队/出队决策

```java
void afterExecute(RetryTask task, boolean success) {
    // 注意：此时 DB 已更新 (status/retryNum/nextPlanTime)
    String key = taskKey(task);
    inMemoryTaskKeys.remove(key);          // 无论如何先移除

    if (success || task.getRetryNum() <= 0) {
        return;                            // 成功或重试耗尽 -> 结束
    }

    // 任务失败 + 还有重试次数 + DB 已有新 next_plan_time
    boolean inWindow = task.getNextPlanTime().getTime()
                       <= System.currentTimeMillis() + preloadWindowMs;

    if (inWindow) {
        enqueue(task);                     // 窗口内 -> 精准调度
    }
    // else: 窗口外 -> 内存移除，由 Producer 后续扫描加载
}
```

#### 2.3.6 长间隔任务处理

对于重试间隔很长的任务（如 `next_plan_time` 在 1 小时后），执行失败后从内存移除。Producer 每 20s 扫描一次，在临近执行时间时自动加载。对长间隔任务，20s 的加载延迟相对误差可忽略（3600s 中延迟 20s ≈ 0.6%）。

---

## 3. 数据一致性保障

### 3.1 核心原则

**DB 永远是 source of truth，DelayQueue 是性能加速层。**

### 3.2 每次执行后 DB 同步

每次任务执行都有两次 DB 更新（`beforeProcessTask` + `finally`），与现有逻辑完全一致：

| 时机 | DB 操作 | 说明 |
|------|---------|------|
| 执行前 | `status=RUNNING`, `retryNum-1`, `nextPlanTime` 重新计算, `executor` 记录 | 标记任务正在执行 |
| 执行后 | `status=SUCCESS` 或 `status=FAIL` | 标记最终结果 |
| 通知 | `RetryTaskNotify` 回调 | 与现有逻辑一致 |

无论 DelayQueue 调度还是 Producer 兜底扫描触发，执行路径都经过同一个 `DefaultInnovation.invoke()`，DB 更新行为完全一致。

### 3.3 三种异常恢复场景

#### 场景 1：JVM 崩溃时任务在 DelayQueue 中（尚未执行）

```
DB 状态: WAITING, next_plan_time = 10:30:00
DelayQueue: 有该任务
JVM 崩溃 -> DelayQueue 丢失
---------------------------------
恢复: Producer 在 taskFindInterval 秒内扫描 DB
      -> 发现 next_plan_time < now() 且 status=WAITING
      -> 重新加入 DelayQueue
      -> 延迟 <= taskFindInterval (20s)
```

#### 场景 2：JVM 崩溃时任务正在执行中

```
DB 状态: RUNNING (已在 beforeProcessTask 中更新)
JVM 崩溃 -> 执行中断
---------------------------------
恢复: DeadLetterTask 检测 RUNNING 超过 taskMaxExecuteTimeout
      -> 重置为 WAITING
      -> Producer 扫描 -> 重新入队执行
      (和现有恢复机制完全一致，无新增逻辑)
```

#### 场景 3：createTask() 后立即崩溃

```
DB 状态: WAITING (已写入，与业务事务同连接)
DelayQueue: 可能还没来得及加入
JVM 崩溃
---------------------------------
恢复: Producer 扫描发现 -> 入队执行
      -> 无数据丢失
```

### 3.4 内存去重

使用独立的 `inMemoryTaskKeys`（`ConcurrentHashMap.newKeySet()`），key = `taskCode + "-" + uniqueKey`。

```java
boolean enqueue(RetryTask task) {
    String key = taskKey(task);
    if (!inMemoryTaskKeys.add(key)) {
        return false;  // 已在内存中，跳过
    }
    delayQueue.put(new ScheduledTask(task));
    return true;
}
```

与现有 `RetryTaskCache.retryTasks` 互不干扰。

### 3.5 执行前 DB 状态校验

任务从 DelayQueue 出队后、提交执行前，校验 DB 状态：

```java
RetryTask dbTask = access.getRetryTask(task.getId());
if (dbTask == null) return;                                    // 任务已被删除
if (dbTask.getStatus() not in (WAITING, FAIL)) return;         // 状态已变更
if (dbTask.getRetryNum() <= 0) return;                         // 重试次数已耗尽
if (!shardingContextHolder.contains(dbTask.getShardingKey())) return; // 分片已失去
```

防止以下情况导致的无效执行：
- 管理员手动修改了任务状态
- 分片已被其他实例接管
- 其他异常导致的内存与 DB 不一致

---

## 4. 多实例兼容性

### 4.1 分片机制不变

每个实例的 DelayQueue 只加载自己分片内的任务。`ShardingContextHolder.shardingIndex()` 返回值用于过滤，和现有的 SQL 查询逻辑一致。

### 4.2 分片接管

当 `ScrambleDeadShardingTask` 接管宕机实例的分片后：
- `ShardingContextHolder.initShardingIndex()` 更新本地分片集合
- Producer 下一次扫描（20s 内）自动加载新分片的任务到 DelayQueue
- 无需额外处理，接管延迟 20s 对于故障恢复可接受

### 4.3 无分布式协调

DelayQueue 是进程内组件，各实例独立维护。依赖现有的 DB 分片机制避免任务被多实例重复执行，无需引入分布式锁。

---

## 5. 配置属性

新增配置（`SmartExecutorConfigure`）：

| 属性 | 默认值 | 说明 |
|------|--------|------|
| `scan-preload-multiplier` | 2 | 预加载倍数，`preloadWindow = taskFindInterval * multiplier` |
| `scheduler.max-in-memory` | 5000 | 内存中最大任务数量 |

复用现有配置：

| 属性 | 默认值 | 说明 |
|------|--------|------|
| `task-find-interval` | 20 | 语义变为兜底扫描间隔（秒），原轮询功能由 DelayQueue 取代 |
| `executor.*` | — | 线程池配置，DelayQueue 调度线程同样向该线程池提交任务 |
| `health.*` | — | 心跳和分片接管，不变 |
| `dead-task.*` | — | 死信检测，不变 |
| `clear-task.*` | — | 历史清理，不变 |

---

## 6. 改动范围

### 6.1 代码改动

| 文件 | 改动级别 | 改动内容 |
|------|---------|---------|
| `SimpleContainer.java` | **核心** | 新增 `DelayQueue<ScheduledTask>` + `SchedulerThread`；`ProducerTask` 改为低频兜底（仅加载到 DelayQueue，不直接提交执行）；新增 `enqueue()`/`afterExecute()`/`ScheduledTask` 内部类 |
| `RemoteRetryer.java` | 小 | `registerRemoteRetyTask()` 中 DB 写入后调用 `enqueue()` |
| `DefaultInnovation.java` | 小 | `finally` 块后通过回调接口通知调度器执行结果 |
| `SmartExecutorConfigure.java` | 小 | 新增 2 个配置属性 |

### 6.2 不动的文件

| 层次 | 文件 |
|------|------|
| DB 访问层 | `RetryTaskAccess`, `MybatisAccess`, `RetryTaskRepoImpl`, Mapper XML |
| 策略层 | `NextPlanTimeStrategy` 及 4 种实现 |
| 心跳/分片 | `MybatisHeart`, `ShardingContextHolder`, `HeartbeatContainer` |
| 死信/清理 | `DeadLetterTask`, `ClearTask` |
| 公共接口 | `RetryConfiguration`, `SmartSerializer`, `Identifier` |
| 缓存 | `RetryTaskCache` (保留不动，新方案用独立的去重集合) |
| 启动流程 | `RetryScannerMannger`, `SmartRetryRunFlag` |

---

## 7. 新增组件详细设计

### 7.1 ScheduledTask（DelayQueue 元素）

```java
class ScheduledTask implements Delayed {
    private final RetryTask task;
    private final long executeTimeMillis;  // next_plan_time 的毫秒值

    ScheduledTask(RetryTask task) {
        this.task = task;
        this.executeTimeMillis = task.getNextPlanTime().getTime();
    }

    @Override
    public long getDelay(TimeUnit unit) {
        return unit.convert(
            executeTimeMillis - System.currentTimeMillis(),
            TimeUnit.MILLISECONDS);
    }

    @Override
    public int compareTo(Delayed o) {
        return Long.compare(this.executeTimeMillis,
            ((ScheduledTask) o).executeTimeMillis);
    }
}
```

### 7.2 SchedulerThread（调度线程）

```java
class SchedulerThread implements Runnable {
    @Override
    public void run() {
        while (SmartRetryExit.isExit()) {
            ScheduledTask scheduled = delayQueue.take();  // 阻塞等待
            RetryTask task = scheduled.getTask();

            // 1. DB 状态校验
            if (!validateTaskInDB(task)) {
                inMemoryTaskKeys.remove(taskKey(task));
                continue;
            }

            // 2. 提交到线程池执行
            // ConsumerTask 执行完毕后会回调 afterExecute()，完成 DB 更新和入队/出队决策
            CompletableFuture.runAsync(
                new ConsumerTask(task, retryConfiguration, this::afterExecute),
                consumerExecutor
            );
        }
    }
}
```

### 7.3 ProducerTask（简化为兜底扫描）

```java
class ProducerTask implements Runnable {
    @Override
    public void run() {
        while (SmartRetryExit.isExit()) {
            if (!SmartRetryRunFlag.getFlag()) {
                sleep(taskFindInterval);
                continue;
            }

            // 扫描 DB，加载遗漏的任务到 DelayQueue
            List<RetryTask> tasks = access.listRetryTask(
                nextPlanTime <= now() + preloadWindow,
                limit = maxInMemory - inMemoryTaskKeys.size()
            );

            for (RetryTask task : tasks) {
                enqueue(task);  // 仅入队，不提交执行
            }

            sleep(taskFindInterval);
        }
    }
}
```

---

## 8. 启动与关闭流程

### 8.1 启动

1. `initTaskExecutor()` — 创建线程池（不变）
2. Producer 兜底扫描线程启动
3. DeadLetterTask / ClearTask 启动（如配置，不变）
4. `RetryScannerMannger` 完成扫描 -> `SmartRetryRunFlag.flag = true`
5. SchedulerThread 启动，开始从 DelayQueue 消费

### 8.2 关闭

在现有的 shutdown hook 中增加：
1. `SmartRetryExit` 标记退出，所有循环线程退出
2. SchedulerThread 中断，`delayQueue.take()` 被中断
3. 等待 `consumerExecutor` 中已有任务执行完毕
4. `consumerExecutor.shutdown()`

---

## 9. 风险与缓解

| 风险 | 缓解措施 |
|------|---------|
| DelayQueue 中任务因 JVM 崩溃丢失 | Producer 兜底扫描在 20s 内恢复 |
| 内存占用过高 | `maxInMemory` 上限 + 时间窗口过滤 |
| 时钟跳变导致 DelayQueue 行为异常 | 执行前 DB 校验作为安全阀 |
| 单 SchedulerThread 成为瓶颈 | `take()` 后仅做校验+提交，极快；任务执行在线程池中并行 |
| 启动时积压任务集中触发 | DelayQueue 按 `next_plan_time` 排序，按序触发，不会瞬时洪峰 |

---

## 10. 迁移兼容性

- `task-find-interval` 配置项保留，语义从"主轮询间隔"变为"兜底扫描间隔"
- 新增 `scan-preload-multiplier`、`scheduler.max-in-memory` 两个配置项，均有合理默认值
- DB schema 无变化
- API 无变化（`createTask()` 签名不变）
- 对外行为：任务执行延迟从 <=20s 降低到秒级精准
