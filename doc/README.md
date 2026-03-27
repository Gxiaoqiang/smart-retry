# Smart-Retry 数据库脚本文档

## 概述

本目录包含 Smart-Retry 框架支持的三种数据库的建表脚本：

1. **MySQL** - `smart_retry_mysql.sql`
2. **Oracle** - `smart_retry_oracle.sql`
3. **PostgreSQL** - `smart_retry_pg.sql`

## 表结构说明

### 1. retry_sharding 表（分片元数据表）

| 字段名 | 类型 | 必填 | 描述 |
|--------|------|------|------|
| id | BIGINT/NUMBER(20) | 是 | 主键，自增ID |
| gmt_create | DATETIME/DATE/TIMESTAMP | 是 | 创建时间 |
| status | TINYINT(4)/NUMBER(3)/SMALLINT | 是 | 状态：0-未分配，1-已分配 |
| creator_id | VARCHAR(128) | 否 | 创建分片的实例ID |
| instance_id | VARCHAR(128) | 否 | 当前持有分片的实例ID |
| last_heartbeat | DATETIME/DATE/TIMESTAMP | 否 | 最后心跳时间 |

**索引**：
- `idx_instance_id` - 实例ID索引
- `idx_last_heartbeat` - 最后心跳时间索引

### 2. retry_task 表（重试任务表）

| 字段名 | 类型 | 必填 | 描述 |
|--------|------|------|------|
| id | BIGINT/NUMBER(20) | 是 | 主键，自增ID |
| gmt_create | DATETIME/DATE/TIMESTAMP | 是 | 创建时间 |
| gmt_modified | DATETIME/DATE/TIMESTAMP | 是 | 修改时间 |
| sharding_key | BIGINT/NUMBER(20) | 是 | 分片键 |
| task_desc | VARCHAR(128) | 否 | 任务描述 |
| task_code | VARCHAR(128) | 否 | 需要执行的任务编码 |
| parameters | TEXT/CLOB | 否 | 参数数据 |
| attribute | TEXT/CLOB | 否 | 属性 |
| status | TINYINT/NUMBER(3)/SMALLINT | 是 | 最终执行状态：0-待执行，1-执行中，-1-执行失败，2-执行成功 |
| interval_second | INT/NUMBER(10)/INTEGER | 否 | 执行间隔秒，默认600秒（十分钟） |
| delay_second | INT/NUMBER(10)/INTEGER | 否 | 初次创建任务延迟时间，默认100秒 |
| max_execute_time | INT/NUMBER(10)/INTEGER | 否 | 任务最大执行时间 |
| next_plan_time | DATETIME/DATE/TIMESTAMP | 否 | 下次执行时间 |
| retry_num | INT/NUMBER(10)/INTEGER | 否 | 重试次数 |
| creator | VARCHAR(64) | 否 | 创建者（默认IP） |
| executor | VARCHAR(64) | 否 | 执行者 |
| origin_retry_num | INT/NUMBER(10)/INTEGER | 否 | 存放任务原始的次数 |
| current_log_id | BIGINT/NUMBER(20) | 否 | 当前运行日志id |
| unique_key | VARCHAR(64) | 否 | 唯一标识 |
| next_plan_time_strategy | INT/NUMBER(10)/INTEGER | 否 | 下次计划时间策略（对应 NextPlanTimeStrategyEnum 枚举） |

**索引**：
- `idx_next_plan_time` - 下次执行时间索引
- `idx_status_sharding_key_next_plan_time_retry_num` - 状态-分片键-下次执行时间-重试次数联合索引
- `idx_gmt_create_sharding_key` - 创建时间-分片键索引
- `idx_unique_key` - 唯一标识索引

## 使用指南

### MySQL
1. 登录 MySQL 数据库
2. 选择目标数据库
3. 执行：`source smart_retry_mysql.sql` 或复制脚本内容执行

### Oracle
1. 使用 SQL*Plus 或 SQL Developer 连接数据库
2. 以具有 DDL 权限的用户登录
3. 复制脚本内容执行（注意：索引名已缩短以适应 30 字符限制）

### PostgreSQL
1. 使用 psql 或 pgAdmin 连接数据库
2. 选择目标数据库
3. 执行：`\i smart_retry_pg.sql` 或复制脚本内容执行

## 注意事项

1. **执行顺序**：建议先执行 `retry_sharding` 表创建，再执行 `retry_task` 表创建
2. **权限要求**：需要 CREATE TABLE、CREATE INDEX、CREATE SEQUENCE（Oracle）、CREATE TRIGGER（Oracle）权限
3. **版本兼容**：
   - MySQL：5.7 或更高版本
   - Oracle：11g 或更高版本
   - PostgreSQL：9.6 或更高版本
4. **字符集**：MySQL 使用 utf8mb4，其他数据库使用默认字符集
5. **自增机制**：
   - MySQL：AUTO_INCREMENT
   - Oracle：序列 + 触发器
   - PostgreSQL：GENERATED ALWAYS AS IDENTITY

## 维护建议

1. 定期监控 `retry_task` 表数据量，考虑历史数据归档
2. 关注 `retry_sharding.last_heartbeat` 字段，确保分片心跳正常
3. 根据业务量调整索引策略
4. 建议对重要字段（如 status、next_plan_time）建立监控告警

## 更新记录

| 日期 | 版本 | 描述 |
|------|------|------|
| 2026-03-27 | 1.0.0 | 初始版本，完善注释和文档 |

## 联系支持

如有问题，请参考项目代码或联系开发团队。