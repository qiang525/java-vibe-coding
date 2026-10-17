@echo off
title CourseHub - 数据库备份
echo ============================================================
echo   把 coursehub 数据库备份成一个 .sql 文件
echo   存放位置：F://coursehub_backup//
echo ------------------------------------------------------------
echo   接下来会提示输入 MySQL 密码，输入时屏幕不显示，这是正常的安全设计。
echo   每次备份会覆盖上一次的同名文件。
echo ============================================================
echo.

if not exist "F://coursehub_backup" mkdir "F://coursehub_backup"

set OUTFILE=F://coursehub_backup//coursehub_backup.sql
echo 目标文件：%OUTFILE%
echo.

"E://java//MySQL//MySQL Server 8.2//bin//mysqldump.exe" -uroot -p --default-character-set=utf8mb4 --single-transaction --routines --triggers coursehub > "%OUTFILE%"
if errorlevel 1 goto FAIL

echo.
echo [成功] 备份已生成：
dir "%OUTFILE%"
pause
exit /b 0

:FAIL
echo.
echo [失败] 没有生成备份文件。
echo        常见原因：密码输错、MySQL 没启动、F 盘不可写。
del "%OUTFILE%" 2>nul
pause
exit /b 1
