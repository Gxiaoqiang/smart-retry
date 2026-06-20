# DelayQueue 精准调度 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 smart-retry 任务调度从 DB 轮询（最大 20s 延迟）升级为内存 DelayQueue 精准调度（百毫秒级延迟），DB 轮询降级为低频兜底。

**Architecture:** 新增 `DelayQueue<ScheduledTask>` + `SchedulerThread` 作为主调度通道，`ProducerTask` 简化为低频兜底扫描（仅入队不提交执行），`ConsumerTask` 执行完毕后通过 `afterExecute()` 回调决定出队或重新入队。DB 更新逻辑完全不动。

**Tech Stack:** Java 17, Spring Boot 3.3.5, `java.util.concurrent.DelayQueue`, MyBatis

---

## 文件结构

| 文件 | 职责 | 改动类型 |
|------|------|---------|
| `SmartExecutorConfigure.java` | 新增 `scanPreloadMultiplier`、`maxInMemory` 配置 | 小改 |
| `RetryTaskAccess.java` | 新增带 `maxNextPlanTime`+`limit` 参数的查询方法 | 小改 |
| `MybatisAccess.java` | 实现新查询方法，委托给 Repo | 小改 |
| `RetryTaskRepoImpl.java` | 新增支持 `maxNextPlanTime`+`limit` 的查询 SQL | 小改 |
| `retry-task-mapper.xml` (MySQL/PG/Oracle) | SQL 新增 `maxNextPlanTime` 参数支持 | 小改 |
| `SimpleContainer.java` | 核心改造：DelayQueue、ScheduledTask、SchedulerThread、ProducerTask 简化、ConsumerTask 回调 | **大改** |
| `RemoteRetryer.java` | DB 写入后调用 `SimpleContainer.enqueue()` | 小改 |
| `SimpleRetryTaskOperator.java` | `createTask()` 后调用 `SimpleContainer.enqueue()` | 小改 |
| `smart-retry-test` | 新增精准调度验证测试 | 新测试 |

---

### Task 1: SmartExecutorConfigure 新增配置属性

**Files:**
- Modify: `smart-retry-core/src/main/java/com/smart/retry/core/config/SmartExecutorConfigure.java`

- [ ] **Step 1: 新增两个配置属性**

在 `SmartExecutorConfigure` 类中添加（`taskFindInterval` 字段附近）：

```java
/**
 * 预加载倍数，preloadWindow = taskFindInterval * scanPreloadMultiplier
 * 默认 2，即预加载窗口 = 20s × 2 = 40s
 */
private int scanPreloadMultiplier = 2;

/**
 * 内存中最大任务数量，达到上限后 Producer 停止加载新任务
 */
private int maxInMemory = 3000;

public int getScanPreloadMultiplier() {
    return scanPreloadMultiplier;
}

public void setScanPreloadMultiplier(int scanPreloadMultiplier) {
    if (scanPreloadMultiplier < 1) {
        throw new IllegalArgumentException("scanPreloadMultiplier must be greater than 0");
    }
    this.scanPreloadMultiplier = scanPreloadMultiplier;
}

public int getMaxInMemory() {
    return maxInMemory;
}

public void setMaxInMemory(int maxInMemory) {
    if (maxInMemory < 100) {
        throw new IllegalArgumentException("maxInMemory must be greater than 100");
    }
    this.maxInMemory = maxInMemory;
}
```

- [ ] **Step 2: 编译验证**

```bash
mvn -pl smart-retry-core compile
```
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add smart-retry-core/src/main/java/com/smart/retry/core/config/SmartExecutorConfigure.java
git commit -m "feat: SmartExecutorConfigure 新增 scanPreloadMultiplier 和 maxInMemory 配置

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

### Task 2: RetryTaskAccess 新增带参数的查询方法

**Files:**
- Modify: `smart-retry-common/src/main/java/com/smart/retry/common/RetryTaskAccess.java`

- [ ] **Step 1: 新增接口方法**

在 `RetryTaskAccess` 接口中新增：

```java
/**
 * 获取待重试任务（支持预加载窗口和数量限制）
 * @param maxNextPlanTime 最大下次执行时间，null 则默认 now()
 * @param limit 每次拉取的最大数量
 * @return 待重试任务列表
 */
List<RetryTask> listRetryTask(java.util.Date maxNextPlanTime, int limit);
```

- [ ] **Step 2: 编译验证**

```bash
mvn -pl smart-retry-common compile
```
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add smart-retry-common/src/main/java/com/smart/retry/common/RetryTaskAccess.java
git commit -m "feat: RetryTaskAccess 新增带 maxNextPlanTime 和 limit 参数的 listRetryTask 方法

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

### Task 3: RetryTaskRepoImpl + MybatisAccess 实现新查询

**Files:**
- Modify: `smart-retry-extensions/smart-retry-mybatis/src/main/java/com/smart/retry/mybatis/access/MybatisAccess.java`
- Modify: `smart-retry-extensions/smart-retry-mybatis/src/main/java/com/smart/retry/mybatis/repo/impl/RetryTaskRepoImpl.java`
- Modify: `smart-retry-extensions/smart-retry-mybatis/src/main/resources/mysql/retry-task-mapper.xml`
- Modify: `smart-retry-extensions/smart-retry-mybatis/src/main/resources/postgresql/retry-task-mapper.xml` (如存在)
- Modify: `smart-retry-extensions/smart-retry-mybatis/src/main/resources/oracle/retry-task-mapper.xml` (如存在)

> 注意：需要先找到 PostgresSQL 和 Oracle 的 mapper 文件路径。以下以 MySQL 为例，其他数据库同理。

- [ ] **Step 1: MybatisAccess 实现新接口方法**

在 `MybatisAccess.java` 中新增：

```java
@Override
public List<RetryTask> listRetryTask(Date maxNextPlanTime, int limit) {
    return retryTaskRepo.listAllWaitingRetryTask(maxNextPlanTime, limit);
}
```

- [ ] **Step 2: RetryTaskRepoImpl 新增重载方法**

在 `RetryTaskRepoImpl.java` 中新增方法（类中需注入 `RetryTaskDao`）：

```java
public List<RetryTask> listAllWaitingRetryTask(Date maxNextPlanTime, int limit) {
    RetryTaskQuery query = new RetryTaskQuery();
    query.setStatusList(Arrays.asList(
        RetryTaskStatus.WAITING.getCode(),
        RetryTaskStatus.FAIL.getCode()
    ));
    query.setMinRetryNum(1);
    query.setMaxNextPlanTime(maxNextPlanTime != null ? maxNextPlanTime : new Date());
    query.setLimit(limit);
    List<Integer> shardingKeyList = ShardingContextHolder.shardingIndex();
    query.setShardingKeyList(shardingKeyList);
    return retryTaskDao.selectByQuery(query);
}
```

- [ ] **Step 3: 确认 Mapper XML 支持 limit 参数**

查看 MySQL mapper XML 中 `selectByQuery` 是否已支持 `<if test="limit != null">LIMIT #{limit}</if>`。如果没有则添加：

```xml
<if test="limit != null">
    LIMIT #{limit}
</if>
```

- [ ] **Step 4: 编译验证**

```bash
mvn -pl smart-retry-extensions/smart-retry-mybatis compile
```
Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add smart-retry-extensions/
git commit -m "feat: 实现带 maxNextPlanTime 和 limit 参数的 listRetryTask 查询

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

### Task 4: SimpleContainer 核心改造 — ScheduledTask + DelayQueue + SchedulerThread

**Files:**
- Modify: `smart-retry-core/src/main/java/com/smart/retry/core/SimpleContainer.java`

这是最大的改动。分 4 个子步骤。

- [ ] **Step 1: 新增字段和内部类 ScheduledTask**

在 `SimpleContainer` 类中新增以下字段（放在 `MAX_QUEUE_SIZE` 后面）：

```java
// ========== DelayQueue 精准调度相关 ==========

/** 内存精准调度队列 */
private final DelayQueue<ScheduledTask> delayQueue = new DelayQueue<>();

/** 内存中去重集合，key = taskCode + "-" + uniqueKey */
private final Set<String> inMemoryTaskKeys = ConcurrentHashMap.newKeySet();

/** 调度线程 */
private Thread schedulerThread;

/** 预加载窗口毫秒数 */
private long preloadWindowMs;

/**
 * DelayQueue 元素，按 next_plan_time 排序
 */
static class ScheduledTask implements Delayed {
    private final RetryTask task;
    private final long executeTimeMillis;

    ScheduledTask(RetryTask task) {
        this.task = task;
        this.executeTimeMillis = task.getNextPlanTime().getTime();
    }

    RetryTask getTask() {
        return task;
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

- [ ] **Step 2: 新增 enqueue()、afterExecute()、validateTaskInDB() 方法**

```java
/**
 * 将任务加入 DelayQueue，自动去重
 * @return true=入队成功，false=已在内存中
 */
boolean enqueue(RetryTask task) {
    String key = getUniqueKey(task);
    if (!inMemoryTaskKeys.add(key)) {
        return false;  // 已在内存中，跳过
    }
    delayQueue.put(new ScheduledTask(task));
    return true;
}

/**
 * 任务执行完毕后的回调
 * 注意：此时 DB 已更新 (status/retryNum/nextPlanTime)
 */
void afterExecute(RetryTask task) {
    String key = getUniqueKey(task);
    inMemoryTaskKeys.remove(key);  // 无论如何先移除

    Integer status = task.getStatus();
    Integer retryNum = task.getRetryNum();

    // 成功或重试次数耗尽 → 结束
    if (RetryTaskStatus.SUCCESS.getCode().equals(status) || retryNum <= 0) {
        return;
    }

    // 失败 + 还有重试次数
    boolean inWindow = task.getNextPlanTime().getTime()
                       <= System.currentTimeMillis() + preloadWindowMs;

    if (inWindow) {
        enqueue(task);  // 窗口内 → 精准调度
    }
    // else: 窗口外 → 内存移除，由 Producer 后续扫描加载
}

/**
 * 执行前校验 DB 状态，防止无效执行
 */
private boolean validateTaskInDB(RetryTask task) {
    try {
        RetryTask dbTask = retryConfiguration.getRetryTaskAcess().getRetryTask(task.getId());
        if (dbTask == null) {
            return false;
        }
        Integer status = dbTask.getStatus();
        if (!RetryTaskStatus.WAITING.getCode().equals(status)
            && !RetryTaskStatus.FAIL.getCode().equals(status)) {
            return false;
        }
        if (dbTask.getRetryNum() <= 0) {
            return false;
        }
        if (!ShardingContextHolder.shardingIndex().contains(dbTask.getShardingKey())) {
            return false;
        }
        return true;
    } catch (Exception e) {
        LOGGER.warn("[validateTaskInDB] check failed for task:{}", task.getId(), e);
        return true;  // 查询异常时放行，避免因 DB 抖动阻塞调度
    }
}
```

- [ ] **Step 3: 新增 SchedulerThread 内部类**

```java
/**
 * 调度线程：从 DelayQueue 中 take() 到期任务，校验后提交执行
 */
class SchedulerThread implements Runnable {
    @Override
    public void run() {
        while (SmartRetryExit.isExit()) {
            try {
                ScheduledTask scheduled = delayQueue.take();  // 阻塞等待
                RetryTask task = scheduled.getTask();

                // 1. DB 状态校验
                if (!validateTaskInDB(task)) {
                    inMemoryTaskKeys.remove(getUniqueKey(task));
                    continue;
                }

                // 2. 提交到线程池执行
                CompletableFuture.runAsync(
                    new ConsumerTask(task, retryConfiguration),
                    consumerExecutor
                );
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;  // 线程被中断，退出
            } catch (Exception e) {
                LOGGER.error("[SchedulerThread] error", e);
            }
        }
    }
}
```

- [ ] **Step 4: 编译验证**

```bash
mvn -pl smart-retry-core compile
```
Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add smart-retry-core/src/main/java/com/smart/retry/core/SimpleContainer.java
git commit -m "feat: SimpleContainer 新增 DelayQueue/ScheduledTask/SchedulerThread 核心组件

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

### Task 5: SimpleContainer — 改造 ProducerTask 为兜底扫描

**Files:**
- Modify: `smart-retry-core/src/main/java/com/smart/retry/core/SimpleContainer.java`

- [ ] **Step 1: 重写 ProducerTask**

将 `ProducerTask` 内部类替换为：

```java
/**
 * 兜底扫描线程：低频扫描 DB，将遗漏任务加入 DelayQueue
 * 不再直接提交任务到 executor
 */
class ProducerTask implements Runnable {
    private long SLEEP_BASE_TIME_MILLISECONDS;

    ProducerTask() {
        this.SLEEP_BASE_TIME_MILLISECONDS = smartConfigure.getTaskFindInterval() * 1000L;
    }

    @Override
    public void run() {
        while (SmartRetryExit.isExit()) {

            if (!SmartRetryRunFlag.getFlag()) {
                sleepOneInterval();
                continue;
            }

            try {
                int currentSize = inMemoryTaskKeys.size();
                int availableSlots = smartConfigure.getMaxInMemory() - currentSize;
                if (availableSlots <= 0) {
                    LOGGER.warn("[ProducerTask] 内存任务数达到上限 {}, 跳过本轮扫描", smartConfigure.getMaxInMemory());
                    sleepOneInterval();
                    continue;
                }

                Date maxNextPlanTime = new Date(
                    System.currentTimeMillis() + preloadWindowMs);
                List<RetryTask> allRetryTask = retryConfiguration
                    .getRetryTaskAcess()
                    .listRetryTask(maxNextPlanTime, Math.min(availableSlots, 500));

                if (CollectionUtils.isEmpty(allRetryTask)) {
                    sleepOneInterval();
                    continue;
                }

                int enqueued = 0;
                for (RetryTask retryTask : allRetryTask) {
                    if (enqueue(retryTask)) {
                        enqueued++;
                    }
                }

                if (smartConfigure.shouldLogInfo() && enqueued > 0) {
                    LOGGER.info("[ProducerTask] 兜底扫描加载 {} 个任务到 DelayQueue, 内存中任务数: {}",
                        enqueued, inMemoryTaskKeys.size());
                }

                sleepOneInterval();
            } catch (Exception e) {
                LOGGER.error("[ProducerTask] error", e);
            }
        }
    }

    private void sleepOneInterval() {
        try {
            TimeUnit.MILLISECONDS.sleep(SLEEP_BASE_TIME_MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
```

- [ ] **Step 2: 编译验证**

```bash
mvn -pl smart-retry-core compile
```
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add smart-retry-core/src/main/java/com/smart/retry/core/SimpleContainer.java
git commit -m "feat: ProducerTask 改造为兜底扫描，仅加载到 DelayQueue 不直接提交执行

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

### Task 6: SimpleContainer — 改造 ConsumerTask 增加回调

**Files:**
- Modify: `smart-retry-core/src/main/java/com/smart/retry/core/SimpleContainer.java`

- [ ] **Step 1: 修改 ConsumerTask 的 run() 方法**

在 `ConsumerTask` 的 `run()` 方法的 `finally` 块中，增加 `afterExecute()` 回调：

```java
@Override
public void run() {
    String uniqueKey = getUniqueKey(retryTask);

    try {
        SmartInnovation innovation = new DefaultInnovation(retryTask, retryConfiguration);
        innovation.invoke();
    } catch (Throwable e) {
        LOGGER.error("[ConsumerTask-run error,retryTask:{} ", GsonTool.toJsonString(retryTask), e);
    } finally {
        RetryTaskCache.removeTaskFlag(uniqueKey);
        // 通知调度器执行结果（决定出队或重新入队）
        afterExecute(retryTask);
    }
}
```

> 注意：`afterExecute(retryTask)` 在 `RetryTaskCache.removeTaskFlag(uniqueKey)` **之后**调用，保证先清理旧标记再决策。

- [ ] **Step 2: 编译验证**

```bash
mvn -pl smart-retry-core compile
```
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add smart-retry-core/src/main/java/com/smart/retry/core/SimpleContainer.java
git commit -m "feat: ConsumerTask 增加 afterExecute 回调，执行完成后通知调度器

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

### Task 7: SimpleContainer — 修改 start() 和关闭流程

**Files:**
- Modify: `smart-retry-core/src/main/java/com/smart/retry/core/SimpleContainer.java`

- [ ] **Step 1: 修改 start() 方法**

```java
@Override
public void start() {
    initTaskExecutor(smartConfigure);

    // 初始化预加载窗口
    this.preloadWindowMs = (long) smartConfigure.getTaskFindInterval()
        * smartConfigure.getScanPreloadMultiplier() * 1000L;

    // Producer 兜底扫描线程（低频，仅加载到 DelayQueue）
    Thread producerTask = new Thread(new ProducerTask(), "smart-retry-producer");
    producerTask.start();

    // SchedulerThread 调度线程（从 DelayQueue 消费，精准触发）
    schedulerThread = new Thread(new SchedulerThread(), "smart-retry-scheduler");
    schedulerThread.start();

    if (smartConfigure.getDeadTask().getDeadTaskCheck()) {
        Thread deadLetterTask = new Thread(new DeadLetterTask());
        deadLetterTask.setDaemon(true);
        deadLetterTask.start();
    }

    if (smartConfigure.getClearTask().getEnabled()) {
        initTaskScheduler();
        CronTrigger trigger = new CronTrigger(smartConfigure.getClearTask().getCron());
        taskScheduler.schedule(new ClearTask(), trigger);
    }
}
```

- [ ] **Step 2: 修改 shutdown hook**

在已有的 static shutdown hook 中增加 scheduler 中断：

```java
static {
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
        if (schedulerThread != null) {
            schedulerThread.interrupt();
        }
        if (consumerExecutor != null) {
            consumerExecutor.shutdown();
        }
        if (taskScheduler != null) {
            taskScheduler.shutdown();
        }
    }));
}
```

- [ ] **Step 3: 编译验证**

```bash
mvn -pl smart-retry-core compile
```
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add smart-retry-core/src/main/java/com/smart/retry/core/SimpleContainer.java
git commit -m "feat: SimpleContainer.start() 启动 SchedulerThread，shutdown hook 增加中断处理

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

### Task 8: RemoteRetryer — AOP 模式创建任务后入队

**Files:**
- Modify: `smart-retry-core/src/main/java/com/smart/retry/core/retry/RemoteRetryer.java`

- [ ] **Step 1: registerRemoteRetyTask() 末尾增加入队**

在 `retryConfiguration.getRetryTaskAcess().saveRetryTask(retryTask);` 之后增加：

```java
// 将任务加入 DelayQueue 精准调度（窗口内才入队）
retryConfiguration.getRetryTaskAcess().saveRetryTask(retryTask);

// 获取 SimpleContainer 实例并入队
// 注意：需要通过 RetryConfiguration 或静态方法暴露 enqueue 能力
SimpleContainer.enqueueIfInWindow(retryTask);
```

> 注意：`enqueueIfInWindow` 需要是一个静态方法或通过 `SimpleContainer` 实例访问。由于 `RemoteRetryer` 不持有 `SimpleContainer` 引用，需要设计访问方式。

**设计决策**：在 `SimpleContainer` 中新增静态方法 `enqueueIfInWindow`，通过静态字段持有自身引用（类似 Spring 的 `ApplicationContext` 模式）。

在 `SimpleContainer.java` 中新增：

```java
// 自身引用，供 RemoteRetryer 等外部调用
private static SimpleContainer INSTANCE;

public SimpleContainer(RetryConfiguration retryConfiguration,
                       SmartExecutorConfigure smartExecutorConfigure) {
    this.retryConfiguration = retryConfiguration;
    this.smartConfigure = smartExecutorConfigure;
    INSTANCE = this;  // 持有自身引用
}

/**
 * 静态方法：任务写入 DB 后调用，窗口内则入队
 * 供 RemoteRetryer 和 SimpleRetryTaskOperator 使用
 */
static void enqueueIfInWindow(RetryTask task) {
    if (INSTANCE == null) {
        return;  // 容器未初始化
    }
    long nextPlanTime = task.getNextPlanTime().getTime();
    long windowEnd = System.currentTimeMillis() + INSTANCE.preloadWindowMs;
    if (nextPlanTime <= windowEnd) {
        INSTANCE.enqueue(task);
    }
}
```

然后在 `RemoteRetryer.registerRemoteRetyTask()` 末尾调用 `SimpleContainer.enqueueIfInWindow(retryTask)`。

- [ ] **Step 2: 编译验证**

```bash
mvn -pl smart-retry-core compile
```
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add smart-retry-core/src/main/java/com/smart/retry/core/retry/RemoteRetryer.java
git add smart-retry-core/src/main/java/com/smart/retry/core/SimpleContainer.java
git commit -m "feat: RemoteRetryer DB 写入后将任务加入 DelayQueue，实现 AOP 模式的秒级调度

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

### Task 9: SimpleRetryTaskOperator — CLASS 模式创建任务后入队

**Files:**
- Modify: `smart-retry-core/src/main/java/com/smart/retry/core/SimpleRetryTaskOperator.java`

- [ ] **Step 1: createTask() 末尾增加入队**

在 `SimpleRetryTaskOperator.createTask()` 方法末尾，`saveRetryTask` 返回后，添加：

```java
// 获取完整的 RetryTask（含 saveRetryTask 中设置的 next_plan_time）
// 注意：saveRetryTask 内部会设置 next_plan_time，返回的是 taskId
// 需要重新获取完整对象或直接在 saveRetryTask 调用后入队

// 由于 saveRetryTask 在 DB 层设置了 next_plan_time，但内存中的 retryTask 对象
// 的 next_plan_time 可能未更新。需要在 saveRetryTask 之前计算好 next_plan_time
// 或 saveRetryTask 之后重新查询。

// 实际上，查看 RetryTaskRepoImpl.saveRetryTask():
//   long nextTime = System.currentTimeMillis() + retryTask.getDelaySecond() * 1000;
//   retryTask.setNextPlanTime(new Date(nextTime));
// 在 save 过程中已经 set 到了 retryTask 对象上！

long taskId = retryConfiguration.getRetryTaskAcess().saveRetryTask(retryTask);
SimpleContainer.enqueueIfInWindow(retryTask);
return taskId;
```

- [ ] **Step 2: 编译验证**

```bash
mvn -pl smart-retry-core compile
```
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add smart-retry-core/src/main/java/com/smart/retry/core/SimpleRetryTaskOperator.java
git commit -m "feat: SimpleRetryTaskOperator createTask 后将任务加入 DelayQueue

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

### Task 10: 编写集成测试 — 验证精准调度

**Files:**
- Create: `smart-retry-test/src/test/java/com/smart/retry/test/DelayQueueSchedulingTest.java`

- [ ] **Step 1: 编写测试类**

```java
package com.smart.retry.test;

import com.smart.retry.common.RetryTaskOperator;
import com.smart.retry.common.constant.NextPlanTimeStrategyEnum;
import com.smart.retry.common.constant.RetryTaskStatus;
import com.smart.retry.common.model.RetryTask;
import com.smart.retry.common.model.RetryTaskBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
public class DelayQueueSchedulingTest {

    @Autowired
    private RetryTaskOperator<TestParam> retryTaskOperator;

    @Autowired
    private TestRetryListener testRetryListener;

    /**
     * 验证：任务创建后能在 taskFindInterval 内被触发（而非等待完整的轮询周期）
     */
    @Test
    void testTaskTriggeredWithinShortDelay() throws Exception {
        // 准备参数
        TestParam param = new TestParam("delay-queue-test");

        // 创建任务：delaySecond=5s（当前时间+5s后执行）
        RetryTaskBuilder<TestParam> builder = new RetryTaskBuilder<>();
        builder.setTaskCode("test-delay-queue");
        builder.setParam(param);
        builder.setRetryNum(1);
        builder.setDelaySecond(5);
        builder.setIntervalSecond(10);
        builder.setNextPlanTimeStrategy(NextPlanTimeStrategyEnum.FIXED);

        long startTime = System.currentTimeMillis();
        long taskId = retryTaskOperator.createTask(builder);

        // 等待任务执行完成（最多等待 15s，远超 taskFindInterval 但用于验证快速触发）
        boolean executed = testRetryListener.awaitExecution(15, TimeUnit.SECONDS);

        long elapsed = System.currentTimeMillis() - startTime;

        assertTrue(executed, "任务应在 5s delay + 调度延迟 内被触发");
        assertTrue(elapsed < 10000,
            "任务应在 10s 内完成（5s delay + 最多几秒调度延迟），实际: " + elapsed + "ms");
    }
}
```

- [ ] **Step 2: 编写辅助测试组件**

```java
// TestParam.java
package com.smart.retry.test;

public class TestParam {
    private String value;
    public TestParam() {}
    public TestParam(String value) { this.value = value; }
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
}

// TestRetryListener.java
package com.smart.retry.test;

import com.smart.retry.common.RetryLinstener;
import com.smart.retry.common.constant.ExecuteResultStatus;
import com.smart.retry.common.annotation.RetryOnClass;
import org.springframework.stereotype.Component;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Component
@RetryOnClass(taskCode = "test-delay-queue")
public class TestRetryListener implements RetryLinstener<TestParam> {

    private volatile CountDownLatch latch = new CountDownLatch(1);

    @Override
    public ExecuteResultStatus consume(TestParam param) {
        latch.countDown();
        return ExecuteResultStatus.SUCCESS;
    }

    public boolean awaitExecution(long timeout, TimeUnit unit) throws InterruptedException {
        return latch.await(timeout, unit);
    }

    public void reset() {
        latch = new CountDownLatch(1);
    }
}
```

- [ ] **Step 3: 运行测试**

```bash
mvn -pl smart-retry-test test -Dtest=DelayQueueSchedulingTest
```
Expected: PASS — 任务在 5s delay + 少量调度延迟内被执行

- [ ] **Step 4: Commit**

```bash
git add smart-retry-test/
git commit -m "test: 新增 DelayQueue 精准调度集成测试

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

### Task 11: 运行全部测试，确保无回归

- [ ] **Step 1: 运行全部测试**

```bash
mvn clean test
```
Expected: All tests PASS

- [ ] **Step 2: 修复任何失败的测试**

检查测试失败原因，修复代码。

- [ ] **Step 3: Commit（如有修复）**

```bash
git add -u
git commit -m "fix: 修复 DelayQueue 改造导致的测试回归

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

### Task 12: 手动验证 — 启动 Web 管理后台端到端验证

- [ ] **Step 1: 启动应用**

```bash
mvn -pl smart-retry-web spring-boot:run
```

- [ ] **Step 2: 创建测试任务并观察日志**

通过 API 或前端创建一个 delaySecond=10s 的任务，观察日志中的调度时间是否精准（预计在 10s ± 2s 范围内触发）。

- [ ] **Step 3: 验证异常恢复**

1. 创建一个重试 3 次、间隔 15s 的任务
2. 故意让业务方法抛出异常（让其不断重试）
3. 观察每次重试的时间精度
4. 停止应用，检查 DB 中任务状态是否正确
5. 重新启动应用，检查 Producer 兜底扫描是否将遗漏任务恢复

- [ ] **Step 4: Commit（如有修复）**

```bash
git add -u
git commit -m "fix: 端到端验证修复

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

## 风险检查清单

- [ ] `taskFindInterval` 配置从"主轮询间隔"变为"兜底扫描间隔"，语义变更需在文档中说明
- [ ] `RetryTaskCache.retryTasks` 保留不动，新方案使用独立的 `inMemoryTaskKeys`
- [ ] `invokeTaskAsync`/`invokeTaskSync` 是手动触发接口，保持原逻辑（直接提交到 executor，不经过 DelayQueue）
- [ ] `SmartRetryRunFlag` 时序：SchedulerThread 不依赖该 flag（因为 DelayQueue 初始为空），Producer 仍依赖它
- [ ] Mapper XML 文件需要确认其他数据库（PG/Oracle）的版本也支持 `maxNextPlanTime` 参数
