@echo off
chcp 65001 >nul
setlocal EnableDelayedExpansion

echo ========================================
echo   Smart Retry 前端打包脚本 (Windows)
echo ========================================
echo.

REM 获取脚本所在目录
set "SCRIPT_DIR=%~dp0"
set "UI_DIR=%SCRIPT_DIR%src\main\resources\smart-retry-ui"
set "STATIC_DIR=%SCRIPT_DIR%src\main\resources\static"

echo 前端项目目录: %UI_DIR%
echo 输出目录: %STATIC_DIR%
echo.

REM 检查 Node.js 是否安装
where node >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo 错误: 未检测到 Node.js，请先安装 Node.js
    echo 下载地址: https://nodejs.org/
    pause
    exit /b 1
)

echo Node.js 版本:
node -v
echo.

REM 检查 npm 是否安装
where npm >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo 错误: 未检测到 npm
    pause
    exit /b 1
)

echo npm 版本:
npm -v
echo.

REM 进入前端目录
cd /d "%UI_DIR%"

REM 检查 node_modules 是否存在，不存在则安装依赖
if not exist "node_modules\" (
    echo 正在安装依赖...
    call npm install
    if %ERRORLEVEL% NEQ 0 (
        echo 错误: 依赖安装失败
        pause
        exit /b 1
    )
    echo 依赖安装成功！
    echo.
) else (
    echo 检测到已安装的依赖
    echo.
)

REM 清理旧的打包文件
if exist "%STATIC_DIR%" (
    echo 清理旧的打包文件...
    rmdir /s /q "%STATIC_DIR%"
    echo 清理完成
    echo.
)

REM 执行打包
echo 开始打包前端项目...
echo.
call npm run build

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo 错误: 前端打包失败
    pause
    exit /b 1
)

echo.
echo ========================================
echo   打包成功！
echo ========================================
echo.
echo 打包文件位置: %STATIC_DIR%
echo.

REM 显示打包结果
if exist "%STATIC_DIR%" (
    echo 打包文件列表:
    dir "%STATIC_DIR%" /b
    echo.
)

echo 下一步操作:
echo 1. 启动后端服务: run.bat
echo 2. 访问地址: http://localhost:8080
echo.
pause
