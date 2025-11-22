-- 1. 创建表
CREATE TABLE retry_sharding (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    gmt_create TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    status SMALLINT NOT NULL,
    instance_id VARCHAR(64),
    last_heartbeat TIMESTAMP WITHOUT TIME ZONE DEFAULT NULL
);

-- 2. 添加表注释
COMMENT ON TABLE retry_sharding IS '分片元数据表';

-- 3. 添加列注释
COMMENT ON COLUMN retry_sharding.id IS 'ID';
COMMENT ON COLUMN retry_sharding.gmt_create IS '创建时间';
COMMENT ON COLUMN retry_sharding.status IS '状态 0:未分配 1:已分配';
COMMENT ON COLUMN retry_sharding.instance_id IS '当前持有分片的实例ID';
COMMENT ON COLUMN retry_sharding.last_heartbeat IS '最后心跳时间';

-- 4. 创建索引
CREATE INDEX idx_instance_id ON retry_sharding (instance_id);

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
COMMENT ON COLUMN retry_task.next_plan_time_strategy IS '下次计划时间策略';

-- 4. 创建索引
CREATE INDEX idx_next_plan_time ON retry_task (next_plan_time);
CREATE INDEX idx_status_next_plan_time_retry_num
    ON retry_task (status, next_plan_time, retry_num);
CREATE INDEX IF NOT EXISTS idx_retry_task_gmt_create ON retry_task(gmt_create);
