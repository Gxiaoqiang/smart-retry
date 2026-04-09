#!/bin/bash

# Smart Retry 前端打包脚本
# 将Vue前端项目打包并输出到 resources/static 目录

echo "========================================"
echo "  Smart Retry 前端打包脚本"
echo "========================================"
echo ""

# 获取脚本所在目录
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
UI_DIR="$SCRIPT_DIR/src/main/resources/smart-retry-ui"
STATIC_DIR="$SCRIPT_DIR/src/main/resources/static"

# 检查前端目录是否存在
if [ ! -d "$UI_DIR" ]; then
    echo "错误: 前端目录不存在: $UI_DIR"
    exit 1
fi

# 进入前端目录
cd "$UI_DIR"

echo "前端项目目录: $UI_DIR"
echo "输出目录: $STATIC_DIR"
echo ""

# 检查 Node.js 是否安装
if ! command -v node &> /dev/null; then
    echo "错误: 未检测到 Node.js，请先安装 Node.js"
    echo "下载地址: https://nodejs.org/"
    exit 1
fi

echo "Node.js 版本:"
node -v
echo ""

# 检查 npm 是否安装
if ! command -v npm &> /dev/null; then
    echo "错误: 未检测到 npm"
    exit 1
fi

echo "npm 版本:"
npm -v
echo ""

# 检查 node_modules 是否存在，不存在则安装依赖
if [ ! -d "node_modules" ]; then
    echo "正在安装依赖..."
    npm install
    if [ $? -ne 0 ]; then
        echo "错误: 依赖安装失败"
        exit 1
    fi
    echo "依赖安装成功！"
    echo ""
else
    echo "检测到已安装的依赖"
    echo ""
fi

# 清理旧的打包文件
if [ -d "$STATIC_DIR" ]; then
    echo "清理旧的打包文件..."
    rm -rf "$STATIC_DIR"/*
    echo "清理完成"
    echo ""
fi

# 执行打包
echo "开始打包前端项目..."
echo ""
npm run build

if [ $? -ne 0 ]; then
    echo ""
    echo "错误: 前端打包失败"
    exit 1
fi

echo ""
echo "========================================"
echo "  打包成功！"
echo "========================================"
echo ""
echo "打包文件位置: $STATIC_DIR"
echo ""

# 显示打包结果
if [ -d "$STATIC_DIR" ]; then
    echo "打包文件列表:"
    ls -lh "$STATIC_DIR"
    echo ""
    
    # 统计文件大小
    TOTAL_SIZE=$(du -sh "$STATIC_DIR" | cut -f1)
    echo "总大小: $TOTAL_SIZE"
    echo ""
fi

echo "下一步操作:"
echo "1. 启动后端服务: ./run.sh"
echo "2. 访问地址: http://localhost:8080"
echo ""
