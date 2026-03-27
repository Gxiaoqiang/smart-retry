-- ============================================================
-- 文件：smart_retry_pg.sql
-- 描述：Smart-Retry 框架 PostgreSQL 数据库建表脚本
-- 版本：1.0.0
-- 创建时间：2026-03-27
-- 更新记录：
--   2026-03-27 初始版本，包含 retry_sharding 和 retry_task 表
-- ============================================================
-- 使用说明：
-- 1. 本脚本用于创建 Smart-Retry 框架所需的数据库表结构
-- 2. 请在 PostgreSQL 9.6 或更高版本中执行
-- 3. 执行前请确保有足够的权限创建表、索引等对象
-- 4. 建议在生产环境执行前在测试环境验证
-- 5. PostgreSQL 使用 GENERATED ALWAYS AS IDENTITY 实现自增，兼容性较好
-- ============================================================

-- ============================================================
-- 表：retry_sharding
-- 描述：分片元数据表，用于存储任务分片信息
-- ============================================================
-- 1. 创建表
CREATE TABLE retry_sharding (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    gmt_create TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    status SMALLINT NOT NULL,
    creator_id VARCHAR(128),
    instance_id VARCHAR(128),
    last_heartbeat TIMESTAMP WITHOUT TIME ZONE DEFAULT NULL
);

-- 2. 添加表注释
COMMENT ON TABLE retry_sharding IS '分片元数据表';

-- 3. 添加列注释
COMMENT ON COLUMN retry_sharding.id IS 'ID';
COMMENT ON COLUMN retry_sharding.gmt_create IS '创建时间';
COMMENT ON COLUMN retry_sharding.status IS '状态 0:未分配 1:已分配';
COMMENT ON COLUMN retry_sharding.creator_id IS '创建当前分片的创建者ID';
COMMENT ON COLUMN retry_sharding.instance_id IS '当前持有分片的实例ID';
COMMENT ON COLUMN retry_sharding.last_heartbeat IS '最后心跳时间';

-- 4. 创建索引
CREATE INDEX idx_instance_id ON retry_sharding (instance_id);
CREATE INDEX idx_last_heartbeat ON retry_sharding (last_heartbeat);

-- ============================================================
-- 表：retry_task
-- 描述：重试任务表，用于存储所有重试任务的信息
-- ============================================================
-- 1. 创建表
CREATE TABLE retry_task (
    id BIGINT GENERATED ALWAYS AS IDENTITY (START WITH 1094) PRIMARY KEY,
    gmt_create TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    gmt_modified TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    sharding_key BIGINT NOT NULL,
    task_desc VARCHAR(128),
    task_code VARCHAR(128),
    parameters TEXT,
    attribute TEXT,
    status SMALLINT NOT NULL,
    interval_second INTEGER,
    delay_second INTEGER,
    max_execute_time INTEGER,
    next_plan_time TIMESTAMP WITHOUT TIME ZONE,
    retry_num INTEGER,
    creator VARCHAR(64),
    executor VARCHAR(64),
    origin_retry_num INTEGER,
    current_log_id BIGINT,
    unique_key VARCHAR(64),
    next_plan_time_strategy INTEGER
);

-- 2. 添加表注释
COMMENT ON TABLE retry_task IS '重试任务表';

-- 3. 添加列注释
COMMENT ON COLUMN retry_task.id IS 'ID';
COMMENT ON COLUMN retry_task.gmt_create IS '创建时间';
COMMENT ON COLUMN retry_task.gmt_modified IS '修改时间';
COMMENT ON COLUMN retry_task.sharding_key IS '分片键';
COMMENT ON COLUMN retry_task.task_desc IS '任务描述';
COMMENT ON COLUMN retry_task.task_code IS '需要执行的任务编码';
COMMENT ON COLUMN retry_task.parameters IS '参数数据';
COMMENT ON COLUMN retry_task.attribute IS '属性';
COMMENT ON COLUMN retry_task.status IS '最终执行状态 0:待执行,1:执行中,-1:执行失败,2:执行成功';
COMMENT ON COLUMN retry_task.interval_second IS '执行间隔秒,如果不填写默认是600秒(十分钟执行一次)';
COMMENT ON COLUMN retry_task.delay_second IS '初次创建任务延迟时间，默认是100秒后执行';
COMMENT ON COLUMN retry_task.max_execute_time IS '任务最大执行时间';
COMMENT ON COLUMN retry_task.next_plan_time IS '下次执行时间';
COMMENT ON COLUMN retry_task.retry_num IS '重试次数';
COMMENT ON COLUMN retry_task.creator IS '创建者(默认是IP)';
COMMENT ON COLUMN retry_task.executor IS '执行者';
COMMENT ON COLUMN retry_task.origin_retry_num IS '存放任务原始的次数';
COMMENT ON COLUMN retry_task.current_log_id IS '当前运行日志id';
COMMENT ON COLUMN retry_task.unique_key IS '唯一标识';
COMMENT ON COLUMN retry_task.next_plan_time_strategy IS '下次计划时间策略（对应 NextPlanTimeStrategyEnum 枚举）';

-- 4. 创建索引
CREATE INDEX idx_next_plan_time ON retry_task (next_plan_time);
CREATE INDEX idx_status_sharding_key_next_plan_time_retry_num
    ON retry_task (status,sharding_key, next_plan_time, retry_num);
CREATE INDEX IF NOT EXISTS idx_retry_task_gmt_create_sharding_key ON retry_task(gmt_create, sharding_key);
CREATE INDEX IF NOT EXISTS idx_unique_key ON retry_task(unique_key);

-- ============================================================
-- 索引说明：
-- 1. retry_sharding.idx_instance_id: 实例ID索引，用于快速查找实例持有的分片
-- 2. retry_sharding.idx_last_heartbeat: 最后心跳时间索引，用于心跳检测
-- 3. retry_task.idx_next_plan_time: 下次执行时间索引，用于任务调度
-- 4. retry_task.idx_status_sharding_key_next_plan_time_retry_num:
--    状态-分片键-下次执行时间-重试次数联合索引，用于任务分片查询
-- 5. idx_retry_task_gmt_create_sharding_key: 创建时间-分片键索引，用于时间范围查询
-- 6. idx_unique_key: 唯一标识索引，用于任务去重
-- 注意：PostgreSQL 支持 CREATE INDEX IF NOT EXISTS 语法，避免重复创建
-- ============================================================
-- 结束