# 授权架构（Permission Architecture）

本文档概述 DatI 授权（Authorization）体系的设计理念、核心模型、判定链路与扩展机制。与 [authentication.md](authentication.md)（身份认证，解决"你是谁"）互补，本文解决"你能做什么"。

## 设计理念

- **身份与授权分离**：`auth` 模块管身份（用户、登录、组成员关系），`permission` 模块管授权（资源访问控制）。模块间只通过 service 层交互。
- **三层判定**：全局管理员（按用户名）→ 资源创建者（owner，自动全权）→ ACL 精确授权，短路求值。
- **级别制权限**：`VIEW < EDIT`，EDIT 隐含 VIEW，判定用覆盖（covers）而非精确匹配。
- **全公开即隐式组**：`ALL_USERS` 被建模为"所有用户隐式属于的组"，组成员解析是唯一事实来源，SQL 层不感知该特殊主体。
- **可插拔判定**：`PermissionChecker` SPI 隔离 ACL 实现，未来对接外部权限中心业务代码零改动。

## 核心模型

| 模型 | 取值 | 说明 |
|------|------|------|
| `ResourceType` | `DATA_SOURCE` / `SUBJECT` / `MCP_SERVICE` | 三类可授权资源 |
| `PrincipalType` | `USER` / `GROUP` | 主体类型；GROUP 预留团队（V1 仅支持 `ALL_USERS`） |
| `Permission` | `VIEW` / `EDIT` | 级别制：`covers(required)` 按 ordinal 判断，EDIT 覆盖 VIEW |
| `ResourceAcl` | — | ACL 领域模型（继承 `BaseResource`） |
| `PrincipalType.ALL_USERS` | `"ALL_USERS"` | 全公开组 id，**唯一来源在身份侧**（`UserGroupService.ALL_USERS`） |

**ACL 表（`resource_acl`）**：四元组唯一约束 `(resource_type, resource_id, principal_type, principal_id)`，保证同一主体对同一资源只有一条授权记录（重复授权即覆盖升级，天然幂等）。

## 核心组件

| 组件 | 职责 | 位置 |
|------|------|------|
| `PermissionService` | 判定门面：admin → owner → checker 三层短路；提供 `requireCurrentUser` 及 `requireDataSource` / `requireSubject` / `requireMcpService` 语义化断言方法（支持 ID 与 PO 实体重载，避免二次查库） | `permission/domain/service/` |
| `PermissionChecker` | 授权判定 SPI：`can(userId, resourceType, resourceId, permission)` | `permission/domain/service/` |
| `AclPermissionChecker` | 默认实现：用户个体行 → 用户所在组行，anyMatch(covers) | `permission/domain/service/` |
| `AclService` | 授权管理：grant / revoke / list，写入前校验当前用户 EDIT | `permission/domain/service/` |
| `UserGroupService` | 组成员解析（身份侧）：`groupIdsOf(userId)` → `{ALL_USERS}`（V2 合并真实组） | `auth/domain/service/` |
| `ResourceAclDAO` | ACL 持久化：派生查询（枚举参数） | `permission/repository/dao/` |
| `AclController` | REST：`/v1/acls/{type}/{resourceId}[/...]` | `permission/server/controller/` |

## 权限判定链路

### 写操作 / 详情校验（PermissionService）

```
requireCurrentUser(type, resourceId, permission, ownerId)
    │
    ▼
┌─────────────────────────────┐
│ 1. isAdmin(userName)？      │── 是 → 放行（auth.admin-users 配置）
└─────────────────────────────┘
    │ 否
    ▼
┌─────────────────────────────┐
│ 2. ownerId == userId？      │── 是 → 放行（创建者自动全权）
└─────────────────────────────┘
    │ 否
    ▼
┌─────────────────────────────┐
│ 3. checker.can(userId,...)  │── 是 → 放行；否 → 403 PM001
└─────────────────────────────┘
```

### ACL 判定（AclPermissionChecker）

```
can(userId, type, resourceId, permission)
    │
    ├─ ① 查用户个体行（USER + userId）
    │    命中且 covers → true（短路）
    │
    └─ ② 解析组成员：groupIdsOf(userId) = {ALL_USERS, ...未来组}
         查组行（GROUP + principalId IN groupIds）
         anyMatch(covers) → true / false
```

主体匹配（个体 / 所在组）是 checker 的内部逻辑，SPI 契约只接受 userId——`principal_type` 是 ACL 表的存储维度，不是判定入参。

### 列表过滤（SQL 层）

列表接口（数据源 / 主题 / MCP 服务）不走 checker，由 DAO 的 `*Accessible` 查询内联 EXISTS 子查询静默过滤：

```sql
WHERE d.createdBy = :userId                      -- owner 分支
   OR EXISTS (SELECT 1 FROM resource_acl a
              WHERE a.resource_type = 'DATA_SOURCE'
                AND a.resource_id = d.id
                AND ((a.principal_type = 'USER'  AND a.principal_id = :userId)
                     OR (a.principal_type = 'GROUP' AND a.principal_id IN :groupIds)))
```

- `:groupIds` 由 service 层先调 `UserGroupService.groupIdsOf()` 解析后传入（JPQL 无法调 Java，解析必须前置）
- 管理员走原生的无过滤查询
- **列表静默过滤（看不到 403），详情与写操作强校验（403）**——两种错误呈现策略

## 授权管理 API

| 方法 | 路径 | 描述 |
|------|------|------|
| POST | `/v1/acls/{type}/{resourceId}` | 授权（body：`principal_type` / `principal_id` / `permission`，均为枚举绑定，非法值 400） |
| GET | `/v1/acls/{type}/{resourceId}` | 授权列表（含 `principal_name`，用户已删则 null） |
| DELETE | `/v1/acls/{type}/{resourceId}/{principalType}/{principalId}` | 撤销 |
| GET | `/v1/users/search?keyword=` | 用户搜索（授权弹窗选人，最小字段） |

**写入约束（AclService）**：
- 授权 / 撤销 / 列表均要求当前用户对资源有 EDIT
- `GROUP` 主体 V1 仅接受 `ALL_USERS`，且权限仅允许 `VIEW`（全公开只读）
- `USER` 主体必须真实存在（通过 UserService 校验）

**已知设计问题**：grant 返回 ACL 记录 id，revoke 返回资源 id，响应语义不对称（待统一）。

## 分层与级联权限体系（Cascading Permissions）

平台权限以三大根资源（`DATA_SOURCE`、`SUBJECT`、`MCP_SERVICE`）为锚点。所有子资源的操作鉴权向上级联至其所属的根资源：

### 1. 资源鉴权映射表

| 根资源类型 | 覆盖操作 / 子资源 | 所需权限 | 鉴权方法 | 校验位置 |
|---|---|---|---|---|
| **DATA_SOURCE** | 数据源详情 / 元数据（schemas / tables）查询 | `VIEW` | `requireDataSource(id, VIEW)` | `DataSourceService` |
| | 数据源更新 / 删除 / 物理库断开 | `EDIT` | `requireDataSource(po, EDIT)` | `DataSourceService` |
| | 表查询（`getTables`, `getAddedTableNames`） | `VIEW` | `requireDataSource(dsId, VIEW)` | `TableService` |
| | 批量加表 / 删表 / 改表元数据 | `EDIT` | `requireDataSource(dsId, EDIT)` | `TableService` |
| | 列查询（`getColumns`） | `VIEW` | `requireDataSource(dsId, VIEW)` | `ColumnService` |
| | 列元数据更新 / 列结构同步（`syncColumns`） | `EDIT` | `requireDataSource(dsId, EDIT)` | `ColumnService` |
| | 列值查询（`getValues`） | `VIEW` | `requireDataSource(dsId, VIEW)` | `ColumnValueService` |
| | 列值抽取（`extractValues`）/ 列值保存（`saveValues`） | `EDIT` | `requireDataSource(dsId, EDIT)` | `ColumnValueService` |
| **SUBJECT** | 主题详情 / 列表 / 主题关联表查询 | `VIEW` | `requireSubject(id, VIEW)` | `SubjectService` |
| | 主题创建（校验关联数据源权限） | `VIEW (DS)` | `requireDataSource(dsId, VIEW)` | `SubjectService` |
| | 主题更新 / 删除 / 主题关联表增删 | `EDIT` | `requireSubject(po, EDIT)` | `SubjectService` |
| | 术语详情 / 列表查询 | `VIEW` | `requireSubject(subjectId, VIEW)` | `TermService` |
| | 术语创建 / 更新 / 删除 / 关联表字段绑定 | `EDIT` | `requireSubject(subjectId, EDIT)` | `TermService` |
| **MCP_SERVICE** | MCP 服务详情 / 列表 / 运行时协议调用（JSON-RPC Endpoint） | `VIEW` | `requireMcpService(id, VIEW)` / `can` | `McpServiceService`, `McpEndpointService` |
| | MCP 服务创建 / 更新 | `EDIT` + 传播校验 | `requireCurrentUser` + `requireDataSource/requireSubject(VIEW)` | `McpServiceService` |
| | 工具列表 / 提示词列表 / 发布快照查询 / 差异对比（getDiff） | `VIEW` | `requireMcpService(po, VIEW)` | `McpToolService`, `McpPromptService`, `McpServicePublishService` |
| | 工具配置 / 动态工具增删改 / 提示词增删改 / 发布回滚 | `EDIT` | `requireMcpService(po, EDIT)` | `McpToolService`, `McpPromptService`, `McpServicePublishService` |
| | 工具在线调试测试（`McpToolTestService.test`） | `EDIT` | `requireMcpService(serviceId, EDIT)` | `McpToolTestService` |
| | 数据范围保存（`saveDataScope`） | `EDIT` + 传播校验 | `requireMcpService(serviceId, EDIT)` + 绑定资源 `VIEW` | `McpServiceDataScopeService` |

### 2. 双通道数据源访问机制

为解决"用户拥有 MCP Service 权限但无底层直属数据源权限时工具无法执行"以及"绕过服务管控直接执行 SQL"的冲突，平台建立了双通道访问架构：

- **用户通道**（`DataSourceService.getDataSource`）：
  - 面向管理端用户交互 API。
  - 强制执行当前用户针对 `DATA_SOURCE` 资源的 `VIEW` 鉴权与密码脱敏。
- **内部工具执行通道**（`DataSourceService.getDataSourceInternal`）：
  - 面向 MCP 运行时执行引擎（如 `ExecuteSqlExecutor`、`ParameterizedSqlExecutor`）。
  - 解耦对底层数据源的直接用户权限强依赖，由网关在 `MCP_SERVICE` 层做 `VIEW` 鉴权，并在执行期严格由 `DataScope` 限制其可访问的表与数据源，确保权限受控且安全。

### 3. 跨资源一致性校验原则

所有接收子资源 ID（如 `columnId`）与父资源 ID（如 `datasourceId`）的接口（如 `ColumnValueService.extractValues`），必须在查出子资源后强校验其父级所属关系（`tablePO.getDataSourceId().equals(datasourceId)`），防止利用跨数据源参数组合越权读取或污染索引。

**传播校验（防间接泄露）**：创建 / 更新 / 发布 MCP 服务时，校验当前用户对每个绑定的数据源 / 主题至少 VIEW，防止把无权访问的数据打包进服务间接泄露。授权**不反向传播**：服务授权不授予底层数据源权限。

## 配置项

```yaml
auth:
  admin-users: ${ADMIN_USERS:admin}   # 全局管理员（按登录名，逗号分隔）
```

## 错误码

| 错误码 | HTTP | 说明 |
|--------|------|------|
| `PM001` | 403 | 无权限访问该资源（列表已静默过滤，此码出现在详情 / 写操作 / 授权管理） |
| `INVALID_PARAMETER` | 400 | 非法枚举值（小写或未知的 principal_type / resource type）等 |

## 扩展机制

### V2：真实用户组（团队）

已预埋的扩展点，落地时只改两处，DAO / SQL / 判定逻辑零改动：

1. `UserGroupService.groupIdsOf(userId)`：从常量 `{ALL_USERS}` 改为合并组成员表查询结果
2. `AclService.grant`：放开 GROUP 主体校验（从"仅 ALL_USERS"扩展为任意存在的组）

### 外部权限中心

实现 `PermissionChecker` 接口并注入容器即可替换本地 ACL 判定，`PermissionService` 及全部业务代码无需改动。

## 前端架构

| 组件 | 职责 | 位置 |
|------|------|------|
| `AuthDialog.vue` | 通用授权弹窗：远程搜索用户 + VIEW/EDIT 角色 + 撤销 + 公开开关 | `frontend/src/components/common/` |
| `permission.ts` | ACL API 封装（list/grant/revoke） | `frontend/src/api/` |
| `user.ts` | 用户搜索 API | `frontend/src/api/` |
| 403 拦截 | `http.ts` 拦截器：提示"没有权限"但不登出、不清 token（后端是唯一权威，列表已静默过滤） | `frontend/src/api/http.ts` |

三个列表页（数据源 / MCP 服务 / 主题）共用 `AuthDialog`，通过 `resource-type` / `resource-id` 区分授权对象；公开开关对应 `GROUP/ALL_USERS` 授权（开启有确认弹窗）。

## 测试覆盖

- **单元 / 集成**：`AclServiceTest`、`AclPermissionCheckerTest`、`PermissionServiceTest`、`ResourceAclRepositoryTest`、`UserGroupServiceTest`、三个 `*AccessibleTest`（列表过滤 SQL）
- **E2E**：`e2e-tests/test-cases/permission.md`（12 个用例：主线共享 ds-1 + 分支自包含；覆盖授权升降级、撤销、传播校验、管理员、全公开只读、枚举契约）

## 参考

- 认证架构：[authentication.md](authentication.md)
- 后端代码位置：`backend/src/main/java/com/dati/permission/`、`backend/src/main/java/com/dati/auth/domain/service/UserGroupService.java`
- E2E 用例：`e2e-tests/test-cases/permission.md`
