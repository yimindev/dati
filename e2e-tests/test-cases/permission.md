# 权限系统 - 端到端测试

> 覆盖资源授权（grant/revoke）、列表过滤、操作 403、MCP 传播校验、全局管理员。
> 涉及三个用户：A（资源创建者）、B（被授权者）、admin（全局管理员）。

**主体标识约定（v2）：** ACL 的 `principal_id` 存**用户 UUID**。授权前先调 `GET /v1/users/search?keyword=<用户名>` 获取目标用户 UUID，再提交授权；撤销同理用 UUID。

**测试用户约定：**
- A = `qa-user-a`（用例中注册，随机后缀）
- B = `qa-user-b`（用例中注册，随机后缀）
- admin = `qa-admin`（注册后用 `auth.admin-users` 配置中的 admin 用户名登录，如默认配置 admin 不存在则注册 admin 用户名）

---

## TC-PM-010 创建者立即可见自己的资源（回归）
**级别：** P0
**前置：** 用户 A 已注册并登录

1. A 创建数据源 → 记录 id
2. A GET /v1/data-sources 列表 → 预期列表中**包含**刚创建的数据源（owner 分支：`created_by = :userId`）

---

## TC-PM-001 未授权用户不可见资源（列表过滤 + 详情 403）
**级别：** P0
**前置：** 用户 A、B 均已注册并登录

1. A 创建一个数据源（可用 `bad_connection` 的坏连接配置避免真实建连；若需真实数据源则用 `postgres_local`），记录 id
2. B 登录，GET /v1/data-sources 列表
3. 预期：列表中**不包含** A 创建的数据源
4. B GET /v1/data-sources/{id} 详情
5. 预期：返回 403（PM001），响应体 code 为 `PM001`

---

## TC-PM-002 授权 VIEW 后可见可查但不可编辑
**级别：** P0
**前置：** TC-PM-001 完成（A 拥有数据源 ds-1）

1. 先 GET /v1/users/search?keyword=<B 用户名> 取 B 的 UUID（B_UID）
2. A 调 POST /v1/acls/DATA_SOURCE/{ds-1}，body：`{"principal_type":"USER","principal_id":"<B_UID>","permission":"VIEW"}`
3. 预期返回 200，返回 id 非空
4. B 刷新列表 → 数据源 ds-1 可见
5. B GET /v1/data-sources/{ds-1} → 200
6. B PUT /v1/data-sources/{ds-1}（改 name）→ 预期 403
7. 校验重复授权幂等：A 再调一次相同 POST → 200，ACL 列表仍只有一条记录（GET /v1/acls/DATA_SOURCE/{ds-1}）

---

## TC-PM-003 升级 EDIT 后可编辑
**级别：** P0
**前置：** TC-PM-002 完成

1. A 调 POST /v1/acls/DATA_SOURCE/{ds-1}，permission 改为 `EDIT`
2. 预期返回 200（覆盖升级，非新增）
3. B PUT /v1/data-sources/{ds-1}（改 name）→ 预期 200
4. GET /v1/acls/DATA_SOURCE/{ds-1} → 只有 1 条记录，permission = EDIT

---

## TC-PM-004 撤销后立即失效
**级别：** P0
**前置：** TC-PM-003 完成

1. A 调 DELETE /v1/acls/DATA_SOURCE/{ds-1}/USER/{B_UID}
2. 预期返回 200
3. B 刷新列表 → 数据源 ds-1 不可见
4. B GET /v1/data-sources/{ds-1} → 预期 403

---

## TC-PM-005 无权用户不能授权他人
**级别：** P1
**前置：** TC-PM-002 完成（B 对 ds-1 有 VIEW）

1. B 调 POST /v1/acls/DATA_SOURCE/{ds-1} 授权给第三个用户
2. 预期返回 403

---

## TC-PM-006 全局管理员可访问所有资源
**级别：** P1
**前置：** TC-PM-003 完成；admin 用户存在

1. admin 登录
2. GET /v1/data-sources → 列表中包含 A 和 B 的全部数据源
3. GET /v1/data-sources/{ds-1} → 200（即使未被授权）
4. DELETE /v1/acls/... 等操作均可用

---

## TC-PM-007 MCP 服务传播校验：无权绑定他人数据源
**级别：** P0
**前置：** TC-PM-001 完成（A 拥有 ds-1，B 对 ds-1 无权限）

1. B 创建 MCP 服务，data_scopes 绑定 ds-1
   - POST /v1/mcp-services，body 含 `{"data_scopes":[{"scope_type":"DATA_SOURCE","reference_id":"<ds-1>"}]}`
2. 预期返回 403（PM001，传播校验拦截）
3. B 创建服务绑定一个**自己拥有**的数据源（B 先创建数据源 ds-B）→ 预期 200

---

## TC-PM-008 服务授权不传播到底层数据源
**级别：** P1
**前置：** TC-PM-007 完成（A 的服务 svc-1 绑定 ds-1）

1. A 创建 MCP 服务 svc-1 绑定 ds-1 → 200
2. A 授权 C（第三个用户）对 svc-1 VIEW
3. C 访问 svc-1 详情 GET /v1/mcp-services/{svc-1} → 200
4. C 查看 ds-1 列表 → **不可见**（数据源权限不随服务授权传播）

---

## TC-PM-009 主题资源授权
**级别：** P2
**前置：** TC-PM-002 完成；A 有数据源 ds-1

1. A 创建主题 subject-1（绑定 ds-1）→ 200
2. A 授权 B 对 SUBJECT 的 VIEW（POST /v1/acls/SUBJECT/{subject-1}）
3. B GET /v1/subjects 列表 → 可见
4. B 未授权前 GET /v1/subjects/{subject-1} → 403（若先测 3 则需先撤销或换新主题）

---

## TC-PUB-001 全公开只读（GROUP/ALL_USERS）
**级别：** P1
**前置：** 用户 A（资源 owner）、B（无关用户）均已注册并登录

1. A 创建数据源 ds-1
2. 公开前：B 列表不可见 ds-1、详情 403
3. A 调 POST /v1/acls/DATA_SOURCE/{ds-1}，body：`{"principal_type":"GROUP","principal_id":"ALL_USERS","permission":"VIEW"}` → 200
4. 公开后：B 列表可见、详情 200（只读）
5. B 编辑 ds-1 → 403（公开只读，不授予 EDIT）
6. A 尝试公开行升级 EDIT → 400（后端拒绝）
7. A 调 DELETE /v1/acls/DATA_SOURCE/{ds-1}/GROUP/ALL_USERS → 200
8. 撤销后：B 列表恢复不可见
