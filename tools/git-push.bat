@echo off
title CourseHub - Git 一键提交并推送
cd /d %~dp0..

echo ============================================================
echo   CourseHub 一键提交并推送
echo ------------------------------------------------------------
echo   它会依次做三件事：
echo     1. 列出你改了哪些文件
echo     2. 让你输入这次改动的说明
echo     3. 自动执行 add 然后 commit 然后 push
echo.
echo   注意：它会提交全部改动。不确定就先关掉本窗口，
echo         手动执行 git status 看清楚再来。
echo ============================================================
echo.

git -c core.quotepath=false status --short
echo.
echo ------------------------------------------------------------

set /p MSG=请输入这次改动的说明（例如  docs: 补充 Git 教程）: 
if "%MSG%"=="" goto NOMSG

git add -A
git commit -m "%MSG%"
if errorlevel 1 goto COMMITFAIL

git push
if errorlevel 1 goto PUSHFAIL

echo.
echo [成功] 已经推送到 GitHub。
echo        打开 https://github.com/qiang525/java-vibe-coding 看看
pause
exit /b 0

:NOMSG
echo.
echo [取消] 你没有输入说明，已退出，什么都没改。
pause
exit /b 1

:COMMITFAIL
echo.
echo [失败] 提交失败。常见原因：
echo        1. 没有任何改动（工作区是干净的）
echo        2. 用户名邮箱没配（见第 8 册第 4 节）
echo        把上面的报错截图发给 AI。
pause
exit /b 1

:PUSHFAIL
echo.
echo [失败] 推送失败。最常见的原因是远程有你没有的提交。
echo        解决办法：先执行 git pull，再重新双击本脚本。
echo        如果 git pull 报冲突，看第 9 册第 6 节。
pause
exit /b 1
