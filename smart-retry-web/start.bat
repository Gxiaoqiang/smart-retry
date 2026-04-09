@echo off
echo =========================================
echo   Smart Retry Web - 快速启动脚本
echo =========================================

REM 检查Java环境
echo.
echo 检查Java环境...
java -version
if errorlevel 1 (
    echo 错误: 未找到Java环境，请先安装JDK 17+
    pause
    exit /b 1
)

REM Maven打包
echo.
echo 开始Maven打包...
call mvn clean package -DskipTests
if errorlevel 1 (
    echo 错误: Maven打包失败
    pause
    exit /b 1
)

REM 查找jar文件
for /f "delims=" %%i in ('dir /b target\smart-retry-web-*.jar 2^>nul') do (
    set JAR_FILE=target\%%i
    goto :found
)

:found
if "%JAR_FILE%"=="" (
    echo 错误: 未找到生成的jar文件
    pause
    exit /b 1
)

echo.
echo =========================================
echo   启动应用...
echo =========================================
echo JAR文件: %JAR_FILE%
echo 访问地址: http://localhost:8080
echo 按 Ctrl+C 停止应用
echo =========================================
echo.

REM 启动应用
java -jar "%JAR_FILE%"
pause
