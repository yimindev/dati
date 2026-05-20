# MCP Service 管理 — 架构文档

> 版本：v1.1（对应 US-01 + US-02）
> 最后更新：2026-05-19

---

## 1. 概述

MCP Service 管理模块提供 **MCP（Model Context Protocol）服务的生命周期管理**。用户可创建、编辑、查看 MCP 服务实例，为后续绑定数据源、配置 Tools / Resources / Prompts 提供载体。

**核心能力：**
- 创建服务（默认状态 `DRAFT`）
- 编辑基础信息（名称、描述）
- 分页列表查询（支持关键词 + 状态筛选）
- 服务详情查看与信息编辑
- Endpoint 路径展示（运行时推导，不持久化）
- **数据范围配置：添加数据源或主题，限定服务可访问的数据**

---

## 2. 后端架构

### 2.1 分层结构

遵循项目 DDD 分层约定，模块包路径为 `com.dati.mcp`：

```
com.dati.mcp/
├── domain/
│   ├── model/           # 领域实体（McpService, McpServiceStatus, McpDataScopeType, McpServiceDataScope）
│   └── service/         # 业务逻辑（McpServiceService, McpServiceDataScopeService）
├── repository/
│   ├── dao/             # JPA Repository（McpServiceDAO, McpServiceDataScopeDAO）
│   ├── po/              # 持久化对象（McpServicePO, McpServiceDataScopePO）
│   └── mapper/          # PO ↔ Model 转换（McpServiceMapper, McpServiceDataScopeMapper）
└── server/
    ├── controller/      # REST API（McpServiceController）
    ├── pojo/            # 响应 VO（McpServiceVO, DataScopeItemVO, DataScopeResponse, DataScopeRequest）
    └── assembler/       # Model ↔ VO 转换 + 用户信息填充（McpServiceAssembler）
```

### 2.2 核心类职责

#### MCP 服务本体

| 类 | 职责 |
|---|---|
| `McpService` | 领域实体，继承 `BaseResource`，当前仅扩展 `status` 字段 |
| `McpServiceStatus` | 状态枚举：`DRAFT`（草稿）→ `PUBLISHED`（已发布）→ `DISABLED`（已停用） |
| `McpServiceService` | 领域服务。创建时强制设为 `DRAFT`；更新时仅修改 `name` / `description`，不触碰状态 |
| `McpServiceDAO` | JPA Repository，提供按名称/ID 模糊搜索 + 状态筛选的分页查询 |
| `McpServiceMapper` | 静态方法，负责 `McpService` ↔ `McpServicePO` 的字段映射（含 `MapperUtils.copyBaseInfo`） |
| `McpServiceController` | REST 入口，`/v1/mcp-services`，处理 CRUD + 分页列表 + 数据范围接口 |
| `McpServiceAssembler` | 继承 `BaseAssembler`。`toMcpServiceVO()` 将 Model 转为 VO，并**运行时推导** `endpointPath` |
| `McpServiceVO` | 响应视图对象，含 `status`、`endpoint_path`、`tool_count` |

#### 数据范围（Data Scope）

| 类 | 职责 |
|---|---|
| `McpDataScopeType` | 范围类型枚举：`DATA_SOURCE`（数据源）、`SUBJECT`（主题） |
| `McpServiceDataScope` | 领域实体。`serviceId` + `scopeType` + `referenceId` + `referenceName` |
| `McpServiceDataScopeService` | 业务逻辑。`saveDataScope` 全量替换（删除旧 + 保存新）；`getDataScope` 查询并展开主题表列表 |
| `McpServiceDataScopeDAO` | JPA Repository，`findByServiceId` 查询某服务的全部数据范围 |
| `McpServiceDataScopeMapper` | `McpServiceDataScope` ↔ `McpServiceDataScopePO` 静态映射 |
| `DataScopeItemVO` | 单个数据范围项 VO，含 `scope_type`、`reference_id`、`reference_name`、`tables` |
| `DataScopeResponse` | 数据范围查询响应：`{ items: DataScopeItemVO[] }` |
| `DataScopeRequest` | 保存请求体：`{ items: DataScopeItemVO[] }` |

### 2.3 数据流

#### MCP 服务 CRUD

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

#### 数据范围配置

```
GET /{id}/data-scope
    ↓
Controller → McpServiceDataScopeService.getDataScope(serviceId)
    ↓
DAO.findByServiceId(serviceId)  // 查询 mcp_service_data_scope 表
    ↓
Mapper.toDataScopeItemList(poList)
    ↓
若 scope_type = SUBJECT → 通过 SubjectDAO 展开表列表（仅前端展示）
    ↓
返回 DataScopeResponse { items: [...] }

PUT /{id}/data-scope
    ↓
Controller → McpServiceDataScopeService.saveDataScope(serviceId, items)
    ↓
1. DAO.deleteByServiceId(serviceId)   // 全量删除旧数据
2. Mapper.toPOList(items) → DAO.saveAll(newPOs)   // 批量保存新数据
    ↓
返回 IdResponse
```

### 2.4 API 端点

#### MCP 服务本体

| 方法 | 路径 | 说明 | 响应 |
|---|---|---|---|
| `POST` | `/v1/mcp-services` | 创建服务 | `IdResponse` |
| `PUT` | `/v1/mcp-services/{id}` | 更新名称/描述 | `IdResponse` |
| `GET` | `/v1/mcp-services/{id}` | 服务详情 | `McpServiceVO` |
| `GET` | `/v1/mcp-services` | 分页列表（支持 `keyword`、`status`） | `PageResponse<McpServiceVO>` |

#### 数据范围

| 方法 | 路径 | 说明 | 响应 |
|---|---|---|---|
| `GET` | `/v1/mcp-services/{id}/data-scope` | 查询当前数据范围 | `DataScopeResponse` |
| `PUT` | `/v1/mcp-services/{id}/data-scope` | 全量替换数据范围 | `IdResponse` |

### 2.5 关键设计决策

#### endpoint_path 不存储于数据库
- **原因**：MCP 服务的 endpoint 格式固定为 `/{id}/mcp`，完全由 ID 推导，无独立业务语义。
- **实现**：`McpServiceAssembler.toMcpServiceVO()` 运行时拼接字符串 `"/" + service.getId() + "/mcp"`。
- **优势**：避免数据冗余，删除后 ID 失效，路径自然失效，无需额外维护。

#### 状态机与更新隔离
- 创建时由后端强制写入 `DRAFT`，前端无需传状态。
- 编辑接口（`PUT`）**仅允许修改名称和描述**，状态变更（发布/停用/启用）由独立接口控制，防止误操作。

#### 数据范围全量替换策略
- `PUT /data-scope` 采用**先删后插**策略，简化前端逻辑（无需区分增删改）。
- 主题类型以**引用模式**保存，不展开为独立表。主题配置变更后，数据范围自动同步。

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
│   ├── mcp-service.ts           # API 调用层（axios）
│   ├── datasource.ts            # 数据源 API（列表查询，支持关键词分页）
│   └── subject.ts               # 主题 API（列表查询，支持关键词分页）
├── components/mcp-service/
│   ├── McpServiceTable.vue      # 列表表格（含状态标签、Endpoint、操作列）
│   ├── McpServiceDialog.vue     # 创建/编辑弹窗壳
│   ├── McpServiceForm.vue       # 表单（名称、描述）
│   └── DataScopeTab.vue         # 数据范围 Tab（列表 + 添加弹窗）
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
| `DataScopeTab` | 数据范围管理组件。展示当前数据范围列表（数据源/主题），支持删除；提供添加弹窗（Tab 切换类型 + 搜索 + checkbox 列表 + 分页 + 全选）。 |
| `pages/mcp-services/index.vue` | 列表页容器。管理搜索关键词、状态筛选、分页状态，协调 Table、Dialog、DataTableShell。 |
| `pages/mcp-services/[id].vue` | 详情页。面包屑导航 + 左侧 Tab 菜单（8 Tabs）+ 右侧内容区。基础信息 Tab 支持编辑并保存。 |

### 3.3 DataScopeTab 组件设计

**列表区：**
- 顶部标题 + "添加数据范围"按钮
- 每项显示：图标 + 名称 + 类型标签（数据源/主题）+ 引用 ID + 删除按钮
- 已发布服务显示黄色提示条

**添加弹窗：**
- 顶部 pill 按钮组切换数据源/主题
- 搜索框（300ms 防抖，关键词搜索）
- 表格化列表：checkbox + 名称（粗体）+ ID（等宽）+ 描述 + 状态标签
- 当前页全选 checkbox
- 已添加项：置灰 + 不可选 + "已添加"标签
- 已选项：蓝色高亮 + "已选择"标签
- 分页："第 X–Y 条，共 Z 条" + 上/下页
- 底部：左侧计数 + 右侧取消/确认

### 3.4 数据流

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
    ├─ 保存按钮：仅当内容变更（isDirty）时才可点击
    ├─ 服务概览：简洁元信息展示（ID、Tool 数、Endpoint、更新时间），无图标
    ├─ 数据范围 Tab → DataScopeTab
    │       ├─ 加载 api.getDataScope(serviceId)
    │       ├─ 删除某一项 → api.saveDataScope（过滤后全量替换）
    │       └─ 添加弹窗 → 选择数据源/主题 → api.saveDataScope（追加后全量替换）
    └─ 面包屑点击 → router.push('/mcp-services') 返回列表
```

### 3.5 路由

| 路径 | 页面 | 说明 |
|---|---|---|
| `/mcp-services` | `pages/mcp-services/index.vue` | 列表页，侧边栏高亮 `activeMenu: /mcp-services` |
| `/mcp-services/:id` | `pages/mcp-services/[id]/index.vue` | 详情页，面包屑可返回列表 |

### 3.6 状态展示

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

数据范围相关 Key 统一在 `mcpService.dataScope.*` 命名空间下：

```
mcpService:
  dataScope:
    subtitle: "为当前服务配置可访问的数据源和主题"
    addScope: "添加数据范围"
    typeDataSource: "数据源"
    typeSubject: "主题"
    addDialogTitle: "添加数据范围"
    deleteConfirm: "确定删除此项吗？"
    publishedHint: "已发布服务修改数据范围后需重新发布才生效"
    empty: "暂无数据范围"
    searchDataSource: "搜索数据源..."
    searchSubject: "搜索主题..."
    alreadyAdded: "已添加"
    selected: "已选择"
    selectFirst: "请至少选择一项"
    noResults: "未找到匹配项"
    confirmAdd: "确认添加 ({count})"
    selectedCount: "已选择 {count} 项"
    noSelection: "未选择任何项"
    showingRange: "第 {from}–{to} 条，共 {total} 条"
    pageText: "第 {page} 页 / 共 {total} 页"
    prevPage: "上一页"
    nextPage: "下一页"
```

中英双语定义于 `frontend/src/locales/zh.ts` 与 `en.ts`。

---

## 5. 测试

### 5.1 后端测试

| 测试类 | 类型 | 覆盖场景 |
|---|---|---|
| `McpServiceServiceTest` | 单元测试（Mockito） | 创建、更新（含不存在抛异常）、详情（含不存在抛异常）、分页（4 种筛选组合） |
| `McpServiceControllerTest` | WebMvcTest（MockMvc） | 创建、更新、详情返回字段校验、分页列表、带关键词+状态分页 |
| `McpServiceDataScopeServiceTest` | 单元测试（Mockito） | 保存数据范围（全量替换）、查询数据范围（含主题展开表列表）、空列表保存、主题表列表展开 |
| `McpServiceControllerTest`（扩展） | WebMvcTest（MockMvc） | 数据范围查询、数据范围保存 |

### 5.2 前端测试

- 使用 Playwright CLI 进行端到端验证（登录 → 列表 → 创建 → 详情 → 返回）。
- 构建检查：`pnpm build`（`vue-tsc -b && vite build`）零错误。

---

## 6. 扩展预留

当前架构已为后续 User Story 预留扩展点：

| 扩展点 | 当前状态 | 未来规划 |
|---|---|---|
| `tool_count` | 固定返回 `0` | 关联 Tool 实体后动态统计 |
| 发布/停用/启用 | `ElMessage.info("comingSoon")` | US-02+：状态变更接口 + 按钮对接 |
| 删除 | 前端占位，后端未实现 | US-10：软删除接口 |
| Tools、Resources、Prompts、Security、Debug、Logs | Tab 占位 | 后续 US 逐个填充 |
| 数据范围 | ✅ 已实现（数据源 + 主题） | 未来如需支持直接表选择，需扩展 `McpDataScopeType` 枚举 |

---

## 7. 关联文档

- [US-01 需求文档](../prd/us/US-01.md)
- [US-02 需求文档](../prd/us/US-02.md)
- [MCP Builder PRD](../prd/2026-05-11-mcp-builder-prd.md)
- [US-01 实现计划](../superpowers/plans/2026-05-15-us-01-mcp-service-crud.md)
- [US-02 实现计划](../superpowers/plans/2026-05-17-us-02-data-scope.md)
