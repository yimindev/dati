# 认证 - 端到端测试

## TC-AUTH-001 注册与登录全流程
**级别：** P0
**前置：** 无

1. 用 qa-e2e-tester 用户信息（见 test-data.yaml `users.qa_user`）注册新用户
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

## TC-AUTH-004 重复注册
**级别：** P2
**前置：** 已注册的用户存在

1. 用已存在的 name 再次注册
2. 预期返回 400 错误（AUTH003: Username already exists）
