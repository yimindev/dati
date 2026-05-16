# MCP 构建功能原始需求设计

## 1. 背景

DatI 已初步完成数据源维护和语义管理能力。数据源维护提供数据库连接、表结构、字段等基础元数据；语义管理通过主题、术语、表字段关联等方式补充业务语义。

MCP 构建功能的目标是在这些已有能力之上，将数据库和元数据能力封装为符合 MCP 协议的服务，让外部 Agent、LLM 应用或 MCP Client 能够通过标准 MCP 方式访问数据库能力。

本功能不是一个固定的 NL2SQL 应用，也不是强业务流程编排系统。它更接近一个「数据库能力到 MCP 服务的中间层」：DatI 提供元数据检索、SQL 执行、参数化 SQL、资源和 Prompt 等原子能力，用户根据自己的场景自由组合成 MCP 服务。

### 1.1 MCP 协议版本

V1 基于 MCP 协议 **2025-11-25** 版本，传输方式为 **Streamable HTTP**。

MCP endpoint 路由：`/{serviceId}/mcp`，**同时支持 POST 和 GET 方法**，通过 JSON-RPC 方法名区分 `tools/list`、`tools/call`、`resources/list`、`prompts/list` 等操作。

**传输层关键要求**（协议硬性规定）：

- 所有 HTTP 请求必须携带 `MCP-Protocol-Version: 2025-11-25` Header（协议硬性要求，由 MCP 框架实现）。
- 服务初始化时可返回 `MCP-Session-Id` Header，Client 后续请求必须携带该 Session ID（V1 不启用，无状态模式）。
- Client 请求的 `Accept` Header 必须包含 `application/json` 和 `text/event-stream`。
- 服务 `initialize` 响应中声明的能力应与该 MCP 服务实际配置匹配（有 Tool 则声明 `tools`，有 Resource 则声明 `resources`，有 Prompt 则声明 `prompts`）。

---

## 2. 产品定位

MCP 构建模块用于将 DatI 中维护的数据源、表、字段、术语、主题等元数据和数据库执行能力封装为 MCP 服务。

用户可以自由配置 MCP 服务中的：

- Tools
- Resources
- Prompts
- 数据访问范围
- SQL 执行权限
- 服务认证方式
- 调用审计策略

主题不是最终对外暴露的核心概念。主题主要用于 DatI 内部管理元数据，构建 MCP 服务时可作为快捷选择，帮助用户快速选中一组表、字段、术语和描述信息。最终 MCP Client 是否感知主题，由服务配置决定。

---

## 3. 设计原则

### 3.1 简单优先

DatI 尽量少做业务假设，不强行把 MCP 服务限定为某种特定应用形态。系统提供通用原子能力，用户负责按场景组合。

### 3.2 灵活优先

原则上，只要是数据库和对应元数据能支撑的能力，都应该可以被封装为 MCP 服务能力。系统不应把工具类型、资源内容、Prompt 模板限制得过死。

### 3.3 元数据辅助构建

语义层维护的主题、术语、维度、表字段描述等信息，主要用于帮助用户更方便地构建 MCP 服务，也用于帮助 Agent 理解数据结构。

### 3.4 SQL 是核心能力

MCP 服务需要支持自由 SQL 执行，也需要支持参数化 SQL。SQL 是否允许 SELECT、INSERT、UPDATE、DELETE、DDL、多语句等能力，由用户在 Tool 层配置。

### 3.5 默认可控，显式放开

系统默认配置应相对保守，例如自由 SQL 默认只允许 SELECT，写操作和 DDL 需要用户显式开启。但产品能力本身要允许用户放开限制。

### 3.6 权限模型可扩展

V1 按 MCP 服务凭证执行数据库访问。长期需要支持按调用者身份执行，因此执行上下文和权限模型需要预留扩展点。

### 3.7 协议适配而非协议绑定

MCP 协议在 Tool / Resource / Prompt 的抽象上已经比较完备，DatI 的内部实体可以直接沿用这些概念命名。但核心业务逻辑（Tool 执行、权限校验、认证鉴权、审计记录）应作为**协议无关的能力层**实现，不在代码中直接依赖 MCP 特有的数据结构（如 `content[]`、`isError`、JSON-RPC 错误码）。

MCP JSON-RPC 层只是这些能力的**一个调用方**。它的职责是：
- 接收 MCP Client 的请求 → 调用能力层获取执行结果 → 将结果包装为 MCP 协议格式返回
- 未来如果支持 Skill / OpenAI Function Calling / REST API，只需新增对应的适配层，复用同一套能力

V1 只实现 MCP 这一个调用方，但能力层的接口和返回值设计不嵌入 MCP 概念。

---

## 4. 典型使用场景

### 4.1 NL2SQL 数据分析服务

用户已经在 DatI 中维护好数据源、表字段描述、术语和主题。

用户创建一个「销售数据分析 MCP 服务」，选择相关数据源、表或主题，开启以下工具：

- 元数据关键词检索（`SEARCH_METADATA`）
- 表结构详情查询（`GET_TABLE_INFO`）
- SQL 执行（`EXECUTE_SQL`）

Agent 调用流程：

1. 用户向 Agent 提问：「上个月华东区销售额是多少？」
2. Agent 调用元数据检索工具，搜索「销售额」「华东」「月份」等关键词。
3. DatI 返回相关表、字段、术语和描述（含 `data_source_id`）。
4. Agent 生成 SQL。
5. Agent 调用 SQL 执行工具，传入 SQL 和 `data_source_id`。
6. DatI 返回查询结果，Agent 组织最终回答。

在该场景中，DatI 不需要理解完整业务问题，只需要提供可靠的元数据检索和 SQL 执行能力。

### 4.2 任务管理 MCP 服务

用户有一套任务管理相关数据库表，例如：

```text
tasks
- id
- title
- status
- assignee
- due_date
- priority
```

用户创建一个「任务管理 MCP 服务」，通过参数化 SQL（`PARAMETERIZED_SQL`）将数据库操作封装为业务工具：

- `list_tasks`
- `get_task`
- `create_task`
- `update_task_status`
- `assign_task`
- `delete_task`

每个工具绑定一条参数化 SQL 模板。例如 `update_task_status`：

```json
{
  "task_id": "123",
  "status": "done"
}
```

背后执行：

```sql
UPDATE tasks SET status = :status WHERE id = :task_id
```

在该场景中，MCP Client 看到的是清晰的业务工具，而不是通用 SQL 工具。

---

## 5. 核心概念

### 5.1 MCP 服务

最终发布给外部 MCP Client 使用的服务实例。

一个 MCP 服务包含：

- 基础信息
- 数据访问范围
- Tools 配置
- Resources 配置
- Prompts 配置
- SQL 权限策略（Tool 级）
- 认证配置
- 执行身份策略
- 审计配置
- 发布状态

### 5.2 数据范围

MCP 服务允许访问的数据集合。

数据范围可以从以下来源选择：

- 数据源
- schema
- 表
- 主题
- 术语或元数据集合

主题只是快捷选择方式之一。用户选择主题后，系统读取该主题关联的表、字段、术语和描述，作为构建 MCP 服务的可用元数据。

### 5.3 原子能力

DatI 内置的、可被 MCP Tool 绑定的底层能力。

V1 支持：

| 原子能力 | 说明 |
| --- | --- |
| 元数据检索 | 根据关键词搜索表、字段、术语、描述等元数据，跨数据源搜索 |
| 元数据详情 | 精确查询指定表、字段等对象的详细信息 |
| 自由 SQL 执行 | 执行调用方传入的 SQL |
| 参数化 SQL | 执行用户预先配置的 SQL 模板，调用方传入参数值 |

### 5.4 MCP Tool

对外暴露给 MCP Client 的可调用工具。

Tool 不等同于原子能力。一个 Tool 可以绑定一个原子能力，并通过名称、描述、输入参数和执行配置包装成具体场景下的工具。

MCP 协议中 Tool 包含以下标准字段，DatI 根据用户配置和系统规则自动生成：

| 协议字段 | 说明 | DatI 来源 |
| --- | --- | --- |
| `name` | 唯一标识，1-128 字符，允许字符：A-Z, a-z, 0-9, `_`, `-`, `.`，服务内唯一 | 用户配置 |
| `title` | 可选的人类可读显示名 | 用户配置 |
| `description` | 功能描述 | 用户配置 |
| `inputSchema` | JSON Schema 定义输入参数，**必须**是合法 JSON Schema 对象（不能为 null）。无参数 Tool 使用 `{"type":"object","additionalProperties":false}` | 用户配置 |
| `annotations` | 工具行为标注（`readOnlyHint`、`destructiveHint`、`idempotentHint`），由 SQL 权限自动映射（见 7.10 节） | 系统生成 |
| `outputSchema` | 可选的输出 JSON Schema | V1 不做 |

Tool 的执行由**能力层**完成，返回协议无关的执行结果（成功时返回结果数据 + 元信息，失败时返回错误描述 + 错误类型）。**MCP 协议适配层**负责将执行结果包装为 MCP 标准格式（`content` 数组 + `isError` 标志），并将协议错误（如未知 Tool、无效参数）映射为 JSON-RPC 标准 `error` 响应。

### 5.5 MCP Resource

对外暴露给 MCP Client 的资源内容。

Resource 可用于暴露 schema、术语表、说明文档、示例 SQL、服务能力说明等上下文。

MCP 协议中 Resource 支持可选的 `annotations`，帮助 Client 判断资源用途：

- `audience`：目标受众，`"user"`（人类用户）或 `"assistant"`（AI 模型），可同时指定
- `priority`：重要性，0.0（可选）到 1.0（必须）
- `lastModified`：ISO 8601 格式的最后修改时间

V1 这些 annotations 使用系统默认值，暂不提供 UI 配置入口。

### 5.6 MCP Prompt

对外暴露给 MCP Client 的 Prompt 模板。

Prompt 可用于辅助 NL2SQL、结果解释、SQL 风险检查、业务操作引导等场景。

MCP 协议中 Prompt 的参数模型为简单结构（非 JSON Schema）：每个参数包含 `name`（名称）、`description`（描述）、`required`（是否必填）。V1 参数模型与此对齐。

---

## 6. 已确认产品决策

| 问题 | 决策 |
| --- | --- |
| MCP 协议版本 | 2025-11-25，Streamable HTTP |
| Endpoint 路由 | `/{serviceId}/mcp`（同时支持 POST 和 GET），以系统生成的 ID 作为服务标识 |
| 数据范围选择 | 支持数据源、表、主题。主题作为引用保存（主题更新后服务范围自动反映），不展开为独立表。schema 不作为独立选择维度，仅作为表归属前缀展示 |
| 主题定位 | 主题是内部元数据组织方式，不是最终用户必须感知的对外概念 |
| 是否支持多主题 | 支持，主题仅作为数据范围快捷选择 |
| 是否支持用户自定义 Tool | 支持，且应作为核心能力 |
| 自定义 Tool V1 执行方式 | 参数化 SQL（`PARAMETERIZED_SQL`） |
| 是否提供自由 SQL Tool | 提供，作为 NL2SQL 场景核心能力（`EXECUTE_SQL`） |
| 表 CRUD 快速生成 | V1 不提供，通过参数化 SQL 手写模板实现，后续版本支持 |
| 多数据源 | 支持。元数据检索跨数据源搜索，返回结果携带 `data_source_id`；SQL 执行时由调用方指定 `data_source_id` |
| SQL 写操作 | 支持，但需要显式开启 |
| DDL 操作 | 支持，但需要显式开启 |
| 权限模型 | V1 仅 Tool 级权限，不做服务级 |
| 执行身份 | V1 按服务凭证执行，长期支持调用者身份 |
| 主题版本 | 不固定主题版本，主题和元数据更新后服务读取最新信息 |
| 修改已发布服务 | 编辑时线上继续服务旧版本，修改保存到草稿副本，发布时草稿覆盖线上 |
| 数据范围变更 | 为引用主题模式，主题增删表自动反映到服务范围。直接选的表如被 Tool 引用则不允许移除，保存时校验阻止 |
| 模板引用语法 | V1 不做，Prompt 为纯文本。Prompt 文本中参数用 `{{paramName}}` 双花括号占位替换 |
| 错误处理 | 分两层：协议错误（未知 Tool、无效参数等）由 MCP 适配层映射为 JSON-RPC 标准错误码；Tool 执行错误（SQL 错误等）由适配层包装为 `isError: true` 透传给 Client，供 LLM 自我纠正 |
| V1 Resource 类型 | 只保留 `examples`（示例 SQL，用户手写）和 `sql-policy`（权限说明，系统生成）。`schema` 和 `terms` 由 Tool 覆盖 |
| 审计日志 | 删除服务时级联删除审计日志。调用来源记录 Client IP + MCP Client 的 `clientInfo.name` |
| 调试 Tool 调用 | 先执行 Tool 自身权限校验 → 写操作弹出二次确认 → 确认后真实执行 |
| 无 Tool 发布 | 允许（纯 Resource + Prompt 的 MCP 服务也有场景） |
| 停用后启用 | URL 不变，Token 不变，保留原配置 |
| 内置 Prompt 模板 | V1 不提供，用户自行编写 Prompt 内容 |
| SEARCH_METADATA 返回格式 | 按数据源分组返回 |
| GET_TABLE_INFO 数据来源 | 读 DatI 本地元数据（TableInfoPO），Tool 描述中注明可用 SQL 查询最新结构 |
| PARAMETERIZED_SQL 参数类型 | String / Number / Boolean / Date / Array |
| 多数据源重名表 | 展示时带数据源名前缀区分 |
| Token 认证 | `Authorization: Bearer <token>`。每个服务支持多 Token，可分别命名、启停 |
| 并发控制 | V1 不做 SQL 执行并发限制。调用频率限制由网关层统一控制，不在 MCP 模块处理 |
| Tool annotations | 由系统根据 SQL 权限配置自动生成（`readOnlyHint` / `destructiveHint` / `idempotentHint`），无需用户手动配置 |
| Resource annotations | `audience` / `priority` / `lastModified` 使用系统默认值，V1 暂不提供 UI 配置 |
| Prompt 参数模型 | 对齐 MCP 协议：`{ name, description, required }` 简单结构，非 JSON Schema。Prompt 文本中参数用 `{{paramName}}` 占位 |

---

## 7. 功能需求

### 7.1 MCP 服务管理

用户可以创建、编辑、发布、停用和删除 MCP 服务。

MCP 服务基础信息包括：

- 服务名称
- 服务描述
- 服务状态
- Endpoint
- 创建人
- 更新人
- 发布时间

服务状态包括：

- 草稿
- 已发布
- 已停用

**MCP 连接生命周期**：当外部 MCP Client 连接时，须完成协议规定的初始化握手：

1. Client 发送 `initialize` 请求（含协议版本、客户端能力）。
2. Server（DatI）返回 `InitializeResponse`，声明本服务支持的能力：`tools` / `resources` / `prompts`，均带 `listChanged: true`。如果该 MCP 服务未配置某类能力（如无 Prompt），则不声明对应 capability。
3. Client 发送 `notifications/initialized` 通知，握手完成，进入正常操作阶段。

要求：

- 草稿状态允许编辑全部配置。
- 新创建的服务为草稿状态。
- 发布后生成 MCP endpoint，对外提供服务。**发布时系统生成一份已发布快照**，MCP endpoint 始终读取已发布快照的内容。Client 通过定期 `tools/list` 等方式感知配置变更，V1 不做 `list_changed` 推送通知。
- 已发布服务可继续编辑，编辑保存到**草稿副本**，线上服务不受影响。需再次发布后，草稿副本覆盖已发布快照，变更才对外生效。
- 已停用状态不再允许外部调用（endpoint 返回明确错误）。
- 停用后再次启用，URL 和 Token 均保持不变，保留原配置，但需重新发布。

### 7.2 数据范围配置

用户可以为 MCP 服务配置可访问的数据范围。

数据范围选择方式：

- **选择数据源**：整体选中一个数据源，该数据源下的所有 schema 和表自动纳入范围。
- **选择表**：直接从数据源下选择具体表。多数据源下有重名表时，通过数据源名前缀区分（如 `ds_A / public.tasks`）。
- **选择主题**：以**引用**方式选择 DatI 中已维护的主题。主题后续新增或删除表时，服务的数据范围自动同步更新。

schema 不作为独立选择维度，仅作为表列表中每张表的归属前缀信息展示。

要求：

- 数据范围可以同时包含数据源、直接选表和主题引用，取并集。
- MCP Client 默认不需要感知主题的存在。
- 保存时校验：**直接选择的表**如果已被 Tool 引用，则不允许从范围中移除，需先处理关联 Tool。主题引用内的表不受此约束（由主题自身管理）。
- 如果主题、术语、字段描述更新，MCP 服务读取最新元数据。

### 7.3 Tool 配置

用户可以创建、编辑、删除、启用、禁用 Tool。

Tool 配置字段：

| 配置字段 | 说明 | 对应协议字段 |
| --- | --- | --- |
| Tool 名称 | Tool 的唯一标识。命名约束：1-128 字符，仅允许 A-Z, a-z, 0-9, `_`, `-`, `.`，服务内唯一 | `name` |
| Tool 显示名 | 可选的人类可读名称 | `title` |
| Tool 描述 | 功能描述，帮助 MCP Client 理解工具用途 | `description` |
| 是否启用 | 控制是否在 `tools/list` 中出现 | — |
| 输入参数 schema | JSON Schema 定义输入参数。**必须**为合法 JSON Schema 对象（不能为 null）。无参数时使用 `{"type":"object","additionalProperties":false}` | `inputSchema` |
| 输出格式说明 | 描述工具返回内容的格式，供 Agent 理解 | — |
| 绑定能力类型 | 选择 `SEARCH_METADATA` / `GET_TABLE_INFO` / `EXECUTE_SQL` / `PARAMETERIZED_SQL` | — |
| 执行配置 | 数据源绑定、超时时间、最大返回行数 | — |
| 权限配置 | SQL 操作权限（见 7.10 节），系统据此自动生成 `annotations` | `annotations` |
| 是否需要确认 | 调用前是否要求用户确认。不影响 annotations 字段本身，确认逻辑由 Client 根据 annotations 自行决定 | — |

**Tool Annotations 自动映射**：根据 Tool 的 SQL 权限配置，系统自动生成 MCP 协议 `annotations` 字段：

| 权限配置 | 生成的 annotations |
| --- | --- |
| 仅允许 SELECT | `readOnlyHint: true`, `destructiveHint: false`, `idempotentHint: true` |
| 允许 INSERT / UPDATE / DELETE | `destructiveHint: true`, `readOnlyHint: false` |
| 允许 DDL | `destructiveHint: true` |

V1 Tool 类型：

| Tool 类型 | 说明 |
| --- | --- |
| `SEARCH_METADATA` | 元数据关键词模糊检索，跨数据源搜索表、字段、术语、描述 |
| `GET_TABLE_INFO` | 精确查询指定表、字段的详细定义 |
| `EXECUTE_SQL` | 自由 SQL 执行工具 |
| `PARAMETERIZED_SQL` | 参数化 SQL 模板工具 |

要求：

- 用户可以从内置模板快速创建 Tool。
- 用户可以修改 Tool 的名称、显示名、描述和参数。
- Tool 描述中应包含其可操作的数据范围和能力限制，帮助 Agent 正确选择工具。
- Tool 名称需校验命名约束。
- Tool 的执行由能力层完成，返回协议无关的执行结果。具体返回格式由调用方的协议适配层决定（MCP 适配层包装为 content/isError 格式）。

### 7.4 元数据检索 Tool（SEARCH_METADATA）

元数据检索 Tool 用于 NL2SQL 场景下帮助 Agent 查找相关表、字段、术语和描述。

输入示例：

```json
{
  "keyword": "销售额",
  "limit": 10
}
```

返回内容包括：

- 匹配类型：表、字段、术语、字段值等
- 名称
- 描述
- 所属数据源
- 所属表
- 字段类型
- 相关术语
- 匹配原因

要求：

- 检索范围受 MCP 服务数据范围约束。
- 跨数据源搜索，**返回结果按数据源分组**，每组内的匹配项按相关性排序。
- 每个匹配项携带 `data_source_id`，Agent 可据此构造后续 SQL 调用。
- 可以复用语义管理已有 Elasticsearch 索引能力。
- 返回结果应便于 Agent 生成 SQL。

### 7.5 元数据详情 Tool（GET_TABLE_INFO）

元数据详情 Tool 用于精确查询指定表或字段的详细信息。

输入示例：

```json
{
  "data_source_id": "ds_001",
  "table": "tasks",
  "fields": ["id", "status", "due_date"]
}
```

返回内容包括：

- 表名、描述
- 字段列表（名称、类型、是否可空、默认值、描述、关联术语）
- 所属数据源

要求：

- 查询范围受 MCP 服务数据范围约束。
- **数据来源为 DatI 本地元数据**（`TableInfoPO` / `ColumnInfoPO`），不实时连接数据源。
- Tool 描述中应注明「基于已同步的元数据。如需最新表结构，可执行 `SHOW COLUMNS FROM table` 或查询 `information_schema`」。
- 返回的表结构信息应足够 Agent 生成正确的 SQL。

### 7.6 自由 SQL 执行 Tool（EXECUTE_SQL）

自由 SQL 执行 Tool 用于执行调用方传入的 SQL。

输入示例：

```json
{
  "data_source_id": "ds_001",
  "sql": "SELECT * FROM tasks LIMIT 10"
}
```

要求：

- `data_source_id` 由调用方指定，Agent 可以从元数据检索结果中获取。
- 是否启用由 Tool 配置决定。
- SQL 操作类型受 Tool 级权限策略控制。
- 执行范围受数据范围约束。
- 支持最大返回行数限制。
- 支持超时时间限制。
- 所有调用必须记录审计日志。
- 执行结果由能力层返回（成功返回结果数据 + 元信息，失败返回错误描述），由 MCP 适配层包装为 content/isError 格式。出错时错误信息需足够清晰，供 LLM 阅读并尝试自我纠正。

### 7.7 参数化 SQL Tool（PARAMETERIZED_SQL）

参数化 SQL Tool 用于将固定 SQL 模板封装为业务 Tool。

配置示例：

```sql
SELECT *
FROM tasks
WHERE status = :status
ORDER BY due_date ASC
LIMIT :limit
```

Tool 输入参数示例：

```json
{
  "status": "todo",
  "limit": 20
}
```

要求：

- 绑定数据源（创建 Tool 时指定，不在调用时传入）。
- 用户可以编辑 SQL 模板。
- 用户可以定义参数名称、类型（String / Number / Boolean / Date / Array）、是否必填、默认值和描述。
- 执行时使用参数绑定，避免字符串拼接。
- 参数化 SQL 仍受 Tool 级 SQL 权限策略约束。
- 可用于查询、写入、更新、删除等场景。
- 执行结果由能力层返回（成功返回结果数据 + 元信息，失败返回错误描述），由 MCP 适配层包装为 content/isError 格式。出错时错误信息需足够清晰。

### 7.8 Resource 配置

用户可以创建、编辑、删除、启用、禁用 Resource。

V1 支持两种 Resource：

| Resource | URI | 内容来源 | 说明 |
| --- | --- | --- | --- |
| 示例 SQL | `dati://services/{serviceId}/examples` | 用户手写 | 用户维护的常用 SQL 范例，供 Agent 参考 |
| 权限说明 | `dati://services/{serviceId}/sql-policy` | 系统生成 | 根据当前 Tool 的 SQL 权限配置自动生成的能力说明文本 |

要求：

- 示例 SQL Resource 的 URI 和内容由用户自由编辑。
- 权限说明 Resource 由系统自动维护，随 Tool 权限配置变化实时更新，用户不可编辑。
- 用户可以预览 Resource 内容。
- Resource 是否暴露由用户配置决定。

### 7.9 Prompt 配置

用户可以创建、编辑、删除、启用、禁用 Prompt。

要求：

- Prompt 内容允许用户自由编辑，V1 **不提供内置模板**，用户自行编写。
- Prompt 文本中可使用 `{{paramName}}` 双花括号语法插入参数占位，渲染时替换为参数值。
- Prompt 参数模型对齐 MCP 协议：每个参数包含 `name`（参数名）、`description`（参数描述）、`required`（是否必填），不使用 JSON Schema。
- V1 不支持模板引用语法（如 `{{tools}}`），Prompt 为纯文本。
- 用户可以测试 Prompt 渲染结果。

### 7.10 SQL 权限策略

SQL 权限在 **Tool 级别** 配置，每个 Tool 独立控制。

| 配置项 | 说明 |
| --- | --- |
| 允许 SELECT | 是否允许查询 |
| 允许 INSERT | 是否允许插入 |
| 允许 UPDATE | 是否允许更新 |
| 允许 DELETE | 是否允许删除 |
| 允许 DDL | 是否允许 CREATE、ALTER、DROP 等结构变更 |
| 允许多语句 | 是否允许一次执行多条 SQL |
| 允许事务 | 是否允许事务控制语句 |
| 最大返回行数 | 查询结果最大返回行数 |
| 超时时间 | SQL 最大执行时间 |

默认策略：

- SELECT 默认允许。
- INSERT、UPDATE、DELETE 默认关闭。
- DDL 默认关闭。
- 多语句默认关闭。
- 写操作和 DDL 操作默认需要确认。

**Annotations 映射**：系统根据上述权限配置自动生成 Tool 的 `annotations` 字段（见 7.3 节映射表），供 MCP Client 判断工具行为和是否弹出确认框。

### 7.11 认证与执行身份

V1 使用服务凭证执行。

Token 设计：

- 每个 MCP 服务支持**多个 Token**，可分别命名（如「生产环境」「测试环境」）、启停。
- Token 通过 `Authorization: Bearer <token>` Header 传递。
- Token 由系统生成（随机字符串），用户可在管理端查看和复制。
- 外部 MCP Client 使用有效 Token 调用该服务。
- DatI 按该 MCP 服务配置的数据范围和权限执行数据库操作。

长期需要支持调用者身份执行。

因此执行上下文需要预留：

```text
service_id
credential_id
principal_type: SERVICE / CALLER
principal_id
caller_id
scopes
```

V1 固定：

```text
principal_type = SERVICE
```

未来可扩展：

```text
principal_type = CALLER
```

### 7.12 调试与发布

用户发布前可以在页面中调试 MCP 服务。

调试能力包括：

- 查看 MCP tools/list 结果
- 查看 resources/list 结果
- 查看 prompts/list 结果
- 测试 Tool 调用
- 测试 SQL 执行
- 查看 Resource 内容
- 测试 Prompt 渲染
- 查看错误信息

发布要求：

- 发布前进行基础配置校验。
- **无 Tool 也可以发布**（纯 Resource + Prompt 的 MCP 服务同样有效）。
- 已发布服务生成 MCP endpoint（`/{serviceId}/mcp`）。
- 已发布服务允许停用。
- 服务停用后 endpoint 返回明确错误。

调试 Tool 调用流程：

1. **权限校验**：先按 Tool 自身权限策略校验（如只允许 SELECT 则 DELETE 直接拦截提示）。
2. **二次确认**：权限通过后，对于写操作（INSERT / UPDATE / DELETE / DDL），弹出确认框告知用户即将执行的具体操作。
3. **真实执行**：用户确认后在真实数据源上执行，结果如实展示。

如 7.1 所述，已发布服务修改配置后需重新发布。

### 7.13 审计日志

所有 MCP Tool 调用需要记录审计日志。

日志字段包括：

- 服务 ID
- Tool 名称
- 调用时间
- 调用来源（Client IP + MCP Client `clientInfo.name`）
- 执行身份
- 输入参数摘要
- SQL 摘要
- SQL 操作类型
- 是否成功
- 耗时
- 返回行数
- 错误信息

存储方案：

- 使用 Spring `@Async` + 内存队列异步批量写入 MySQL。
- Tool 调用结束时异步提交日志，不阻塞响应。
- 批量刷入（攒够 50 条或 5 秒），减少写入频率。

要求：

- 写操作、删除操作、DDL 操作需要重点审计。
- 是否记录完整 SQL 和完整参数可配置。
- 审计日志应支持按服务、Tool、时间、状态查询。
- **删除 MCP 服务时，关联的审计日志级联删除**。

---

## 8. 管理端页面需求

### 8.1 信息架构

创建 MCP 服务采用分步引导 + 自由编辑的混合模式：

- **首次创建**：按基础信息 + 数据范围 → 配置 Tool → 发布的步骤引导，覆盖最小可用路径。
- **编辑已有服务**：左侧导航栏式自由切换所有配置 Tab。

### 8.2 页面列表

| 页面 | 核心交互 |
| --- | --- |
| 服务列表 | 分页列表，支持按名称搜索、按状态筛选。展示服务路径（`/{serviceId}/mcp`） |
| 基础信息 | 名称、描述编辑；状态展示；endpoint 完整 URL 展示（含域名，支持复制）；发布 / 停用 / 删除操作 |
| 数据范围 | 选择数据源、表（多数据源重名表带数据源前缀）、主题（引用模式）；查看已选范围 |
| Tools | Tool 列表（启用/禁用）；新建 Tool（选择 Tool 类型）；编辑 Tool 名称、显示名、描述、参数和执行配置；测试 Tool 调用 |
| Resources | Resource 列表；编辑示例 SQL Resource（URI + 内容）；查看权限说明 Resource（只读）；预览内容 |
| Prompts | Prompt 列表；新建 Prompt；编辑 Prompt 内容（纯文本，`{{paramName}}` 语法）；配置参数（name / description / required）；测试渲染 |
| 安全策略 | 服务 Token 列表（多 Token，命名/启停/复制）；各 Tool 的 SQL 权限独立配置 |
| 调试发布 | MCP 能力预览；Tool 调用测试；Resource 读取测试；Prompt 渲染测试；配置校验与发布 |
| 调用日志 | 按时间、Tool、状态筛选；查看调用详情（输入摘要、SQL 摘要、耗时、结果） |

### 8.3 Tool 参数编辑

V1 Tool 输入参数使用 JSON Schema 描述。前端提供结构化表单编辑模式（字段名、类型、必填、描述），高级用户可切换到 Raw JSON Schema 自由编辑。

---

## 9. V1 范围

### 9.1 V1 必须支持

- 创建、编辑、发布、停用 MCP 服务
- 配置数据范围
- 支持数据源、表、主题作为选择入口
- 保存时校验：引用了 Tool 的表不允许从数据范围移除
- 自定义 Tool（4 种类型）
- `SEARCH_METADATA`：元数据关键词模糊检索
- `GET_TABLE_INFO`：表结构精确查询
- `EXECUTE_SQL`：自由 SQL 执行
- `PARAMETERIZED_SQL`：参数化 SQL 模板
- Resource 配置（固定 URI，仅 `examples` + `sql-policy`）
- Prompt 配置（纯文本，`{{paramName}}` 参数替换语法，无内置模板）
- 服务 Token 认证（多 Token，`Authorization: Bearer` Header）
- 服务凭证执行
- Tool 级 SQL 权限配置
- 已发布服务编辑后线上不受影响，需重新发布才覆盖
- Tool 调试
- 调用审计（异步批量写入 MySQL）
- MCP 协议初始化握手和能力协商
- MCP 适配层：将能力层执行结果包装为 content/isError 格式
- Tool annotations 自动映射
- 传输层 Origin 校验、协议版本 Header

### 9.2 V1 暂不强求

- 表级 CRUD 自动生成（通过 `PARAMETERIZED_SQL` 手写模板实现）
- 复杂多步骤 Tool 编排
- Resource Template（URI 模板）
- ACCESS_SCOPE Tool（能力信息融入 Tool description）
- 服务级 SQL 权限
- 模板引用语法
- 调用者身份权限透传
- OAuth 完整授权流
- 复杂行级权限
- SQL 智能优化
- SQL lineage 分析
- 审批流
- 多版本主题冻结
- 完整工具市场
- 外部 API Tool

---

## 10. 非目标

V1 不把 MCP 构建模块设计成以下系统：

- 固定 NL2SQL 应用
- 固定业务指标分析平台
- 复杂工作流编排器
- 数据治理审批系统
- 完整 IAM 权限系统

这些能力可以后续扩展，但不应成为 V1 的复杂度来源。

---

## 11. 验收标准

V1 完成后应满足：

1. 用户可以创建一个 MCP 服务并配置数据范围。
2. 用户可以通过数据源、表或主题快捷选择元数据。
3. 用户可以创建 4 种类型的自定义 Tool（SEARCH_METADATA / GET_TABLE_INFO / EXECUTE_SQL / PARAMETERIZED_SQL）。
4. `SEARCH_METADATA` 返回结果按数据源分组，`GET_TABLE_INFO` 读本地元数据并提示可用 SQL 查最新结构。
5. `PARAMETERIZED_SQL` 支持 String / Number / Boolean / Date / Array 类型参数。
6. 用户可以配置 Resource（`examples` + `sql-policy`）和 Prompt（`{{paramName}}` 替换语法，无内置模板）。
7. 用户可以不配置 Tool 直接发布，或配置 Tool 后发布，均获得 endpoint。
8. 已发布服务编辑时线上不受影响，发布后才覆盖。
9. MCP Client 可以**通过 MCP 协议标准握手流程**建立连接，发现已配置的 Tools、Resources 和 Prompts。
10. MCP Client 可以调用 Tool 并获得符合 MCP 协议格式的执行结果（`content` + `isError`）。
11. 服务停用后 endpoint 不可用，重新启用后 URL 和 Token 不变。
12. Token 支持多 Token 管理（命名、启停），通过 `Authorization: Bearer` 传递。
13. Tool 调用会产生审计日志（含 Client IP + clientInfo.name），删除服务时级联删除。
14. Tool annotations 正确反映 SQL 权限配置，Client 可据此展示确认框。

---

## 12. 后续待细化

以下内容需要进入详细设计阶段继续明确：

- 已发布快照 / 草稿副本的具体存储方案（单表双记录 vs 快照表）
- MCP 协议 `MCP-Session-Id` 不启用，无状态模式
- Streamable HTTP 传输的 SSE 流管理细节
- Tool 输入 JSON Schema 的前端编辑体验（结构化表单 vs Raw 编辑器）
- SQL 执行结果 JSON 的具体内部结构（字段名、类型表示、NULL 处理等）
- 参数化 SQL 的参数绑定规则和类型推断
- PARAMETERIZED_SQL Array 类型参数的展开规则
- `sql-policy` Resource 的自动生成模板
- Prompt 参数渲染机制和 `prompts/get` 实现
- 服务 Token 生命周期（生成算法、过期、多 Token 存储模型）
- 审计日志保留策略和查询接口
- 调用者身份权限扩展设计（预留字段的具体实现）
- SQL 类型识别方式（解析器选型：JSqlParser vs 正则 vs 其他）
- 服务凭证的加密存储和验证流程
- V1 不做 `list_changed` 推送通知，Client 通过定期拉取感知变更
- `list_changed` 通知的推送时机和去重策略

---

## 13. 后续版本规划

以下能力在 V1 范围之外，但对 DatI 的场景有明确价值，建议纳入后续版本评估。

### 13.1 表级 CRUD 快速生成

从数据范围内的表一键生成 Query / Insert / Update / Delete Tool，自动推断参数（主键作为条件、非自增字段作为可写参数）。V1 中用户需手动编写 `PARAMETERIZED_SQL` 模板实现相同效果，配置效率较低。

### 13.2 表数据预览 Tool

新增 `TABLE_PREVIEW` 原子能力：Agent 在生成 SQL 前可先请求目标表的少量样本数据（如 `SELECT * FROM t LIMIT 5`），了解实际数据内容，提高 NL2SQL 的字段映射准确率。

### 13.3 Resource Template（URI 模板）

支持 `dati://{serviceId}/tables/{table}/schema` 等参数化 Resource URI，Client 可按需读取指定表的结构。相比 V1 的固定 URI 全量暴露，URI 模板更灵活，适合数据范围较大的服务。

### 13.4 Prompt 模板引用语法

允许 Prompt 中通过模板语法引用 Tool 列表、Resource 内容、服务元数据，例如：

```text
你是一个数据分析助手。当前可用的工具有：
{{tools}}

数据库 Schema：
{{resource:schema}}
```

V1 的 Prompt 为纯文本，需要用户手动维护和服务配置的一致性。

### 13.5 调用者身份权限透传

企业场景中，MCP 服务需要按最终用户（而非服务凭证）的身份执行 SQL。这要求：

- 支持 `principal_type = CALLER` 的执行模式
- 调用方在请求中携带用户身份信息
- SQL 执行时注入调用者身份（如通过数据库 Session 变量实现行级安全）

V1 已预留了 `principal_type` 扩展字段。

### 13.6 OAuth 2.1 授权流

MCP 协议定义了基于 OAuth 2.1 的标准授权机制（Authorization Code Flow + DCR + Client ID Metadata Documents）。对需要接入第三方 MCP Client 的场景，OAuth 比简单的 Token 认证更安全、更标准。

### 13.7 服务级 SQL 权限兜底

在 Tool 级权限之上增加服务级全局约束（如「该服务绝对不允许 DDL」），作为安全兜底。当服务由多人协作配置时，防止个别 Tool 误配。V1 只有单用户配置，仅 Tool 级权限足够。

### 13.8 工具市场

允许用户将配置好的 Tool 模板（如「任务管理 CRUD 套件」「电商数据检索套件」）发布到市场，其他用户一键导入。降低重复配置成本，形成生态。
