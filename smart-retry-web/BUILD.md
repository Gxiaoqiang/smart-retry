# Smart Retry Web 构建说明

## 📦 构建脚本

本项目提供了便捷的构建脚本，支持前后端自动化打包。

### macOS / Linux

#### 1. 仅打包前端
```bash
./build-ui.sh
```

**功能**:
- 检查 Node.js 环境
- 自动安装依赖（如果需要）
- 清理旧的打包文件
- 执行 Vite 打包
- 输出到 `src/main/resources/static` 目录

#### 2. 完整构建（前端 + 后端）
```bash
./build-all.sh
```

**功能**:
- 执行前端打包
- 执行 Maven 编译打包
- 生成可执行 JAR 文件

#### 3. 启动应用
```bash
./run.sh
```

---

### Windows

#### 1. 仅打包前端
```cmd
build-ui.bat
```

#### 2. 完整构建（前端 + 后端）
```cmd
build-all.bat
```

#### 3. 启动应用
```cmd
run.bat
```

---

## 📁 项目结构

```
smart-retry-web/
├── src/
│   └── main/
│       ├── resources/
│       │   ├── smart-retry-ui/    # Vue 前端源码
│       │   │   ├── src/
│       │   │   ├── package.json
│       │   │   └── vite.config.js
│       │   └── static/            # 前端打包输出目录
│       │       ├── index.html
│       │       └── assets/
│       └── java/
│           └── com/smart/retry/
│               └── Main.java      # Spring Boot 启动类
├── build-ui.sh                    # 前端打包脚本 (Unix)
├── build-ui.bat                   # 前端打包脚本 (Windows)
├── build-all.sh                   # 完整构建脚本 (Unix)
├── build-all.bat                  # 完整构建脚本 (Windows)
├── run.sh                         # 启动脚本 (Unix)
└── pom.xml
```

---

## 🔧 环境要求

### 前端
- **Node.js**: 16+ (推荐 18+ 或 20+)
- **npm**: 8+ 

### 后端
- **JDK**: 17
- **Maven**: 3.6+

---

## 🚀 快速开始

### 方式一：使用完整构建脚本（推荐）

```bash
# macOS/Linux
./build-all.sh

# Windows
build-all.bat
```

构建完成后，运行：
```bash
# macOS/Linux
./run.sh

# Windows
run.bat
```

访问: http://localhost:8080

---

### 方式二：分步构建

#### 步骤 1: 打包前端
```bash
./build-ui.sh
```

#### 步骤 2: 打包后端
```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home
mvn clean package -DskipTests
```

#### 步骤 3: 运行应用
```bash
java -jar target/smart-retry-web-1.0.1.jar
```

---

### 方式三：开发模式

#### 前端开发服务器
```bash
cd src/main/resources/smart-retry-ui
npm run dev
```
访问: http://localhost:3000

#### 后端服务
```bash
./run.sh
```
访问: http://localhost:8080

---

## 📝 注意事项

### 1. JAVA_HOME 配置

确保设置了正确的 JAVA_HOME 环境变量：

**macOS/Linux**:
```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home
```

**Windows**:
```cmd
set JAVA_HOME=C:\Program Files\Java\jdk-17
```

### 2. 数据库配置

修改 `src/main/resources/application.yml` 中的数据库配置：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/smart_retry
    username: root
    password: your_password
```

### 3. 初始化数据库

执行 SQL 脚本创建表结构：
```bash
mysql -u root -p < doc/smart_retry_mysql.sql
```

### 4. 前端代理配置

开发模式下，前端通过 Vite 代理访问后端 API：

```javascript
// vite.config.js
server: {
  port: 3000,
  proxy: {
    '/api': {
      target: 'http://localhost:8080',
      changeOrigin: true
    }
  }
}
```

生产模式下，前端直接访问同域的后端 API。

---

## 🐛 常见问题

### 问题 1: Node.js 版本过低

**错误信息**: 
```
Error: Node.js version must be 16 or higher
```

**解决方案**:
升级 Node.js 到 16+ 版本，推荐使用 nvm 管理多版本：
```bash
# 安装 nvm
curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.0/install.sh | bash

# 安装并使用 Node.js 20
nvm install 20
nvm use 20
```

### 问题 2: Maven 编译失败

**错误信息**:
```
Fatal error compiling: java.lang.ExceptionInInitializerError
```

**解决方案**:
确保使用 Java 17：
```bash
java -version
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home
```

### 问题 3: 前端打包后页面空白

**原因**: 静态资源路径不正确

**解决方案**:
检查 `vite.config.js` 中的 `base` 配置，默认为 `/`，通常不需要修改。

### 问题 4: 后端启动失败 - 端口被占用

**错误信息**:
```
Web server failed to start. Port 8080 was already in use.
```

**解决方案**:
修改 `application.yml` 中的端口号：
```yaml
server:
  port: 8081
```

或者杀死占用端口的进程：
```bash
# macOS/Linux
lsof -ti:8080 | xargs kill -9

# Windows
netstat -ano | findstr :8080
taskkill /PID <PID> /F
```

---

## 📊 构建产物

### 前端打包结果
位置: `src/main/resources/static/`

```
static/
├── index.html
└── assets/
    ├── index-xxxxx.css
    ├── index-xxxxx.js
    ├── Dashboard-xxxxx.js
    ├── Instance-xxxxx.js
    └── Task-xxxxx.js
```

### 后端打包结果
位置: `target/`

```
target/
├── smart-retry-web-1.0.1.jar          # 可执行 JAR
├── smart-retry-web-1.0.1.jar.original # 原始 JAR（未重新打包）
└── classes/                            # 编译后的类文件
```

---

## 🎯 部署

### 单机部署

1. 构建项目
   ```bash
   ./build-all.sh
   ```

2. 上传 JAR 文件到服务器
   ```bash
   scp target/smart-retry-web-1.0.1.jar user@server:/opt/smart-retry/
   ```

3. 在服务器上运行
   ```bash
   java -jar /opt/smart-retry/smart-retry-web-1.0.1.jar
   ```

### Docker 部署（待实现）

可以创建 Dockerfile 进行容器化部署。

---

## 📞 技术支持

如有问题，请查看：
- 项目文档: `doc/README.md`
- 数据库脚本: `doc/smart_retry_*.sql`
