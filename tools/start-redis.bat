@echo off
title CourseHub - Redis (6379)
cd /d E://java//redis
echo ============================================================
echo   启动 Redis（项目要用它做缓存和断点续传）
echo ------------------------------------------------------------
echo   启动成功的样子：
echo     窗口刷出一堆日志，最后停在
echo         Ready to accept connections
echo   然后光标不动了 —— 这是正常的，它是个服务程序。
echo
echo   这个黑色窗口千万不要关！
echo   关掉窗口 = Redis 停了 = 网站会报错。
echo ============================================================
echo.
redis-server.exe redis.conf
echo.
echo [Redis 已退出]
echo 如果上面出现 error 或闪退，把报错截图发给 AI。
pause
