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

CREATE TABLE `retry_task` (
  `id` bigint NOT NULL PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
  `gmt_create` datetime NOT NULL COMMENT '创建时间',
  `gmt_modified` datetime NOT NULL COMMENT '修改时间',
  `sharding_key` bigint NOT NULL COMMENT '分片键',
  `task_desc` varchar(128) DEFAULT NULL COMMENT '任务描述',
  `task_code` varchar(128) DEFAULT NULL COMMENT '需要执行的任务编码',
  `parameters` text COMMENT '参数数据',
  `attribute` text COMMENT '属性',
  `status` tinyint NOT NULL COMMENT '最终执行状态 0:待执行,1:执行中,-1:执行失败,2:执行成功',
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
  `next_plan_time_strategy` int DEFAULT NULL,
  KEY `idx_next_plan_time` (`next_plan_time`),
  KEY `idx_status_sharding_key_next_plan_time_retry_num` (`status`,sharding_key,`next_plan_time`,`retry_num`),
  KEY `idx_gmt_create_sharding_key` (`gmt_create`,`sharding_key`),
  KEY `idx_unique_key` (`unique_key`)
) ENGINE=InnoDB AUTO_INCREMENT=1094 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='重试任务表';
