# Smart Retry 管理系统

## 项目简介

Smart Retry 管理系统是一个基于 Spring Boot + Vue3 的重试任务管理平台，提供实例监控、任务管理等核心功能。

## 技术栈

### 后端
- Spring Boot 3.2.0
- MyBatis
- MySQL / PostgreSQL / Oracle
- Maven

### 前端
- Vue 3
- Element Plus
- ECharts
- Vite

## 功能模块

### 1. 整体分布（Dashboard）
- 活跃实例数量监控
- 分片分布情况
- 实例心跳延迟监控
- 任务状态分布（饼图）
- 各任务类型积压量
- 死信任务趋势（24小时）
- 任务处理速率
- 任务平均耗时

### 2. 实例管理
- 实例列表查询（支持分页）
- 按创建者ID、实例ID模糊查询
- 编辑实例（仅可修改instanceId，格式必须为ip:port）
- 删除实例（需检查是否有相关待执行或执行中的任务）

### 3. 任务管理
- 任务列表查询（支持分页）
- 按任务编码、任务描述、状态模糊查询
- 创建任务（必填：taskCode, taskDesc, retryNum, delaySecond, intervalSecond, param, shardingKey）
- 编辑任务（仅可编辑待执行和失败的任务，只能修改nextPlanTime, retryNum, param）
- 删除任务（执行中的任务不能删除）
- 批量删除任务
- 参数JSON格式校验

## 快速开始

### 环境要求
- JDK 17+
- Node.js 16+
- Maven 3.6+
- MySQL 5.7+ / PostgreSQL 12+ / Oracle 11g+

### 数据库初始化

执行对应的SQL脚本初始化数据库：
- MySQL: `doc/smart_retry_mysql.sql`
- PostgreSQL: `doc/smart_retry_pg.sql`
- Oracle: `doc/smart_retry_oracle.sql`

### 后端启动

1. 修改配置文件 `smart-retry-web/src/main/resources/application.yml`
   ```yaml
   spring:
     datasource:
       url: jdbc:mysql://localhost:3306/smart_retry?useUnicode=true&characterEncoding=utf8
       username: your_username
       password: your_password
   ```

2. 编译打包
   ```bash
   cd smart-retry-web
   mvn clean package
   ```

3. 运行
   ```bash
   java -jar target/smart-retry-web-1.0.1.jar
   ```

   或在IDE中直接运行 `com.smart.retry.Main` 类

4. 访问 http://localhost:8080

### 前端开发模式

1. 安装依赖
   ```bash
   cd smart-retry-web/src/main/resources/smart-retry-ui
   npm install
   ```

2. 启动开发服务器
   ```bash
   npm run dev
   ```

3. 访问 http://localhost:3000

### 前端生产构建

1. 构建前端
   ```bash
   cd smart-retry-web/src/main/resources/smart-retry-ui
   npm run build
   ```

2. 构建后的文件会自动输出到 `smart-retry-web/src/main/resources/static` 目录

3. 重新启动后端应用即可访问

## API接口说明

### 仪表盘接口
- `GET /api/dashboard/data` - 获取仪表盘监控数据

### 实例管理接口
- `POST /api/instance/query` - 分页查询实例列表
- `PUT /api/instance/update` - 更新实例信息
- `DELETE /api/instance/delete/{id}` - 删除实例

### 任务管理接口
- `POST /api/task/query` - 分页查询任务列表
- `POST /api/task/create` - 创建任务
- `PUT /api/task/update` - 更新任务
- `DELETE /api/task/delete/{id}` - 删除任务
- `DELETE /api/task/batch-delete` - 批量删除任务
- `GET /api/task/sharding-options` - 获取分片选择列表

## 项目结构

```
smart-retry-web/
├── src/main/java/com/smart/retry/
│   ├── Main.java                          # 启动类
│   └── web/
│       ├── config/                        # 配置类
│       │   └── WebConfig.java
│       ├── controller/                    # 控制器层
│       │   ├── DashboardController.java
│       │   ├── InstanceController.java
│       │   └── TaskController.java
│       ├── service/                       # 服务层
│       │   ├── DashboardService.java
│       │   ├── InstanceService.java
│       │   └── TaskService.java
│       └── dto/                           # 数据传输对象
│           ├── Result.java
│           ├── PageRequest.java
│           ├── PageResult.java
│           ├── dashboard/                 # 仪表盘相关DTO
│           ├── instance/                  # 实例相关DTO
│           └── task/                      # 任务相关DTO
├── src/main/resources/
│   ├── application.yml                    # 应用配置
│   ├── static/                            # 前端静态资源（构建后）
│   └── smart-retry-ui/                    # 前端源码
│       ├── src/
│       │   ├── api/                       # API接口
│       │   ├── layout/                    # 布局组件
│       │   ├── router/                    # 路由配置
│       │   ├── utils/                     # 工具类
│       │   ├── views/                     # 页面组件
│       │   ├── App.vue
│       │   └── main.js
│       ├── index.html
│       ├── package.json
│       └── vite.config.js
└── pom.xml
```

## 注意事项

1. **数据库配置**：默认使用MySQL，如需切换到PostgreSQL或Oracle，请修改 `application.yml` 中的数据源配置和MyBatis mapper位置

2. **实例ID格式**：instanceId必须符合 `ip:port` 格式，例如：`192.168.1.100:8080`

3. **任务参数**：param字段必须是有效的JSON格式，前端会进行校验

4. **删除限制**：
   - 执行中的任务不能删除
   - 删除实例前会检查是否有相关的待执行或执行中的任务

5. **编辑限制**：
   - 执行中的任务不能编辑
   - 实例只能编辑instanceId字段
   - 任务只能编辑nextPlanTime、retryNum、param字段

## 监控指标说明

### 告警建议
- **任务积压增长过快**：某任务类型待执行数量每分钟增长 > 100，等级P1
- **死信任务产生**：5分钟内产生死信任务，等级P0（严重）
- **分片负载不均**：某实例持有分片数 > 平均分片数 * 2，等级P2

## 常见问题

### 1. 前端跨域问题
开发模式下，Vite已配置代理转发API请求到后端。生产模式下，前端打包后由后端提供，不存在跨域问题。

### 2. 数据库连接失败
检查 `application.yml` 中的数据库配置是否正确，确保数据库服务已启动。

### 3. MyBatis Mapper找不到
确保 `application.yml` 中的 `mybatis.mapper-locations` 配置正确指向对应的XML文件。

## 开发者

Smart Retry Team

## 许可证

MIT License
