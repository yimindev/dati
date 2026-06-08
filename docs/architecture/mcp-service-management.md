# MCP Service 管理 — 架构文档

> 版本：v1.3（US-01 + US-02 + US-03 完整实现）
> 最后更新：2026-05-24

---

## 1. 概述

MCP Service 管理模块提供 **MCP（Model Context Protocol）服务的生命周期管理**。

**已实现能力：**
- 创建服务（`DRAFT`）、编辑基础信息、分页列表、详情查看
- Endpoint 路径展示（运行时推导）
- 数据范围配置（数据源 + 主题引用模式）
- 预置工具开关与配置（SEARCH_METADATA / GET_TABLE_INFO / EXECUTE_SQL）
- 自定义工具 CRUD（参数化 SQL）

---

## 2. 后端架构

### 2.1 分层结构

```
com.dati.mcp/
├── domain/
│   ├── model/           # 领域实体（McpService, McpToolType, ToolConfig, SqlPolicy,
│   │                    #   McpPrebuiltToolConfig, McpCustomTool 等）
│   └── service/         # 业务逻辑（McpServiceService, McpServiceDataScopeService, McpToolService）
├── repository/
│   ├── dao/             # JPA Repository
│   ├── po/              # 持久化对象（McpPrebuiltToolConfigPO extends BaseResourcePO,
│   │                    #   McpCustomToolPO extends BasePO）
│   └── mapper/          # PO ↔ Model 转换
└── server/
    ├── controller/      # REST API（McpServiceController, McpToolController）
    ├── pojo/            # VO / Request（ToolsResponse, McpToolVO, CustomToolRequest）
    └── assembler/       # Model ↔ VO 转换
```

### 2.2 核心类职责

#### Tool 管理

**领域模型与枚举**

| 类 | 职责 |
|---|---|
| `McpToolType` | 枚举 4 种类型，含预置工具的元数据（name / description / inputSchema）和 `getDefaultConfig()` 方法 |
| `ToolConfig` | `sealed interface`，子类：`SearchMetadataConfig` / `GetTableInfoConfig` / `ExecuteSqlConfig` / `ParamSqlConfig`。每个子类有对应可配置字段 + Jackson 序列化默认值 |
| `SqlPolicy` | SQL 权限策略：`allowSelect` / `allowInsert` / `allowUpdate` / `allowDelete` / `allowDdl` / `allowMulti` |

**预置工具**

| 类 | 职责 |
|---|---|
| `McpPrebuiltToolConfig` | 领域实体。`serviceId` + `toolType` + `enabled` + `config: ToolConfig` |
| `McpPrebuiltToolConfigPO` | 持久化对象，继承 `BaseResourcePO`。`config` 列存 JSON 字符串 |
| `McpPrebuiltToolConfigDAO` | JPA Repository。`findByServiceIdAndToolType` |
| `McpPrebuiltToolConfigMapper` | PO ↔ Model。序列化/反序列化 `config` 字段 |

**自定义工具**

| 类 | 职责 |
|---|---|
| `McpCustomTool` | 领域实体。`serviceId` + `name` + `toolType` + `title` + `description` + `enabled` + `config: ToolConfig` |
| `McpCustomToolPO` | 持久化对象，继承 `BasePO`，手动声明 `name` / `description` 列。`config` 列存 JSON 字符串 |
| `McpCustomToolDAO` | JPA Repository。`findByServiceId`、`existsByServiceIdAndName`、`countByServiceId` |
| `McpCustomToolMapper` | PO ↔ Model。序列化/反序列化 `config` 字段 |

**服务层与 API**

| 类 | 职责 |
|---|---|
| `McpToolService` | 分组列表返回 `ToolsResult` record；`updatePrebuiltTool` / `updateCustomTool` / `createCustomTool` / `deleteCustomTool` / `countToolsByServiceId`。name 格式校验 |
| `McpToolController` | 5 端点（见 2.4），`@Valid` 校验 `CustomToolRequest.toolType` |
| `McpToolAssembler` | Model ↔ VO / Request 转换，config JSON 解析 |
| `McpToolVO` | 响应：`id`、`tool_type`、`name`、`title`、`description`、`enabled`、`config` |
| `ToolsResponse` | `{ prebuilt, custom }` |
| `CustomToolRequest` | 创建/更新请求体。`toolType: @NotNull McpToolType`，`config: String`（JSON） |

### 2.3 数据模型

```
McpToolType (enum)
  ├─ SEARCH_METADATA ─→ SearchMetadataConfig { timeout }
  ├─ GET_TABLE_INFO  ─→ GetTableInfoConfig { timeout }
  ├─ EXECUTE_SQL     ─→ ExecuteSqlConfig { sqlPolicy, timeout, maxRows, confirmRequired }
  └─ PARAMETERIZED_SQL → ParamSqlConfig { dataSourceId, sqlTemplate, parameters[], sqlPolicy, timeout, maxRows, confirmRequired }

McpPrebuiltToolConfig (per service, UNIQUE(service_id, tool_type))
  ├─ serviceId, toolType, enabled (default true), config (ToolConfig)
  └─ 懒初始化：无 DB 记录时使用 McpToolType.getDefaultConfig()

McpCustomTool (per service, 多个, UNIQUE(service_id, name))
  ├─ serviceId, name, toolType (default PARAMETERIZED_SQL), title, description, enabled, config (ToolConfig)
  └─ 完整 CRUD
```

### 2.4 API 端点

#### Tool 管理

| 方法 | 路径 | 说明 |
|---|---|---|
| `GET` | `/v1/mcp-services/{id}/tools` | 全量列表，分组返回 `{ prebuilt, custom }` |
| `PUT` | `/v1/mcp-services/{id}/tools/{toolId}` | 更新（含 enabled 开关）。`@Valid` 要求 `tool_type` 必传 |
| `POST` | `/v1/mcp-services/{id}/tools` | 创建自定义工具 |
| `DELETE` | `/v1/mcp-services/{id}/tools/{toolId}` | 删除自定义工具 |

`toolId`：预置工具用枚举值（`SEARCH_METADATA` 等），自定义工具用 UUID。

### 2.5 关键设计决策

#### 工具分为「预置」和「自定义」

- **预置工具**：名称/描述/inputSchema 存代码，per-service 仅存差异化 `enabled` + `config`。懒初始化默认启用。
- **自定义工具**：用户完整 CRUD。统一路径 `/tools/`，请求体 `tool_type` 区分预置/自定义路由。
- 开关合并到 `PUT` 接口，不再需要独立 toggle 端点。

#### config 单 JSON 字段

- Domain 层 `config: ToolConfig`（强类型），PO 层 `config TEXT`（JSON 字符串）。Mapper 负责序列化/反序列化。
- 管理端 API 不返回 `annotations` / `inputSchema`（MCP 协议层自行生成）。

#### 数据源限定 data_scope

- 自定义工具创建时的数据源选择列表限定为当前服务的 `data_scope` 内数据源。

#### JsonUtils 统一 SNAKE_CASE

- `JsonUtils` 配置 `PropertyNamingStrategies.SNAKE_CASE`，确保 config JSON 与 Spring MVC 命名一致。

---

## 3. 前端架构

### 3.1 目录结构

```
src/
├── api/
│   ├── mcp-service.ts
│   └── mcp-tool.ts              # 类型（SqlPolicy, ToolParameter, McpToolVO）+ API 函数
├── components/mcp-service/
│   ├── ToolsTab.vue             # 容器：子 Tab 切换（预置/自定义）
│   ├── PrebuiltToolList.vue     # 预置工具卡片列表（开关 + Setting 图标跳转配置弹窗）
│   ├── ExecuteSqlConfigDialog.vue  # EXECUTE_SQL 权限配置弹窗
│   ├── CustomToolList.vue       # 自定义工具列表（搜索、图标操作、数据源名称映射）
│   └── CustomToolDialog.vue     # 创建/编辑弹窗表单（el-form + FormRules 校验）
└── pages/mcp-services/
    └── [id]/index.vue           # 详情页（ToolsTab 集成在 tools Tab）
```

### 3.2 组件职责

| 组件 | 职责 |
|---|---|
| `ToolsTab` | 加载分组数据，渲染预置/自定义子区域。子 Tab 带计数。 |
| `PrebuiltToolList` | 卡片列表：名称、描述、EXECUTE_SQL 的 sql_policy meta 信息。Setting 图标打开配置弹窗。`el-switch` 开关。 |
| `ExecuteSqlConfigDialog` | 权限勾选组（SELECT ~ MULTI）+ maxRows + timeout + confirmRequired。`WarningFilled` 安全提示。 |
| `CustomToolList` | 搜索栏 + 工具列表。图标编辑/删除。显示数据源名称（通过 `getDataScope` 解析 ID→名称）。`el-switch` 开关。 |
| `CustomToolDialog` | el-dialog 弹窗表单。el-form 校验（name/desc/SQL/数据源必填）。参数编辑器。权限勾选。 |

### 3.3 交互细节

- **预置工具区**：开关 + EXECUTE_SQL 的 Setting 图标。无删除、无编辑名称。
- **自定义工具区**：Edit/Delete 图标操作。hover 变色（Edit 蓝色，Delete 红色）。
- **配置弹窗**：权限 pills 切换（选中态紫色 → 蓝色高亮）。安全警告黄色提示。
- **抽屉表单**：使用 Element Plus `el-form` 的 `FormRules` 校验，保存前 `validate()`，异常时 `clearValidate()`。
- **错误处理**：统一 `catch (e: any)` + `e?.message` 展示后端错误信息。

### 3.4 数据流

```
详情页 → ToolsTab
    ├─ GET /tools → { prebuilt, custom }
    │
    ├─ 预置：
    │    ├─ 开关 → PUT /tools/{toolType} { tool_type, enabled }
    │    └─ 配置 → PUT /tools/EXECUTE_SQL { tool_type, enabled, config: JSON.stringify({sql_policy, timeout, max_rows, confirm_required}) }
    │
    └─ 自定义：
         ├─ 新建 → POST /tools { tool_type, name, description, config: JSON.stringify({...}) }
         ├─ 编辑 → PUT /tools/{id} { tool_type, name, description, enabled, config }
         ├─ 开关 → PUT /tools/{id} { tool_type, enabled }
         └─ 删除 → DELETE /tools/{id} → 确认弹窗
```

---

## 4. 国际化（i18n）

```
mcpService.tool:
  title, subtitle
  prebuiltTitle, customTitle
  addCustom, editCustom
  searchPlaceholder, totalCount, matchCount, emptySearch, emptyCustom
  basicInfo, execConfig, security
  toolName, toolTitle, titlePlaceholder, descPlaceholder, nameFormatHint
  nameRequired, descRequired, sqlRequired, dataSourceRequired
  sqlTemplate, dataSourceBinding, selectDataSource
  parameters, addParam, noParams, paramCount, paramDesc, paramRequired
  configExecuteSql, allowedOps, sqlRiskWarning, maxRows, timeout, confirmRequired
  deleteConfirm
  type: { SEARCH_METADATA, GET_TABLE_INFO, EXECUTE_SQL, PARAMETERIZED_SQL }
```

---

## 5. 测试

| 测试类 | 覆盖 |
|---|---|
| `McpToolServiceTest` | 预置列表（默认值回退）、自定义 CRUD、name 校验、计数 |
| `McpToolControllerTest` | 分组列表、预置/自定义更新、创建、删除 |

---

## 6. 关联文档

- [US-01 需求文档](../prd/us/US-01.md)
- [US-02 需求文档](../prd/us/US-02.md)
- [US-03 需求文档](../prd/us/US-03.md)
- [MCP Builder PRD](../prd/2026-05-11-mcp-builder-prd.md)
- [US-03 实施计划](../superpowers/plans/2026-05-21-us-03-mcp-tool.md)
