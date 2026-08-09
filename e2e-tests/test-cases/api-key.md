# API Key - 端到端测试

## TC-AK-001 创建永久 API Key
**级别：** P0
**前置：** 已登录（qa_user）

1. 登录获取 JWT（见 test-data.yaml `users.qa_user`）
2. 创建 API Key：`POST /v1/auth/api-keys`，body `{"name": "e2e-permanent-{timestamp}", "expires_in_days": null}`
3. 预期返回 201，响应包含：
   - `key`：明文，以 `sk_` 开头，长度约 46 字符
   - `key_mask`：格式 `sk_<4位>***<4位>`
   - `expires_at`：null（永久）
   - `id` / `name` 与请求一致

---

## TC-AK-002 创建带过期时间的 API Key
**级别：** P1
**前置：** 已登录

1. 创建 API Key：`POST /v1/auth/api-keys`，body `{"name": "e2e-expiry-{timestamp}", "expires_in_days": 30}`
2. 预期 201，`expires_at` 约为当前时间 + 30 天（非 null）

---

## TC-AK-003 非法过期天数被拒绝
**级别：** P1
**前置：** 已登录

1. 创建 API Key：`expires_in_days: 5`（不在 7/30/90/180/365 白名单）
2. 预期 400，错误码 `AUTH006`

---

## TC-AK-004 列表展示掩码且不含明文
**级别：** P0
**前置：** TC-AK-001/002 已创建

1. `GET /v1/auth/api-keys`
2. 预期 200，列表包含刚创建的 key，且：
   - 每条有 `key_mask`（`sk_xxxx***xxxx` 格式）但**无 `key` 字段**
   - 字段完整：`id` / `name` / `key_mask` / `created_at` / `expires_at` / `last_used_at`
   - 只包含当前用户自己的 key（不含其他用户的）

---

## TC-AK-005 用 API Key 调用 REST API
**级别：** P0
**前置：** 有一个有效 API Key

1. 用 API Key 作为 `Authorization: Bearer <sk_...>` 调用 `GET /v1/auth/me`
2. 预期 200，返回的用户 name 与创建 key 的用户一致

---

## TC-AK-006 用 API Key 调用 MCP Endpoint
**级别：** P1
**前置：** 存在已发布 MCP 服务（`test-data.yaml` 无预置，需环境提供）；无已发布服务时跳过

1. 用 API Key 作为 `Authorization: Bearer <sk_...>` 调用 `POST /{code}/mcp`（JSON-RPC initialize）
2. 预期与 JWT 调用行为一致：已发布服务 200、DRAFT/不存在 404、已停用 503
3. 与 TC-END-001~011 相同断言，仅凭证不同

---

## TC-AK-007 删除后立即失效
**级别：** P0
**前置：** 有一个有效 API Key

1. 用该 key 调用 `GET /v1/auth/me` → 200（基线确认）
2. `DELETE /v1/auth/api-keys/{id}` → 204
3. 再次用该 key 调用 `GET /v1/auth/me` → 401
4. 重复 DELETE 同一 id → 204（幂等）

---

## TC-AK-008 越权删除被拒绝
**级别：** P1
**前置：** 两个用户 A、B 各有一个 API Key

1. 用户 B 登录，用 B 的 JWT 删除用户 A 的 key id
2. 预期 403，错误码 `AUTH005`
3. 用户 A 的 key 仍可用（`GET /v1/auth/me` → 200）

---

## TC-AK-009 构造固定 API Key（供 MCP 验收复用）
**级别：** P2（工具链）
**前置：** 已登录

1. 创建永久 API Key：`POST /v1/auth/api-keys`，body `{"name": "e2e-mcp-verify", "expires_in_days": null}`
2. 若已存在同名 key（幂等约定），跳过创建
3. 将明文保存到本地文件（如 `/tmp/dati_apikey.txt`），供 `mcp-verify.sh` 以 `MCP_TOKEN=<sk_...>` 复用
4. API Key 不过期（expires_at=null），替代 JWT 作为长期有效的 MCP 验收凭证
