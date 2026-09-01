#!/usr/bin/env bash
# 拉取后端 OpenAPI 文档并分发到两处(唯一入口,替代已删除的 springdoc maven 插件)
#
# 逻辑:
#   1. 检查后端服务是否运行(拉取前提)
#   2. 从 /v3/api-docs 拉取最新文档 -> docs/api/openapi.json(仓库文档,人/e2e-tester 使用)
#   3. 分发副本 -> skills/dati-ops/openapi.json(技能自包含快照,保证两份一致)
#
# 前置: 后端服务运行中(mvn -f backend/pom.xml -pl app spring-boot:run)
# 覆盖: DATI_API_DOCS_URL 环境变量可指定其他环境的 api-docs 地址
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
API_DOCS_URL="${DATI_API_DOCS_URL:-http://localhost:8085/v3/api-docs}"
DST_REPO="$ROOT/docs/api/openapi.json"
DST_SKILL="$ROOT/skills/dati-ops/openapi.json"

if ! curl -sf -m 3 "$API_DOCS_URL" -o /dev/null; then
  echo "后端未运行在 $API_DOCS_URL,请先启动: mvn -f backend/pom.xml -pl app spring-boot:run" >&2
  exit 1
fi

curl -sf "$API_DOCS_URL" -o "$DST_REPO"
cp "$DST_REPO" "$DST_SKILL"

echo "已生成: $DST_REPO ($(wc -c < "$DST_REPO") bytes)"
echo "已同步: $DST_SKILL"
