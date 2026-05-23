# MCP Service 管理 — 架构文档

> 版本：v1.2（对应 US-01 + US-02 + US-03）
> 最后更新：2026-05-21

---

## 1. 概述

MCP Service 管理模块提供 **MCP（Model Context Protocol）服务的生命周期管理**。

**已实现能力（US-01 + US-02）：**
- 创建服务（默认状态 `DRAFT`）、编辑基础信息、分页列表、详情查看
- Endpoint 路径展示（运行时推导）
- 数据范围配置

**US-03 能力：**
- 预置工具开关与配置
- 自定义工具 CRUD

---

## 2. 后端架构

### 2.1 分层结构

遵循项目 DDD 分层约定，模块包路径为 `com.dati.mcp`：

```
com.dati.mcp/
├── domain/
│   ├── model/           # 领域实体（McpService, McpServiceStatus, McpDataScopeType, McpServiceDataScope,
│   │                    #   McpToolType, PrebuiltToolConfig, McpCustomTool, ParamSqlConfig 等）
│   └── service/         # 业务逻辑（McpServiceService, McpServiceDataScopeService, McpToolService）
├── repository/
│   ├── dao/             # JPA Repository（McpServiceDAO, McpServiceDataScopeDAO,
│   │                    #   McpPrebuiltToolConfigDAO, McpCustomToolDAO）
│   ├── po/              # 持久化对象（McpServicePO, McpServiceDataScopePO,
│   │                    #   McpPrebuiltToolConfigPO, McpCustomToolPO）
│   └── mapper/          # PO ↔ Model 转换
└── server/
    ├── controller/      # REST API（McpServiceController, McpToolController）
    ├── pojo/            # 响应 VO（ToolsResponse, McpToolVO, ...）
    └── assembler/       # Model ↔ VO 转换 + 用户信息填充
```

### 2.2 核心类职责

#### MCP 服务本体

| 类 | 职责 |
|---|---|
| `McpService` | 领域实体，继承 `BaseResource`，当前仅扩展 `status` 字段 |
| `McpServiceStatus` | 状态枚举：`DRAFT`（草稿）→ `PUBLISHED`（已发布）→ `DISABLED`（已停用） |
| `McpServiceService` | 领域服务。创建时强制设为 `DRAFT`；更新时仅修改 `name` / `description`，不触碰状态 |
| `McpServiceDAO` | JPA Repository，提供按名称/ID 模糊搜索 + 状态筛选的分页查询 |
| `McpServiceMapper` | 静态方法，负责 `McpService` ↔ `McpServicePO` 的字段映射 |
| `McpServiceController` | REST 入口，`/v1/mcp-services` |
| `McpServiceAssembler` | 继承 `BaseAssembler`。运行时推导 `endpointPath`，从 `McpToolService` 查询 `toolCount` |
| `McpServiceVO` | 响应视图对象，含 `status`、`endpoint_path`、`tool_count` |

#### 数据范围（Data Scope）

| 类 | 职责 |
|---|---|
| `McpDataScopeType` | 范围类型枚举：`DATA_SOURCE`（数据源）、`SUBJECT`（主题） |
| `McpServiceDataScope` | 领域实体 |
| `McpServiceDataScopeService` | 业务逻辑。`saveDataScope` 全量替换；`getDataScope` 查询 |
| `McpServiceDataScopeDAO` | JPA Repository，`findByServiceId` |

#### Tool 管理（US-03）

**预置工具**

| 类 | 职责 |
|---|---|
| `McpToolType` | 枚举：`SEARCH_METADATA` / `GET_TABLE_INFO` / `EXECUTE_SQL` / `PARAMETERIZED_SQL`。含各预置工具的元数据定义（name、description、inputSchema）和默认配置 |
| `PrebuiltToolConfig` | `sealed interface`，子类：`SearchMetadataConfig`、`GetTableInfoConfig`、`ExecuteSqlConfig` |
| `PermConfig` | SQL 权限配置对象：`allowSelect`、`allowInsert`、`allowUpdate`、`allowDelete`、`allowDdl`、`allowMulti` |
| `McpPrebuiltToolConfig` | 领域实体。`serviceId` + `toolType` + `enabled` + `config`（JSON）。per-service 的差异化配置 |
| `McpPrebuiltToolConfigPO` | 持久化对象。`config` 列存 JSON 字符串 |
| `McpPrebuiltToolConfigDAO` | JPA Repository。`findByServiceIdAndToolType` |

**自定义工具**

| 类 | 职责 |
|---|---|
| `McpCustomTool` | 领域实体。`serviceId` + `name`（服务内唯一）+ `title` + `description` + `enabled` + `config`（JSON → `ParamSqlConfig`） |
| `ParamSqlConfig` | 参数化 SQL 配置：`dataSourceId`、`sqlTemplate`、`parameters[]`、`permConfig`、`timeout`、`maxRows`、`confirmRequired` |
| `McpToolParameter` | 参数定义：`name`、`type`（String/Number/Boolean/Date/Array）、`required`、`defaultValue`、`description` |
| `McpCustomToolPO` | 持久化对象。`config` 列存 JSON 字符串 |
| `McpCustomToolDAO` | JPA Repository。`findByServiceId`、`existsByServiceIdAndName`、`countByServiceId` |

**工具服务层**

| 类 | 职责 |
|---|---|
| `McpToolService` | 统一工具服务。列表返回分组响应（prebuilt + custom）。name 格式校验 |
| `McpToolController` | REST API，`/v1/mcp-services/{id}/tools`。5 个端点（见 2.4） |
| `McpToolAssembler` | Model → VO 转换。解析 config JSON |
| `McpToolVO` | 响应 VO：`id`、`tool_type`、`name`、`title`（自定义）、`description`、`enabled`、`config`（Object） |
| `ToolsResponse` | 分组响应：`{ prebuilt: List<McpToolVO>, custom: List<McpToolVO> }` |

### 2.3 数据模型关系

```
McpToolType (enum)
  ├─ SEARCH_METADATA  ─→ SearchMetadataConfig { timeout }
  ├─ GET_TABLE_INFO   ─→ GetTableInfoConfig { timeout }
  ├─ EXECUTE_SQL      ─→ ExecuteSqlConfig { permConfig, timeout, maxRows, confirmRequired }
  └─ PARAMETERIZED_SQL (不在枚举中存储默认配置，由 CustomTool 承载)

McpPrebuiltToolConfig (per service)
  ├─ serviceId
  ├─ toolType (FK → 枚举值 SEARCH_METADATA/GET_TABLE_INFO/EXECUTE_SQL)
  ├─ enabled (default true)
  └─ config (JSON, null = 使用枚举中的默认值)

McpCustomTool (per service, 多个)
  ├─ serviceId
  ├─ name (服务内唯一)
  ├─ title, description
  ├─ enabled
  └─ config (JSON → ParamSqlConfig: dataSourceId, sqlTemplate, parameters[], permConfig, timeout, maxRows, confirmRequired)
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

#### Tool 管理

| 方法 | 路径 | 说明 | 响应 |
|---|---|---|---|
| `GET` | `/v1/mcp-services/{id}/tools` | 全量列表，分组返回 | `ToolsResponse (prebuilt + custom)` |
| `PUT` | `/v1/mcp-services/{id}/tools/{toolId}` | 更新（含 enabled 开关） | `IdResponse` |
| `POST` | `/v1/mcp-services/{id}/tools` | 创建自定义工具 | `IdResponse` |
| `DELETE` | `/v1/mcp-services/{id}/tools/{toolId}` | 删除自定义工具 | `IdResponse` |
| `POST` | `/v1/mcp-services/{id}/tools/{toolId}/test` | 调试执行（US-07 实现） | — |

`toolId` 取值：
- 预置工具：`SEARCH_METADATA`、`GET_TABLE_INFO`、`EXECUTE_SQL`
- 自定义工具：DB 生成的 UUID

### 2.5 关键设计决策

#### 工具分为「预置」和「自定义」

- **预置工具**：名称、描述、inputSchema 和默认配置存储在 `McpToolType` 枚举中，全服务统一。per-service 仅记录差异化的 `enabled` 和 `config`。
- **自定义工具**：用户完整 CRUD，当前仅支持 `PARAMETERIZED_SQL`。存储在独立的 `mcp_custom_tool` 表。
- 统一 API 路径 `/tools/`，通过 `toolId` 匹配预置枚举值或 DB 记录来路由。
- API 返回分组响应 `{ prebuilt: [...], custom: [...] }`，前端直接按区域渲染。

#### 预置工具懒初始化 + 默认启用

- 新建 MCP 服务时，`mcp_prebuilt_tool_config` 表无记录。
- 查询时无记录 → 使用枚举中的默认配置，视为「已启用」。
- 用户保存配置后写入 DB 行，之后读取 DB 行为主。

#### config 单字段存多态 JSON

- 不再拆分成多个 nullable 列（`perm_config`、`timeout`、`max_rows`、`confirm_required` 等）。
- 一个 `config TEXT` 列 + Jackson 多态反序列化。按 `tool_type` 解析为不同配置类。
- 扩展新配置项无需改表，只改 Java 类。

#### annotations 和 inputSchema 不在管理端 API 返回

- `annotations` 和 `inputSchema` 由 MCP 协议适配层按需生成，管理端不需要。
- 管理端 API 返回可配置字段（name、description、enabled、config），不携带协议层字段。
- 前端 annotations 预览（配置弹窗中）纯前端根据 `perm_config` 计算。

#### 开关合并到 PUT 接口

- 不再提供独立的 toggle 端点。前端从当前数据取反 `enabled` 后直接调 `PUT`。

#### 数据源选择限定 data_scope

- 自定义工具创建时的数据源下拉列表限定为当前服务 data_scope 内的数据源。

#### endpoint_path 不存储于数据库

- MCP 服务的 endpoint 格式固定为 `/{id}/mcp`，完全由 ID 推导。

#### 状态机与更新隔离

- 创建时由后端强制写入 `DRAFT`。编辑接口仅允许修改名称和描述。

#### 数据范围全量替换策略

- `PUT /data-scope` 采用先删后插策略。

---

## 3. 前端架构

### 3.1 目录结构

```
src/
├── api/
│   ├── mcp-service.ts           # MCP 服务 API
│   ├── mcp-tool.ts              # Tool API（US-03 新增）
│   ├── datasource.ts            # 数据源 API
│   └── subject.ts               # 主题 API
├── components/mcp-service/
│   ├── McpServiceTable.vue      # 列表表格
│   ├── McpServiceDialog.vue     # 创建/编辑弹窗
│   ├── McpServiceForm.vue       # 表单（名称、描述）
│   ├── DataScopeTab.vue         # 数据范围 Tab
│   ├── ToolsTab.vue             # Tools Tab 容器（US-03）
│   ├── PrebuiltToolCard.vue     # 预置工具卡片行（US-03）
│   ├── ExecuteSqlConfigDialog.vue  # EXECUTE_SQL 权限配置弹窗（US-03）
│   ├── CustomToolTable.vue      # 自定义工具列表表格（US-03）
│   ├── CustomToolDialog.vue     # 自定义工具创建/编辑弹窗（US-03）
│   └── CustomToolForm.vue       # 自定义工具表单（US-03）
└── pages/mcp-services/
    ├── index.vue                # 列表页
    └── [id]/index.vue           # 详情页
```

### 3.2 新增组件职责（US-03）

| 组件 | 职责 |
|---|---|
| `ToolsTab` | Tools Tab 容器。加载分组数据，渲染预置工具区 + 自定义工具区。 |
| `PrebuiltToolCard` | 单个预置工具的卡片行。开关即时调 PUT 更新。EXECUTE_SQL 额外显示「配置」按钮。 |
| `ExecuteSqlConfigDialog` | EXECUTE_SQL 权限配置弹窗。勾选组 + timeout + maxRows + confirmRequired。 |
| `CustomToolTable` | 自定义工具列表表格。列：名称、描述、开关、操作。顶部新建按钮。 |
| `CustomToolDialog` | 自定义工具新建/编辑弹窗壳。校验 + API 调用。 |
| `CustomToolForm` | 自定义工具受控表单。通用配置 + 数据源选择 + SQL 模板 + 参数编辑 + 权限配置。暴露 `validate()`。 |

### 3.3 数据流

```
详情页 → ToolsTab
    ├─ loaded → api.listTools(serviceId)
    │       → 返回 { prebuilt: [...], custom: [...] }
    │       → 按分组渲染两个区域
    │
    ├─ 预置工具区：
    │    ├─ 开关切换 → api.updateTool(serviceId, "SEARCH_METADATA", {enabled: !current})
    │    └─ EXECUTE_SQL 配置按钮 → ExecuteSqlConfigDialog
    │         └─ 保存 → api.updateTool(serviceId, "EXECUTE_SQL", {config: {...}})
    │
    └─ 自定义工具区：
         ├─ 新建按钮 → CustomToolDialog (POST)
         ├─ 编辑按钮 → CustomToolDialog (PUT)
         ├─ 开关切换 → api.updateTool(serviceId, toolId, {enabled: !current})
         ├─ 删除按钮 → 确认弹窗 → api.deleteTool(serviceId, toolId)
         └─ 测试按钮 → US-07
```

### 3.4 状态展示

| 状态 | Tag 颜色 | 说明 |
|---|---|---|
| `DRAFT` | info（灰色） | 草稿 |
| `PUBLISHED` | success（绿色） | 已发布 |
| `DISABLED` | danger（红色） | 已停用 |

---

## 4. 国际化（i18n）

工具相关 Key 统一在 `mcpService.tool.*` 命名空间下：

```
mcpService:
  tool:
    prebuiltTitle: "预置工具"
    prebuiltDesc: "系统内置的 MCP 原子能力"
    customTitle: "自定义工具"
    customDesc: "参数化 SQL 模板工具"
    addCustom: "新建参数化 SQL 工具"
    typeLabel: "类型"
    type:
      SEARCH_METADATA: "元数据检索"
      GET_TABLE_INFO: "表结构查询"
      EXECUTE_SQL: "自由 SQL 执行"
      PARAMETERIZED_SQL: "参数化 SQL"
    config: "配置"
    configExecuteSql: "配置 EXECUTE_SQL 权限"
    sqlTemplate: "SQL 模板"
    parameters: "参数列表"
    paramName: "参数名"
    paramType: "类型"
    paramRequired: "必填"
    paramDefault: "默认值"
    paramDesc: "描述"
    dataSourceBinding: "绑定数据源"
    deleteConfirm: "删除后 MCP Client 将无法调用此工具，确认删除？"
    nameFormatError: "仅允许 A-Z, a-z, 0-9, _, -, .，1-128 字符"
    nameExists: "Tool 名称已被使用"
```

---

## 5. 测试

### 5.1 后端测试

| 测试类 | 类型 | 覆盖场景 |
|---|---|---|
| `McpServiceServiceTest` | 单元测试（Mockito） | 创建、更新、详情、分页（4 种筛选组合） |
| `McpServiceControllerTest` | WebMvcTest（MockMvc） | 服务 CRUD + 数据范围接口 |
| `McpServiceDataScopeServiceTest` | 单元测试（Mockito） | 保存数据范围、查询数据范围、空列表、主题展开 |
| `McpToolServiceTest` | 单元测试（Mockito） | 预置工具列表（含默认值回退）、自定义工具 CRUD、name 格式/唯一性校验 |
| `McpToolControllerTest` | WebMvcTest（MockMvc） | 全量列表（分组返回）、更新、创建、删除 |

### 5.2 前端测试

- 使用 Playwright CLI 进行端到端验证。
- 构建检查：`pnpm build`（`vue-tsc -b && vite build`）零错误。

---

## 6. 扩展预留

| 扩展点 | 当前状态 | 未来规划 |
|---|---|---|
| `tool_count` | McpToolService.countToolsByServiceId 动态统计 | ✅ |
| 发布/停用/启用 | `ElMessage.info("comingSoon")` | US-08 |
| 删除 | 前端占位，后端未实现 | US-10 |
| Resources、Prompts、Security、Debug、Logs | Tab 占位 | 后续 US |
| 自定义工具扩展 | 仅 PARAMETERIZED_SQL | 未来可扩展 API_CALL、SCRIPT 等 |

---

## 7. 关联文档

- [US-01 需求文档](../prd/us/US-01.md)
- [US-02 需求文档](../prd/us/US-02.md)
- [US-03 需求文档](../prd/us/US-03.md)
- [MCP Builder PRD](../prd/2026-05-11-mcp-builder-prd.md)
