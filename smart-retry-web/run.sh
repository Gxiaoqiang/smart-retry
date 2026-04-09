#!/bin/bash

# 设置JAVA_HOME
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home
export PATH=$JAVA_HOME/bin:$PATH

echo "========================================"
echo "  Smart Retry Web 启动脚本"
echo "========================================"
echo ""
echo "使用 Java 版本:"
java -version
echo ""

# 检查是否已经编译
if [ ! -d "target/classes" ]; then
    echo "正在编译项目..."
    mvn clean compile -DskipTests
    if [ $? -ne 0 ]; then
        echo "编译失败！"
        exit 1
    fi
    echo "编译成功！"
    echo ""
fi

echo "启动 Smart Retry Web..."
echo "访问地址: http://localhost:8080"
echo "按 Ctrl+C 停止服务"
echo ""

# 运行应用
mvn spring-boot:run
