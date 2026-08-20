# MCP Service 管理 — 架构文档

> 版本：v2.4（US-01–05, US-5.5, US-07, US-08, US-10 完整实现；MCP Endpoint 已上线；结构化参数校验 + 元数据更新预置工具）
> 最后更新：2026-08-14

---

## 1. 概述

MCP Service 管理模块提供 **MCP（Model Context Protocol）服务的生命周期管理**。

**已实现能力：**
- 创建服务（`DRAFT`）、编辑基础信息、分页列表、详情查看。**创建即含数据范围**（必填，事务内创建服务 + 数据范围）
- Service Code（唯一标识）+ Endpoint 路径运行时推导（`/{code}/mcp`）
- 数据范围配置（数据源 + 主题引用模式，全量替换保存）
- 预置工具开关与配置（SEARCH_METADATA / GET_TABLE_INFO / LIST_TABLES / EXECUTE_SQL / UPDATE_TABLE_INFO / UPDATE_COLUMN_INFO / UPSERT_TERM）
- 自定义工具 CRUD（参数化 SQL）
- **结构化参数校验**：预置工具参数以 record 声明（`domain.model.param` 包），`tools/list` 的 inputSchema 由注解生成、运行时校验同一事实源（victools jsonschema + Jakarta Validation）
- **元数据更新预置工具**：UPDATE_TABLE_INFO / UPDATE_COLUMN_INFO / UPSERT_TERM，LLM 直接写入共享元数据（表/列描述与别名、业务术语 upsert），每次写入同事务落审计日志（`mcp_metadata_audit_log`）
- **Prompt 模板管理**（CRUD + 模板语法校验 + 参数一致性检查）
- **模板预览引擎**（TEXT 模式渲染 / SQL 模式渲染 + 参数提取）
- **SQL 安全分析引擎**（操作类型识别、表提取、多语句检测、事务/元数据/SET 分类）
- **工具测试（Tool Test）**：参数输入 → 安全校验 → 执行 → 结果展示（支持 8 种工具类型、多语句 SQL、部分失败、scope 校验、逐条 METADATA_UPDATE 结果）
- **发布与版本管理（US-08）**：草稿-快照隔离、发布/发布变更、停用/启用、版本历史与回滚、草稿 vs 线上 diff
- **删除服务（US-10）**：事务级联删除（快照/数据范围/工具/Prompt），前端与全站删除操作一致（简单确认弹窗）
- **MCP Endpoint（JSON-RPC over HTTP）**：`POST /{code}/mcp` 协议入口，支持 `initialize` / `ping` / `tools/list` / `tools/call` / `prompts/list` / `prompts/get`；仅读取激活快照（草稿永不暴露）；服务状态语义（未知 code / DRAFT → 404，DISABLED → 503）；Origin DNS-rebinding 防护 + `MCP-Protocol-Version` 校验

> **遗留**：调用审计日志（US-09）未实现；元数据写入审计（`mcp_metadata_audit_log`）已落地（见 2.2 元数据更新工具）。

**设计原则**：
- **简单优先 / 灵活优先**：提供通用原子能力（元数据检索、SQL 执行、参数化 SQL、Prompt），不做业务假设，由用户按场景组合
- **协议适配而非协议绑定**：核心业务逻辑（Tool 执行、权限校验、审计）作为协议无关的能力层实现，MCP JSON-RPC 层只是其中一个调用方（未来可扩展 Skill / OpenAI Function Calling / REST API）
- **默认可控，显式放开**：自由 SQL 默认只允许 SELECT，写操作与 DDL 需用户显式开启
- **SQL 是核心能力**：支持自由 SQL 与参数化 SQL，操作权限在 Tool 层配置

---

## 2. 后端架构

### 2.1 分层结构

```
com.dati.mcp/
├── domain/
│   ├── model/               # 领域实体与枚举
│   │   ├── McpService.java              # 服务聚合（code, status, activeVersionId/Number）
│   │   ├── McpServiceStatus.java        # DRAFT / PUBLISHED / DISABLED（状态机）
│   │   ├── McpServiceSnapshot.java      # 只读快照（content 全量打包）
│   │   ├── McpServiceDataScope.java     # 数据范围实体
│   │   ├── McpDataScopeType.java        # DATA_SOURCE / SUBJECT
│   │   ├── McpToolType.java             # 工具类型枚举（7 预置 + 1 动态，含 parameterType record / title / annotations）
│   │   ├── param/                       # 预置工具参数 record（inputSchema 与运行时校验的单一事实源）
│   │   │   ├── SearchMetadataArgs.java
│   │   │   ├── GetTableInfoArgs.java
│   │   │   ├── ListTablesArgs.java
│   │   │   ├── ExecuteSqlArgs.java
│   │   │   ├── UpdateTableInfoArgs.java
│   │   │   ├── UpdateColumnInfoArgs.java
│   │   │   └── UpsertTermArgs.java
│   │   ├── ToolConfig.java              # sealed interface 配置体系
│   │   ├── SqlPolicy.java               # SQL 权限策略（9 字段 + validate）
│   │   ├── ToolParameter.java           # 工具参数描述（String/Number/Boolean/DateTime/Array）
│   │   ├── ToolError.java               # 工具执行错误枚举（9 种 + TIMEOUT 预留，含 category）
│   │   ├── McpPrebuiltToolConfig.java
│   │   ├── McpCustomTool.java
│   │   ├── McpPrompt.java               # Prompt 实体
│   │   ├── PromptParameter.java         # Prompt 参数描述
│   │   └── TemplateRenderMode.java      # TEXT / SQL
│   └── service/
│       ├── McpServiceService.java           # 服务 CRUD + code 校验
│       ├── McpServicePublishService.java    # 发布/停用/启用/diff/回滚（快照管理核心）
│       ├── McpServiceDataScopeService.java  # 数据范围全量替换 + 解析 dsId 集合
│       ├── McpToolService.java              # 工具 CRUD + 分组
│       ├── McpPromptService.java            # Prompt CRUD + 模板校验
│       ├── McpToolTestService.java          # 工具测试编排（resolve → scope → bind → execute）
│       ├── McpParameterSchemaGenerator.java # 参数 record → JSON Schema（victools，DRAFT_2020_12，按类型缓存）
│       ├── ToolParameterBinder.java         # 参数反序列化 + Jakarta Validation 校验（PARAM_INVALID，英文消息）
│       ├── ToolResolver.java               # toolId 解析（枚举 → 预置，UUID → 自定义）
│       ├── ScopeValidator.java             # 两层 scope 校验（数据源级 + 表级）+ 元数据写工具的 ds 级/主题定位
│       ├── ToolExecutor.java               # 接口：getToolType() + execute(ctx)
│       ├── ToolExecutionContext.java        # record：serviceId, toolType, config, boundArgs, scopeItems
│       ├── ExecuteSqlExecutor.java          # EXECUTE_SQL 执行器
│       ├── ParameterizedSqlExecutor.java    # PARAMETERIZED_SQL 执行器
│       ├── GetTableInfoExecutor.java        # GET_TABLE_INFO 执行器
│       ├── ListTablesExecutor.java          # LIST_TABLES 执行器（表级清单，无列）
│       ├── SearchMetadataExecutor.java      # SEARCH_METADATA 执行器
│       ├── MetadataEntityResolver.java      # 元数据实体定位（dsId+schema+table → id，供写工具捕获旧值）
│       ├── UpdateTableInfoExecutor.java     # UPDATE_TABLE_INFO 执行器（写表描述/别名 + 审计）
│       ├── UpdateColumnInfoExecutor.java    # UPDATE_COLUMN_INFO 执行器（写列描述/别名 + 审计）
│       ├── UpsertTermExecutor.java          # UPSERT_TERM 执行器（术语 upsert + 审计）
│       ├── SqlExecutorHelper.java           # JDBC getMoreResults() 循环（package-private）
│       ├── ToolExecuteException.java        # 工具执行异常（extends RuntimeException）
│       └── ToolsResult.java                 # { prebuilt, custom } record
├── repository/
│   ├── dao/
│   │   ├── McpServiceDAO.java
│   │   ├── McpServiceSnapshotDAO.java
│   │   ├── McpServiceDataScopeDAO.java
│   │   ├── McpPrebuiltToolConfigDAO.java
│   │   ├── McpCustomToolDAO.java
│   │   ├── McpPromptDAO.java
│   │   └── McpMetadataAuditLogDAO.java
│   ├── po/                  # 持久化对象（继承 BasePO / BaseResourcePO）
│   │   ├── McpServicePO.java
│   │   ├── McpServiceSnapshotPO.java
│   │   ├── McpServiceDataScopePO.java
│   │   ├── McpPrebuiltToolConfigPO.java
│   │   ├── McpCustomToolPO.java
│   │   ├── McpPromptPO.java
│   │   └── McpMetadataAuditLogPO.java       # mcp_metadata_audit_log（old/new 值 JSON）
│   └── mapper/              # 静态方法 PO ↔ Model（含 JSON 序列化/反序列化）
│       ├── McpServiceMapper.java
│       ├── McpServiceSnapshotMapper.java   # 快照 content 反序列化（按 tool_type 路由 ToolConfig）
│       ├── McpServiceDataScopeMapper.java
│       ├── McpPrebuiltToolConfigMapper.java
│       ├── McpCustomToolMapper.java
│       └── McpPromptMapper.java
└── server/
    ├── controller/
    │   ├── McpServiceController.java      # 服务 CRUD + 数据范围 + 发布/停用/启用/diff/快照/回滚端点
    │   ├── McpToolController.java         # 工具 CRUD + 测试端点
    │   ├── McpPromptController.java       # Prompt CRUD 端点
    │   └── TemplatePreviewController.java # 模板预览/提取端点
    ├── endpoint/                # MCP 协议入口（/{code}/mcp）
    │   ├── McpEndpointController.java     # HTTP 适配层：路由 / 响应状态码
    │   ├── McpEndpointService.java        # 编排：状态语义 / Origin 校验 / 激活快照加载
    │   └── McpProtocolHandler.java        # JSON-RPC 方法分发
    ├── converter/               # 快照 draft → MCP 协议类型
    │   ├── ToolDefinitionConverter.java   # Tool 定义 + inputSchema 生成
    │   ├── PromptDefinitionConverter.java # Prompt 定义 + prompts/get 渲染
    │   └── ToolResultConverter.java       # 执行结果 → CallToolResult
    ├── resolver/
    │   └── SnapshotToolResolver.java      # 快照内工具解析（enabled-only）
    ├── pojo/                 # VO / Request / Response
    │   ├── McpServiceVO.java, DataScopeItemVO.java, DataScopeRequest.java, DataScopeResponse.java
    │   ├── McpServiceDiffVO.java, McpServiceSnapshotVO.java, PublishRequest.java, RollbackRequest.java
    │   ├── McpToolVO.java, ToolsResponse.java, CustomToolRequest.java
    │   ├── McpPromptVO.java, McpPromptRequest.java
    │   ├── ToolTestRequest.java, ToolTestResponse.java, ToolTestError.java, ToolTestData.java
    │   ├── SqlExecution.java, StatementResult.java
    │   ├── TableMetadata.java, SearchHit.java, TableListData.java   # TABLE_LIST 表级清单（复用 DataSourceDef/TableDef）
    │   ├── MetadataUpdateData.java, MetadataUpdateResult.java   # METADATA_UPDATE 结果（逐条 old/new/error）
    │   └── TemplatePreviewRequest.java, TemplatePreviewResponse.java, TemplateExtractRequest.java, TemplateExtractResponse.java
    └── assembler/            # @Component extends BaseAssembler, Model ↔ VO
        ├── McpServiceAssembler.java
        ├── McpToolAssembler.java
        ├── McpDataScopeAssembler.java
        └── McpPromptAssembler.java
```

**关键跨模块依赖（domain Def 类型）：**

```
com.dati.datasource.domain.model/
├── TableDef.java      # record(schema, table, description, aliases, columns: List<ColumnDef>)
├── ColumnDef.java     # record(name, type, comment, sampleValues: List<String>)
└── DataSourceDef.java # record(dataSourceId, dataSourceName, dbType, defaultSchema, description, tables: List<TableDef>)

com.dati.semantic.domain.model/
└── TermDef.java       # record(name, description, subjectName)
```

这些 Def 类型是纯数据 record，不含运行时状态，被 MCP 层 pojo（`TableMetadata`、`SearchHit`、`TableListData`）引用。

### 2.2 核心类职责

#### Service 管理

| 类 | 职责 |
|---|---|
| `McpService` | 领域实体，继承 `BaseResource`。关键字段：`code`（唯一标识）、`status` |
| `McpServiceStatus` | 枚举：`DRAFT` / `PUBLISHED` / `DISABLED` |
| `McpServicePO` | 持久化对象，继承 `BasePO`。数据库约束：`code` 唯一索引 |
| `McpServiceDAO` | JPA Repository。`existsByCode`、多条件模糊分页查询 |
| `McpServiceService` | 创建时校验 code 格式（正则 `^[a-z0-9]([a-z0-9_-]{0,62}[a-z0-9])?$`）和唯一性；分页列表支持 keyword + status 过滤；级联删除 `deleteMcpService()`（事务内逐表删：快照→数据范围→预置工具→自定义工具→Prompt→服务） |
| `McpServiceAssembler` | Model→VO。推导 `endpointPath = "/{code}/mcp"`；统计 `toolCount` 调用 `McpToolService.countToolsByServiceId()`；快照列表 VO 转换（`toSnapshotVO`，仅元信息不含 content） |
| `McpServiceVO` | 响应：`code`、`status`、`endpoint_path`、`tool_count`、`active_version_number` |

#### 版本管理（Publish / Snapshot / Rollback）

| 类 | 职责 |
|---|---|
| `McpServiceSnapshot` | 只读快照领域实体：`serviceId` + `versionNumber`（单调递增）+ `releaseNote` + `content: SnapshotContent`。`SnapshotContent` = `{ serviceInfo, dataScopes, prebuiltTools, customTools, prompts }` 全量打包 |
| `McpServiceSnapshotPO` | 持久化对象，`content` 列存 JSON 字符串 |
| `McpServiceSnapshotDAO` | JPA Repository。`findMaxVersionNumberByServiceId`、`findByServiceIdAndVersionNumber`、`findAllByServiceIdOrderByVersionNumberDesc` |
| `McpServiceSnapshotMapper` | PO ↔ Model。**关键**：`parseContent()` 手动反序列化快照 content，`config` 字段根据父级 `tool_type` 显式路由到具体 `ToolConfig` 实现类（不依赖 `@JsonTypeInfo`，避免污染其他反序列化路径） |
| `McpServicePublishService` | 版本管理核心服务：`publish()` 打包草稿 → 生成快照 vN+1 → 更新 `active_version_id`；`disable()` / `enable()` 状态切换（不产生快照）；`rollback()` 目标快照内容**全量写回草稿**后重新发布；`getDiff()` 草稿 vs 激活快照业务字段比较；`getSnapshots()` 版本历史 |
| `McpServiceDiffVO` | diff 响应：`has_changes` + `modified_components` + 各组件 changed 标志 + Tools/Prompts 增删改明细列表 |
| `McpServiceSnapshotVO` | 快照列表 VO：仅元信息（`serviceId` / `versionNumber` / `releaseNote` + 审计字段），**不含 content** |
| `PublishRequest` | 发布请求体：`{ release_note? }` |
| `RollbackRequest` | 回滚请求体：`{ target_version_number, release_note? }` |

**状态机（含前置条件校验）：**

| 操作 | 合法转换 | 非法时 |
|---|---|---|
| publish | DRAFT→PUBLISHED；PUBLISHED→PUBLISHED；DISABLED→**DISABLED**（发布≠上线） | — |
| disable | 仅 PUBLISHED→DISABLED | DRAFT / DISABLED → 409 MS016 |
| enable | 仅 DISABLED→PUBLISHED | DRAFT（无激活快照）→ 409 MS016 |
| rollback | 需存在目标版本快照 | 版本不存在 → 404 MS017 |

**diff 业务字段比较（关键设计）：**

- 比较对象为**业务字段**（toolType/name/enabled/config/scopeType/referenceId 等），**排除 id 与审计字段**（created_at/updated_at/created_by/updated_by）
- 原因：快照反序列化（`parseContent`）不保留审计字段；回滚恢复会刷新时间戳（`@CreationTimestamp` / `@UpdateTimestamp`）—— 全字段比较必然误报
- prebuilt 配置变更并入 `modified_tools`（工具名 = toolType），保证「有变更必有明细」
- `service_info` 仅比较 name/description

#### 数据范围管理（Data Scope）

| 类 | 职责 |
|---|---|
| `McpDataScopeType` | 枚举：`DATA_SOURCE`（数据源）、`SUBJECT`（主题） |
| `McpServiceDataScope` | 领域实体。`serviceId` + `scopeType` + `referenceId` + `referenceName` |
| `McpServiceDataScopePO` | 持久化对象，继承 `BasePO` |
| `McpServiceDataScopeDAO` | JPA Repository。`findAllByServiceId`、`deleteAllByServiceId` |
| `McpServiceDataScopeMapper` | PO ↔ Model 转换 |
| `McpServiceDataScopeService` | 全量替换：先 `deleteAllByServiceId` 再 `saveAll`。新增 `getResolvedDataSourceIds(serviceId)`：遍历 scope 并展开 SUBJECT 类型为实际数据源 ID 集合，供 SEARCH_METADATA 等使用 |
| `DataScopeRequest` | 请求体：`{ items: [{ scopeType, referenceId, referenceName }] }` |
| `DataScopeResponse` | 响应：`{ items: [...], resolved_data_sources: [...] }` |

#### Tool 管理

| 类 | 职责 |
|---|---|
| `McpToolType` | 枚举 7 种预置 + `PARAMETERIZED_SQL`（动态，无预置名）。每项含 `toolName` / `title`（协议标题）/ `description` / `parameterType`（参数 record 类）/ `annotationsJson`（MCP ToolAnnotations：SEARCH_METADATA / GET_TABLE_INFO / LIST_TABLES 标记 `readOnlyHint=true`，三个元数据写工具标记 `idempotentHint=true, openWorldHint=true`）和 `getDefaultConfig()` 方法。SEARCH_METADATA inputSchema 的 `keywords` 为 array<string>；LIST_TABLES 无参数（空 record → inputSchema `{"type":"object"}`） |
| `ToolConfig` | `sealed interface`，子类：`SearchMetadataConfig` / `GetTableInfoConfig` / `ListTablesConfig` / `ExecuteSqlConfig` / `ParamSqlConfig` / `UpdateMetadataConfig`。注：已移除 `confirmRequired` 字段（MCP 协议不支持二次确认） |
| `SqlPolicy` | SQL 权限策略（9 字段 + `allowMulti`）。提供 `validate(type)` 方法（内联 switch 逐条校验）。`validateAllowed(result)` 额外处理 MULTI 标记 |
| `ToolParameter` | 工具参数描述：`name`、`type`（String / Number / Boolean / DateTime / Array）、`required`、`defaultValue`、`description` |
| `McpPrebuiltToolConfig` | 领域实体。`serviceId` + `toolType` + `enabled` + `config: ToolConfig` |
| `McpPrebuiltToolConfigPO` | 持久化对象，继承 `BaseResourcePO`。`config` 列存 JSON 字符串 |
| `McpPrebuiltToolConfigDAO` | JPA Repository。`findByServiceIdAndToolType` |
| `McpPrebuiltToolConfigMapper` | PO ↔ Model。序列化/反序列化 `config` 字段 |
| `McpCustomTool` | 领域实体。`serviceId` + `name` + `toolType` + `title` + `description` + `enabled` + `config: ToolConfig` |
| `McpCustomToolPO` | 持久化对象，继承 `BasePO`，手动声明 `name` / `description` 列。`config` 列存 JSON 字符串 |
| `McpCustomToolDAO` | JPA Repository。`findByServiceId`、`existsByServiceIdAndName`、`countByServiceId` |
| `McpCustomToolMapper` | PO ↔ Model。序列化/反序列化 `config` 字段 |
| `McpToolService` | 分组列表返回 `ToolsResult` record；`updatePrebuiltTool` / `updateCustomTool` / `createCustomTool` / `deleteCustomTool` / `countToolsByServiceId`。name 格式校验 |
| `McpToolAssembler` | Model ↔ VO / Request 转换，config JSON 解析，预置 VO 填充 title |
| `McpToolVO` | 响应：`id`、`tool_type`、`name`、`title`、`description`、`enabled`、`config` |
| `ToolsResponse` | `{ prebuilt, custom }` |
| `CustomToolRequest` | 创建/更新请求体。`toolType: @NotNull McpToolType`，`config: String`（JSON） |

#### 结构化参数校验（Parameter Records）

| 类 | 职责 |
|---|---|
| `SearchMetadataArgs` / `GetTableInfoArgs` / `ExecuteSqlArgs` / `UpdateTableInfoArgs` / `UpdateColumnInfoArgs` / `UpsertTermArgs` | 预置工具参数 record（`domain.model.param` 包）。`@JsonProperty`（snake_case）+ `@JsonPropertyDescription` + Jakarta Validation 注解同时驱动 inputSchema 生成与运行时校验 —— 单一事实源，schema 与校验不可能漂移 |
| `McpParameterSchemaGenerator` | `@Component`。victools `jsonschema-generator` 4.38.0（DRAFT_2020_12 + JacksonModule + JakartaValidationModule `NOT_NULLABLE_FIELD_IS_REQUIRED` + `FORBIDDEN_ADDITIONAL_PROPERTIES_BY_DEFAULT` + `INLINE_ALL_SCHEMAS`），从 record 生成 `tools/list` 的 inputSchema，按类型缓存 |
| `ToolParameterBinder` | `@Component`。工具测试与 MCP 调用共用：原始 arguments → 反序列化为参数 record → Jakarta Validation 校验。失败统一 `ToolExecuteException(PARAM_INVALID)`；`parameterType=null`（PARAMETERIZED_SQL）时透传原始 Map。Validator 固定英文 locale（协议错误消息对 LLM 客户端确定性），`@PreDestroy` 关闭工厂 |

**参数契约要点：**

- GET_TABLE_INFO（decision 12）：`data_source_id` 移入每个 `tables[]` 项内（`tables: [{data_source_id, schema?, table, fields?}]`，1–20 个），顶层不再有 `data_source_id`
- 批量写工具（UPDATE_TABLE_INFO / UPDATE_COLUMN_INFO / UPSERT_TERM）：`tables`/`columns`/`terms` 数组（1–20 个），`description` ≤500 字符、`aliases` ≤20×100
- 错误分层：整批反序列化/校验失败 → 顶层 `error`（PARAM_INVALID，无 data）；单条执行失败 → 逐条 `results[i].error`（部分失败语义）

#### 元数据更新工具（Metadata Update Tools）

| 类 | 职责 |
|---|---|
| `UpdateTableInfoExecutor` | UPDATE_TABLE_INFO：写表 description/aliases 到共享元数据（`TableService.updateTable`）。未填写的写字段保持原值；调用内按 (dsId+schema+table) 去重；单条失败（ENTITY_NOT_FOUND / SCOPE_VIOLATION）记入 results 不阻塞其他条目；同事务写审计日志 |
| `UpdateColumnInfoExecutor` | UPDATE_COLUMN_INFO：同表工具，写列 description/aliases（`ColumnService.updateColumn`），去重键含 column |
| `UpsertTermExecutor` | UPSERT_TERM：按 (subjectName + name) 定位术语 —— 存在则更新 description/aliases，不存在则创建（`TermService.createTerm`）。subjectName 经 `resolveSubjectInScope` 在服务 scope 内解析为 subjectId（首匹配）；重命名不支持（走管理端 TermManager） |
| `MetadataEntityResolver` | 实体定位：`resolveTable(dsId, schema, table) → TableTarget(id, description, aliases)`、`resolveColumn(...) → ColumnTarget`（含 columnId/tableId）。schema 匹配语义同 GET_TABLE_INFO：null schema 匹配数据源内任意同名表 |
| `McpMetadataAuditLogPO` / `McpMetadataAuditLogDAO` | 审计日志 `mcp_metadata_audit_log`：service_id / tool_type / entity_type（TABLE\|COLUMN\|TERM）/ entity_id / entity_name / change_type（CREATE\|UPDATE）/ old_value / new_value（JSON）。与元数据更新同事务写入，v1 无管理端 UI |

**设计要点：**

- **写后即生效**：直接写共享元数据存储（非草稿/快照），GET_TABLE_INFO / SEARCH_METADATA 立即可见
- **scope 只校验数据源级**（`ScopeValidator.validateDataSource`）：元数据写入不是数据访问，不做表级 scope；UPSERT_TERM 用 `resolveSubjectInScope` 按名称在 scope SUBJECT 内定位主题
- **旧值捕获**：写入前经 `MetadataEntityResolver` 读当前 description/aliases 作为 `old` 值，`new` 反映合并后的最终状态（漏写字段保持原值）
- **aliases 全量替换语义**：工具描述明确提示先 `get_table_info` 查当前值再写
- **幂等**：annotations 声明 `idempotentHint=true`；审计日志完整记录每次变更（恢复基线可追溯）

#### Tool Test（工具测试）

| 类 | 职责 |
|---|---|
| `ToolExecutor` | 接口：`McpToolType getToolType()` + `ToolTestData execute(ToolExecutionContext ctx)`。Spring 自动注入所有 `@Component` 实现，`McpToolTestService` 构造器中 `List<ToolExecutor>` → `Map<McpToolType, ToolExecutor>` |
| `ToolExecutionContext` | record：`serviceId`、`toolType`、`config: ToolConfig`、`arguments: Object`（预置工具 = 绑定后的参数 record；动态工具 = 原始 Map）、`scopeItems: List<McpServiceDataScope>`。访问器：`args(Class<T>)` 取参数 record、`argumentsMap()` 取原始 Map |
| `ToolResolver` | 解析 toolId：先尝试 `McpToolType.valueOf(toolId)` 匹配枚举 → 查 `McpPrebuiltToolConfigDAO`（有则取 DB 值，无则用默认）；解析失败则当 UUID 查 `McpCustomToolDAO`。disabled → throw `ToolExecuteException(TOOL_DISABLED)` |
| `ScopeValidator` | 两层 scope 校验：① 数据源级 — 遍历 scopeItems 收集所有覆盖的 dsId（DATA_SOURCE 直接 + SUBJECT 展开）；② 表级 — 构建允许的 TableRef 集合，对比 SQL 分析结果。支持 `defaultSchema` 参数解析 schema-less 表引用。另提供 `validateDataSource()`（仅数据源级，供元数据写工具）与 `resolveSubjectInScope()`（按名称在 scope SUBJECT 中定位 subjectId，供 UPSERT_TERM） |
| `McpToolTestService` | 测试编排（极简，无分支）：`resolve()` → 查 scope → `ToolParameterBinder.bind()` 绑定参数 → `execute()` → 计时 → 组装响应。`ToolExecuteException` 在此层 catch 并转为 `ToolTestResponse(success=false, error=...)` |
| `ExecuteSqlExecutor` | EXECUTE_SQL 执行器。`ctx.args(ExecuteSqlArgs.class)` 取参，dsId + sql 从 record 取，`Statement.execute()` 执行，`SqlExecutorHelper.collect()` 收集多语句结果。每条语句独立 policy 校验和 try-catch |
| `ParameterizedSqlExecutor` | PARAMETERIZED_SQL 执行器。dsId 从 config 取，`ctx.argumentsMap()` 取原始参数 → 模板渲染 → SQL，`PreparedStatement.execute()` 执行。支持 DateTime 类型参数转换（`DateTimeUtils.parseDateTime()`）。`bindings` 回传前端 |
| `GetTableInfoExecutor` | GET_TABLE_INFO 执行器。`ctx.args(GetTableInfoArgs.class)` 取参，逐条 `tables[]` 项做数据源级 scope 校验（每项自带 data_source_id），通过 `TableMetadataService` 查询平台元数据。不存在的表静默跳过 |
| `ListTablesExecutor` | LIST_TABLES 执行器。无参数（`ListTablesArgs` 空 record），通过 `McpServiceDataScopeService.getResolvedDataSourceIds()` 解析 scope → 逐数据源 `TableInfoDAO.findByDataSourceId()` 查表 → 组装表级清单（schema/name/description/aliases，columns=null 不输出）。空 scope 返回空结果。纯 DB 读、不查 ES/列 |
| `SearchMetadataExecutor` | SEARCH_METADATA 执行器。`ctx.args(SearchMetadataArgs.class)` 取 keywords，通过 `McpServiceDataScopeService.getResolvedDataSourceIds()` 解析 scope → `SemanticSearchService.search()` → 组装分组结果。空 scope 返回空结果（不报错） |
| `UpdateTableInfoExecutor` / `UpdateColumnInfoExecutor` / `UpsertTermExecutor` | 元数据写执行器（见「元数据更新工具」小节） |
| `SqlExecutorHelper` | package-private 工具类：`collect(Statement)` — JDBC `getMoreResults() / getResultSet() / getUpdateCount()` 循环，每条独立 try-catch 返回 `StatementResult` |
| `ToolExecuteException` | `extends RuntimeException`（不继承 DatiException）。包含 `ToolError` 和格式化后的消息。在 `McpToolTestService` 中被 catch |
| `ToolError` | 枚举，9 种错误 + TIMEOUT 预留（见 2.3 节） |

**异常处理流程**：

```
Executor / ScopeValidator / ToolResolver
  │  throw new ToolExecuteException(ToolError.XXX, args...)
  ▼
McpToolTestService.test()
  │  catch (ToolExecuteException e)
  │  → new ToolTestResponse(false, elapsed, null,
  │        new ToolTestError(e.getErrorCategory(), e.getMessage()))
  ▼
Controller → HTTP 200 + ToolTestResponse JSON
```

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
| `McpPromptController` | 4 端点（见 2.5） |
| `McpPromptVO` | 响应：`id`、`serviceId`、`name`、`description`、`enabled`、`content`、`parameters` |
| `McpPromptRequest` | 请求体：`name`、`description`、`enabled`、`content`、`parameters` |

**Prompt 模板校验逻辑 (`McpPromptService.validateContentParams`)：**

1. **语法校验**：使用 `TemplateParser` 解析 `content`，捕获 `TemplateParseException` → 返回 `MS_TEMPLATE_SYNTAX_ERROR`
2. **参数一致性（双向检查）**：
   - `content` 中 `{{var}}` 提取的变量集 vs `parameters` 中定义的参数名集
   - 定义但未引用的参数 → 抛出 `MS_PROMPT_ARG_MISMATCH`（Unused parameter）
   - 引用但未定义的变量 → 抛出 `MS_PROMPT_ARG_MISMATCH`（Unknown parameter）

#### MCP Endpoint（协议层）

| 类 | 职责 |
|---|---|
| `McpEndpointController` | `POST /{code}/mcp` HTTP 适配层：路由、请求体透传、响应状态码映射。不含协议逻辑。认证由全局 `AuthInterceptor` 覆盖（WebMvcConfig 注册 `/*/mcp` 路径） |
| `McpEndpointService` | 端点编排：① 服务状态语义（未知 code / DRAFT → 404；DISABLED → 503 + JSON-RPC error）② 传输层校验（Origin DNS-rebinding 防护 + `MCP-Protocol-Version`，initialize 豁免）③ 加载激活快照 ④ JSON-RPC 反序列化与分发委托。无状态：不签发 `Mcp-Session-Id` |
| `McpProtocolHandler` | JSON-RPC 方法分发：`initialize`（按快照内容声明 capabilities.tools/prompts）、`ping`、`tools/list`、`tools/call`、`prompts/list`、`prompts/get`；未知方法 → `METHOD_NOT_FOUND`。`tools/call` 与测试链路一致，参数经 `ToolParameterBinder` 绑定校验。`ToolExecuteException` → `isError=true` 的 `CallToolResult`（LLM 可自纠正）；其余异常 → `INTERNAL_ERROR` |
| `ToolDefinitionConverter` | 快照 tool drafts → `McpSchema.Tool`。确定性顺序：预置固定顺序 → 自定义按名称排序。与预置/先前自定义重名的自定义工具跳过（WARN 日志）。预置 inputSchema 由 `McpParameterSchemaGenerator` 从参数 record 生成，定义含 `title` 与 `annotations`（枚举 `annotationsJson` 解析，`ToolAnnotations` builder）；PARAMETERIZED_SQL 从 `ToolParameter` 列表生成（类型映射 String/DateTime→string、Number→number、Boolean→boolean、Array→array；required 列表；default 值） |
| `PromptDefinitionConverter` | 快照 prompt drafts → `McpSchema.Prompt`（name/description/arguments）；`prompts/get` 复用 `TextRenderer` 渲染（模板引擎），必填参数缺失 → `INVALID_PARAMS` |
| `ToolResultConverter` | `ToolTestData` → `CallToolResult`（`textContent` + `structuredContent` JSON，`isError=false`）；`ToolExecuteException` → `isError=true` |
| `SnapshotToolResolver` | 从**激活快照**解析工具（仅 enabled；disabled 工具与未知工具不可区分），构建 scope items 供 `ScopeValidator` 消费 |

**关键设计：**

- **版本隔离在协议层落地**：endpoint 只读 `active_version_id` 指向的快照 content，草稿数据永不对外暴露（US-08 遗留任务 #2）
- **状态语义**：未知 code 与 DRAFT 不可区分 → 404（不泄露服务存在性）；DISABLED → 503 + JSON-RPC error「Service is disabled」（US-08 遗留任务 #3）
- **空服务语义**：`initialize` 按快照内容声明 capabilities —— 无启用工具则不声明 `tools`，无启用 Prompt 则不声明 `prompts`（US-08 遗留任务 #4）
- **Origin DNS-rebinding 防护**（MCP 2025-11-25 安全最佳实践）：loopback origin（localhost / 127.0.0.1 / ::1）始终信任（浏览器端 MCP Client 本地运行）；远程部署需通过 `dati.mcp.allowed-origins`（逗号分隔完整 origin，支持 `MCP_ALLOWED_ORIGINS` 环境变量覆盖）白名单；无 Origin 头不检查
- **协议版本校验**：除 `initialize` 外，所有请求必须携带 `MCP-Protocol-Version: 2025-11-25`，否则 400
- **通知处理**：notification 接受但不响应（2025-11-25 规范允许），返回 202
- **JSON 序列化**：MCP 消息使用 SDK camelCase mapper（`McpProtocolMessageConverter` 注册于 message converters 首位），与 Dev profile 的 SNAKE_CASE API 响应互不影响
- **认证集成**：`/*/mcp` 路径纳入全局 `AuthInterceptor`，与应用层认证体系（APIKey 等）统一鉴权

### 2.3 ToolError 枚举

| 枚举值 | category | 含义 | 来源 |
|--------|----------|------|------|
| `TOOL_NOT_FOUND` | `PARAM_ERROR` | 工具不存在 | `ToolResolver` |
| `TOOL_DISABLED` | `PARAM_ERROR` | 工具已禁用 | `ToolResolver` |
| `PARAM_MISSING` | `PARAM_ERROR` | 缺少必填参数 | 各 Executor |
| `PARAM_INVALID` | `PARAM_ERROR` | 参数不合法（binder 反序列化/校验失败） | `ToolParameterBinder` |
| `DATA_SOURCE_NOT_FOUND` | `PARAM_ERROR` | 数据源不存在 | 各 Executor |
| `ENTITY_NOT_FOUND` | `PARAM_ERROR` | 表/列/术语不存在 | 元数据写 Executor（经 `MetadataEntityResolver`） |
| `SCOPE_VIOLATION` | `SCOPE_ERROR` | 数据源/表不在服务范围 | `ScopeValidator` |
| `SQL_POLICY_VIOLATION` | `PERMISSION_DENIED` | SQL 操作被策略禁止 | `SqlPolicy` |
| `SQL_EXECUTION_ERROR` | `SQL_ERROR` | SQL 执行失败 | SQL Executors |
| （预留）`TIMEOUT` | `TIMEOUT` | 执行超时 | 待超时检测功能 |

`ToolError.category` 直接映射到前端 `ToolTestError.error_category`，用于差异化 UI 处理（高亮表单字段 / 提示调整 Scope / 展示 SQL 错误等）。

#### SQL 安全分析（SqlAnalyzer）

`com.dati.db.analysis` — 数据库层的静态 SQL 分析工具，在 MCP 工具执行前对模板渲染后的 SQL 做安全分析，供权限管控使用。依赖 JSqlParser 5.1 做 AST 解析。

| 类 | 职责 |
|---|---|
| `SqlAnalyzer` | `public final class` + 私有构造器，纯静态工具。唯一入口：`SqlAnalyzer.analyze(String sql) → SqlAnalysisResult` |
| `SqlOperationType` | 枚举 9 种操作类型：`SELECT`、`INSERT`、`UPDATE`、`DELETE`、`MERGE`、`DDL`、`METADATA`（SHOW/DESCRIBE/EXPLAIN）、`TRANSACTION`（COMMIT/ROLLBACK）、`SET`、`MULTI`（多语句标记）、`OTHER`（兜底拒绝）。注：已新增 `MERGE` 类型，`SET_STATEMENT` 更名为 `SET` |
| `SqlAnalysisResult` | Record：`type`（快捷分发）、`statementTypes`（逐条校验完整列表）、`tables`（所有表引用并集）。提供 `isMulti()` 便捷方法 |
| `TableRef` | Record：`schema`（`@Nullable`）、`name`。支持 `qualifiedName()` |

**核心分析能力：**

| 能力 | 实现 |
|------|------|
| **操作类型识别** | `detectOperationType(Statement)` — `instanceof` 分发：`Select`→SELECT, `Insert`→INSERT, `Update`→UPDATE, `Delete`→DELETE, `Merge`→MERGE, DDL 类→DDL, SHOW/DESCRIBE/EXPLAIN→METADATA, Commit/Rollback→TRANSACTION, SetStatement→SET, 其余→OTHER |
| **表引用提取** | `SchemaAwareTableFinder extends TablesNamesFinder<Void>` — 遍历 SQL AST，提取所有表引用（含子查询、JOIN、CTE 中实际表，排除 CTE 定义名）。使用 `getUnquotedSchemaName()` / `getUnquotedName()` 保留完整 schema 信息 |
| **多语句检测** | `parseStatements()` 解析→过滤空语句→`types.size()>1` 时 `type=MULTI`，`statementTypes` 返回每条语句的独立类型 |
| **事务语句预扫描** | JSqlParser 5.1 无法解析 `BEGIN` / `START TRANSACTION`。`analyze()` 在两轮标准解析均失败后，用正则检测并分号拆句逐段处理 |
| **容错处理** | 解析失败不抛异常，返回 `type=OTHER`。空输入/乱码同理 |

**设计决策：**
- 静态工具类而非 Spring Bean — 纯函数无状态，与 `JsonUtils` 风格一致
- `MULTI` 仅作为 `SqlAnalysisResult.type` 标记值，不在 `statementTypes` 列表中出现
- `METADATA` / `OTHER` 默认拒绝 — 元数据查询应走 `SEARCH_METADATA` 工具，非标准 SQL 一律拒绝
- 75 条参数化单元测试覆盖所有类型识别、表提取、多语句、MERGE、注释绕过、事务预扫描场景

#### GET_TABLE_INFO 元数据查询（TableMetadataService）

`com.dati.datasource.domain.service.TableMetadataService` — 平台元数据查询服务，替代了原始的 JDBC `DbClient.getColumns()` 方式。

| 方法 | 功能 |
|------|------|
| `getTableMeta(dsId, schema, table) → Optional<TableMeta>` | 查单表：先从 `TableInfoDAO` 查表记录（按 dataSourceId + schema + name），再通过 `ColumnInfoDAO` 查列信息，最后通过 `SemanticIndexService` 查每列的样本值（前 5 个） |
| `getTableMetasByIds(Set<String>) → List<TableMeta>` | 批量查询（供 SEARCH_METADATA 使用） |

`TableMeta` record：`tableId, tableName, schema, description, aliases, dataSourceId, columns: List<ColumnDef>`。

**设计决策**：不再依赖 JDBC 连接查询表结构，统一使用平台同步的元数据。找不到表返回 `Optional.empty()`（GetTableInfoExecutor 静默跳过）。

#### SEARCH_METADATA 全文搜索（SemanticSearchService）

`com.dati.semantic.domain.service.SemanticSearchService` — 元数据全文搜索编排服务，负责 ES 查询 → 文档归并 → 术语关联展开 → 批量查询 → 数据源分组。

**执行流程**：

```
SemanticSearchService.search(keywords, dsIds, subjectIds)
  │
  ├─ SemanticIndexService.searchMetadata(keywords, dsIds, subjectIds, maxResults=50)
  │     → ES multi_match（keywords + description 加权），filter: datasourceId in dsIds OR subjectIds
  │     → 返回 SemanticSearchDocument 列表
  │
  ├─ 文档归并：
  │     TABLE/FIELD/FIELD_VALUE → 收集 tableId
  │     TERM → 收集 termId
  │     SUBJECT → 忽略
  │
  ├─ TermRelation 展开：termId → 查关联表 tableId → 合并到 tableIdSet
  │
  ├─ TableMetadataService.getTableMetasByIds() → 批量查表元数据
  │     mergeWithMatches() → 将 ES FIELD_VALUE 命中值合并到列的 sampleValues
  │     （命中值在前，不足 5 随机补齐，超 5 丢弃剩余随机值）
  │
  ├─ DataSourceService.getDataSourceBriefs() → 批量查数据源名称/类型/默认 schema
  │
  └─ 按 dataSourceId 分组 → List<DataSourceGroup>
       + TermService.getTermsWithSubject() → List<TermInfo>
       → SearchResult(dataSources, terms)
```

**SearchResult** record：`dataSources: List<DataSourceGroup>`, `terms: List<TermInfo>`。

**DataSourceGroup** record：`dataSourceId, dataSourceName, dbType, defaultSchema, description, tables: List<TableMeta>`。

**关键设计决策**：
- maxResults=50 为 Executor 常量，Service 接受参数
- 空 scope → 空结果，不报错
- 搜索所有实体类型（TABLE, FIELD, TERM, FIELD_VALUE, SUBJECT），不用 type filter
- 不裁剪列（对 NL2SQL 有害）
- BM25 自动加权，不用手动字段加权
- ES filter 利用 `EntityReference.datasourceId` 字段过滤 scope

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

### 2.4 数据模型

```
McpService
  ├─ id (UUID), code (唯一), name, description, status (DRAFT|PUBLISHED|DISABLED)
  ├─ activeVersionId, activeVersionNumber   ← 指向当前对外响应的快照
  └─ endpointPath = 配置项 dati.mcp.endpoint-base-url + "/{code}/mcp"（运行时推导，不存 DB；base-url 未配置时返回相对路径）

McpServiceSnapshot (per service, 多个, 版本号单调递增)
  ├─ serviceId, versionNumber (v1, v2, v3...), releaseNote
  ├─ content: JSON { serviceInfo, dataScopes, prebuiltTools, customTools, prompts }（发布时全量打包）
  └─ 只读：MCP endpoint 仅读取 active 快照，不访问草稿

McpServiceDataScope (per service, 多个)
  ├─ serviceId, scopeType (DATA_SOURCE|SUBJECT), referenceId, referenceName
  └─ 全量替换：先删后插

McpToolType (enum, 每项含 toolName/title/description/parameterType/annotations/defaultEnabled)
  ├─ SEARCH_METADATA ────→ SearchMetadataConfig { timeout }              readOnlyHint=true
  ├─ GET_TABLE_INFO  ────→ GetTableInfoConfig { timeout }                readOnlyHint=true
  ├─ LIST_TABLES     ────→ ListTablesConfig { }（无 per-service 配置）   readOnlyHint=true
  ├─ EXECUTE_SQL     ────→ ExecuteSqlConfig { sqlPolicy, timeout, maxRows }
  ├─ UPDATE_TABLE_INFO ──→ UpdateMetadataConfig { }（无 per-service 配置） idempotent+openWorld
  ├─ UPDATE_COLUMN_INFO ─→ UpdateMetadataConfig { }                      idempotent+openWorld
  ├─ UPSERT_TERM ────────→ UpdateMetadataConfig { }                      idempotent+openWorld
  └─ PARAMETERIZED_SQL ──→ ParamSqlConfig { dataSourceId, sqlTemplate, parameters[], timeout, maxRows }

McpPrebuiltToolConfig (per service, UNIQUE(service_id, tool_type))
  ├─ serviceId, toolType, enabled (default true), config (ToolConfig)
  └─ 懒初始化：无 DB 记录时使用 McpToolType.getDefaultConfig()

McpCustomTool (per service, 多个, UNIQUE(service_id, name))
  ├─ serviceId, name, toolType (default PARAMETERIZED_SQL), title, description, enabled, config (ToolConfig)
  └─ 完整 CRUD

McpPrompt (per service, 多个, UNIQUE(service_id, name))
  ├─ serviceId, name, enabled (default true), content (模板字符串), parameters (JSON)
  └─ 完整 CRUD + 模板语法校验 + 参数一致性检查

McpMetadataAuditLog (per metadata write, 多个)
  ├─ serviceId, toolType (枚举名), entityType (TABLE|COLUMN|TERM), entityId, entityName
  ├─ changeType (CREATE|UPDATE), oldValue / newValue (JSON: {description, aliases})
  └─ 与元数据更新同事务写入；v1 无管理端 UI
```

### 2.5 API 端点

#### Service 管理

| 方法 | 路径 | 说明 |
|---|---|---|
| `POST` | `/v1/mcp-services` | 创建服务（需传 `code`，自动设为 `DRAFT`；`data_scopes` 必填非空，事务内创建服务与数据范围） |
| `PUT` | `/v1/mcp-services/{id}` | 编辑基础信息（name / description） |
| `GET` | `/v1/mcp-services/{id}` | 详情（含 `endpoint_path`、`tool_count`） |
| `GET` | `/v1/mcp-services` | 分页列表，支持 `keyword`、`status` 过滤 |
| `GET` | `/v1/mcp-services/{id}/data-scope` | 查询数据范围列表 |
| `PUT` | `/v1/mcp-services/{id}/data-scope` | 全量保存数据范围 |
| `DELETE` | `/v1/mcp-services/{id}` | **删除服务（US-10）**：级联删除快照/数据范围/预置工具/自定义工具/Prompt，返回 `IdResponse`。已发布服务可直接删除，无需先停用 |

#### 发布与版本管理

| 方法 | 路径 | 说明 |
|---|---|---|
| `POST` | `/v1/mcp-services/{id}/publish` | 发布。打包草稿生成快照 vN+1 并激活。DRAFT→PUBLISHED；DISABLED 时保持 DISABLED。Body：`{ release_note? }` |
| `POST` | `/v1/mcp-services/{id}/rollback` | 回滚。目标快照内容全量写回草稿（基础信息/Data Scope/Tools/Prompts）→ 生成新快照 vN+1。Body：`{ target_version_number, release_note? }` |
| `POST` | `/v1/mcp-services/{id}/disable` | 停用。仅 PUBLISHED→DISABLED，不产生新快照 |
| `POST` | `/v1/mcp-services/{id}/enable` | 启用。仅 DISABLED→PUBLISHED，不产生新快照 |
| `GET` | `/v1/mcp-services/{id}/snapshots` | 版本历史列表，倒序。返回元信息（版本号/Release Note/时间），**不含快照正文 content** |
| `GET` | `/v1/mcp-services/{id}/diff` | 草稿 vs 激活快照差异：`has_changes` + 变更明细（基础信息/Data Scope/Tools/Prompts） |

#### Tool 管理

| 方法 | 路径 | 说明 |
|---|---|---|
| `GET` | `/v1/mcp-services/{id}/tools` | 全量列表，分组返回 `{ prebuilt, custom }` |
| `PUT` | `/v1/mcp-services/{id}/tools/{toolId}` | 更新（含 enabled 开关）。`@Valid` 要求 `tool_type` 必传 |
| `POST` | `/v1/mcp-services/{id}/tools` | 创建自定义工具 |
| `DELETE` | `/v1/mcp-services/{id}/tools/{toolId}` | 删除自定义工具 |
| `POST` | `/v1/mcp-services/{id}/tools/{toolId}/test` | **测试工具**（US-07） |

`toolId`：预置工具用枚举值（`SEARCH_METADATA` 等），自定义工具用 UUID。

#### Tool Test 响应结构

**`POST /test` 请求体**：`{ arguments: { ... } }`

**响应体 `ToolTestResponse`**：

```json
{
  "success": true,              // 整体成功/失败
  "execution_time_ms": 42,      // 耗时，无论成败都返回
  "data": {                     // 成功时，按 type 分发
    "type": "SQL_EXECUTION",
    "executed_sql": "SELECT ...",
    "results": [ ... ]
  },
  "error": {                    // 失败时（顶层错误）
    "error_category": "PARAM_ERROR",
    "message": "缺少必填参数：sql"
  }
}
```

**`data` discriminated union（ToolTestData）**：

| data.type | 对应 pojo | 工具类型 |
|-----------|----------|----------|
| `SQL_EXECUTION` | `SqlExecution{ executedSql, bindings?, results: StatementResult[] }` | EXECUTE_SQL / PARAMETERIZED_SQL |
| `TABLE_METADATA` | `TableMetadata{ tables: TableDef[] }` | GET_TABLE_INFO |
| `TABLE_LIST` | `TableListData{ dataSources: DataSourceDef[] }`（表级清单：schema/name/description/aliases，无列） | LIST_TABLES |
| `SEARCH_HIT` | `SearchHit{ keywords, dataSources: DataSourceDef[], terms: TermDef[] }` | SEARCH_METADATA |
| `METADATA_UPDATE` | `MetadataUpdateData{ results: MetadataUpdateResult[] }` | UPDATE_TABLE_INFO / UPDATE_COLUMN_INFO / UPSERT_TERM |

**`StatementResult` 工厂方法**（非 Jackson 多态，手写 `type()` getter）：

```java
StatementResult.select(columns, rows, totalRows)    // SELECT 成功
StatementResult.selectFailure(errorMessage)          // SELECT 失败
StatementResult.write(affectedRows)                  // WRITE 成功
StatementResult.writeFailure(errorMessage)           // WRITE 失败
```

每条 result 自带 `success: boolean`、`type: "SELECT"|"WRITE"`、`errorMessage`。多语句 SQL 会产生多个 results[]，各独立成功/失败。

**`MetadataUpdateResult`**（元数据写工具逐条结果）：`{ entity_type: TABLE|COLUMN|TERM, entity, success, change_type: CREATE|UPDATE?, old?, new?, error? }`。`old`/`new` 为 `{description, aliases}`（`new` 经 `@JsonProperty("new")` 输出，Java 关键字规避）；`error` 含 `error_category` + `message`。单条失败不阻塞其他条目（部分失败语义）。

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

#### MCP Endpoint（协议入口）

| 方法 | 路径 | 说明 |
|---|---|---|
| `POST` | `/{code}/mcp` | MCP JSON-RPC over HTTP 入口。支持 `initialize` / `ping` / `tools/list` / `tools/call` / `prompts/list` / `prompts/get`。仅读取激活快照。未知 code / DRAFT → 404；DISABLED → 503 + JSON-RPC error。请求需携带 `MCP-Protocol-Version: 2025-11-25`（initialize 豁免）；浏览器 Origin 需为 loopback 或 `dati.mcp.allowed-origins` 白名单。认证走全局 `AuthInterceptor`（APIKey 等） |

### 2.6 错误码（McpService 模块）

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
| `MS014` | 数据源或表不在 scope 内 (400) |
| `MS015` | 工具已禁用 (400) |
| `MS016` | 服务状态转换非法（仅 PUBLISHED 可停用 / 仅 DISABLED 可启用）(409) |
| `MS017` | 服务版本不存在（回滚目标版本缺失）(404) |
| `MS018` | 创建服务时数据范围必填（服务层兜底，HTTP 层由 `@NotEmpty` 拦截返回通用 400） (400) |
| `MS019` | 发布时数据范围为空 (400) |

> **注**：工具测试（US-07）使用独立的 `ToolError` 枚举 + `ToolExecuteException`，不经过 `ErrorCode`。`ToolExecuteException` 在 `McpToolTestService` 中 catch 并转为 `ToolTestResponse`，不进入 `GlobalExceptionHandler`。

### 2.7 关键设计决策

#### 创建即含数据范围

- 创建弹窗一步完成基本信息 + 数据范围（必填），`POST /v1/mcp-services` 携带 `data_scopes`，后端 `@Transactional` 内创建服务与数据范围（MS018 兜底）
- 数据范围选择器抽为 `ScopePicker` 组件，创建弹窗与详情页「数据范围」Tab 共用
- 发布时兜底校验：数据范围为空 → MS019（防旧数据/异常状态绕过创建校验）

#### 草稿-快照隔离（版本管理核心）

- **草稿区**：实时配置数据表（服务信息/Data Scope/Tools/Prompts），用户自由修改、自动保存，不影响线上
- **激活快照**：发布时全量打包生成的只读快照，`active_version_id` 指针指向当前对外响应版本，版本号单调递增
- **回滚语义**：目标版本内容全量写回草稿（含基础信息）→ 重新发布生成 vN+1。回滚后草稿 = 线上 = 目标版本内容，diff 无变化；历史保留回滚事件（Release Note "Rollback to vN"）
- **发布 ≠ 上线**：DISABLED 状态下发布只更新快照，不改变状态，需手动启用才恢复对外

#### diff 只比较业务字段

- 快照反序列化不保留审计字段、回滚恢复刷新时间戳 → 全字段 JSON 比较必然误报「已修改」
- diff 比较业务字段（toolType/name/enabled/config/scopeType/referenceId），排除 id 与审计字段
- prebuilt 变更并入 `modified_tools`（工具名 = toolType），保证有变更必有明细

#### 快照列表不暴露 content

- `GET /snapshots` 经 Assembler 转换为元信息 VO（版本号/Release Note/时间），domain 不泄漏到 API 边界
- 快照全文 content（含 SQL 模板等内部配置）保留在 domain 层，未来如需版本详情走独立接口

#### Service Code 与 Endpoint

- Service Code 作为唯一标识，格式要求：小写字母、数字、连字符、下划线，1-64 字符。
- Endpoint 路径运行时推导为 `/{code}/mcp`，不存 DB，始终反映最新 code。
- 完整 URL 前缀由配置项 `dati.mcp.endpoint-base-url`（yml，支持 `MCP_ENDPOINT_BASE_URL` 环境变量覆盖）控制：配置后返回完整 URL（如 `https://mcp.example.com/{code}/mcp`），未配置则返回相对路径。**前端不拼接 origin，以后端返回值为准**（MCP 服务独立域名部署时配置该值）。

#### 工具分为「预置」和「自定义」

- **预置工具**：名称/描述/title/annotations 存代码，inputSchema 由参数 record 生成（见「结构化参数校验」），per-service 仅存差异化 `enabled` + `config`。懒初始化默认启用，**元数据更新三工具（UPDATE_TABLE_INFO / UPDATE_COLUMN_INFO / UPSERT_TERM）默认关闭**（`McpToolType.defaultEnabled`，需显式开启，防止 LLM 未经确认写共享元数据）。元数据更新三工具无 per-service 配置（`UpdateMetadataConfig {}`）。
- **自定义工具**：用户完整 CRUD。统一路径 `/tools/`，请求体 `tool_type` 区分预置/自定义路由。
- 开关合并到 `PUT` 接口，不再需要独立 toggle 端点。

#### config 单 JSON 字段

- Domain 层 `config: ToolConfig`（强类型），PO 层 `config TEXT`（JSON 字符串）。Mapper 负责序列化/反序列化。
- 管理端 API 不返回 `annotations` / `inputSchema`（MCP 协议层自行生成）。

#### 数据范围全量替换

- 数据范围采用「先删后插」策略，客户端每次提交完整列表。空列表即清空。
- 自定义工具创建时的数据源选择列表限定为当前服务的 `data_scope` 内数据源。

#### Tool Test 异常架构

- `ToolExecuteException extends RuntimeException`（不继承 `DatiException`），避免与 HTTP 层混淆。
- 在 `McpToolTestService.test()` 中 catch → 转为 `ToolTestResponse(success=false)` → HTTP 200。
- `ToolError` 自带 `category` 字段，前端据此做差异化处理（高亮表单 / 提示调整 Scope / 展示 SQL 错误）。
- `GlobalExceptionHandler` 已移除 tool-test 专用逻辑，恢复为纯 HTTP 异常处理。

#### 结构化参数校验（单一事实源）

- 预置工具参数以 record（`param` 包）+ 注解声明，`McpParameterSchemaGenerator`（victools 4.38.0：jsonschema-generator / jsonschema-module-jackson / jsonschema-module-jakarta-validation）生成 `tools/list` inputSchema，`ToolParameterBinder`（Jakarta Validation）做运行时校验 —— 同一份注解，schema 与校验不可能漂移
- GET_TABLE_INFO 采用 decision 12：`data_source_id` 在每个 `tables[]` 项内（支持一次跨多数据源查询）
- 校验失败统一 `PARAM_INVALID`；validator 固定英文 locale（错误消息对 LLM 客户端确定性）；动态工具（PARAMETERIZED_SQL）无 record 契约，原始 Map 透传
- `ToolExecutionContext.arguments` 由 `Map<String,Object>` 改为 `Object`：预置工具为绑定后 record（`args(Class)` 访问），动态工具为原始 Map（`argumentsMap()` 访问）

#### 元数据更新工具（写共享元数据）

- 定位为「LLM 把学到的知识写回平台」：直接写共享元数据，不经过草稿/快照，写后 GET_TABLE_INFO / SEARCH_METADATA 立即可见
- scope 语义降级：写操作仅校验数据源级（`ScopeValidator.validateDataSource`），不做表级 —— 元数据写入不是数据访问；UPSERT_TERM 经 `resolveSubjectInScope` 按名称在 scope 内定位主题（找不到 → SCOPE_VIOLATION）
- 审计：每次变更（含旧值）同事务写入 `mcp_metadata_audit_log`，v1 无管理端 UI（US-09 调用日志仍未实现，但元数据写入有独立审计）
- 批量语义：1–20 条/次，调用内去重，单条失败记入 `results[i].error`（部分失败）；参数级失败（超长/类型错）为整体 PARAM_INVALID（无 data）
- aliases 全量替换（先查后写）+ 幂等语义通过 MCP annotations（idempotentHint/openWorldHint）声明给 LLM

#### 测试编排最简原则

- `McpToolTestService.test()` 仅做：resolve → 查 scope → bind（`ToolParameterBinder`）→ execute → 计时 → 组装响应。无校验、无分支（参数校验收敛在 binder 与各 Executor 内）。
- 每个 Executor 内部自行完成参数校验、scope 校验、SQL 分析、策略校验、执行、异常捕获。
- `ScopeValidator` 由 Executor 调用，不由 Service 层调用。

#### GET_TABLE_INFO 使用平台元数据

- 不再依赖 JDBC 连接查询表结构，统一使用 `TableMetadataService` 查询平台同步的元数据。
- 好处：离线可查、不消耗连接池、与 SEARCH_METADATA 返回结构一致。

#### SEARCH_METADATA Executor 薄层化

- `SearchMetadataExecutor` 只做 scope 解析 + pojo 映射，业务逻辑全在 `SemanticSearchService`。
- 结果按数据源分组 → `DataSourceDef`（含 dbType / defaultSchema / description），前端可按组展示。

#### Def 类型按领域归属

- `TableDef`、`ColumnDef`、`DataSourceDef` 放 `datasource.domain.model`
- `TermDef` 放 `semantic.domain.model`
- MCP 层 pojo（`TableMetadata`、`SearchHit`）引用这些 Def 类型，不重复定义

#### DateTime 参数类型

- `ToolParameter.type` 支持 `DateTime`。前端 `el-date-picker type="datetime"`。
- 后端 `DateTimeUtils.parseDateTime()` 处理 3 种 ISO 8601 格式 → `LocalDateTime` 用于 PreparedStatement 绑定。
- 解析失败时记录 WARN 日志，原始字符串原样传给 JDBC（不阻断执行）。

#### confirmRequired 已移除

- MCP 协议不支持工具执行的二次确认，故从 `ToolConfig.ExecuteSqlConfig` 和 `ParamSqlConfig` 中移除该字段。
- 前端已同步移除确认弹窗逻辑和相关 UI。

#### 参数化工具不做运行时 SQL 权限校验（sqlPolicy 已移除）

- 信任模型：EXECUTE_SQL 的 `sqlPolicy` 是沙箱（LLM 运行时传任意 SQL）；PARAMETERIZED_SQL 的模板由作者配置时编写，运行时仅注入参数值，作者给自己写权限属于自我设限，逻辑冗余。
- 已从 `ParamSqlConfig` 移除 `sqlPolicy` 字段与 `ParameterizedSqlExecutor` 中的 `policy.validate()` 调用（旧数据中残留的 `sql_policy` JSON 键被 `@JsonIgnoreProperties(ignoreUnknown=true)` 静默丢弃，无需迁移）。
- 保留的护栏：`ScopeValidator` 表级 scope 校验（基于渲染后 SQL 的表引用）与 `timeout` / `maxRows` 执行限制。
- 遗留：PARAMETERIZED_SQL 的 MCP annotations 仍未实现 —— 预置工具的 annotations 已落地（枚举 `annotationsJson` 声明 + `ToolDefinitionConverter.buildAnnotations` 解析），动态工具无声明来源，待后续从模板静态分析推导。

#### SQL 安全分析引擎独立于 MCP 模块

- `com.dati.db.analysis` 包是数据库层的 SQL 分析工具，不依赖 MCP 模块。
- 依赖 JSqlParser 5.1 做 AST 解析，支持操作类型识别、表引用提取、多语句检测。
- 解析失败不抛异常，统一返回 `OTHER` 类型。调用方必须显式拒绝 `OTHER`。

#### 模板引擎独立于 MCP 模块

- `com.dati.common.template` 包是通用模板引擎，不依赖 MCP 模块。
- 支持 TEXT 和 SQL 两种渲染模式，SQL 模式下对参数值按类型做安全格式化。

#### MCP Endpoint 设计决策

- **Endpoint 是薄适配层**：HTTP 关注点（路由/状态码/头）在 `McpEndpointController`，协议逻辑在 `McpEndpointService` + `McpProtocolHandler`，工具执行复用草稿区同一套 `ToolExecutor` 体系（调试与线上调用同链路）
- **无状态、无 session**：每个请求独立处理，不签发 `Mcp-Session-Id`（2025-11-25 规范允许）；Streamable HTTP 的 GET/SSE 流式传输留待后续
- **协议错误与工具错误分离**：JSON-RPC 层错误（未知方法/未知工具/参数缺失）走 `JSONRPCError`；工具执行失败（`ToolExecuteException`）转为 `CallToolResult(isError=true)`，LLM 可读取错误信息自纠正
- **tools/list 确定性输出**：预置固定顺序（SEARCH_METADATA → GET_TABLE_INFO → LIST_TABLES → EXECUTE_SQL → UPDATE_TABLE_INFO → UPDATE_COLUMN_INFO → UPSERT_TERM → PARAMETERIZED_SQL）+ 自定义按名称排序，便于 Client 端 diff；与预置/先前自定义重名的自定义工具静默跳过（防输入误导）

---

## 3. 前端架构

### 3.1 目录结构

```
src/
├── api/
│   ├── mcp-service.ts              # McpServiceVO + DataScope 类型与 API
│   ├── mcp-tool.ts                 # 工具类型（SqlPolicy, ToolParameter, McpToolVO）+ API
│   ├── mcp-tool-test.ts            # 工具测试类型（ToolTestResponse, SqlExecution, StatementResult 等）+ testTool()
│   ├── mcp-prompt.ts               # Prompt 类型（McpPromptVO, McpPromptPayload）+ API
│   └── template-preview.ts         # 模板预览/提取 API
├── components/mcp-service/
│   ├── ScopePicker.vue             # 共享数据范围选择器（创建弹窗 + 数据范围 Tab 共用）
│   ├── ToolsTab.vue                # 容器：子 Tab 切换（预置/自定义），保存后 emit refresh
│   ├── PrebuiltToolList.vue        # 预置工具卡片列表 + 测试按钮
│   ├── ExecuteSqlConfigDialog.vue  # EXECUTE_SQL 权限配置弹窗
│   ├── CustomToolList.vue          # 自定义工具列表 + 测试按钮
│   ├── CustomToolDialog.vue        # 创建/编辑弹窗表单
│   ├── DataScopeTab.vue            # 数据范围管理页面
│   ├── PromptsTab.vue              # Prompt 管理 Tab 容器
│   ├── PromptList.vue              # Prompt 列表（搜索、开关、编辑、删除）
│   ├── PromptDialog.vue            # Prompt 创建/编辑弹窗（含模板编辑器、参数提取）
│   ├── TemplatePreviewDialog.vue   # 模板预览弹窗（TEXT/SQL 双模式）
│   ├── ToolTestDialog.vue          # 工具测试弹窗薄壳（左右分栏：参数 / 结果，按类型动态加载子组件）
│   ├── tool-test/
│   │   ├── ToolTestResult.vue      # 结果容器：按 data.type 分发到 results/ 子组件
│   │   ├── params/                 # 按工具类型的参数表单
│   │   │   ├── ExecuteSqlParams.vue / GetTableInfoParams.vue / SearchMetadataParams.vue
│   │   │   ├── ListTablesParams.vue / ParameterizedSqlParams.vue
│   │   │   └── UpdateTableInfoParams.vue / UpdateColumnInfoParams.vue / UpsertTermParams.vue
│   │   └── results/                # 按结果类型的展示组件
│   │       ├── SqlExecutionResult.vue / TableMetadataResult.vue / SearchHitResult.vue
│   │       ├── TableListResult.vue
│   │       └── MetadataUpdateResult.vue
│   ├── DebugPublishTab.vue         # 版本管理 Tab（原「调试发布」）：版本历史 + 回滚 + Endpoint
│   ├── DiffSummaryList.vue         # 变更摘要组件（popover 与发布弹窗共用，支持截断）
│   ├── ParameterInput.vue          # 共享参数输入组件（按类型渲染不同控件）
│   └── SqlSecurityConfig.vue       # SQL 安全配置组件（权限 pill + 限流）
├── composables/
│   └── useTablePicker.ts           # 工具测试表/列选择器：模块级缓存 + 选中回显当前值
├── components/common/editors/
│   ├── PromptTemplateEditor.vue    # CodeMirror 编辑器（模板语法高亮 + 智能补全）
│   ├── SqlTemplateEditor.vue       # CodeMirror 编辑器（SQL 语法高亮 + 模板补全）
│   └── SqlEditor.vue               # CodeMirror 编辑器（纯 SQL 语法高亮，用于 EXECUTE_SQL 测试）
├── utils/codemirror/
│   ├── template-decorations.ts     # {{var}} / {{#if}} 语法高亮装饰器
│   ├── sql-highlight.ts            # SQL 自定义高亮样式
│   └── completions/                # 模板自动补全
│       ├── template-completions.ts
│       ├── template-completions.test.ts
│       ├── template-auto-close.ts
│       └── template-auto-close.test.ts
├── utils/
│   └── stripEmpty.ts               # 去除空值字段（null/undefined/""/[]），写工具提交前使用
└── pages/mcp-services/
    └── [id]/index.vue              # 详情页（左侧菜单：Basic / Data Scope / Tools / Prompts / ...）
```

### 3.2 组件职责

| 组件 | 职责 |
|---|---|
| `ToolsTab` | 加载分组数据，渲染预置/自定义子区域。子 Tab 带计数。 |
| `PrebuiltToolList` | 卡片列表：工具名称、描述、EXECUTE_SQL 的 sql_policy meta 信息。Setting 图标打开配置弹窗。`el-switch` 开关。每张卡片右侧有「测试」按钮。 |
| `ExecuteSqlConfigDialog` | 权限勾选组（SELECT ~ MULTI）+ maxRows + timeout。 |
| `CustomToolList` | 搜索栏 + 工具列表。编辑/删除图标。显示数据源名称。`el-switch` 开关。每张卡片操作区有「测试」按钮。 |
| `CustomToolDialog` | el-dialog 弹窗表单。el-form 校验（name/desc/SQL/数据源必填）。参数编辑器 + 执行限制（maxRows/timeout）。无 SQL 权限配置（模板由作者编写，不做运行时策略校验）。 |
| `DataScopeTab` | 数据范围管理。显示已添加列表（数据源/主题 Tag），支持删除。**选择器为独立 `ScopePicker` 组件**（数据源/主题双 Tab、分页、搜索、多选 + 已添加去重、全量提交）。含已发布提示。 |
| `PromptsTab` | Prompt 管理 Tab 容器。加载 Prompt 列表，集成 `PromptList`。 |
| `PromptList` | 搜索栏 + 列表。每个条目显示 name、description、参数计数、开关、编辑/删除图标。 |
| `PromptDialog` | 创建/编辑弹窗。**基本信息**（name/description）。**模板内容**：使用 `PromptTemplateEditor`（CodeMirror）。**参数列表**：el-table 编辑（name/required/description）+ 「提取参数」按钮调用 `/v1/template/extract` 自动填充。底部「测试渲染」按钮打开预览。 |
| `TemplatePreviewDialog` | 通用模板预览弹窗。显示原始模板 → 参数输入 → 渲染结果。支持 TEXT 和 SQL 双模式。Copy 按钮复制结果。 |
| `ToolTestDialog` | **工具测试弹窗薄壳**（左右分栏布局）：左侧参数表单、右侧结果区，按工具类型/结果类型动态加载 `tool-test/params/*` 与 `tool-test/results/*` 子组件。打开时 `resetTablePickerCache()` 清空表/列缓存；提交前 `stripEmpty()` 剔除空字段（写工具未填字段保持原值）。 |
| `ParameterInput` | **共享参数输入组件**。按 `ToolParameter.type` 渲染：String → el-input，Number → el-input type="number"，Boolean → el-switch，DateTime → el-date-picker type="datetime"，Array → el-input-tag，default → el-input。被 ToolTestDialog 和 TemplatePreviewDialog 共用。 |
| `DebugPublishTab` | **版本管理 Tab**（原「调试发布」）。展示当前版本 Tag + MCP Endpoint 复制 + 版本历史表格（Live Tag / Release Note / 回滚按钮）。纯展示 + 回滚：发布/停用/启用已移至详情页右上角。回滚确认弹窗明示「未发布的草稿修改将被覆盖」。 |
| `DiffSummaryList` | **变更摘要组件**。props：`items`（label/detail/added/modified/deleted）+ `limit?`（截断数）+ `title?`。内置 max-height 滚动。popover（hover 感知，截断 5 项）与发布弹窗（完整展示）共用。 |
| `SqlSecurityConfig` | 可复用的 SQL 安全配置组件。权限 pill（SELECT/INSERT/UPDATE/DELETE/DDL/MULTI）+ maxRows + timeout。 |
| `ExecuteSqlParams` / `GetTableInfoParams` / `SearchMetadataParams` / `ParameterizedSqlParams` | 读工具参数表单：SQL 编辑器（CodeMirror）、表/列下拉（useTablePicker）、关键词 `el-input-tag`、`ParameterInput` 动态表单等，按工具类型切换。 |
| `ListTablesParams` | LIST_TABLES 参数表单：无参数，仅提示文案（`getArgs` 返回空对象）。 |
| `UpdateTableInfoParams` / `UpdateColumnInfoParams` / `UpsertTermParams` | 写工具参数表单：可增删的条目列表 + useTablePicker 表/列级联选择，选中即回显当前 description/aliases（「先查后写」+ 别名全量替换提示），提交前 stripEmpty。 |
| `SqlExecutionResult` / `TableMetadataResult` / `SearchHitResult` / `MetadataUpdateResult` | 结果展示组件：SELECT 表格/写操作卡片、表元数据卡片、术语 + 分组表卡片、逐条变更对照（old→new、CREATE/UPDATE 标签、失败条目 error_category 高亮）。 |
| `TableListResult` | TABLE_LIST 结果组件：按数据源分组的表级清单卡片（schema.表名 + 描述 + 别名 tag + 总数统计）。 |
| `useTablePicker` | 工具测试共享 composable：模块级 tables/columns 缓存（弹窗打开时重置），ds→schema/table 级联、表→列级联，选中后拉取并回显当前元数据值（description/aliases）。 |
| `SqlEditor` | CodeMirror 6 + `@codemirror/lang-sql`。纯 SQL 语法高亮编辑器，用于 EXECUTE_SQL 工具测试的 SQL 输入。 |
| `PromptTemplateEditor` | CodeMirror 6 包装。支持：模板语法高亮（`{{}}`、`{{#if}}`）、智能补全、自动闭合、bracket matching、行包裹。 |
| `SqlTemplateEditor` | CodeMirror 6 + `@codemirror/lang-sql`。SQL 语法高亮 + 模板语法高亮 + 自定义自动补全 + 自动闭合。 |

### 3.3 交互细节

- **详情页右上角（服务级状态操作区）**：DRAFT → [发布]；PUBLISHED → [发布变更]（仅 has_changes，hover 弹出变更摘要 popover）+ [停用]；DISABLED → [启用] + [发布变更]。删除入口统一在列表页
- **变更感知三级递进**：① hover「发布变更」→ popover 摘要（截断 5 项）② 发布弹窗 → 完整变更摘要 + release note + 确认 ③ 版本管理 Tab → 版本历史/回滚
- **草稿修改联动**：Tools / Prompts / DataScope / 基础信息保存后 emit refresh → 父页刷新 service + diff →「发布变更」按钮实时出现
- **发布弹窗文案按状态区分**：DRAFT 首次发布 / PUBLISHED 覆盖线上 / DISABLED 提示「发布后仍保持停用，需启用才对外」
- **回滚确认弹窗**：明示「将回滚至 vN，当前未发布的草稿修改将被 vN 内容覆盖」
- **预置工具区**：开关 + EXECUTE_SQL 的 Setting 图标 + 「测试」按钮。无删除、无编辑名称。
- **自定义工具区**：Edit/Delete 图标 + 「测试」按钮。hover 变色（Edit 蓝色，Delete 红色）。
- **配置弹窗**：权限 pills 切换（选中态紫色 → 蓝色高亮）。安全警告黄色提示。
- **工具测试弹窗**：左右分栏布局（参数左 / 结果右），参数表单与结果组件按类型拆分子组件（`tool-test/params|results`）。`SqlEditor`（CodeMirror）用于 SQL 输入。PARAMETERIZED_SQL 使用 `ParameterInput` 动态表单。GET_TABLE_INFO / 元数据更新工具使用 `useTablePicker` 表/列选择器（选中表/列自动回显当前 description/aliases，写工具提示「别名全量替换、留空保持不变」）。SEARCH_METADATA 的关键词用 `el-input-tag` 输入；LIST_TABLES 无参数直接执行。执行结果按 `data.type` 分发渲染：SELECT → `el-table` + 行数提示；WRITE → 操作摘要卡片；TABLE_METADATA → 每表一个卡片（表名/列/别名/样本值）；TABLE_LIST → 按数据源分组的表清单卡片；SEARCH_HIT → 术语卡片 + 按数据源分组表卡片；METADATA_UPDATE → 逐条变更对照（old→new、CREATE/UPDATE 标签、失败条目红字 error）。关闭弹窗时自动清空表单和结果。
- **删除确认（US-10）**：与全站其他删除操作一致 —— `ElMessageBox.confirm`（黄色警告图标 + 单句文案「确定要删除 MCP 服务「{name}」吗？」）。**删除入口仅在列表页行尾**（详情页不提供，与 US-08 定稿一致）。列表页删除成功 → 刷新列表（删除最后一条时回退一页）；删除失败 toast 提示可重试。
- **抽屉表单**：使用 Element Plus `el-form` 的 `FormRules` 校验，保存前 `validate()`，异常时 `clearValidate()`。
- **错误处理**：统一 `catch (e: any)` + `e?.message` 展示后端错误信息。
- **数据范围弹窗**：Tab 切换（数据源/主题）。分页加载 + 搜索防抖 300ms。已添加项显示灰色 + `el-tag type="info"` 禁用勾选。确认提交后全量替换。已发布服务显示警告提示。
- **Prompt 弹窗**：`content` 使用 CodeMirror 编辑器。「提取参数」调用 `/v1/template/extract` 自动扫描 `{{var}}` 并填充参数表。参数表 el-table 内联编辑。
- **模板预览**：参数输入框自动按 parameters 列表生成，使用 `ParameterInput` 共享组件。SQL 模式下根据 `type` 字段做类型转换。结果显示在只读代码区域。

### 3.4 数据流

```
详情页（侧边导航） → Tool / Data Scope / Prompts 各 Tab

ToolsTab
    ├─ GET /tools → { prebuilt, custom }
    │
    ├─ 预置：
    │    ├─ 开关 → PUT /tools/{toolType} { tool_type, enabled }
    │    ├─ 配置 → PUT /tools/EXECUTE_SQL { tool_type, enabled, config: JSON.stringify(...) }
    │    └─ 测试 → POST /tools/{toolType}/test { arguments: {...} } → ToolTestDialog
    │
    └─ 自定义：
         ├─ 新建 → POST /tools { tool_type, name, description, config: JSON.stringify(...) }
         ├─ 编辑 → PUT /tools/{id} { tool_type, name, description, enabled, config }
         ├─ 开关 → PUT /tools/{id} { tool_type, enabled }
         ├─ 测试 → POST /tools/{id}/test { arguments: {...} } → ToolTestDialog
         └─ 删除 → DELETE /tools/{id} → 确认弹窗

DataScopeTab
    ├─ GET /data-scope → { items: [...], resolved_data_sources: [...] }
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

ToolTestDialog
    ├─ EXECUTE_SQL:      GET /data-scope → 数据源下拉选项
    ├─ GET_TABLE_INFO:   GET /data-sources/{id}/table-infos → schema/table 下拉选项（useTablePicker）
    ├─ UPDATE_* / UPSERT_TERM: useTablePicker 级联选择 + 当前值回显 → stripEmpty 提交
    ├─ ALL:              POST /tools/{toolId}/test { arguments } → ToolTestResponse
    └─ 结果分发:        data.type → SELECT table / WRITE card / TABLE_METADATA list / TABLE_LIST groups / SEARCH_HIT groups / METADATA_UPDATE per-item list

详情页（发布与版本管理）
    ├─ 右上角按钮区（页面级）:
    │    ├─ 发布 → POST /publish { release_note }（弹窗内嵌变更摘要）
    │    ├─ 停用 → POST /disable（确认弹窗）
    │    ├─ 启用 → POST /enable
    │    └─ 变更摘要 popover ← GET /diff（页面级 diff 状态）
    │
    └─ 版本管理 Tab（DebugPublishTab）:
         ├─ GET /snapshots → 版本历史表格（Live Tag / 回滚）
         ├─ 回滚 → POST /rollback { target_version_number }（确认弹窗）
         └─ 回滚成功 → emit refresh → 父页刷新 service + diff → Tab 重载版本历史

数据流（状态提升）:
    index.vue 持有 service + diff（真相源）
    ├─ 右上角操作 → refreshAll() = loadService() + loadDiff()
    ├─ ToolsTab / PromptsTab / DataScopeTab 保存后 emit refresh → refreshAll
    └─ DebugPublishTab（props: service）内部加载 snapshots，回滚后 emit refresh
```

---

## 4. 测试

后端总计 **830 测试**（2026-08-14 全量回归），核心覆盖：

- `McpServiceServiceTest` / `McpServiceControllerTest`：服务 CRUD、code 校验、级联删除
- `McpServicePublishServiceTest`：发布 / 停用 / 启用 / diff / 回滚 / 状态机（21 用例）
- `McpEndpointControllerTest` / `McpProtocolHandlerTest`：JSON-RPC 端点与协议分发
- `SqlAnalyzerTest`：75 条参数化用例（类型识别 / 表提取 / 多语句 / 事务预扫描）
- `ToolParameterBinderTest` / `McpParameterSchemaGeneratorTest`：结构化参数校验与 inputSchema 生成
- 各 Executor 测试：`ExecuteSqlExecutorTest` / `GetTableInfoExecutorTest` / `ListTablesExecutorTest` / `SearchMetadataExecutorTest` / `UpdateTableInfoExecutorTest` / `UpdateColumnInfoExecutorTest` / `UpsertTermExecutorTest`
- `McpPromptServiceTest` / `TemplatePreviewControllerTest`：Prompt 校验与模板渲染

完整测试类清单见源码 `backend/src/test/java/com/dati/mcp/`。
## 5. 已实现 vs 未实现

| User Story | 标题 | 状态 | 说明 |
|---|---|---|---|
| US-01 | 服务创建与基础管理 | ✅ 已实现 | 服务 CRUD、code 校验、分页列表 |
| US-02 | 数据范围配置 | ✅ 已实现 | 数据源 + 主题引用，全量替换 |
| US-03 | 工具管理 | ✅ 已实现 | 预置工具（7 种）+ 自定义工具 CRUD。**2026-08 追加元数据更新预置工具**（UPDATE_TABLE_INFO / UPDATE_COLUMN_INFO / UPSERT_TERM，见 2.2 元数据更新工具）；**追加 LIST_TABLES 表清单工具**（表级概览，供 LLM 全局视图后接 get_table_info） |
| US-04 | 创建与配置 Resource | ❌ V1 暂缓 | 见 US-04 文档说明 |
| US-05 | 创建与配置 Prompt | ✅ 已实现 | Prompt CRUD + 模板校验 + 参数一致性检查 |
| US-5.5 | 模板引擎基础设施 | ✅ 已实现 | Handlebars 风格 Parser + Text/SQL Renderer |
| US-06 | 管理服务 Token | ❌ V1 暂缓 | 统一在应用层认证，MCP 模块不单独设计 |
| US-07 | 调试 Tool 调用 | ✅ 已实现 | 工具测试弹窗、8 种 Executor、scope 校验、异常处理、前端结果渲染（参数表单/结果组件拆分为 tool-test 子组件） |
| US-08 | 发布与版本管理 | ✅ 已实现 | 草稿-快照隔离、发布/发布变更、停用/启用、版本历史与回滚、草稿 vs 线上 diff。**MCP Endpoint 已实现**（JSON-RPC over HTTP，见 2.2 协议层），US-08 遗留任务 #2/#3/#4 已落地；遗留：Streamable HTTP 的 GET/SSE 流式传输与 session 管理 |
| US-09 | 查看服务调用日志 | ❌ 未实现 | 无 `mcp_audit_log` 表和对应接口。**注**：元数据写入审计（`mcp_metadata_audit_log`）已实现，仅覆盖 UPDATE_TABLE_INFO / UPDATE_COLUMN_INFO / UPSERT_TERM 的写入变更（含旧值），不覆盖工具调用日志 |
| US-10 | 删除 MCP 服务 | ✅ 已实现 | 事务级联删除（快照/数据范围/预置工具/自定义工具/Prompt），已发布服务可直接删除；前端与全站删除操作一致（`ElMessageBox.confirm` 简单确认）。**遗留**：「仅管理员可删除」待角色体系统一实现 |

> **说明**：详情页侧边导航已实现 Tab：基础信息 / 数据范围 / Tools / Prompts / 版本管理。`security`（US-06 暂缓）、`logs`（US-09 未实现）Tab 已从侧边导航移除（占位入口不下发，等实现后再加回）。发布/停用/启用操作位于详情页右上角，删除仅在列表页行尾。

---

## 6. 关联文档

- 模板引擎：[template-engine.md](template-engine.md)
- 授权架构：[permission.md](permission.md)（scope 校验与传播校验）
- 数据源模块：[datasource.md](datasource.md)
- 语义管理模块：[semantic.md](semantic.md)
- E2E 用例：`e2e-tests/test-cases/mcp-service.md`、`e2e-tests/test-cases/mcp-endpoint.md`
