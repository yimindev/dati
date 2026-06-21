# MCP Service 管理 — 架构文档

> 版本：v1.5（US-01 + US-02 + US-03 + US-05 + US-5.5 完整实现，含 Service Code、Data Scope、Prompt 管理、Template Preview 引擎、SQL 安全分析）
> 最后更新：2026-06-21

---

## 1. 概述

MCP Service 管理模块提供 **MCP（Model Context Protocol）服务的生命周期管理**。

**已实现能力：**
- 创建服务（`DRAFT`）、编辑基础信息、分页列表、详情查看
- Service Code（唯一标识）+ Endpoint 路径运行时推导（`/{code}/mcp`）
- 数据范围配置（数据源 + 主题引用模式，全量替换保存）
- 预置工具开关与配置（SEARCH_METADATA / GET_TABLE_INFO / EXECUTE_SQL）
- 自定义工具 CRUD（参数化 SQL）
- **Prompt 模板管理**（CRUD + 模板语法校验 + 参数一致性检查）
- **模板预览引擎**（TEXT 模式渲染 / SQL 模式渲染 + 参数提取）
- **SQL 安全配置组件**（权限勾选、限流、确认执行）
- **SQL 安全分析引擎**（操作类型识别、表提取、多语句检测、事务/元数据/SET 分类）

---

## 2. 后端架构

### 2.1 分层结构

```
com.dati.mcp/
├── domain/
│   ├── model/               # 领域实体与枚举
│   │   ├── McpService.java          # 服务聚合（code, status）
│   │   ├── McpServiceStatus.java    # DRAFT / PUBLISHED / DISABLED
│   │   ├── McpServiceDataScope.java # 数据范围实体
│   │   ├── McpDataScopeType.java    # DATA_SOURCE / SUBJECT
│   │   ├── McpToolType.java         # 工具类型枚举（4 种）
│   │   ├── ToolConfig.java          # sealed interface 配置体系
│   │   ├── SqlPolicy.java           # SQL 权限策略
│   │   ├── ToolParameter.java       # 工具参数描述
│   │   ├── McpPrebuiltToolConfig.java
│   │   ├── McpCustomTool.java
│   │   ├── McpPrompt.java           # Prompt 实体
│   │   ├── PromptParameter.java     # Prompt 参数描述
│   │   └── TemplateRenderMode.java  # TEXT / SQL
│   └── service/
│       ├── McpServiceService.java           # 服务 CRUD + code 校验
│       ├── McpServiceDataScopeService.java  # 数据范围全量替换
│       ├── McpToolService.java               # 工具 CRUD + 分组
│       ├── McpPromptService.java             # Prompt CRUD + 模板校验
│       └── ToolsResult.java                  # { prebuilt, custom } record
├── repository/
│   ├── dao/
│   │   ├── McpServiceDAO.java
│   │   ├── McpServiceDataScopeDAO.java
│   │   ├── McpPrebuiltToolConfigDAO.java
│   │   ├── McpCustomToolDAO.java
│   │   └── McpPromptDAO.java
│   ├── po/                  # 持久化对象（继承 BasePO / BaseResourcePO）
│   │   ├── McpServicePO.java
│   │   ├── McpServiceDataScopePO.java
│   │   ├── McpPrebuiltToolConfigPO.java
│   │   ├── McpCustomToolPO.java
│   │   └── McpPromptPO.java
│   └── mapper/              # 静态方法 PO ↔ Model（含 JSON 序列化/反序列化）
│       ├── McpServiceMapper.java
│       ├── McpServiceDataScopeMapper.java
│       ├── McpPrebuiltToolConfigMapper.java
│       ├── McpCustomToolMapper.java
│       └── McpPromptMapper.java
└── server/
    ├── controller/
    │   ├── McpServiceController.java      # 服务 CRUD + 数据范围端点
    │   ├── McpToolController.java         # 工具 CRUD 端点
    │   ├── McpPromptController.java       # Prompt CRUD 端点
    │   └── TemplatePreviewController.java # 模板预览/提取端点
    ├── pojo/                 # VO / Request / Response
    │   ├── McpServiceVO.java, DataScopeItemVO.java, DataScopeRequest.java,
    │   │   DataScopeResponse.java
    │   ├── McpToolVO.java, ToolsResponse.java, CustomToolRequest.java
    │   ├── McpPromptVO.java, McpPromptRequest.java
    │   └── TemplatePreviewRequest.java, TemplatePreviewResponse.java,
    │       TemplateExtractRequest.java, TemplateExtractResponse.java
    └── assembler/            # @Component extends BaseAssembler, Model ↔ VO
        ├── McpServiceAssembler.java
        ├── McpToolAssembler.java
        └── McpPromptAssembler.java
```

### 2.2 核心类职责

#### Service 管理

| 类 | 职责 |
|---|---|
| `McpService` | 领域实体，继承 `BaseResource`。关键字段：`code`（唯一标识）、`status` |
| `McpServiceStatus` | 枚举：`DRAFT` / `PUBLISHED` / `DISABLED` |
| `McpServicePO` | 持久化对象，继承 `BasePO`。数据库约束：`code` 唯一索引 |
| `McpServiceDAO` | JPA Repository。`existsByCode`、多条件模糊分页查询 |
| `McpServiceService` | 创建时校验 code 格式（正则 `^[a-z0-9]([a-z0-9_-]{0,62}[a-z0-9])?$`）和唯一性；分页列表支持 keyword + status 过滤 |
| `McpServiceAssembler` | Model→VO。推导 `endpointPath = "/{code}/mcp"`；统计 `toolCount` 调用 `McpToolService.countToolsByServiceId()` |
| `McpServiceVO` | 响应：`code`、`status`、`endpoint_path`、`tool_count` |

#### 数据范围管理（Data Scope）

| 类 | 职责 |
|---|---|
| `McpDataScopeType` | 枚举：`DATA_SOURCE`（数据源）、`SUBJECT`（主题） |
| `McpServiceDataScope` | 领域实体。`serviceId` + `scopeType` + `referenceId` + `referenceName` |
| `McpServiceDataScopePO` | 持久化对象，继承 `BasePO` |
| `McpServiceDataScopeDAO` | JPA Repository。`findAllByServiceId`、`deleteAllByServiceId` |
| `McpServiceDataScopeMapper` | PO ↔ Model 转换 |
| `McpServiceDataScopeService` | 全量替换：先 `deleteAllByServiceId` 再 `saveAll`。空列表即清空 |
| `DataScopeRequest` | 请求体：`{ items: [{ scopeType, referenceId, referenceName }] }` |
| `DataScopeResponse` | 响应：`{ items: [...] }` |

#### Tool 管理

| 类 | 职责 |
|---|---|
| `McpToolType` | 枚举 4 种类型，含预置工具的元数据（name / description / inputSchema）和 `getDefaultConfig()` 方法 |
| `ToolConfig` | `sealed interface`，子类：`SearchMetadataConfig` / `GetTableInfoConfig` / `ExecuteSqlConfig` / `ParamSqlConfig`。每个子类有对应可配置字段 + Jackson 序列化默认值 |
| `SqlPolicy` | SQL 权限策略（9 字段）：`allowSelect` / `allowInsert` / `allowUpdate` / `allowDelete` / `allowDdl` / `allowMulti` / `allowMetadata` / `allowTransaction` / `allowSet`。提供 `allows(type)` 和 `validateAllowed(result)` |
| `ToolParameter` | 工具参数描述：`name`、`type`（String/Number/Boolean/Date/Array）、`required`、`defaultValue`、`description` |
| `McpPrebuiltToolConfig` | 领域实体。`serviceId` + `toolType` + `enabled` + `config: ToolConfig` |
| `McpPrebuiltToolConfigPO` | 持久化对象，继承 `BaseResourcePO`。`config` 列存 JSON 字符串 |
| `McpPrebuiltToolConfigDAO` | JPA Repository。`findByServiceIdAndToolType` |
| `McpPrebuiltToolConfigMapper` | PO ↔ Model。序列化/反序列化 `config` 字段 |
| `McpCustomTool` | 领域实体。`serviceId` + `name` + `toolType` + `title` + `description` + `enabled` + `config: ToolConfig` |
| `McpCustomToolPO` | 持久化对象，继承 `BasePO`，手动声明 `name` / `description` 列。`config` 列存 JSON 字符串 |
| `McpCustomToolDAO` | JPA Repository。`findByServiceId`、`existsByServiceIdAndName`、`countByServiceId` |
| `McpCustomToolMapper` | PO ↔ Model。序列化/反序列化 `config` 字段 |
| `McpToolService` | 分组列表返回 `ToolsResult` record；`updatePrebuiltTool` / `updateCustomTool` / `createCustomTool` / `deleteCustomTool` / `countToolsByServiceId`。name 格式校验 |
| `McpToolController` | 5 端点（见 2.4），`@Valid` 校验 `CustomToolRequest.toolType` |
| `McpToolAssembler` | Model ↔ VO / Request 转换，config JSON 解析 |
| `McpToolVO` | 响应：`id`、`tool_type`、`name`、`title`、`description`、`enabled`、`config` |
| `ToolsResponse` | `{ prebuilt, custom }` |
| `CustomToolRequest` | 创建/更新请求体。`toolType: @NotNull McpToolType`，`config: String`（JSON） |

#### Prompt 管理

| 类 | 职责 |
|---|---|
| `McpPrompt` | 领域实体，继承 `BaseResource`。`serviceId` + `name` + `enabled` + `content`（模板字符串）+ `parameters: List<PromptParameter>` |
| `PromptParameter` | VO：`name`、`description`、`required` |
| `McpPromptPO` | 持久化对象。数据库约束：`UNIQUE(service_id, name)`。`parameters` 列存 JSON 字符串 |
| `McpPromptDAO` | JPA Repository。`findAllByServiceIdOrderByCreatedAtDesc`、`findByServiceIdAndId`、`existsByServiceIdAndName`、`existsByServiceIdAndNameAndIdNot` |
| `McpPromptMapper` | PO ↔ Model。`parameters` JSON ↔ `List<PromptParameter>` |
| `McpPromptAssembler` | Model ↔ VO / Request |
| `McpPromptService` | CRUD 完整实现。**核心逻辑**：创建/更新时进行模板语法校验和参数双向一致性检查 |
| `McpPromptController` | 4 端点（见 2.4） |
| `McpPromptVO` | 响应：`id`、`serviceId`、`name`、`description`、`enabled`、`content`、`parameters` |
| `McpPromptRequest` | 请求体：`name`、`description`、`enabled`、`content`、`parameters` |

**Prompt 模板校验逻辑 (`McpPromptService.validateContentParams`)：**

1. **语法校验**：使用 `TemplateParser` 解析 `content`，捕获 `TemplateParseException` → 返回 `MS_TEMPLATE_SYNTAX_ERROR`
2. **参数一致性（双向检查）**：
   - `content` 中 `{{var}}` 提取的变量集 vs `parameters` 中定义的参数名集
   - 定义但未引用的参数 → 抛出 `MS_PROMPT_ARG_MISMATCH`（Unused parameter）
   - 引用但未定义的变量 → 抛出 `MS_PROMPT_ARG_MISMATCH`（Unknown parameter）

#### SQL 安全分析（SqlAnalyzer）

`com.dati.db.analysis` — 数据库层的静态 SQL 分析工具，在 MCP 工具执行前对模板渲染后的 SQL 做安全分析，供权限管控使用。依赖 JSqlParser 5.1 做 AST 解析。

| 类 | 职责 |
|---|---|
| `SqlAnalyzer` | `public final class` + 私有构造器，纯静态工具。唯一入口：`SqlAnalyzer.analyze(String sql) → SqlAnalysisResult` |
| `SqlOperationType` | 枚举 9 种操作类型：`SELECT`、`INSERT`、`UPDATE`、`DELETE`、`DDL`、`METADATA`（SHOW/DESCRIBE/EXPLAIN）、`TRANSACTION`（COMMIT/ROLLBACK）、`SET_STATEMENT`、`MULTI`（多语句标记）、`OTHER`（兜底拒绝）|
| `SqlAnalysisResult` | Record：`type`（快捷分发）、`statementTypes`（逐条校验完整列表）、`tables`（所有表引用并集）。提供 `isMulti()` 便捷方法 |
| `TableRef` | Record：`schema`（`@Nullable`）、`name`。支持 `qualifiedName()` |

**核心分析能力：**

| 能力 | 实现 |
|------|------|
| **操作类型识别** | `detectOperationType(Statement)` — `instanceof` 分发：`Select`→SELECT, `Insert`→INSERT, `Update`→UPDATE, `Delete`→DELETE, DDL 类→DDL, SHOW/DESCRIBE/EXPLAIN→METADATA, `Commit`/`RollbackStatement`→TRANSACTION, `SetStatement`→SET_STATEMENT, 其余→OTHER |
| **表引用提取** | `SchemaAwareTableFinder extends TablesNamesFinder<Void>` — 遍历 SQL AST，提取所有表引用（含子查询、JOIN、CTE 中实际表，排除 CTE 定义名）。使用 `getUnquotedSchemaName()` / `getUnquotedName()` 保留完整 schema 信息 |
| **多语句检测** | `parseStatements()` 解析→过滤空语句→`types.size()>1` 时 `type=MULTI`，`statementTypes` 返回每条语句的独立类型 |
| **事务语句预扫描** | JSqlParser 5.1 无法解析 `BEGIN` / `START TRANSACTION`。`analyze()` 在两轮标准解析均失败后，用正则检测并分号拆句逐段处理 |
| **容错处理** | 解析失败不抛异常，返回 `type=OTHER`。空输入/乱码同理 |

**调用方使用模式：**

```java
var result = SqlAnalyzer.analyze(preparedSql.sql());

if (result.isMulti()) {
    for (var t : result.statementTypes()) policy.assertAllowed(t);
} else {
    policy.assertAllowed(result.type());
}
scope.validate(result.tables());
```

**设计决策：**
- 静态工具类而非 Spring Bean — 纯函数无状态，与 `JsonUtils` 风格一致
- `MULTI` 仅作为 `SqlAnalysisResult.type` 标记值，不在 `statementTypes` 列表中出现
- `METADATA` / `OTHER` 默认拒绝 — 元数据查询应走 `SEARCH_METADATA` 工具，非标准 SQL 一律拒绝
- 74 条参数化单元测试覆盖所有类型识别、表提取、多语句、注释绕过、事务预扫描场景

#### 模板预览引擎（Template Preview）

| 类 | 职责 |
|---|---|
| `TemplateRenderMode` | 枚举：`TEXT` / `SQL` |
| `TemplatePreviewController` | 2 端点：`POST /v1/template/preview` 渲染、`POST /v1/template/extract` 提取变量 |
| `TemplatePreviewRequest` | `mode`（TEXT/SQL）+ `template` + `values: Map<String,Object>` |
| `TemplatePreviewResponse` | `{ rendered: String }` |
| `TemplateExtractRequest` | `{ template: String }` |
| `TemplateExtractResponse` | `{ variables: String[] }` |

**渲染引擎（`com.dati.common.template` 包）：**

| 类/接口 | 职责 |
|---|---|
| `TemplateParser` | 接口：`parse(template) → CompiledTemplate` |
| `HandlebarsStyleParser` | 实现：解析 Handlebars 风格模板（`{{var}}`、`{{{var}}}`、`{{#if}}` / `{{/if}}`、`{{#where}}` / `{{/where}}`、默认值 `{{var:default}}`、转义 `\\{{}}`） |
| `CompiledTemplate` | 接口：`getVariables() → Set<String>` |
| `TextRenderer` | 接口：`render(compiled, values) → String` |
| `TextRendererImpl` | 实现：TEXT 模式渲染，直接替换变量值为字符串 |
| `SqlRenderer` | 接口：`render(compiled, values) → PreparedSql` |
| `SqlRendererImpl` | 实现：SQL 模式渲染，按值类型格式化（字符串加引号、数值/布尔值不加、null→NULL、数组展开），返回 `PreparedSql` |
| `PreparedSql` | Record：`{ sql: String, bindings: List<ParamBinding> }` |
| `ParamBinding` | Record：`{ index, value, type }` |
| `Node` / `TextNode` / `VarNode` / `IfNode` / `WhereNode` | AST 节点 |

**SQL 模式渲染特性：**
- `{{var}}`：值按类型格式化（字符串 `'value'`、数值 `42`、布尔 `true`、null `NULL`、数组 `(1, 2, 3)`）
- `{{{var}}}`：原始变量直接内联（用于表名、列名等标识符），数组类型拒绝
- `{{var:default}}`：无值时使用默认值
- `{{#if var}}...{{/if}}`：条件块（值不为 null/false/空列表时渲染）
- `{{#where}}...{{/where}}`：WHERE 子句自动拼接（添加 `WHERE` 前缀、`AND` 连接非空条件）

### 2.3 数据模型

```
McpService
  ├─ id (UUID), code (唯一), name, description, status (DRAFT|PUBLISHED|DISABLED)
  └─ endpointPath = "/{code}/mcp" (运行时推导，不存 DB)

McpServiceDataScope (per service, 多个)
  ├─ serviceId, scopeType (DATA_SOURCE|SUBJECT), referenceId, referenceName
  └─ 全量替换：先删后插

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

McpPrompt (per service, 多个, UNIQUE(service_id, name))
  ├─ serviceId, name, enabled (default true), content (模板字符串), parameters (JSON)
  └─ 完整 CRUD + 模板语法校验 + 参数一致性检查
```

### 2.4 API 端点

#### Service 管理

| 方法 | 路径 | 说明 |
|---|---|---|
| `POST` | `/v1/mcp-services` | 创建服务（需传 `code`，自动设为 `DRAFT`） |
| `PUT` | `/v1/mcp-services/{id}` | 编辑基础信息（name / description） |
| `GET` | `/v1/mcp-services/{id}` | 详情（含 `endpoint_path`、`tool_count`） |
| `GET` | `/v1/mcp-services` | 分页列表，支持 `keyword`、`status` 过滤 |
| `GET` | `/v1/mcp-services/{id}/data-scope` | 查询数据范围列表 |
| `PUT` | `/v1/mcp-services/{id}/data-scope` | 全量保存数据范围 |

#### Tool 管理

| 方法 | 路径 | 说明 |
|---|---|---|
| `GET` | `/v1/mcp-services/{id}/tools` | 全量列表，分组返回 `{ prebuilt, custom }` |
| `PUT` | `/v1/mcp-services/{id}/tools/{toolId}` | 更新（含 enabled 开关）。`@Valid` 要求 `tool_type` 必传 |
| `POST` | `/v1/mcp-services/{id}/tools` | 创建自定义工具 |
| `DELETE` | `/v1/mcp-services/{id}/tools/{toolId}` | 删除自定义工具 |

`toolId`：预置工具用枚举值（`SEARCH_METADATA` 等），自定义工具用 UUID。

#### Prompt 管理

| 方法 | 路径 | 说明 |
|---|---|---|
| `GET` | `/v1/mcp-services/{serviceId}/prompts` | 按创建时间倒序列出所有 Prompt |
| `POST` | `/v1/mcp-services/{serviceId}/prompts` | 创建 Prompt（含模板语法校验） |
| `PUT` | `/v1/mcp-services/{serviceId}/prompts/{promptId}` | 更新 Prompt（支持 enabled 开关） |
| `DELETE` | `/v1/mcp-services/{serviceId}/prompts/{promptId}` | 删除 Prompt |

#### 模板预览

| 方法 | 路径 | 说明 |
|---|---|---|
| `POST` | `/v1/template/preview` | 渲染模板（TEXT 或 SQL 模式），需指定 `mode` + `template` + `values` |
| `POST` | `/v1/template/extract` | 提取模板中的所有变量名，返回 `{ variables: [...] }` |

### 2.5 错误码（McpService 模块）

| Code | 含义 |
|---|---|
| `MS001` | 服务未找到 (404) |
| `MS002` | 工具未找到 (404) |
| `MS003` | 工具名称已存在 (409) |
| `MS004` | 工具名称格式无效 (400) |
| `MS005` | Prompt 未找到 (404) |
| `MS006` | Prompt 名称已存在 (409) |
| `MS007` | Prompt 参数不匹配 (400) |
| `MS008` | 模板语法错误 (400) |
| `MS009` | 工具参数不匹配 (400) |
| `MS010` | 服务 code 已存在 (409) |
| `MS011` | 服务 code 格式无效 (400) |
| `MS012` | 服务 code 必填 (400) |
| `MS013` | SQL 操作违反策略 (403) |

### 2.6 关键设计决策

#### Service Code 与 Endpoint

- Service Code 作为唯一标识，格式要求：小写字母、数字、连字符、下划线，1-64 字符。
- Endpoint 路径运行时推导为 `/{code}/mcp`，不存 DB，始终反映最新 code。

#### 工具分为「预置」和「自定义」

- **预置工具**：名称/描述/inputSchema 存代码，per-service 仅存差异化 `enabled` + `config`。懒初始化默认启用。
- **自定义工具**：用户完整 CRUD。统一路径 `/tools/`，请求体 `tool_type` 区分预置/自定义路由。
- 开关合并到 `PUT` 接口，不再需要独立 toggle 端点。

#### config 单 JSON 字段

- Domain 层 `config: ToolConfig`（强类型），PO 层 `config TEXT`（JSON 字符串）。Mapper 负责序列化/反序列化。
- 管理端 API 不返回 `annotations` / `inputSchema`（MCP 协议层自行生成）。

#### JsonUtils 统一 SNAKE_CASE

- `JsonUtils` 配置 `PropertyNamingStrategies.SNAKE_CASE`，确保 config JSON 与 Spring MVC 命名一致。

#### 数据范围全量替换

- 数据范围采用「先删后插」策略，客户端每次提交完整列表。空列表即清空。
- 自定义工具创建时的数据源选择列表限定为当前服务的 `data_scope` 内数据源。

#### Prompt 参数双向校验

- `content` 中的 `{{var}}` 变量必须与 `parameters` 定义一一对应，**多余和缺失均拒绝**。
- `\\{{var}}` 转义语法不会被视为变量引用。

#### SQL 安全分析引擎独立于 MCP 模块

- `com.dati.db.analysis` 包是数据库层的 SQL 分析工具，不依赖 MCP 模块。
- 依赖 JSqlParser 5.1 做 AST 解析，支持操作类型识别、表引用提取、多语句检测。
- 解析失败不抛异常，统一返回 `OTHER` 类型。调用方必须显式拒绝 `OTHER`。
- `BEGIN` / `START TRANSACTION` 因 JSqlParser 语法限制无法解析，通过正则预扫描补足。

#### 模板引擎独立于 MCP 模块

- `com.dati.common.template` 包是通用模板引擎，不依赖 MCP 模块。
- 支持 TEXT 和 SQL 两种渲染模式，SQL 模式下对参数值按类型做安全格式化。

---

## 3. 前端架构

### 3.1 目录结构

```
src/
├── api/
│   ├── mcp-service.ts              # McpServiceVO + DataScope 类型与 API
│   ├── mcp-tool.ts                 # 工具类型（SqlPolicy, ToolParameter, McpToolVO）+ API
│   ├── mcp-prompt.ts               # Prompt 类型（McpPromptVO, McpPromptPayload）+ API
│   └── template-preview.ts         # 模板预览/提取 API
├── components/mcp-service/
│   ├── ToolsTab.vue                # 容器：子 Tab 切换（预置/自定义）
│   ├── PrebuiltToolList.vue        # 预置工具卡片列表
│   ├── ExecuteSqlConfigDialog.vue  # EXECUTE_SQL 权限配置弹窗
│   ├── CustomToolList.vue          # 自定义工具列表
│   ├── CustomToolDialog.vue        # 创建/编辑弹窗表单
│   ├── DataScopeTab.vue            # 数据范围管理页面
│   ├── PromptsTab.vue              # Prompt 管理 Tab 容器
│   ├── PromptList.vue              # Prompt 列表（搜索、开关、编辑、删除）
│   ├── PromptDialog.vue            # Prompt 创建/编辑弹窗（含模板编辑器、参数提取）
│   ├── TemplatePreviewDialog.vue   # 模板预览弹窗（TEXT/SQL 双模式）
│   └── SqlSecurityConfig.vue       # SQL 安全配置组件（权限 pill + 限流 + 确认）
├── components/common/editors/
│   ├── PromptTemplateEditor.vue    # CodeMirror 编辑器（模板语法高亮 + 智能补全）
│   └── SqlTemplateEditor.vue       # CodeMirror 编辑器（SQL 语法高亮 + 模板补全）
├── utils/codemirror/
│   ├── template-decorations.ts     # {{var}} / {{#if}} 语法高亮装饰器
│   ├── sql-highlight.ts            # SQL 自定义高亮样式
│   └── completions/                # 模板自动补全
│       ├── template-completions.ts
│       ├── template-completions.test.ts
│       ├── template-auto-close.ts
│       └── template-auto-close.test.ts
└── pages/mcp-services/
    └── [id]/index.vue              # 详情页（左侧菜单：Basic / Data Scope / Tools / Prompts / ...）
```

### 3.2 组件职责

| 组件 | 职责 |
|---|---|
| `ToolsTab` | 加载分组数据，渲染预置/自定义子区域。子 Tab 带计数。 |
| `PrebuiltToolList` | 卡片列表：名称、描述、EXECUTE_SQL 的 sql_policy meta 信息。Setting 图标打开配置弹窗。`el-switch` 开关。 |
| `ExecuteSqlConfigDialog` | 权限勾选组（SELECT ~ MULTI）+ maxRows + timeout + confirmRequired。`WarningFilled` 安全提示。 |
| `CustomToolList` | 搜索栏 + 工具列表。图标编辑/删除。显示数据源名称（通过 `getDataScope` 解析 ID→名称）。`el-switch` 开关。 |
| `CustomToolDialog` | el-dialog 弹窗表单。el-form 校验（name/desc/SQL/数据源必填）。参数编辑器 + 权限勾选。 |
| `DataScopeTab` | 数据范围管理。显示已添加列表（数据源/主题 Tag），支持删除。**添加弹窗**：Tab 切换（数据源/主题）、分页列表、搜索、多选 + 已添加去重、全量提交。含已发布提示。 |
| `PromptsTab` | Prompt 管理 Tab 容器。加载 Prompt 列表，集成 `PromptList`。 |
| `PromptList` | 搜索栏 + 列表。每个条目显示 name、description、参数计数、开关、编辑/删除图标。 |
| `PromptDialog` | 创建/编辑弹窗。**基本信息**（name/description）。**模板内容**：使用 `PromptTemplateEditor`（CodeMirror）。**参数列表**：el-table 编辑（name/required/description）+ 「提取参数」按钮调用 `/v1/template/extract` 自动填充。底部「测试渲染」按钮打开预览。 |
| `TemplatePreviewDialog` | 通用模板预览弹窗。显示原始模板 → 参数输入 → 渲染结果。支持 TEXT 和 SQL 双模式。Copy 按钮复制结果。 |
| `SqlSecurityConfig` | 可复用的 SQL 安全配置组件。权限 pill（SELECT/INSERT/UPDATE/DELETE/DDL/MULTI）+ maxRows + timeout + confirmRequired + 标注预览。 |
| `PromptTemplateEditor` | CodeMirror 6 包装。支持：模板语法高亮（`{{}}`、`{{#if}}`）、智能补全、自动闭合、bracket matching、行包裹。 |
| `SqlTemplateEditor` | CodeMirror 6 + `@codemirror/lang-sql`。SQL 语法高亮 + 模板语法高亮 + 自定义自动补全 + 自动闭合。 |

### 3.3 交互细节

- **预置工具区**：开关 + EXECUTE_SQL 的 Setting 图标。无删除、无编辑名称。
- **自定义工具区**：Edit/Delete 图标操作。hover 变色（Edit 蓝色，Delete 红色）。
- **配置弹窗**：权限 pills 切换（选中态紫色 → 蓝色高亮）。安全警告黄色提示。
- **抽屉表单**：使用 Element Plus `el-form` 的 `FormRules` 校验，保存前 `validate()`，异常时 `clearValidate()`。
- **错误处理**：统一 `catch (e: any)` + `e?.message` 展示后端错误信息。
- **数据范围弹窗**：Tab 切换（数据源/主题）。分页加载 + 搜索防抖 300ms。已添加项显示灰色 + `el-tag type="info"` 禁用勾选。确认提交后全量替换。已发布服务显示警告提示。
- **Prompt 弹窗**：`content` 使用 CodeMirror 编辑器。「提取参数」调用 `/v1/template/extract` 自动扫描 `{{var}}` 并填充参数表。参数表 el-table 内联编辑。
- **模板预览**：参数输入框自动按 parameters 列表生成。SQL 模式下根据 `type` 字段做类型转换。结果显示在只读代码区域。
- **CodeMirror 编辑器**：`PromptTemplateEditor` 仅模板语法增强，`SqlTemplateEditor` 叠加 SQL 语言支持。均支持模板变量自动补全（`{{table}}`、`{{#if}}`、`{{#where}}` 等）。

### 3.4 数据流

```
详情页（侧边导航） → Tool / Data Scope / Prompts 各 Tab

ToolsTab
    ├─ GET /tools → { prebuilt, custom }
    │
    ├─ 预置：
    │    ├─ 开关 → PUT /tools/{toolType} { tool_type, enabled }
    │    └─ 配置 → PUT /tools/EXECUTE_SQL { tool_type, enabled, config: JSON.stringify(...) }
    │
    └─ 自定义：
         ├─ 新建 → POST /tools { tool_type, name, description, config: JSON.stringify(...) }
         ├─ 编辑 → PUT /tools/{id} { tool_type, name, description, enabled, config }
         ├─ 开关 → PUT /tools/{id} { tool_type, enabled }
         └─ 删除 → DELETE /tools/{id} → 确认弹窗

DataScopeTab
    ├─ GET /data-scope → { items: [...] }
    ├─ 添加 → PUT /data-scope { items: [...完整列表...] }
    └─ 删除 → PUT /data-scope { items: [...删后列表...] }

PromptsTab → PromptList → PromptDialog
    ├─ GET /prompts → McpPromptVO[]
    ├─ 新建 → POST /prompts { name, content, parameters }
    ├─ 编辑 → PUT /prompts/{id} { name, content, parameters, enabled }
    ├─ 开关 → PUT /prompts/{id} { ...原有数据..., enabled: !enabled }
    ├─ 删除 → DELETE /prompts/{id} → 确认弹窗
    └─ 测试渲染 → POST /v1/template/preview { mode: "TEXT", template, values } → TemplatePreviewDialog

TemplatePreviewDialog
    ├─ POST /v1/template/extract { template } → { variables }（提取参数名列表）
    └─ POST /v1/template/preview { mode, template, values } → { rendered }
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
  parameters, addParam, scanParams, scanParamsSuccess, scanParamsNoNew, noParams, paramCount, paramDesc, paramRequired
  configExecuteSql, allowedOps, sqlRiskWarning, maxRows, timeout, confirmRequired
  annotationsPreview, annotationsHint
  previewRender, previewTitle, previewRun, previewParamPlaceholder, previewEmptyValues, previewTextResult, previewSqlResult
  deleteConfirm
  type: { SEARCH_METADATA, GET_TABLE_INFO, EXECUTE_SQL, PARAMETERIZED_SQL }

mcpService.dataScope:
  addScope, addDialogTitle, subtitle, empty
  typeDataSource, typeSubject
  publishedHint, deleteConfirm
  searchDataSource, searchSubject
  alreadyAdded, selected, noSelection, selectedCount
  pageText, prevPage, nextPage
  confirmAdd, selectFirst, noResults, showingRange

mcpService.prompt:
  title, subtitle
  addPrompt, editPrompt
  searchPlaceholder, totalCount, empty, emptySearch
  basicInfo, promptName, descPlaceholder
  content, contentPlaceholder, contentRequired, nameRequired
  parameters, noParams, paramCount
  deleteConfirm, previewRender
```

---

## 5. 测试

| 测试类 | 覆盖 |
|---|---|
| `McpServiceServiceTest` | 服务 CRUD、code 校验、分页过滤 |
| `McpServiceControllerTest` | 端点集成测试（创建、更新、查询、分页） |
| `McpServiceDataScopeServiceTest` | 数据范围全量替换、空列表清空、查询 |
| `McpToolServiceTest` | 预置列表（默认值回退）、自定义 CRUD、name 校验、计数 |
| `McpToolControllerTest` | 分组列表、预置/自定义更新、创建、删除 |
| `McpPromptServiceTest` | Prompt 创建/更新/删除/列表、name 重复、参数双向校验（未定义/未使用）、模板语法错误、转义变量、null content |
| `McpPromptControllerTest` | GET/POST/PUT/DELETE 端点集成测试 |
| `TemplatePreviewControllerTest` | TEXT 模式（简单变量、if 块）、SQL 模式（字符串/数值/布尔/null/数组格式化、原始变量、默认值、完整模板）、语法错误、空 mode、参数提取 |
| `SqlAnalyzerTest` | 74 条参数化用例：DML/DDL/METADATA/TRANSACTION/SET_STATEMENT 类型识别、基础/子查询/CTE/边界表提取、模板渲染后 SQL（`?` 占位符）、多语句检测（含 `MULTI` 标记）、事务预扫描（BEGIN/START TRANSACTION）、解析失败容错、注释绕过安全 |

---

## 6. 已实现 vs 未实现

| User Story | 标题 | 状态 | 说明 |
|---|---|---|---|
| US-01 | 服务创建与基础管理 | ✅ 已实现 | 服务 CRUD、code 校验、分页列表 |
| US-02 | 数据范围配置 | ✅ 已实现 | 数据源 + 主题引用，全量替换 |
| US-03 | 工具管理 | ✅ 已实现 | 预置工具（3 种）+ 自定义工具 CRUD |
| US-04 | 创建与配置 Resource | ❌ V1 暂缓 | 见 US-04 文档说明 |
| US-05 | 创建与配置 Prompt | ✅ 已实现 | Prompt CRUD + 模板校验 + 参数一致性检查 |
| US-5.5 | 模板引擎基础设施 | ✅ 已实现 | Handlebars 风格 Parser + Text/SQL Renderer |
| US-06 | 管理服务 Token | ❌ V1 暂缓 | 统一在应用层认证，MCP 模块不单独设计 |
| US-07 | 调试 Tool 调用 | ❌ 未实现 | 前端占位按钮，后端无接口 |
| US-08 | 发布与停用 MCP 服务 | ❌ 未实现 | 前端占位按钮（comingSoon），无后端 publish/disable/enable 接口 |
| US-09 | 查看服务调用日志 | ❌ 未实现 | 无 `mcp_audit_log` 表和对应接口 |
| US-10 | 删除 MCP 服务 | ❌ 未实现 | 列表页和详情页有删除按钮但仅弹 info（comingSoon），无后端接口 |

> **说明**：详情页侧边导航的 `security`、`debug`、`logs` Tab 均为占位，点击显示 `comingSoon` 空状态。发布/停用/启用/删除按钮同理。

---

## 7. 关联文档

- [US-01 需求文档](../prd/us/US-01.md)
- [US-02 需求文档](../prd/us/US-02.md)
- [US-03 需求文档](../prd/us/US-03.md)
- [US-04 需求文档](../prd/us/US-04.md)（暂缓）
- [US-05 需求文档](../prd/us/US-05.md)
- [US-5.5 需求文档](../prd/us/US-5.5.md)
- [MCP Builder PRD](../prd/2026-05-11-mcp-builder-prd.md)
- [US-03 实施计划](../superpowers/plans/2026-05-21-us-03-mcp-tool.md)
- [SQL 分析工具设计](../superpowers/specs/2026-06-19-sql-analyzer-design.md)
- [SQL 分析工具实施计划](../superpowers/plans/2026-06-19-sql-analyzer.md)
