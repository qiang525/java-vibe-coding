#!/bin/bash
# =============================================================================
#  把前端打包进后端（bash 版，Windows 上双击请用 build-to-backend.bat）
# -----------------------------------------------------------------------------
#  做完之后：只启动后端，浏览器打开 http://localhost:8080 就是完整网页。
#
#  原理：把 vite 构建产物复制到 Spring Boot 的静态资源目录
#        （src/main/resources/static/），Spring Boot 默认就会托管它。
#        前端请求 /api/xxx 是同源请求，不需要代理。
# =============================================================================
set -e
cd "$(dirname "$0")"

export npm_config_cache="${npm_config_cache:-E:/java/npm-cache}"
NPM="${NPM:-E:/node.js/npm.cmd}"

echo "[1/3] 构建前端 ..."
"$NPM" run build

echo "[2/3] 复制到后端静态目录 ..."
DEST="../backend/src/main/resources/static"
rm -rf "$DEST"
mkdir -p "$DEST"
cp -r dist/. "$DEST/"

echo "[3/3] 完成"
echo ""
echo "  文件数：$(find "$DEST" -type f | wc -l)"
echo "  接下来：启动后端，浏览器打开 http://localhost:8080"
