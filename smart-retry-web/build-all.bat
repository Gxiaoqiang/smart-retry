@echo off
chcp 65001 >nul

echo ========================================
echo   Smart Retry 完整构建脚本 (Windows)
echo ========================================
echo.

REM 获取脚本所在目录
set "SCRIPT_DIR=%~dp0"
cd /d "%SCRIPT_DIR%"

REM 步骤1: 打包前端
echo 【步骤 1/2】打包前端项目...
echo ========================================
call build-ui.bat

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo 错误: 前端打包失败，终止构建
    pause
    exit /b 1
)

echo.
echo ========================================
echo 【步骤 2/2】打包后端项目...
echo ========================================
echo.

REM 设置JAVA_HOME (如果未设置)
if not defined JAVA_HOME (
    echo 警告: 未设置 JAVA_HOME，尝试使用默认路径...
    set "JAVA_HOME=C:\Program Files\Java\jdk-17"
    set "PATH=%JAVA_HOME%\bin;%PATH%"
)

echo 使用 Java 版本:
java -version
echo.

REM Maven打包
echo 开始Maven打包...
call mvn clean package -DskipTests

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo 错误: Maven打包失败
    pause
    exit /b 1
)

echo.
echo ========================================
echo   构建完成！
echo ========================================
echo.
echo 构建产物:
echo   JAR文件: target\smart-retry-web-1.0.1.jar
echo.
echo 启动应用:
echo   java -jar target\smart-retry-web-1.0.1.jar
echo.
echo 或使用启动脚本:
echo   run.bat
echo.
echo 访问地址:
echo   http://localhost:8080
echo.
pause
