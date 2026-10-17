@echo off
title CourseHub - Backend (http://localhost:8080)
cd /d %~dp0..\backend

echo ============================================================
echo   CourseHub 后端启动器
echo ------------------------------------------------------------
echo   启动顺序：MySQL(常驻) 然后 Redis 然后 后端
echo   本脚本会自动检查 Redis，没启动会提醒你。
echo ============================================================
echo.

set JAVA_HOME=E://java//jdk11
E://java//redis//redis-cli.exe ping >nul 2>&1
if errorlevel 1 goto NOCACHE
echo [OK] Redis 在线
goto RUN

:NOCACHE
echo [警告] Redis 没有响应！
echo        请先双击 tools\start-redis.bat，看到
echo        Ready to accept connections 再回来。
echo.
echo        按任意键仍然尝试启动（数据库能连、Redis 不能连会报错）
pause >nul

:RUN
echo.
echo 正在启动后端...
echo 第一次启动会下载 Maven 依赖，可能需要几分钟，请耐心等。
echo 成功标志：日志最后出现
echo     Started CourseHubApplication in x.xxx seconds
echo 然后浏览器打开：http://localhost:8080
echo.
call mvn spring-boot:run
echo.
echo [后端已退出]
echo 如果是正常关闭（Ctrl+C），忽略这行。
pause
