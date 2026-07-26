#!/bin/bash
# E2E 测试公共辅助函数
# Agent 在测试会话开头 source 本文件:
#   source .agents/skills/e2e-tester/scripts/helpers.sh
#
# 需要 python3 和 yq (pip install yq) 来解析 test-env.yaml。
# 如果 yq 不可用，可通过环境变量覆盖配置。

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SKILL_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
PROJECT_DIR="$(cd "$SKILL_DIR/../../.." && pwd)"
ENV_FILE="$PROJECT_DIR/e2e-tests/test-env.yaml"

# ---- ES 配置 ----
if command -v yq &>/dev/null; then
    ES_URIS="${ES_URIS:-$(yq -r '.external_services.elasticsearch.uris' "$ENV_FILE")}"
    ES_USER="${ES_USER:-$(yq -r '.external_services.elasticsearch.username' "$ENV_FILE")}"
    ES_PASS="${ES_PASS:-$(yq -r '.external_services.elasticsearch.password' "$ENV_FILE")}"
    ES_INDEX="${ES_INDEX:-$(yq -r '.external_services.elasticsearch.index' "$ENV_FILE")}"
else
    # yq 不可用时使用默认值，可通过环境变量覆盖
    ES_URIS="${ES_URIS:-http://localhost:9200}"
    ES_USER="${ES_USER:-elastic}"
    ES_PASS="${ES_PASS:-yimin123}"
    ES_INDEX="${ES_INDEX:-semantic_search}"
fi
ES_AUTH="${ES_USER}:${ES_PASS}"
ES_BASE="${ES_URIS}/${ES_INDEX}"

# ---- 函数: ES refresh ----
# 强制刷新索引，使刚写入的数据立即可搜索。替代不可靠的 sleep。
# 用法: es_refresh
es_refresh() {
    local code
    code=$(curl -s -o /dev/null -w "%{http_code}" -u "$ES_AUTH" -X POST "${ES_URIS}/_refresh")
    if [ "$code" != "200" ]; then
        echo "[es] WARN: refresh returned $code" >&2
    fi
}
