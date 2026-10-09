#!/bin/bash
# =============================================================================
#  M2-1 文件模块 —— 一键启动并测试
# -----------------------------------------------------------------------------
#  用法：在本文件所在目录执行  bash verify-file.sh
#
#  它会依次做四件事：
#    1. 清空 file 表与 upload 目录（保证测试结果可复现）
#    2. 启动 Redis
#    3. 启动后端
#    4. 跑接口测试，结束后自动停掉后端与 Redis
# =============================================================================

unset http_proxy https_proxy HTTP_PROXY HTTPS_PROXY all_proxy ALL_PROXY
unset SERVER__PORT server__port SERVER_PORT server_port

BASE="D:/桌面/Java项目AIGC/backend"
MYSQL="E:/java/MySQL/MySQL Server 8.2/bin/mysql.exe"

echo "-------- ① 清理上次测试数据 --------"
export MYSQL_PWD='liuwenqiang123'
"$MYSQL" -uroot -D coursehub -e "TRUNCATE TABLE file;" 2>/dev/null && echo "file 表已清空" || echo "file 表清空失败（可忽略首次运行）"
rm -rf "$BASE/upload/2026" && echo "upload 目录已清理"

echo ""
echo "-------- ② 启动 Redis --------"
cd "E:/java/redis" && (./redis-server.exe redis.conf > /dev/null 2>&1 &)
sleep 3
netstat -ano 2>/dev/null | grep LISTENING | grep ":6379 " >/dev/null && echo "Redis 已就绪" || echo "Redis 未启动"

echo ""
echo "-------- ③ 启动后端 --------"
cd "$BASE" || exit 1
export JAVA_HOME="E:/java/jdk11"
taskkill //F //IM java.exe > /dev/null 2>&1
"E:/java/apache-maven-3.9.16/bin/mvn.cmd" spring-boot:run \
  -Dspring-boot.run.arguments=--server.port=8080 > "$BASE/app-run.log" 2>&1 &
echo "已后台启动，等待 30 秒..."
sleep 30

echo ""
echo "-------- ④ 执行接口测试 --------"
"E:/python/python.exe" "$BASE/test-file.py"
TEST_EXIT=$?

echo ""
echo "-------- 启动日志关键行 --------"
grep -E "Started CourseHubApplication|Tomcat started on port|本地文件存储初始化" "$BASE/app-run.log" | head -3

echo ""
echo "-------- 清理 --------"
taskkill //F //IM java.exe > /dev/null 2>&1 && echo "后端已停止"
cd "E:/java/redis" && ./redis-cli.exe shutdown nosave > /dev/null 2>&1
echo "文件模块验证结束 (exit=$TEST_EXIT)"
exit $TEST_EXIT
