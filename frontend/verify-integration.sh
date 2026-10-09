#!/bin/bash
# 前后端联调验证：后端 + 前端开发服务器同时启动，验证代理是否打通
# 结果写入 frontend/integration-verify.log

unset http_proxy https_proxy HTTP_PROXY HTTPS_PROXY all_proxy ALL_PROXY
unset SERVER__PORT server__port SERVER_PORT server_port
export npm_config_cache="E:/java/npm-cache"

ROOT="D:/桌面/Java项目AIGC"
OUT="$ROOT/frontend/integration-verify.log"
PY="E:/python/python.exe"
PROBE="$PY $ROOT/backend/http_probe.py"

exec > "$OUT" 2>&1

echo "======== 前后端联调验证 ========"
echo "时间: $(date '+%Y-%m-%d %H:%M:%S')"
echo ""

# ---------- 1. Redis ----------
echo "-------- 1. Redis --------"
cd "E:/java/redis" && (./redis-server.exe redis.conf > /dev/null 2>&1 &)
sleep 4
netstat -ano 2>/dev/null | grep LISTENING | grep ":6379 " >/dev/null \
  && echo "✅ Redis 6379" || echo "⚠️ Redis 未启动"

# ---------- 2. 后端 ----------
echo ""
echo "-------- 2. 启动后端 --------"
cd "$ROOT/backend" || exit 1
export JAVA_HOME="E:/java/jdk11"
"E:/java/apache-maven-3.9.16/bin/mvn.cmd" spring-boot:run \
  -Dspring-boot.run.arguments=--server.port=8080 > "$ROOT/backend/app-run.log" 2>&1 &

echo "轮询后端就绪..."
BOK=0
for i in $(seq 1 40); do
  sleep 3
  R=$($PROBE http://localhost:8080/api/health 2>/dev/null)
  case "$R" in
    *'"code"'*) echo "✅ 后端就绪（约 $((i*3)) 秒）"; BOK=1; break ;;
  esac
done
[ "$BOK" -eq 0 ] && echo "❌ 后端未就绪"

# ---------- 3. 前端 ----------
echo ""
echo "-------- 3. 启动前端开发服务器 --------"
cd "$ROOT/frontend" || exit 1
E:/node.js/npm.cmd run dev > "$ROOT/frontend/dev-server.log" 2>&1 &

FOK=0
for i in $(seq 1 25); do
  sleep 3
  R=$($PROBE http://localhost:5173/ 2>/dev/null)
  case "$R" in
    *'<!DOCTYPE'*|*'<html'*) echo "✅ 前端就绪（约 $((i*3)) 秒）"; FOK=1; break ;;
  esac
done
[ "$FOK" -eq 0 ] && echo "❌ 前端未就绪"

# ---------- 4. 专项验证 ----------
if [ "$BOK" -eq 1 ] && [ "$FOK" -eq 1 ]; then
  echo ""
  echo "-------- 4. 前端首页（应返回 HTML）--------"
  $PROBE http://localhost:5173/ | head -2
  echo ""
  echo ""
  echo "-------- 5. 关键验证：前端代理 → 后端 --------"
  echo "请求 http://localhost:5173/api/health"
  echo "（由 Vite 代理转发到后端 8080）"
  echo ""
  $PROBE http://localhost:5173/api/health
  echo ""
  echo ""
  echo "-------- 6. 代理 404 也走统一格式 --------"
  $PROBE http://localhost:5173/api/not-exist-xxx
  echo ""
  echo ""
  echo "-------- 7. 前端启动日志 --------"
  grep -E "Local:|Network:|ready in" "$ROOT/frontend/dev-server.log" | head -4
fi

echo ""
echo "-------- 8. 清理 --------"
taskkill //F //IM java.exe > /dev/null 2>&1 && echo "已停止后端"
taskkill //F //IM node.exe > /dev/null 2>&1 && echo "已停止前端"
cd "E:/java/redis" && ./redis-cli.exe shutdown nosave > /dev/null 2>&1
echo "联调验证结束"
