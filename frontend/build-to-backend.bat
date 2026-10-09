@echo off
cd /d %~dp0

echo ==========================================================
echo   CourseHub - 把前端打包进后端
echo ----------------------------------------------------------
echo   做完之后：只启动后端，浏览器打开 http://localhost:8080
echo   就能看到完整网页（不用再单独启动前端）
echo ==========================================================
echo.

echo [1/3] 正在构建前端（npm run build）...
call npm run build
if errorlevel 1 goto FAIL

echo.
echo [2/3] 正在复制到后端静态目录...
if exist "..\backend\src\main\resources\static" rmdir /s /q "..\backend\src\main\resources\static"
mkdir "..\backend\src\main\resources\static"
xcopy /e /i /y "dist" "..\backend\src\main\resources\static" >nul
if errorlevel 1 goto FAIL

echo.
echo [3/3] 完成！
echo.
echo   接下来：
echo     1. 在 IDEA 里启动 CourseHubApplication（或用 mvn spring-boot:run）
echo     2. 浏览器打开 http://localhost:8080
echo.
pause
exit /b 0

:FAIL
echo.
echo 构建失败，请查看上面的错误信息。
pause
exit /b 1
