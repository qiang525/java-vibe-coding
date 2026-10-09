#!/bin/bash
# M1 课程模块启动 + 测试
unset http_proxy https_proxy HTTP_PROXY HTTPS_PROXY all_proxy ALL_PROXY
unset SERVER__PORT server__port SERVER_PORT server_port

BASE="D:/桌面/Java项目AIGC/backend"
OUT="$BASE/course-verify.log"

exec > "$OUT" 2>&1

cd "$BASE" || exit 1
export JAVA_HOME="E:/java/jdk11"

echo "-------- 启动 Redis --------"
cd "E:/java/redis" && (./redis-server.exe redis.conf > /dev/null 2>&1 &)
sleep 4
cd "$BASE"
netstat -ano 2>/dev/null | grep LISTENING | grep ":6379 " >/dev/null && echo "Redis OK" || echo "Redis 未启动"

echo ""
echo "-------- 启动后端 --------"
"E:/java/apache-maven-3.9.16/bin/mvn.cmd" spring-boot:run \
  -Dspring-boot.run.arguments=--server.port=8080 > "$BASE/app-run.log" 2>&1 &
echo "已后台启动"

sleep 25

echo ""
echo "-------- 执行接口测试 --------"
"E:/python/python.exe" "$BASE/test-course.py"
TEST_EXIT=$?

echo ""
echo "-------- 启动日志关键行 --------"
grep -E "Started CourseHubApplication|Tomcat started on port" "$BASE/app-run.log" | head -3

echo ""
echo "-------- 清理 --------"
taskkill //F //IM java.exe > /dev/null 2>&1 && echo "已停止后端"
cd "E:/java/redis" && ./redis-cli.exe shutdown nosave > /dev/null 2>&1
echo "课程验证结束 (exit=$TEST_EXIT)"
