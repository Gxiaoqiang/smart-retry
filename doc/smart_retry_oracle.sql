-- ============================================================
-- 文件：smart_retry_oracle.sql
-- 描述：Smart-Retry 框架 Oracle 数据库建表脚本
-- 版本：1.0.0
-- 创建时间：2026-03-27
-- 更新记录：
--   2026-03-27 初始版本，包含 retry_sharding 和 retry_task 表
-- ============================================================
-- 使用说明：
-- 1. 本脚本用于创建 Smart-Retry 框架所需的数据库表结构
-- 2. 请在 Oracle 11g 或更高版本中执行
-- 3. 执行前请确保有足够的权限创建表、序列、触发器等对象
-- 4. 建议在生产环境执行前在测试环境验证
-- 5. 注意：Oracle 索引名长度限制为 30 字符，部分索引名已缩短
-- ============================================================

-- ============================================================
-- 表：retry_sharding
-- 描述：分片元数据表，用于存储任务分片信息
-- ============================================================
-- 创建 retry_sharding 表
CREATE TABLE retry_sharding (
    id              NUMBER(20)      NOT NULL,
    gmt_create      DATE            NOT NULL,
    status          NUMBER(3)       NOT NULL,
    creator_id     VARCHAR2(128),
    instance_id     VARCHAR2(128),
    last_heartbeat  DATE
);

-- 添加主键约束
ALTER TABLE retry_sharding ADD CONSTRAINT pk_retry_sharding PRIMARY KEY (id);

-- 创建索引
CREATE INDEX idx_instance_id ON retry_sharding (instance_id);
CREATE INDEX idx_last_heartbeat ON retry_sharding (last_heartbeat);

-- 添加列注释
COMMENT ON COLUMN retry_sharding.id IS 'ID';
COMMENT ON COLUMN retry_sharding.gmt_create IS '创建时间';
COMMENT ON COLUMN retry_sharding.status IS '状态 0:未分配 1:已分配';
COMMENT ON COLUMN retry_sharding.creator_id IS '创建当前分片的实例ID';
COMMENT ON COLUMN retry_sharding.instance_id IS '当前持有分片的实例ID';
COMMENT ON COLUMN retry_sharding.last_heartbeat IS '最后心跳时间';

-- 添加表注释
COMMENT ON TABLE retry_sharding IS '分片元数据表';

-- 创建 retry_sharding 表的 ID 自增序列
CREATE SEQUENCE seq_retry_sharding_id START WITH 1 INCREMENT BY 1 NOCACHE;

-- 创建触发器实现 ID 自增
CREATE OR REPLACE TRIGGER trg_retry_sharding_id_autoinc
BEFORE INSERT ON retry_sharding
FOR EACH ROW
WHEN (NEW.id IS NULL)
BEGIN
    SELECT seq_retry_sharding_id.NEXTVAL INTO :NEW.id FROM dual;
END;
/

-- ============================================================
-- 表：retry_task
-- 描述：重试任务表，用于存储所有重试任务的信息
-- ============================================================
-- 创建 retry_task 表
CREATE TABLE retry_task (
    id                      NUMBER(20)      NOT NULL,
    gmt_create              DATE            NOT NULL,
    gmt_modified            DATE            NOT NULL,
    sharding_key            NUMBER(20)      NOT NULL,
    task_desc               VARCHAR2(128),
    task_code               VARCHAR2(128),
    parameters              CLOB,
    attribute               CLOB,
    status                  NUMBER(3)       NOT NULL,
    interval_second         NUMBER(10),
    delay_second            NUMBER(10),
    max_execute_time        NUMBER(10),
    next_plan_time          DATE,
    retry_num               NUMBER(10),
    creator                 VARCHAR2(64),
    executor                VARCHAR2(64),
    origin_retry_num        NUMBER(10),
    current_log_id          NUMBER(20),
    unique_key              VARCHAR2(64),
    next_plan_time_strategy NUMBER(10)
);

-- 添加主键约束
ALTER TABLE retry_task ADD CONSTRAINT pk_retry_task PRIMARY KEY (id);

-- 创建索引 (注意：Oracle 索引名长度限制通常是 30 个字符，原 MySQL 名可能过长)
-- idx_next_plan_time
CREATE INDEX idx_next_plan_time ON retry_task (next_plan_time);

-- idx_status_next_plan_time_retry_num
CREATE INDEX idx_status_npt_rn ON retry_task (status, sharding_key,next_plan_time, retry_num); -- 缩短索引名

-- idx_gmt_create_sharding_key
CREATE INDEX idx_gmt_create_sk ON retry_task (gmt_create, sharding_key); -- 缩短索引名

CREATE INDEX idx_unique_key ON retry_task (unique_key);

-- 添加列注释
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

-- 添加表注释
COMMENT ON TABLE retry_task IS '重试任务表';

-- 创建 retry_task 表的 ID 自增序列 (假设从 1094 开始，如 MySQL 的 AUTO_INCREMENT=1094)
CREATE SEQUENCE seq_retry_task_id START WITH 1094 INCREMENT BY 1 NOCACHE;

-- 创建触发器实现 ID 自增
CREATE OR REPLACE TRIGGER trg_retry_task_id_autoinc
BEFORE INSERT ON retry_task
FOR EACH ROW
WHEN (NEW.id IS NULL)
BEGIN
    SELECT seq_retry_task_id.NEXTVAL INTO :NEW.id FROM dual;
END;
/

-- ============================================================
-- 索引说明：
-- 1. retry_sharding.idx_instance_id: 实例ID索引，用于快速查找实例持有的分片
-- 2. retry_sharding.idx_last_heartbeat: 最后心跳时间索引，用于心跳检测
-- 3. retry_task.idx_next_plan_time: 下次执行时间索引，用于任务调度
-- 4. retry_task.idx_status_npt_rn: 状态-分片键-下次执行时间-重试次数联合索引，用于任务分片查询
-- 5. retry_task.idx_gmt_create_sk: 创建时间-分片键索引，用于时间范围查询
-- 6. retry_task.idx_unique_key: 唯一标识索引，用于任务去重
-- ============================================================
-- 序列和触发器说明：
-- 1. seq_retry_sharding_id: retry_sharding 表自增序列，起始值 1
-- 2. trg_retry_sharding_id_autoinc: retry_sharding 表自增触发器
-- 3. seq_retry_task_id: retry_task 表自增序列，起始值 1094（与 MySQL 保持一致）
-- 4. trg_retry_task_id_autoinc: retry_task 表自增触发器
-- ============================================================
-- 结束