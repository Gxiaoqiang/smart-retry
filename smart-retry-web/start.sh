#!/bin/bash

echo "========================================="
echo "  Smart Retry Web - 快速启动脚本"
echo "========================================="

# 检查Java版本
echo ""
echo "检查Java环境..."
java -version
if [ $? -ne 0 ]; then
    echo "错误: 未找到Java环境，请先安装JDK 17+"
    exit 1
fi

# 进入项目目录
cd "$(dirname "$0")"

# Maven打包
echo ""
echo "开始Maven打包..."
mvn clean package -DskipTests
if [ $? -ne 0 ]; then
    echo "错误: Maven打包失败"
    exit 1
fi

# 查找jar文件
JAR_FILE=$(find target -name "smart-retry-web-*.jar" -type f | head -n 1)
if [ -z "$JAR_FILE" ]; then
    echo "错误: 未找到生成的jar文件"
    exit 1
fi

echo ""
echo "========================================="
echo "  启动应用..."
echo "========================================="
echo "JAR文件: $JAR_FILE"
echo "访问地址: http://localhost:8080"
echo "按 Ctrl+C 停止应用"
echo "========================================="
echo ""

# 启动应用
java -jar "$JAR_FILE"
