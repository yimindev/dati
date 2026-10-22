# 认证 - 端到端测试

## TC-AUTH-001 注册与登录全流程
**级别：** P0
**前置：** 无

1. 用 qa-e2e-tester 用户信息（见 test-data.yaml `users.qa_user`）注册新用户（密码需 8–72 位）
2. 用同样的 name 和 password 登录，登录时必须传 `type: "local"`
3. 预期登录返回 200，响应包含 token 字段且非空
4. 用该 token 访问数据源列表接口（GET /v1/data-sources）
5. 预期返回 200，表示 token 有效

---

## TC-AUTH-002 错误密码登录
**级别：** P1
**前置：** 已注册的用户存在

1. 用 test-data.yaml 中 qa_user 的 name + 一个错误密码登录
2. 预期返回 400 或 401，不应返回 token

---

## TC-AUTH-003 获取当前用户信息
**级别：** P1
**前置：** 已登录

1. 获取当前登录用户的信息（GET /v1/auth/me）
2. 预期返回用户对象，其 name 字段与登录用户一致

---

## TC-AUTH-004 重复注册（登录名大小写不敏感）
**级别：** P2
**前置：** 已注册的用户存在

1. 用已存在的 name 再次注册
2. 预期返回 400 或 409 错误（AUTH003: Username already exists）
3. 把 name 改成全大写（如 `qa-e2e-tester` → `QA-E2E-TESTER`）再次注册
4. 预期同样返回 400 或 409（AUTH003），不得新建第二个账号
5. 用全大写 name + 正确密码登录，预期 200 且返回 token
6. 用该 token 请求 GET /v1/auth/me，预期 `name` 为注册时规范化后的小写值

---

## TC-AUTH-005 游客一键免密登录与身份鉴权
**级别：** P0
**前置：** 无

1. 调用游客登录接口（POST /v1/auth/guest），无需请求体
2. 预期返回 200，响应体包含非空 `token`
3. 携带该 token 请求当前用户信息（GET /v1/auth/me）
4. 预期返回 200，且用户信息的 `id` 和 `name` 以 `guest-` 开头，`display_name` 为 `"Guest"`，并且包含 `is_guest: true` 或 `guest: true`
5. 携带该 token 访问数据源列表（GET /v1/data-sources）
6. 预期返回 200，表明游客凭证具备合法访问权限

---

## TC-AUTH-006 游客多会话独立性验证（会话隔离）
**级别：** P1
**前置：** 无

1. 第一次调用 POST /v1/auth/guest 获取 token1，并通过 GET /v1/auth/me 获取 guest_id_1
2. 第二次调用 POST /v1/auth/guest 获取 token2，并通过 GET /v1/auth/me 获取 guest_id_2
3. 预期 token1 与 token2 不相同
4. 预期 guest_id_1 与 guest_id_2 不相同，确保每个访问者获得独立的临时沙箱身份

---

## TC-AUTH-007 游客资源创建与创建者回显
**级别：** P0
**前置：** 游客已获取 token

1. 调用 POST /v1/auth/guest 获取游客 token
2. 使用该 token 创建数据源（POST /v1/data-sources，使用 test-data.yaml 中的 postgres_local 配置）
3. 预期创建成功返回 200，返回数据源 ID
4. 使用该游客 token 查询数据源详情（GET /v1/data-sources/{id}）
5. 预期能正常查询到，且返回结果的 `created_user_name` 回显为 `"Guest"`
6. 使用该游客 token 删除刚创建的数据源（DELETE /v1/data-sources/{id}）
7. 预期删除成功返回 200

---

## TC-AUTH-008 游客跨会话隔离（数据隔离）
**级别：** P1
**前置：** 游客1已获取 token 并创建私有数据源

1. 游客1调用 POST /v1/auth/guest 获取 token_1，并创建数据源 DS_G1
2. 游客2调用 POST /v1/auth/guest 获取独立的 token_2
3. 游客2请求数据源详情（GET /v1/data-sources/{DS_G1_ID}）
4. 预期返回 403 错误（PM001: No permission to access this resource）
5. 清理：游客1删除数据源 DS_G1


---

## TC-AUTH-009 注册密码强度校验
**级别：** P1
**前置：** 无

1. POST /v1/auth/register 传 `password: "x"`（1 位）与合法 name
2. 预期返回 400（CM001），`message` 中包含 `password`
3. 传 7 位密码再次注册，预期仍为 400（CM001）
4. 传 8 位密码（如 `12345678`）注册新 name，预期 200 并返回 id
5. 前端校验：注册页输入 7 位或 73 位密码提交，预期出现「密码长度需为 8–72 位」提示且不发请求

---

## TC-AUTH-010 未认证 / 无效 token 的统一错误响应
**级别：** P1
**前置：** 无

1. GET /v1/auth/me 不带 Authorization
2. 预期 401，响应头含 `WWW-Authenticate: Bearer`，响应体为 JSON：`{"code":"AUTH009","message":"Authentication token is required","timestamp":...}`
3. 带 `Authorization: Bearer not.a.jwt` 请求 /v1/auth/me
4. 预期 401，响应体 `{"code":"AUTH002","message":"Token invalid or expired",...}`
5. 带 `Accept: text/event-stream` 且无 token 请求 POST /{code}/mcp
6. 预期仍为 401 + 同上 JSON 错误体（不得返回空 body 或 406）
