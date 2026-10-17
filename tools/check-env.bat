@echo off
title CourseHub - 环境自检
echo ============================================================
echo   CourseHub 环境自检（把结果截图发给 AI 也行）
echo ============================================================
echo.
echo [1/5] Java 版本（需要 11）
java -version 2>&1
echo.
echo [2/5] Maven 版本（需要 3.6+）
call mvn -v 2>&1
echo.
echo [3/5] Node 与 npm（前端需要）
node -v 2>&1
call npm -v 2>&1
echo.
echo [4/5] MySQL 端口 3306 是否在监听
netstat -ano | findstr ":3306" | findstr "LISTENING"
if errorlevel 1 goto MYSQLDOWN
echo   [OK] MySQL 正在运行
goto REDISCHK

:MYSQLDOWN
echo   [问题] 3306 没在监听，MySQL 没启动

:REDISCHK
echo.
echo [5/5] Redis 端口 6379 是否在监听
E://java//redis//redis-cli.exe ping 2>&1
echo.
echo ============================================================
echo   以上信息发给 AI，我可以帮你判断哪里有问题
echo ============================================================
pause
