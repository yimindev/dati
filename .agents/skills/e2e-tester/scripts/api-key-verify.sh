#!/bin/bash
BASE=http://localhost:8085
PASS=0; FAIL=0; WARN=0
report() { local r="$1"; local n="$2"; local msg="$3"; if [ "$r" = "PASS" ]; then PASS=$((PASS+1)); else FAIL=$((FAIL+1)); fi; echo "$r  $n  $msg"; }

login() { # $1=name $2=pass → token
  curl -s -X POST $BASE/v1/auth/login -H "Content-Type: application/json" \
    -d "{\"type\":\"local\",\"name\":\"$1\",\"password\":\"$2\"}" | python3 -c "import sys,json;print(json.load(sys.stdin).get('token',''))" 2>/dev/null
}

# ── 前置：qa_user 登录（不存在则注册）──
JWT=$(login qa-e2e-tester QaTest123456)
if [ -z "$JWT" ]; then
  curl -s -X POST $BASE/v1/auth/register -H "Content-Type: application/json" \
    -d '{"type":"local","name":"qa-e2e-tester","password":"QaTest123456"}' > /dev/null
  JWT=$(login qa-e2e-tester QaTest123456)
fi
[ -n "$JWT" ] && echo "前置: qa_user 登录 OK" || { echo "前置失败: 无法登录"; exit 1; }
TS=$(date +%s)

# ── TC-AK-001 创建永久 key ──
RESP=$(curl -s -w "\n__HTTP__%{http_code}" -X POST $BASE/v1/auth/api-keys -H "Authorization: Bearer $JWT" -H "Content-Type: application/json" \
  -d "{\"name\":\"e2e-permanent-$TS\",\"expires_in_days\":null}")
HTTP=$(echo "$RESP" | grep -o '__HTTP__[0-9]*' | sed 's/__HTTP__//'); BODY=$(echo "$RESP" | sed 's/__HTTP__[0-9]*$//')
KEY=$(echo "$BODY" | python3 -c "import sys,json;print(json.load(sys.stdin).get('key',''))" 2>/dev/null)
MASK=$(echo "$BODY" | python3 -c "import sys,json;print(json.load(sys.stdin).get('key_mask',''))" 2>/dev/null)
EXP=$(echo "$BODY" | python3 -c "import sys,json;print(json.load(sys.stdin).get('expires_at'))" 2>/dev/null)
ID1=$(echo "$BODY" | python3 -c "import sys,json;print(json.load(sys.stdin).get('id',''))" 2>/dev/null)
if [ "$HTTP" = "201" ] && [[ "$KEY" == sk_* ]] && [ ${#KEY} -ge 40 ] && [[ "$MASK" =~ ^sk_[a-zA-Z0-9_-]{4}\*\*\*[a-zA-Z0-9_-]{4}$ ]] && [ "$EXP" = "None" ]; then
  report PASS TC-AK-001 "创建永久 key (key=${KEY:0:12}... mask=$MASK)"
else
  report FAIL TC-AK-001 "HTTP=$HTTP key=$KEY mask=$MASK exp=$EXP"
fi

# ── TC-AK-002 创建 30 天 key ──
RESP=$(curl -s -w "\n__HTTP__%{http_code}" -X POST $BASE/v1/auth/api-keys -H "Authorization: Bearer $JWT" -H "Content-Type: application/json" \
  -d "{\"name\":\"e2e-expiry-$TS\",\"expires_in_days\":30}")
HTTP=$(echo "$RESP" | grep -o '__HTTP__[0-9]*' | sed 's/__HTTP__//'); BODY=$(echo "$RESP" | sed 's/__HTTP__[0-9]*$//')
EXP=$(echo "$BODY" | python3 -c "import sys,json;print(json.load(sys.stdin).get('expires_at',''))" 2>/dev/null)
if [ "$HTTP" = "201" ] && [ -n "$EXP" ]; then
  report PASS TC-AK-002 "30 天 key expires_at=$EXP"
else
  report FAIL TC-AK-002 "HTTP=$HTTP exp=$EXP"
fi

# ── TC-AK-003 非法过期天数 ──
HTTP=$(curl -s -o /tmp/ak3.json -w "%{http_code}" -X POST $BASE/v1/auth/api-keys -H "Authorization: Bearer $JWT" -H "Content-Type: application/json" \
  -d "{\"name\":\"e2e-bad-$TS\",\"expires_in_days\":5}")
CODE=$(python3 -c "import json;print(json.load(open('/tmp/ak3.json')).get('code',''))" 2>/dev/null)
[ "$HTTP" = "400" ] && [ "$CODE" = "AUTH006" ] && report PASS TC-AK-003 "400 AUTH006" || report FAIL TC-AK-003 "HTTP=$HTTP code=$CODE"

# ── TC-AK-004 列表掩码无明文 ──
RESP=$(curl -s $BASE/v1/auth/api-keys -H "Authorization: Bearer $JWT")
LEN=$(echo "$RESP" | python3 -c "import sys,json;print(len(json.load(sys.stdin)))" 2>/dev/null)
HAS_KEY=$(echo "$RESP" | python3 -c "import sys,json;d=json.load(sys.stdin);print(any('key' in k for k in d))" 2>/dev/null)
ALL_MASK=$(echo "$RESP" | python3 -c "
import sys,json,re
d=json.load(sys.stdin)
print(all(re.fullmatch(r'sk_[A-Za-z0-9_-]{4}\*{3}[A-Za-z0-9_-]{4}', k['key_mask']) for k in d) if d else True)" 2>/dev/null)
[ "$LEN" -ge 2 ] && [ "$HAS_KEY" = "False" ] && [ "$ALL_MASK" = "True" ] && report PASS TC-AK-004 "列表 $LEN 条, 全掩码无明文" || report FAIL TC-AK-004 "len=$LEN hasKey=$HAS_KEY allMask=$ALL_MASK"

# ── TC-AK-005 用 API key 调 REST ──
RESP=$(curl -s -w "\n__HTTP__%{http_code}" $BASE/v1/auth/me -H "Authorization: Bearer $KEY")
HTTP=$(echo "$RESP" | grep -o '__HTTP__[0-9]*' | sed 's/__HTTP__//'); NAME=$(echo "$RESP" | sed 's/__HTTP__[0-9]*$//' | python3 -c "import sys,json;print(json.load(sys.stdin).get('name',''))" 2>/dev/null)
[ "$HTTP" = "200" ] && [ "$NAME" = "qa-e2e-tester" ] && report PASS TC-AK-005 "API key 调 /v1/auth/me → 200 ($NAME)" || report FAIL TC-AK-005 "HTTP=$HTTP name=$NAME"

# ── TC-AK-007 删除后立即失效 + 幂等 ──
HTTP=$(curl -s -o /dev/null -w "%{http_code}" -X DELETE $BASE/v1/auth/api-keys/$ID1 -H "Authorization: Bearer $JWT")
HTTP2=$(curl -s -o /dev/null -w "%{http_code}" $BASE/v1/auth/me -H "Authorization: Bearer $KEY")
HTTP3=$(curl -s -o /dev/null -w "%{http_code}" -X DELETE $BASE/v1/auth/api-keys/$ID1 -H "Authorization: Bearer $JWT")
[ "$HTTP" = "204" ] && [ "$HTTP2" = "401" ] && [ "$HTTP3" = "204" ] && report PASS TC-AK-007 "删除 204 → 失效 401 → 幂等 204" || report FAIL TC-AK-007 "del=$HTTP after=$HTTP2 idem=$HTTP3"

# ── TC-AK-008 越权删除 ──
# 用户 B：注册新用户
BNAME="qa-ak-b-$TS"
curl -s -X POST $BASE/v1/auth/register -H "Content-Type: application/json" \
  -d "{\"type\":\"local\",\"name\":\"$BNAME\",\"password\":\"QaTest123456\"}" > /dev/null
JWTB=$(login $BNAME QaTest123456)
RESPB=$(curl -s -X POST $BASE/v1/auth/api-keys -H "Authorization: Bearer $JWTB" -H "Content-Type: application/json" \
  -d "{\"name\":\"b-key\",\"expires_in_days\":null}")
KEYB=$(echo "$RESPB" | python3 -c "import sys,json;print(json.load(sys.stdin).get('key',''))" 2>/dev/null)
IDB=$(echo "$RESPB" | python3 -c "import sys,json;print(json.load(sys.stdin).get('id',''))" 2>/dev/null)
# A 用 B 的 key id？不——B 删 A 的 key：先给 A 再造一个 key
RESPA2=$(curl -s -X POST $BASE/v1/auth/api-keys -H "Authorization: Bearer $JWT" -H "Content-Type: application/json" \
  -d "{\"name\":\"a-key-2-$TS\",\"expires_in_days\":null}")
IDA2=$(echo "$RESPA2" | python3 -c "import sys,json;print(json.load(sys.stdin).get('id',''))" 2>/dev/null)
KEYA2=$(echo "$RESPA2" | python3 -c "import sys,json;print(json.load(sys.stdin).get('key',''))" 2>/dev/null)
HTTP=$(curl -s -o /tmp/ak8.json -w "%{http_code}" -X DELETE $BASE/v1/auth/api-keys/$IDA2 -H "Authorization: Bearer $JWTB")
CODE=$(python3 -c "import json;print(json.load(open('/tmp/ak8.json')).get('code',''))" 2>/dev/null)
HTTPA=$(curl -s -o /dev/null -w "%{http_code}" $BASE/v1/auth/me -H "Authorization: Bearer $KEYA2")
[ "$HTTP" = "403" ] && [ "$CODE" = "AUTH005" ] && [ "$HTTPA" = "200" ] && report PASS TC-AK-008 "越权 403 AUTH005, A 的 key 仍可用" || report FAIL TC-AK-008 "del=$HTTP code=$CODE aKeyStill=$HTTPA"

# ── TC-AK-009 固定 key（e2e-mcp-verify）──
# 幂等约定：列表找同名，存在则用其掩码提示；不存在则创建并保存明文
EXISTING=$(curl -s $BASE/v1/auth/api-keys -H "Authorization: Bearer $JWT" | python3 -c "
import sys,json
d=json.load(sys.stdin)
m=[k for k in d if k['name']=='e2e-mcp-verify']
print(m[0]['id'] if m else '')" 2>/dev/null)
if [ -n "$EXISTING" ]; then
  WARN=$((WARN+1)); echo "WARN  TC-AK-009  固定 key 已存在 (id=$EXISTING)，跳过创建（明文不可再见，无法更新 /tmp/dati_apikey.txt）"
else
  RESP=$(curl -s -X POST $BASE/v1/auth/api-keys -H "Authorization: Bearer $JWT" -H "Content-Type: application/json" \
    -d '{"name":"e2e-mcp-verify","expires_in_days":null}')
  FIXED=$(echo "$RESP" | python3 -c "import sys,json;print(json.load(sys.stdin).get('key',''))" 2>/dev/null)
  if [[ "$FIXED" == sk_* ]]; then
    echo "$FIXED" > /tmp/dati_apikey.txt && chmod 600 /tmp/dati_apikey.txt
    report PASS TC-AK-009 "固定 key 已创建并保存 /tmp/dati_apikey.txt (${FIXED:0:12}...)"
  else
    report FAIL TC-AK-009 "创建失败: $RESP"
  fi
fi

# ── TC-AK-006 MCP endpoint 用 API key（依赖已发布服务）──
# 使用固定 key（TC-AK-009 产物），避免 TC-AK-007 删除的 key 导致 401
FIXED_KEY=$(cat /tmp/dati_apikey.txt 2>/dev/null || echo "$KEY")
CODE_SVC=$(curl -s -o /dev/null -w "%{http_code}" -X POST http://localhost:8085/yimin-test/mcp \
  -H "Authorization: Bearer $FIXED_KEY" -H "Content-Type: application/json" \
  -H "MCP-Protocol-Version: 2025-11-25" \
  -d '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-11-25","capabilities":{},"clientInfo":{"name":"e2e","version":"1.0"}}}')
if [ "$CODE_SVC" = "404" ] || [ "$CODE_SVC" = "503" ] || [ "$CODE_SVC" = "200" ]; then
  report PASS TC-AK-006 "MCP endpoint 认证走通 (HTTP=$CODE_SVC, 服务状态语义正常)"
else
  report FAIL TC-AK-006 "MCP endpoint HTTP=$CODE_SVC（期望 200/404/503 之一，认证层未拦截）"
fi

echo ""
echo "---"
echo "Total: $((PASS+FAIL)) executed, $PASS passed, $FAIL failed, Warnings: $WARN"
