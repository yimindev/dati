# 错误处理与本地化 - 端到端测试

> 验证后端 `ErrorResponse` 的 `{ code, message }` 契约，以及 `Accept-Language` 对消息语言的影响。

## TC-ERR-001 重复注册返回本地化 AUTH003
**级别：** P1
**前置：** 已注册用户存在

1. 用 test-data.yaml 中 `users.qa_user` 的 name 再次注册，请求头带 `Accept-Language: zh-CN`
2. 预期返回 409，响应 `code` 为 `AUTH003`，`message` 包含中文“用户名已存在”和该用户名
3. 同样请求带 `Accept-Language: en`
4. 预期返回 409，`code` 为 `AUTH003`，`message` 为英文 `Username already exists: <name>`
5. 两次响应都必须包含非空 `message` 和 `timestamp`

---

## TC-ERR-002 错误密码登录返回本地化 AUTH001
**级别：** P1
**前置：** 已注册用户存在

1. 用 `qa_user` 的 name + 错误密码登录，请求头带 `Accept-Language: zh-CN`
2. 预期返回 401，`code` 为 `AUTH001`，`message` 为中文“用户名或密码错误”
3. 同样请求带 `Accept-Language: en`
4. 预期返回 401，`code` 为 `AUTH001`，`message` 为英文 `Authentication failed: invalid credentials`

---

## TC-ERR-003 未认证请求返回本地化 AUTH009
**级别：** P1
**前置：** 无

1. 不带 Authorization 请求 GET /v1/data-sources，请求头带 `Accept-Language: zh-CN`
2. 预期返回 401，`code` 为 `AUTH009`，`message` 为中文“缺少认证凭证”
3. 同样请求带 `Accept-Language: en`
4. 预期返回 401，`code` 为 `AUTH009`，`message` 为英文 `Authentication token is required`

---

## TC-ERR-004 参数校验错误返回本地化 CM001
**级别：** P1
**前置：** 已登录，且存在至少一个数据源

1. 先 GET /v1/data-sources 取一条已存在的数据源 id
2. 创建主题，请求体 `name` 为空白字符串、`datasource_id` 为该有效 id，请求头带 `Accept-Language: zh-CN`
3. 预期返回 400，`code` 为 `CM001`，`message` 以“请求参数错误：”开头并包含 `name`
4. 同样请求带 `Accept-Language: en`
5. 预期返回 400，`code` 为 `CM001`，`message` 以 `Request parameter error:` 开头并包含 `name`

---

## TC-ERR-005 未带 Accept-Language 时默认英文
**级别：** P2
**前置：** 已注册用户存在

1. 用错误密码登录，不设置 `Accept-Language`
2. 预期返回 401，`code` 为 `AUTH001`，`message` 为英文

---

## TC-ERR-006 未带 Accept-Language 时校验消息保持单一语言
**级别：** P1
**前置：** 已登录，且存在至少一个数据源

1. POST /v1/subjects，请求体 `name` 为空白字符串、`datasource_id` 为有效 id，不设置 `Accept-Language`
2. 预期返回 400，`code` 为 `CM001`，`message` 以 `Request parameter error:` 开头，且不包含中文字符
3. 取一个已有主题，PUT /v1/subjects/{id}，请求体 `name` 为空白字符串，不设置 `Accept-Language`
4. 预期返回 400，`code` 为 `CM001`，`message` 为英文
5. 同样 PUT 请求带 `Accept-Language: zh-CN`
6. 预期返回 400，`code` 为 `CM001`，`message` 以“请求参数错误：”开头并包含“不得为空白”
