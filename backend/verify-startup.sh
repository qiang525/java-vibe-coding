#!/bin/bash
# M0 骨架验收脚本
# 结果写入项目内 m0-verify.log（用 Windows 路径，便于读取）

unset http_proxy https_proxy HTTP_PROXY HTTPS_PROXY all_proxy ALL_PROXY
unset SERVER__PORT server__port SERVER_PORT server_port

BASE="D:/桌面/Java项目AIGC/backend"
OUT="$BASE/m0-verify.log"
APP="$BASE/app-run.log"
PY="E:/python/python.exe"
PROBE="$PY $BASE/http_probe.py"

exec > "$OUT" 2>&1

cd "$BASE" || exit 1
export JAVA_HOME="E:/java/jdk11"

echo "======== M0 骨架验收 ========"
echo "时间: $(date '+%Y-%m-%d %H:%M:%S')"
echo ""

echo "-------- 1. 启动 Redis --------"
cd "E:/java/redis" && (./redis-server.exe redis.conf > /dev/null 2>&1 &)
sleep 4
cd "$BASE"
netstat -ano 2>/dev/null | grep LISTENING | grep ":6379 " >/dev/null \
  && echo "✅ Redis 6379 已启动" || echo "⚠️ Redis 未启动"

echo ""
echo "-------- 2. 启动 Spring Boot --------"
"E:/java/apache-maven-3.9.16/bin/mvn.cmd" spring-boot:run \
  -Dspring-boot.run.arguments=--server.port=8080 > "$APP" 2>&1 &
echo "已后台启动，轮询中..."

echo ""
echo "-------- 3. 就绪检测 --------"
OK=0
for i in $(seq 1 45); do
  sleep 3
  R=$($PROBE http://localhost:8080/api/health 2>/dev/null)
  case "$R" in
    *'"code"'*) echo "✅ 就绪，耗时约 $((i*3)) 秒"; OK=1; break ;;
  esac
done

if [ "$OK" -eq 0 ]; then
  echo "❌ 未就绪"
  tail -40 "$APP"
else
  echo ""
  echo "-------- 4. 统一响应体 --------"
  $PROBE http://localhost:8080/api/health
  echo ""
  echo ""
  echo "-------- 5. ping 接口 --------"
  $PROBE http://localhost:8080/api/health/ping
  echo ""
  echo ""
  echo "-------- 6. 404 统一格式（自定义 ErrorController）--------"
  $PROBE http://localhost:8080/api/not-exist-xxx
  echo ""
  echo ""
  echo "-------- 7. Swagger 接口文档 --------"
  $PROBE http://localhost:8080/doc.html | head -2
  echo ""
  echo ""
  echo "-------- 8. 启动日志 --------"
  grep -E "Started CourseHubApplication|Tomcat started on port|DruidDataSource.*inited|Init DruidDataSource" "$APP" | head -5
fi

echo ""
echo "-------- 9. 清理 --------"
taskkill //F //IM java.exe > /dev/null 2>&1 && echo "已停止 Java"
cd "E:/java/redis" && ./redis-cli.exe shutdown nosave > /dev/null 2>&1
echo "验收结束"
