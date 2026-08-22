#!/bin/bash
# MCP endpoint acceptance verification (M3).
# Verifies the /{code}/mcp endpoint with the official MCP Inspector CLI and
# the Conformance CLI (2025-11-25), using a local proxy that injects the JWT.
#
# Usage:
#   MCP_TOKEN=<jwt-or-api-key> MCP_DS_ID=<dsId> \
#     .agents/skills/e2e-tester/scripts/mcp-verify.sh
#
# Target service code, tool/prompt names and proxy port are read from
# e2e-tests/test-env.yaml (mcp.*), overridable via MCP_CODE/MCP_TOOL_NAME/
# MCP_PROMPT/MCP_PROXY_PORT env vars.
#
# MCP_TOKEN accepts either a JWT (via login) or a user API key (sk_..., see
# e2e-tests/test-cases/api-key.md TC-AK-009); API keys never expire and are the
# recommended credential for repeated acceptance runs.
#
# Environment:
#   MCP_BASE_URL   backend base URL (default test-env.yaml server.base_url)
#   MCP_CODE       published service code (default test-env.yaml mcp.code)
#   MCP_TOKEN      JWT or user API key (sk_...) - Authorization: Bearer <token>
#   MCP_DS_ID      data source id for execute_sql tool call (optional)
#   MCP_TOOL_NAME  custom tool name for tools/call demo (optional, default test-env.yaml mcp.tool_name)
#   MCP_PROMPT     prompt name for prompts/get (optional, default test-env.yaml mcp.prompt)
#   MCP_PROXY_PORT conformance proxy port (optional, default test-env.yaml mcp.proxy_port)
#   MCP_BASELINE   conformance expected-failures baseline (default <repo>/e2e-tests/conformance/baseline.yml)
#   MCP_RESULTS    conformance results output dir (default <repo>/e2e-tests/conformance/results)

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
SKILL_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
PROJECT_DIR="$(cd "$SKILL_DIR/../../.." && pwd)"
ENV_FILE="$PROJECT_DIR/e2e-tests/test-env.yaml"

# 配置优先级：环境变量 > test-env.yaml > 硬编码兜底
if command -v yq &>/dev/null && [ -f "$ENV_FILE" ]; then
    MCP_BASE_URL="${MCP_BASE_URL:-$(yq -r '.server.base_url' "$ENV_FILE")}"
    MCP_CODE="${MCP_CODE:-$(yq -r '.mcp.code' "$ENV_FILE")}"
    MCP_TOOL_NAME="${MCP_TOOL_NAME:-$(yq -r '.mcp.tool_name' "$ENV_FILE")}"
    MCP_PROMPT="${MCP_PROMPT:-$(yq -r '.mcp.prompt' "$ENV_FILE")}"
    PROXY_PORT="${MCP_PROXY_PORT:-$(yq -r '.mcp.proxy_port' "$ENV_FILE")}"
else
    MCP_BASE_URL="${MCP_BASE_URL:-http://localhost:8085}"
    MCP_CODE="${MCP_CODE:-mcp-verify-demo}"
    MCP_TOOL_NAME="${MCP_TOOL_NAME:-list_genres}"
    MCP_PROMPT="${MCP_PROMPT:-analyze_genre}"
    PROXY_PORT="${MCP_PROXY_PORT:-18085}"
fi
MCP_TOKEN="${MCP_TOKEN:?MCP_TOKEN is required}"
MCP_DS_ID="${MCP_DS_ID:-}"
SPEC_VERSION="2025-11-25"

MCP_BASELINE="${MCP_BASELINE:-$PROJECT_DIR/e2e-tests/conformance/baseline.yml}"
MCP_RESULTS="${MCP_RESULTS:-$PROJECT_DIR/e2e-tests/conformance/results}"

ENDPOINT="${MCP_BASE_URL}/${MCP_CODE}/mcp"
echo "==> MCP endpoint: ${ENDPOINT}"

# ── 1. Inspector CLI (external client semantics) ────────────────────────────
echo ""
echo "==> [1/4] Inspector CLI: tools/list"
npx -y @modelcontextprotocol/inspector@latest --cli \
  --server-url "$ENDPOINT" --header "Authorization: Bearer $MCP_TOKEN" \
  --method tools/list

echo ""
echo "==> [2/4] Inspector CLI: tools/call (${MCP_TOOL_NAME})"
ARGS=()
if [ -n "$MCP_DS_ID" ]; then
  ARGS+=(--tool-arg "data_source_id=$MCP_DS_ID" --tool-arg "sql=SELECT 1")
fi
npx -y @modelcontextprotocol/inspector@latest --cli \
  --server-url "$ENDPOINT" --header "Authorization: Bearer $MCP_TOKEN" \
  --method tools/call --tool-name "$MCP_TOOL_NAME" ${ARGS[@]+"${ARGS[@]}"} --tool-arg "genre=Rock"

echo ""
echo "==> [3/4] Inspector CLI: prompts/get (${MCP_PROMPT})"
npx -y @modelcontextprotocol/inspector@latest --cli \
  --server-url "$ENDPOINT" --header "Authorization: Bearer $MCP_TOKEN" \
  --method prompts/get --prompt-name "$MCP_PROMPT" --prompt-args "genre=Jazz"

# ── 2. Conformance (protocol compliance, 2025-11-25) ────────────────────────
echo ""
echo "==> [4/4] Conformance (2025-11-25) with baseline: ${MCP_BASELINE}"
# The Conformance CLI cannot send custom headers; proxy injects the JWT.
BACKEND_PORT=$(echo "$MCP_BASE_URL" | sed -E 's|.*:([0-9]+).*|\1|')
node "$SCRIPT_DIR/mcp-conformance-proxy.js" "$BACKEND_PORT" "$MCP_TOKEN" > /tmp/mcp-conformance-proxy.log 2>&1 &
PROXY_PID=$!
trap 'kill $PROXY_PID 2>/dev/null || true' EXIT
sleep 1

mkdir -p "$MCP_RESULTS"
npx -y @modelcontextprotocol/conformance@latest server \
  --url "http://localhost:${PROXY_PORT}/${MCP_CODE}/mcp" \
  --spec-version "$SPEC_VERSION" \
  --expected-failures "$MCP_BASELINE" \
  -o "$MCP_RESULTS"

echo ""
echo "==> Done. Conformance results saved to ${MCP_RESULTS}"
