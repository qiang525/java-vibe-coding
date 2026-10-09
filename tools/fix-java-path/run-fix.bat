@echo off
cd /d "%~dp0"
title CourseHub - Java PATH Fix

net session >nul 2>&1
if %errorlevel%==0 goto RUN

echo 需要管理员权限，正在请求提升，请在弹出的窗口中点"是"...
start "" /wait powershell -NoProfile -Command "Start-Process -FilePath '%~f0' -Verb RunAs"
exit /b

:RUN
set PY=
if exist "E:\python\python.exe" set PY=E:\python\python.exe
if "%PY%"=="" set PY=python

"%PY%" "%~dp0fix_java_path.py"

echo.
echo 按任意键关闭本窗口...
pause >nul
