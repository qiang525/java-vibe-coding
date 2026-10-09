#!/bin/bash
# =============================================================================
#  M2-3 分片上传 —— 一键启动并测试
# -----------------------------------------------------------------------------
#  用法：bash verify-chunk.sh
#
#  测试用的真实文件：E:\文件办公\PPT\文体活动.pptx（50MB）
#  如果你换了文件，改 test-chunk.py 里的 SRC_FILE 即可
# =============================================================================

unset http_proxy https_proxy HTTP_PROXY HTTPS_PROXY all_proxy ALL_PROXY
unset SERVER__PORT server__port SERVER_PORT server_port

BASE="D:/桌面/Java项目AIGC/backend"
MYSQL="E:/java/MySQL/MySQL Server 8.2/bin/mysql.exe"

echo "-------- ① 清理上次测试数据 --------"
export MYSQL_PWD='liuwenqiang123'
"$MYSQL" -uroot -D coursehub -e "
TRUNCATE TABLE resource;
TRUNCATE TABLE resource_version;
TRUNCATE TABLE file;
UPDATE course SET resource_count = 0;
" 2>/dev/null && echo "数据表已清空" || echo "清理失败（首次运行可忽略）"

echo "清理临时分片目录..."
rm -rf "$BASE/upload/temp/chunks"
rm -rf "$BASE/upload/2026"
echo "已清理"

echo ""
echo "-------- ② 清空 Redis 里的上传会话 --------"
cd "E:/java/redis" && (./redis-server.exe redis.conf > /dev/null 2>&1 &)
sleep 3
./redis-cli.exe --scan --pattern "coursehub:upload:chunk:*" 2>/dev/null | while read k; do
  [ -n "$k" ] && ./redis-cli.exe del "$k" > /dev/null 2>&1
done
echo "Redis 已就绪并清理完上传会话"

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
echo "-------- ④ 执行分片上传测试（50MB 真实文件）--------"
"E:/python/python.exe" "$BASE/test-chunk.py"
TEST_EXIT=$?

echo ""
echo "-------- 启动日志关键行 --------"
grep -E "Started CourseHubApplication|Tomcat started on port" "$BASE/app-run.log" | head -2

echo ""
echo "-------- 清理 --------"
taskkill //F //IM java.exe > /dev/null 2>&1 && echo "后端已停止"
cd "E:/java/redis" && ./redis-cli.exe shutdown nosave > /dev/null 2>&1
echo "分片上传验证结束 (exit=$TEST_EXIT)"
exit $TEST_EXIT
