#!/bin/bash
# =============================================================================
#  M2-2 资料模块 —— 一键启动并测试
# -----------------------------------------------------------------------------
#  用法：bash verify-resource.sh
#
#  重点验证三级可见性：PUBLIC / COURSE_ONLY / PRIVATE
# =============================================================================

unset http_proxy https_proxy HTTP_PROXY HTTPS_PROXY all_proxy ALL_PROXY
unset SERVER__PORT server__port SERVER_PORT server_port

BASE="D:/桌面/Java项目AIGC/backend"
MYSQL="E:/java/MySQL/MySQL Server 8.2/bin/mysql.exe"

echo "-------- ① 清理上次测试数据 --------"
# 数据库口令不写进文件：请先在命令行执行 export MYSQL_PWD=你的MySQL密码
: "${MYSQL_PWD:?请先执行 export MYSQL_PWD=你的MySQL密码 再运行本脚本}"
export MYSQL_PWD
"$MYSQL" -uroot -D coursehub -e "
TRUNCATE TABLE resource;
TRUNCATE TABLE resource_version;
TRUNCATE TABLE file;
UPDATE course SET resource_count = 0;
" 2>/dev/null && echo "resource / resource_version / file 表已清空" || echo "清理失败（首次运行可忽略）"
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
"E:/python/python.exe" "$BASE/test-resource.py"
TEST_EXIT=$?

echo ""
echo "-------- 启动日志关键行 --------"
grep -E "Started CourseHubApplication|Tomcat started on port" "$BASE/app-run.log" | head -2

echo ""
echo "-------- 清理 --------"
taskkill //F //IM java.exe > /dev/null 2>&1 && echo "后端已停止"
cd "E:/java/redis" && ./redis-cli.exe shutdown nosave > /dev/null 2>&1
echo "资料模块验证结束 (exit=$TEST_EXIT)"
exit $TEST_EXIT
