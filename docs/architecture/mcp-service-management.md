# MCP Service 管理 — 架构文档

> 版本：v1.0（对应 US-01）
> 最后更新：2026-05-17

---

## 1. 概述

MCP Service 管理模块提供 **MCP（Model Context Protocol）服务的生命周期管理**。用户可创建、编辑、查看 MCP 服务实例，为后续绑定数据源、配置 Tools / Resources / Prompts 提供载体。

**核心能力：**
- 创建服务（默认状态 `DRAFT`）
- 编辑基础信息（名称、描述）
- 分页列表查询（支持关键词 + 状态筛选）
- 服务详情查看与信息编辑
- Endpoint 路径展示（运行时推导，不持久化）

---

## 2. 后端架构

### 2.1 分层结构

遵循项目 DDD 分层约定，模块包路径为 `com.dati.mcp`：

```
com.dati.mcp/
├── domain/
│   ├── model/           # 领域实体（McpService, McpServiceStatus）
│   └── service/         # 业务逻辑（McpServiceService）
├── repository/
│   ├── dao/             # JPA Repository（McpServiceDAO）
│   ├── po/              # 持久化对象（McpServicePO）
│   └── mapper/          # PO ↔ Model 转换（McpServiceMapper）
└── server/
    ├── controller/      # REST API（McpServiceController）
    ├── pojo/            # 响应 VO（McpServiceVO）
    └── assembler/       # Model ↔ VO 转换 + 用户信息填充（McpServiceAssembler）
```

### 2.2 核心类职责

| 类 | 职责 |
|---|---|
| `McpService` | 领域实体，继承 `BaseResource`，当前仅扩展 `status` 字段 |
| `McpServiceStatus` | 状态枚举：`DRAFT`（草稿）→ `PUBLISHED`（已发布）→ `DISABLED`（已停用） |
| `McpServiceService` | 领域服务。创建时强制设为 `DRAFT`；更新时仅修改 `name` / `description`，不触碰状态 |
| `McpServiceDAO` | JPA Repository，提供按名称/ID 模糊搜索 + 状态筛选的分页查询 |
| `McpServiceMapper` | 静态方法，负责 `McpService` ↔ `McpServicePO` 的字段映射（含 `MapperUtils.copyBaseInfo`） |
| `McpServiceController` | REST 入口，`/v1/mcp-services`，处理 CRUD + 分页列表 |
| `McpServiceAssembler` | 继承 `BaseAssembler`。`toMcpServiceVO()` 将 Model 转为 VO，并**运行时推导** `endpointPath` |
| `McpServiceVO` | 响应视图对象，含 `status`、`endpoint_path`、`tool_count` |

### 2.3 数据流

```
HTTP Request
    ↓
Controller (McpServiceController)
    ├─ Assembler.fillUsersFromRequest(service)   // 注入 created_by / updated_by
    ↓
Service (McpServiceService)
    ├─ create: 强制 status = DRAFT
    ├─ update: 仅更新 name / description
    ├─ get/list: 业务查询
    ↓
DAO (McpServiceDAO)  ← JPA →  DB (mcp_service 表)
    ↓
Mapper (McpServiceMapper)  // PO ↔ Model
    ↓
Service 返回 Model
    ↓
Assembler (McpServiceAssembler)  // Model → VO，注入 endpoint_path、tool_count、userName
    ↓
Controller 返回 VO / PageResponse / IdResponse
```

### 2.4 API 端点

| 方法 | 路径 | 说明 | 响应 |
|---|---|---|---|
| `POST` | `/v1/mcp-services` | 创建服务 | `IdResponse` |
| `PUT` | `/v1/mcp-services/{id}` | 更新名称/描述 | `IdResponse` |
| `GET` | `/v1/mcp-services/{id}` | 服务详情 | `McpServiceVO` |
| `GET` | `/v1/mcp-services` | 分页列表（支持 `keyword`、`status`） | `PageResponse<McpServiceVO>` |

### 2.5 关键设计决策

#### endpoint_path 不存储于数据库
- **原因**：MCP 服务的 endpoint 格式固定为 `/{id}/mcp`，完全由 ID 推导，无独立业务语义。
- **实现**：`McpServiceAssembler.toMcpServiceVO()` 运行时拼接字符串 `"/" + service.getId() + "/mcp"`。
- **优势**：避免数据冗余，删除后 ID 失效，路径自然失效，无需额外维护。

#### 状态机与更新隔离
- 创建时由后端强制写入 `DRAFT`，前端无需传状态。
- 编辑接口（`PUT`）**仅允许修改名称和描述**，状态变更（发布/停用/启用）由独立接口控制（US-02+ 实现），防止误操作。

#### 分页查询策略
- 无筛选 → `findAll(pageable)`
- 仅状态 → `findAllByStatus`
- 仅关键词 → `findAllByNameContainingOrId`
- 关键词 + 状态 → `findAllByNameContainingOrIdAndStatus`

---

## 3. 前端架构

### 3.1 目录结构

```
src/
├── api/
│   └── mcp-service.ts           # API 调用层（axios）
├── components/mcp-service/
│   ├── McpServiceTable.vue      # 列表表格（含状态标签、Endpoint、操作列）
│   ├── McpServiceDialog.vue     # 创建/编辑弹窗壳
│   └── McpServiceForm.vue       # 表单（名称、描述）
└── pages/mcp-services/
    ├── index.vue                # 列表页（搜索、筛选、分页、弹窗）
    └── [id]/index.vue           # 详情页（左侧 Tab 导航 + 右侧内容区）
```

### 3.2 组件职责

| 组件 | 职责 |
|---|---|
| `McpServiceTable` | 纯展示组件。接收 `data` 和 `loading`，发射 `detail`/`edit`/`delete` 事件。支持点击名称跳转详情、复制 Endpoint 路径。 |
| `McpServiceDialog` | 弹窗容器。负责判断新增/编辑，调用 API，成功后发射 `success` 事件通知父组件刷新列表。 |
| `McpServiceForm` | 受控表单组件。暴露 `validate()` 和 `resetValidation()` 供 Dialog 调用。 |
| `pages/mcp-services/index.vue` | 列表页容器。管理搜索关键词、状态筛选、分页状态，协调 Table、Dialog、DataTableShell。 |
| `pages/mcp-services/[id].vue` | 详情页。面包屑导航 + 左侧 Tab 菜单（8 Tabs）+ 右侧内容区。基础信息 Tab 支持编辑并保存。 |

### 3.3 数据流

```
列表页 (index.vue)
    ├─ 状态: searchKeyword, statusFilter, page, pageSize
    ├─ 调用 api.listMcpServices(...) → 更新 serviceList / total
    ├─ 点击新建/编辑 → 打开 McpServiceDialog
    │       └─ McpServiceForm 校验 → api.create/update → emit("success")
    └─ 点击行 → router.push(`/mcp-services/${id}`)

详情页 ([id].vue)
    ├─ 加载 api.getMcpService(id)
    ├─ 表单编辑 → api.updateMcpService → 重新加载详情
    └─ 面包屑点击 → router.push('/mcp-services') 返回列表
```

### 3.4 路由

| 路径 | 页面 | 说明 |
|---|---|---|
| `/mcp-services` | `pages/mcp-services/index.vue` | 列表页，侧边栏高亮 `activeMenu: /mcp-services` |
| `/mcp-services/:id` | `pages/mcp-services/[id]/index.vue` | 详情页，面包屑可返回列表 |

### 3.5 状态展示

| 状态 | Tag 颜色 | 圆点颜色 | 说明 |
|---|---|---|---|
| `DRAFT` | info（灰色） | 灰色 | 草稿，可编辑、可删除、可发布 |
| `PUBLISHED` | success（绿色） | 绿色 | 已发布，可停用，可查看 Endpoint |
| `DISABLED` | danger（红色） | 红色 | 已停用，可重新启用 |

---

## 4. 国际化（i18n）

模块名称统一使用单一 Key `mcpService.title`，多处引用：

- 侧边栏菜单：`t("mcpService.title")`
- 首页功能卡片：`label: "mcpService.title"`
- 面包屑：`t("mcpService.title")`
- 页面标题：`t("mcpService.title")`

中英双语定义于 `frontend/src/locales/zh.ts` 与 `en.ts`。

---

## 5. 测试

### 5.1 后端测试

| 测试类 | 类型 | 覆盖场景 |
|---|---|---|
| `McpServiceServiceTest` | 单元测试（Mockito） | 创建、更新（含不存在抛异常）、详情（含不存在抛异常）、分页（4 种筛选组合） |
| `McpServiceControllerTest` | WebMvcTest（MockMvc） | 创建、更新、详情返回字段校验、分页列表、带关键词+状态分页 |

### 5.2 前端测试

- 使用 Playwright CLI 进行端到端验证（登录 → 列表 → 创建 → 详情 → 返回）。
- 构建检查：`pnpm build`（`vue-tsc -b && vite build`）零错误。

---

## 6. 扩展预留

当前架构已为后续 User Story 预留扩展点：

| 扩展点 | 当前状态 | 未来规划 |
|---|---|---|
| `tool_count` | 固定返回 `0` | 关联 Tool 实体后动态统计 |
| 发布/停用/启用 | `ElMessage.info("comingSoon")` | US-02：状态变更接口 + 按钮对接 |
| 删除 | 前端占位，后端未实现 | US-10：软删除接口 |
| 数据范围、Tools、Resources、Prompts、Security、Debug、Logs | Tab 占位 | 后续 US 逐个填充 |

---

## 7. 关联文档

- [US-01 需求文档](../prd/us/US-01.md)
- [MCP Builder PRD](../prd/2026-05-11-mcp-builder-prd.md)
- [实现计划](../superpowers/plans/2026-05-15-us-01-mcp-service-crud.md)
