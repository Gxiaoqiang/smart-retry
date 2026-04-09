# Smart Retry 管理系统 - 5分钟快速开始

## 前置条件

确保已安装：
- ✅ JDK 17+
- ✅ Maven 3.6+
- ✅ MySQL 5.7+

## 步骤1: 初始化数据库（2分钟）

```bash
# 创建数据库
mysql -u root -p -e "CREATE DATABASE smart_retry DEFAULT CHARACTER SET utf8mb4;"

# 导入表结构
mysql -u root -p smart_retry < doc/smart_retry_mysql.sql
```

## 步骤2: 配置数据库连接（1分钟）

编辑文件：`smart-retry-web/src/main/resources/application.yml`

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/smart_retry?useUnicode=true&characterEncoding=utf8
    username: root        # 修改为你的用户名
    password: your_password  # 修改为你的密码
```

## 步骤3: 启动应用（2分钟）

### 方式一：使用启动脚本（推荐）

**Linux/Mac:**
```bash
cd smart-retry-web
./start.sh
```

**Windows:**
```cmd
cd smart-retry-web
start.bat
```

### 方式二：手动启动

```bash
cd smart-retry-web
mvn clean package -DskipTests
java -jar target/smart-retry-web-1.0.1.jar
```

## 步骤4: 访问系统

浏览器打开：**http://localhost:8080**

你将看到：
- 📊 左侧菜单：整体分布、实例管理、任务管理
- 🎯 默认页面：整体分布监控大屏

## 验证功能

### 1️⃣ 查看仪表盘
- 应该能看到统计卡片（活跃实例数、处理速率等）
- 应该能看到图表（即使数据为0）

### 2️⃣ 测试实例管理
- 点击"实例管理"菜单
- 应该能看到空列表或已有实例
- 点击"查询"按钮测试搜索功能

### 3️⃣ 测试任务管理
- 点击"任务管理"菜单
- 点击"新建任务"按钮
- 填写表单：
  ```
  任务编码: test_task
  任务描述: 测试任务
  重试次数: 3
  延迟时间: 100
  执行间隔: 600
  执行实例: （选择一个）
  参数: {"key": "value"}
  ```
- 点击"确定"，应该提示"创建成功"

## 常见问题

### ❌ 端口8080被占用

**解决方案**: 修改 `application.yml`
```yaml
server:
  port: 8081  # 改为其他端口
```

### ❌ 数据库连接失败

**检查清单**:
1. MySQL服务是否启动？
2. 数据库smart_retry是否创建？
3. 用户名密码是否正确？
4. 防火墙是否阻止？

**测试连接**:
```bash
mysql -u root -p smart_retry -e "SELECT 1"
```

### ❌ Maven构建失败

**解决方案**:
```bash
# 清理Maven缓存
mvn clean

# 重新下载依赖
mvn dependency:purge-local-repository

# 再次构建
mvn clean package -DskipTests
```

### ❌ Java版本不对

**检查版本**:
```bash
java -version  # 应该是17+
```

**设置Java 17**:
```bash
# Linux/Mac
export JAVA_HOME=/path/to/jdk17
export PATH=$JAVA_HOME/bin:$PATH

# Windows
set JAVA_HOME=C:\Program Files\Java\jdk-17
set PATH=%JAVA_HOME%\bin;%PATH%
```

## 下一步

✅ **开发模式**: 前端热更新开发
```bash
cd smart-retry-web/src/main/resources/smart-retry-ui
npm install
npm run dev
# 访问 http://localhost:3000
```

✅ **生产部署**: 查看详细部署文档
```bash
# 构建前端
./build-ui.sh

# 打包后端
mvn clean package -DskipTests

# 运行
java -jar target/smart-retry-web-1.0.1.jar
```

✅ **阅读文档**: 
- [README.md](README.md) - 完整功能说明
- [DEPLOYMENT.md](DEPLOYMENT.md) - 详细部署指南
- [FEATURES.md](FEATURES.md) - 功能清单

## 需要帮助？

- 📖 查看 [PROJECT_SUMMARY.md](PROJECT_SUMMARY.md)
- 🔍 检查日志输出
- 💬 联系技术支持

---

**恭喜！** 🎉 你已经成功启动 Smart Retry 管理系统！
