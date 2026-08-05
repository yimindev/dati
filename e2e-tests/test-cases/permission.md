# 权限系统 - 端到端测试

> 覆盖资源授权（grant/revoke）、列表过滤、操作 403、MCP 传播校验、全局管理员、枚举契约。
> 涉及三个用户：A（资源创建者）、B（被授权者）、C（第三方）、admin（全局管理员）。

**主体标识约定（v2）：** ACL 的 `principal_id` 存**用户 UUID**。授权前先调 `GET /v1/users/search?keyword=<用户名>` 获取目标用户 UUID，再提交授权；撤销同理用 UUID。

**测试用户约定：**
- A = `qa-user-a`（用例中注册，随机后缀）
- B = `qa-user-b`（用例中注册，随机后缀）
- C = `qa-user-c`（用例中注册，随机后缀）
- admin = `qa-admin`（注册后用 `auth.admin-users` 配置中的 admin 用户名登录，如默认配置 admin 不存在则注册 admin 用户名）

**数据源创建约定：** 创建数据源会真实建连探测 schema（BUG-013 修复后的行为），一律使用 test-data.yaml 的 `datasources.postgres_local`。

**已知 API 行为（断言按此，勿当失败）：** `grant` 返回 ACL 记录 id，`revoke` 返回资源 id，二者语义不一致（待 API 层统一）。

**执行结构：**
- 主线用例 TC-PM-010 → TC-PM-001 → TC-PM-002 → TC-PM-003 → TC-PM-004 共享数据源 ds-1，**必须按序执行**
- 其余用例（PM-005~009、PM-011、PUB-001）**自包含**（各自创建独立资源），可单独执行（`--filter` 单跑）

---

## TC-PM-010 创建者立即可见自己的资源（回归）
**级别：** P0
**前置：** 用户 A 已注册并登录

1. A 创建数据源（postgres_local）→ 记录 id
2. A GET /v1/data-sources 列表 → 预期列表中**包含**刚创建的数据源（owner 分支：`created_by = :userId`）

---

## TC-PM-001 未授权用户不可见资源（列表过滤 + 详情 403）
**级别：** P0
**前置：** 用户 A、B 均已注册并登录

1. A 创建数据源 ds-1（postgres_local）→ 记录 id
2. B 登录，GET /v1/data-sources 列表
3. 预期：列表中**不包含** A 创建的数据源
4. B GET /v1/data-sources/{ds-1} 详情
5. 预期：返回 403（PM001），响应体 code 为 `PM001`

---

## TC-PM-002 授权 VIEW 后可见可查但不可编辑
**级别：** P0
**前置：** TC-PM-001 完成（A 拥有数据源 ds-1）

1. 先 GET /v1/users/search?keyword=<B 用户名> 取 B 的 UUID（B_UID）
2. A 调 POST /v1/acls/DATA_SOURCE/{ds-1}，body：`{"principal_type":"USER","principal_id":"<B_UID>","permission":"VIEW"}`
3. 预期返回 200，返回 id 非空（ACL 记录 id）
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
2. 预期返回 200（资源 id）
3. B 刷新列表 → 数据源 ds-1 不可见
4. B GET /v1/data-sources/{ds-1} → 预期 403

---

## TC-PM-005 无权用户不能授权他人（自包含）
**级别：** P1
**前置：** 用户 A、B、C 均已注册并登录；与主线 ds-1 状态无关

1. A 创建数据源 ds-2（postgres_local）→ 记录 id
2. A 授权 B 对 ds-2 仅 VIEW（POST /v1/acls/DATA_SOURCE/{ds-2}，principal_id = B_UID）
3. B 调 POST /v1/acls/DATA_SOURCE/{ds-2} 授权给用户 C（VIEW）
4. 预期返回 403（PM001，B 无 EDIT 权限不能授权他人）
5. 顺带校验：GET /v1/acls/DATA_SOURCE/{ds-2} → 仅 1 条记录（B 的 VIEW），C 未被写入

---

## TC-PM-006 全局管理员可访问所有资源（自包含）
**级别：** P1
**前置：** admin 用户存在；用户 A 已注册并登录

1. A 创建数据源 ds-admin（postgres_local）→ 记录 id
2. admin 登录
3. GET /v1/data-sources → 列表中**包含** ds-admin（以及其他用户全部数据源）
4. GET /v1/data-sources/{ds-admin} → 200（即使未被授权）
5. admin 调 POST /v1/acls/DATA_SOURCE/{ds-admin} 授权他人 → 200（admin 可执行所有授权操作）

---

## TC-PM-007 MCP 传播校验：无权绑定他人数据源（自包含）
**级别：** P0
**前置：** 用户 A、B 均已注册并登录；与主线 ds-1 状态无关

1. A 创建数据源 ds-3（postgres_local）→ 记录 id（B 从未被授权）
2. B 创建 MCP 服务，data_scopes 绑定 ds-3
   - POST /v1/mcp-services，body 含 `{"data_scopes":[{"scope_type":"DATA_SOURCE","reference_id":"<ds-3>"}]}`
3. 预期返回 403（PM001，传播校验拦截）
4. B 创建数据源 ds-B（postgres_local）→ 记录 id
5. B 创建 MCP 服务绑定 ds-B → 预期 200（绑定自己有权限的数据源）

---

## TC-PM-008 服务授权不传播到底层数据源（自包含）
**级别：** P1
**前置：** 用户 A、C 均已注册并登录

1. A 创建数据源 ds-4（postgres_local）→ 记录 id
2. A 创建 MCP 服务 svc-1 绑定 ds-4 → 200
3. A 授权 C 对 svc-1 VIEW（POST /v1/acls/MCP_SERVICE/{svc-1}，principal_id = C_UID）
4. C 访问 svc-1 详情 GET /v1/mcp-services/{svc-1} → 200
5. C 查看数据源列表 → ds-4 **不可见**（数据源权限不随服务授权传播）
6. C GET /v1/data-sources/{ds-4} → 预期 403

---

## TC-PM-009 主题资源授权（自包含）
**级别：** P2
**前置：** 用户 A、B 均已注册并登录

1. A 创建数据源 ds-5（postgres_local）→ 记录 id
2. A 创建主题 subject-1（绑定 ds-5）→ 200
3. **先测未授权**：B GET /v1/subjects/{subject-1} → 预期 403（PM001）
4. B GET /v1/subjects 列表 → 不包含 subject-1
5. A 授权 B 对 SUBJECT 的 VIEW（POST /v1/acls/SUBJECT/{subject-1}，principal_id = B_UID）→ 200
6. B GET /v1/subjects 列表 → 包含 subject-1
7. B GET /v1/subjects/{subject-1} → 200

---

## TC-PM-011 枚举契约：大小写与非法值（自包含）
**级别：** P1
**前置：** 用户 A 已注册并登录

1. A 创建数据源 ds-6（postgres_local）→ 记录 id；GET /v1/users/search?keyword=<B 用户名> 取 B_UID
2. body 小写主体类型：POST /v1/acls/DATA_SOURCE/{ds-6}，`{"principal_type":"user","principal_id":"<B_UID>","permission":"VIEW"}` → 预期 400
3. 路径小写主体类型：DELETE /v1/acls/DATA_SOURCE/{ds-6}/user/{B_UID} → 预期 400
4. 非法主体类型：DELETE /v1/acls/DATA_SOURCE/{ds-6}/TEAM/{B_UID} → 预期 400
5. 非法资源类型：POST /v1/acls/TEAM/{ds-6}，合法 body → 预期 400
6. 合法调用不受影响：POST /v1/acls/DATA_SOURCE/{ds-6}（大写 USER + VIEW）→ 200
7. 校验 ACL 表未被污染：GET /v1/acls/DATA_SOURCE/{ds-6} → 仅 1 条记录（步骤 6 写入的）

---

## TC-PUB-001 全公开只读（GROUP/ALL_USERS）（自包含）
**级别：** P1
**前置：** 用户 A（资源 owner）、B（无关用户）均已注册并登录

1. A 创建数据源 ds-pub（postgres_local）→ 记录 id
2. 公开前：B 列表不可见 ds-pub、详情 403
3. A 调 POST /v1/acls/DATA_SOURCE/{ds-pub}，body：`{"principal_type":"GROUP","principal_id":"ALL_USERS","permission":"VIEW"}` → 200
4. 公开后：B 列表可见、详情 200（只读）
5. B 编辑 ds-pub → 403（公开只读，不授予 EDIT）
6. A 尝试公开行升级 EDIT → 400（后端拒绝）
7. GET /v1/acls/DATA_SOURCE/{ds-pub} → 列表包含 `GROUP/ALL_USERS` 行，permission = VIEW，`principal_name` 为 null（无对应用户）
8. A 调 DELETE /v1/acls/DATA_SOURCE/{ds-pub}/GROUP/ALL_USERS → 200
9. 撤销后：B 列表恢复不可见
