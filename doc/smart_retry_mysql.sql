-- ============================================================
-- 文件：smart_retry_mysql.sql
-- 描述：Smart-Retry 框架 MySQL 数据库建表脚本
-- 版本：1.0.0
-- 创建时间：2026-03-27
-- 更新记录：
--   2026-03-27 初始版本，包含 retry_sharding 和 retry_task 表
-- ============================================================
-- 使用说明：
-- 1. 本脚本用于创建 Smart-Retry 框架所需的数据库表结构
-- 2. 请在 MySQL 5.7 或更高版本中执行
-- 3. 执行前请确保有足够的权限创建表、索引等对象
-- 4. 建议在生产环境执行前在测试环境验证
-- ============================================================

-- ============================================================
-- 表：retry_sharding
-- 描述：分片元数据表，用于存储任务分片信息
-- ============================================================
CREATE TABLE `retry_sharding` (
                               `id` bigint NOT NULL PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
                                gmt_create     DATETIME  NOT NULL COMMENT '创建时间',
                                status         TINYINT(4) NOT NULL COMMENT '状态 0:未分配 1:已分配',
                                creator_id VARCHAR(128) comment '创建分片的实例ID',
                                instance_id VARCHAR(128) comment '当前持有分片的实例ID',
                                last_heartbeat DATETIME DEFAULT NULL COMMENT '最后心跳时间',
                                KEY `idx_instance_id` (`instance_id`),
                                KEY `idx_last_heartbeat` (`last_heartbeat`)
)ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='分片元数据表';

-- ============================================================
-- 表：retry_task
-- 描述：重试任务表，用于存储所有重试任务的信息
-- ============================================================
CREATE TABLE `retry_task` (
  `id` bigint NOT NULL PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
  `gmt_create` datetime NOT NULL COMMENT '创建时间',
  `gmt_modified` datetime NOT NULL COMMENT '修改时间',
  `sharding_key` bigint NOT NULL COMMENT '分片键',
  `task_desc` varchar(128) DEFAULT NULL COMMENT '任务描述',
  `task_code` varchar(128) DEFAULT NULL COMMENT '需要执行的任务编码',
  `parameters` text COMMENT '参数数据',
  `attribute` text COMMENT '属性',
  `status` tinyint NOT NULL COMMENT '最终执行状态 0:待执行,1:执行中,3:执行失败,2:执行成功',
  `interval_second` int DEFAULT NULL COMMENT '执行间隔秒,如果不填写默认是600秒(十分钟执行一次)',
  `delay_second` int DEFAULT NULL COMMENT '初次创建任务延迟时间，默认是100秒后执行',
  `max_execute_time` int DEFAULT NULL COMMENT '任务最大执行时间',
  `next_plan_time` datetime DEFAULT NULL COMMENT '下次执行时间',
  `retry_num` int DEFAULT NULL COMMENT '重试次数',
  `creator` varchar(64) DEFAULT NULL COMMENT '创建者(默认是IP)',
  `executor` varchar(64) DEFAULT NULL COMMENT '执行者',
  `origin_retry_num` int DEFAULT NULL COMMENT '存放任务原始的次数',
  `current_log_id` bigint DEFAULT NULL COMMENT '当前运行日志id',
  `unique_key` varchar(64) DEFAULT NULL COMMENT '唯一标识',
  `next_plan_time_strategy` int DEFAULT NULL COMMENT '下次计划时间策略（对应 NextPlanTimeStrategyEnum 枚举）',
  KEY `idx_next_plan_time` (`next_plan_time`),
  KEY `idx_status_sharding_key_next_plan_time_retry_num` (`status`,sharding_key,`next_plan_time`,`retry_num`),
  KEY `idx_gmt_create_sharding_key` (`gmt_create`,`sharding_key`),
  KEY `idx_unique_key` (`unique_key`)
) ENGINE=InnoDB AUTO_INCREMENT=1094 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='重试任务表';

-- ============================================================
-- 索引说明：
-- 1. retry_sharding.idx_instance_id: 实例ID索引，用于快速查找实例持有的分片
-- 2. retry_sharding.idx_last_heartbeat: 最后心跳时间索引，用于心跳检测
-- 3. retry_task.idx_next_plan_time: 下次执行时间索引，用于任务调度
-- 4. retry_task.idx_status_sharding_key_next_plan_time_retry_num:
--    状态-分片键-下次执行时间-重试次数联合索引，用于任务分片查询
-- 5. retry_task.idx_gmt_create_sharding_key: 创建时间-分片键索引，用于时间范围查询
-- 6. retry_task.idx_unique_key: 唯一标识索引，用于任务去重
-- ============================================================
-- 结束