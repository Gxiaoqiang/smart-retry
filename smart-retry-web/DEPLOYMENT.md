# Smart Retry Web 部署指南

## 一、环境准备

### 1.1 服务器要求
- CPU: 2核及以上
- 内存: 4GB及以上
- 磁盘: 10GB及以上可用空间
- 操作系统: Linux (CentOS 7+/Ubuntu 18.04+) / Windows Server 2016+

### 1.2 软件要求
- JDK 17+
- Maven 3.6+
- Node.js 16+ (仅开发环境需要)
- MySQL 5.7+ / PostgreSQL 12+ / Oracle 11g+

## 二、数据库部署

### 2.1 创建数据库

#### MySQL
```sql
CREATE DATABASE smart_retry DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE smart_retry;
```

#### PostgreSQL
```sql
CREATE DATABASE smart_retry WITH ENCODING='UTF8';
```

#### Oracle
```sql
CREATE USER smart_retry IDENTIFIED BY your_password;
GRANT CONNECT, RESOURCE TO smart_retry;
```

### 2.2 执行建表脚本

根据使用的数据库类型，执行对应的SQL脚本：

```bash
# MySQL
mysql -u root -p smart_retry < doc/smart_retry_mysql.sql

# PostgreSQL
psql -U postgres -d smart_retry -f doc/smart_retry_pg.sql

# Oracle
sqlplus smart_retry/your_password @doc/smart_retry_oracle.sql
```

## 三、后端部署

### 3.1 方式一：使用启动脚本（推荐）

#### Linux/Mac
```bash
cd smart-retry-web
./start.sh
```

#### Windows
双击运行 `start.bat` 或在命令行执行：
```cmd
cd smart-retry-web
start.bat
```

### 3.2 方式二：手动部署

#### 步骤1: 修改配置文件

编辑 `smart-retry-web/src/main/resources/application.yml`：

```yaml
server:
  port: 8080  # 修改为实际端口

spring:
  datasource:
    url: jdbc:mysql://your-db-host:3306/smart_retry?useUnicode=true&characterEncoding=utf8
    username: your_username
    password: your_password
```

#### 步骤2: 编译打包

```bash
cd smart-retry-web
mvn clean package -DskipTests
```

#### 步骤3: 运行应用

```bash
java -jar target/smart-retry-web-1.0.1.jar
```

### 3.3 方式三：后台运行（生产环境）

#### 使用nohup
```bash
nohup java -jar target/smart-retry-web-1.0.1.jar > app.log 2>&1 &
```

#### 使用systemd（推荐）

创建服务文件 `/etc/systemd/system/smart-retry-web.service`：

```ini
[Unit]
Description=Smart Retry Web Application
After=syslog.target network.target

[Service]
Type=simple
User=smartretry
Group=smartretry
WorkingDirectory=/opt/smart-retry-web
ExecStart=/usr/bin/java -jar /opt/smart-retry-web/smart-retry-web-1.0.1.jar
SuccessExitStatus=143
TimeoutStopSec=10
Restart=on-failure
RestartSec=5

[Install]
WantedBy=multi-user.target
```

启动服务：
```bash
sudo systemctl daemon-reload
sudo systemctl enable smart-retry-web
sudo systemctl start smart-retry-web
sudo systemctl status smart-retry-web
```

## 四、前端部署

### 4.1 开发模式

```bash
cd smart-retry-web/src/main/resources/smart-retry-ui
npm install
npm run dev
```

访问 http://localhost:3000

### 4.2 生产模式

#### 步骤1: 构建前端

```bash
cd smart-retry-web
./build-ui.sh
```

或手动构建：
```bash
cd smart-retry-web/src/main/resources/smart-retry-ui
npm install
npm run build
```

构建后的文件会输出到 `smart-retry-web/src/main/resources/static` 目录。

#### 步骤2: 重新打包后端

```bash
cd smart-retry-web
mvn clean package -DskipTests
```

#### 步骤3: 启动应用

前端资源会随后端一起启动，访问 http://your-server-ip:8080

## 五、Nginx反向代理配置（可选）

如果需要使用域名访问或HTTPS，可以配置Nginx反向代理：

```nginx
server {
    listen 80;
    server_name retry.yourdomain.com;

    # 如果需要HTTPS，取消注释
    # return 301 https://$server_name$request_uri;

    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}

# HTTPS配置（可选）
# server {
#     listen 443 ssl http2;
#     server_name retry.yourdomain.com;
#     
#     ssl_certificate /path/to/cert.pem;
#     ssl_certificate_key /path/to/key.pem;
#     
#     location / {
#         proxy_pass http://localhost:8080;
#         proxy_set_header Host $host;
#         proxy_set_header X-Real-IP $remote_addr;
#         proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
#         proxy_set_header X-Forwarded-Proto $scheme;
#     }
# }
```

## 六、Docker部署（可选）

### 6.1 创建Dockerfile

在 `smart-retry-web` 目录创建 `Dockerfile`：

```dockerfile
FROM openjdk:17-slim

WORKDIR /app

COPY target/smart-retry-web-1.0.1.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
```

### 6.2 构建镜像

```bash
cd smart-retry-web
mvn clean package -DskipTests
docker build -t smart-retry-web:1.0.1 .
```

### 6.3 运行容器

```bash
docker run -d \
  --name smart-retry-web \
  -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:mysql://host.docker.internal:3306/smart_retry \
  -e SPRING_DATASOURCE_USERNAME=root \
  -e SPRING_DATASOURCE_PASSWORD=root \
  smart-retry-web:1.0.1
```

或使用 `docker-compose.yml`：

```yaml
version: '3.8'

services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_DATABASE: smart_retry
    volumes:
      - mysql-data:/var/lib/mysql
      - ./doc/smart_retry_mysql.sql:/docker-entrypoint-initdb.d/init.sql
    ports:
      - "3306:3306"

  smart-retry-web:
    build: .
    ports:
      - "8080:8080"
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/smart_retry
      SPRING_DATASOURCE_USERNAME: root
      SPRING_DATASOURCE_PASSWORD: root
    depends_on:
      - mysql
    restart: always

volumes:
  mysql-data:
```

启动：
```bash
docker-compose up -d
```

## 七、监控与维护

### 7.1 日志查看

```bash
# 查看实时日志
tail -f app.log

# 如果使用systemd
journalctl -u smart-retry-web -f
```

### 7.2 健康检查

访问以下接口检查应用状态：
```bash
curl http://localhost:8080/api/dashboard/data
```

### 7.3 性能调优

根据需要调整JVM参数：

```bash
java -jar \
  -Xms2g \
  -Xmx2g \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  smart-retry-web-1.0.1.jar
```

## 八、常见问题

### 8.1 端口被占用

修改 `application.yml` 中的 `server.port` 配置。

### 8.2 数据库连接失败

1. 检查数据库服务是否启动
2. 检查网络连接
3. 检查用户名密码是否正确
4. 检查防火墙设置

### 8.3 内存不足

增加JVM堆内存：
```bash
java -Xms4g -Xmx4g -jar smart-retry-web-1.0.1.jar
```

### 8.4 前端资源404

确保已执行前端构建并将结果放到 `static` 目录。

## 九、备份与恢复

### 9.1 数据库备份

```bash
# MySQL
mysqldump -u root -p smart_retry > backup_$(date +%Y%m%d).sql

# PostgreSQL
pg_dump -U postgres smart_retry > backup_$(date +%Y%m%d).sql
```

### 9.2 数据库恢复

```bash
# MySQL
mysql -u root -p smart_retry < backup_20240101.sql

# PostgreSQL
psql -U postgres smart_retry < backup_20240101.sql
```

## 十、安全建议

1. **修改默认密码**：修改数据库默认密码
2. **启用HTTPS**：生产环境务必启用HTTPS
3. **防火墙配置**：只开放必要的端口
4. **定期更新**：及时更新依赖包和系统补丁
5. **访问控制**：考虑添加登录认证功能
6. **数据加密**：敏感数据加密存储

## 联系支持

如有问题，请联系技术支持团队。
