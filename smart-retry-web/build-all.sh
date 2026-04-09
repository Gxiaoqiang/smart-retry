#!/bin/bash

# Smart Retry 完整构建脚本
# 依次执行：前端打包 -> 后端编译打包

echo "========================================"
echo "  Smart Retry 完整构建脚本"
echo "========================================"
echo ""

# 获取脚本所在目录
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

# 步骤1: 打包前端
echo "【步骤 1/2】打包前端项目..."
echo "========================================"
./build-ui.sh

if [ $? -ne 0 ]; then
    echo ""
    echo "错误: 前端打包失败，终止构建"
    exit 1
fi

echo ""
echo "========================================"
echo "【步骤 2/2】打包后端项目..."
echo "========================================"
echo ""

# 设置JAVA_HOME
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home
export PATH=$JAVA_HOME/bin:$PATH

echo "使用 Java 版本:"
java -version
echo ""

# Maven打包
echo "开始Maven打包..."
mvn clean package -DskipTests

if [ $? -ne 0 ]; then
    echo ""
    echo "错误: Maven打包失败"
    exit 1
fi

echo ""
echo "========================================"
echo "  构建完成！"
echo "========================================"
echo ""
echo "构建产物:"
echo "  JAR文件: target/smart-retry-web-1.0.1.jar"
echo ""
echo "启动应用:"
echo "  java -jar target/smart-retry-web-1.0.1.jar"
echo ""
echo "或使用启动脚本:"
echo "  ./run.sh"
echo ""
echo "访问地址:"
echo "  http://localhost:8080"
echo ""
