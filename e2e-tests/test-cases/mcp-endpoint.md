# MCP Endpoint - 端到端测试

对外 JSON-RPC 接口 `POST /{code}/mcp`（2025-11-25 协议、Streamable HTTP、无状态）。设计细节见 [docs/superpowers/specs/2026-08-06-mcp-endpoint-design.md](../../docs/superpowers/specs/2026-08-06-mcp-endpoint-design.md)。

> **前置约定**：所有用例需要**已发布**的 MCP 服务（`status`=PUBLISHED，含 enabled 工具 + prompt）。
> 数据准备流程：按 `mcp-service.md` 的 TC-MCP-008 创建服务并发布（携带 `data_scopes` 绑定种子数据源），
> 按 TC-MCP-003/004 配置 Prompt 和自定义工具。本模块用例按 `code` 查找服务复用。
>
> **请求头约定**：`Accept` 不校验（服务始终返回 JSON，任意 Accept 均可）；非 initialize 请求必须带 `MCP-Protocol-Version: 2025-11-25`；
> 认证 `Authorization: Bearer <jwt>`。JSON-RPC body 为 camelCase（`method`/`params`/`id`）。

---

## TC-END-001 连接与初始化
**级别：** P0
**前置：** 已登录，已发布服务（code=自定）

1. 向 `POST /{code}/mcp` 发送 initialize 请求（params 含 `protocolVersion`/`capabilities`/`clientInfo`，id 自定）
2. 验证响应：
   - 状态码 200，`Content-Type` 为 `application/json`（**非** text/event-stream，无 SSE 流）
   - `result.protocolVersion` == `2025-11-25`
   - `result.serverInfo.name` == 服务 name；`result.serverInfo.version` == `v{active_version_number}`
   - 响应**不含** `Mcp-Session-Id` 头
3. 服务有 enabled 工具 → `result.capabilities.tools` 存在；有 enabled prompt → `result.capabilities.prompts` 存在

---

## TC-END-002 工具发现 tools/list
**级别：** P0
**前置：** 已登录，已发布服务（配置了 prebuilt + 自定义工具）

1. 发送 `tools/list`（无 params）
2. 验证响应：
   - 状态码 200，`result.tools` 为数组
   - 返回**全部 enabled 工具**：prebuilt **6 个**（`search_metadata`/`get_table_info`/`execute_sql`/`update_table_info`/`update_column_info`/`upsert_term`）+ 自定义工具（name=业务 name）
   - **确定性排序**：prebuilt 固定顺序 SEARCH_METADATA → GET_TABLE_INFO → EXECUTE_SQL → UPDATE_TABLE_INFO → UPDATE_COLUMN_INFO → UPSERT_TERM，custom 按 name 字母序
   - 每个工具含 `name`/`description`/`inputSchema`；**所有 prebuilt 另含 `title`**（`Search Metadata`/`Get Table Info`/`Execute SQL`/`Update Table Metadata`/`Update Column Metadata`/`Upsert Business Term`）；custom 含 `title`
   - **annotations**：`search_metadata`/`get_table_info` 含 `readOnlyHint`=true；`update_table_info`/`update_column_info`/`upsert_term` 含 `readOnlyHint`=false、`destructiveHint`=false、`idempotentHint`=true、`openWorldHint`=true；`execute_sql` **无** annotations（可执行写 SQL，不声明只读）
   - `inputSchema` 为对象，PARAMETERIZED_SQL 工具的 schema `properties`/`required` 与其 `parameters` 一致
   - **GET_TABLE_INFO schema 形态（decision 12 回归锚点）**：`properties.tables.items.properties` 含 `data_source_id`，`items.required`=["data_source_id", "table"]（`data_source_id` 在每行内，非顶层）
3. **空服务语义**：服务无 enabled 工具 → `result.tools` 为**空数组**（不报错）

---

## TC-END-003 工具调用 tools/call
**级别：** P0
**前置：** 已登录，已发布服务（含 EXECUTE_SQL + 一个 PARAMETERIZED_SQL 自定义工具，数据范围绑定种子数据源）

1. **调用预置工具**：`tools/call`，params `{name: "execute_sql", arguments: {data_source_id: <种子数据源 id>, sql: "SELECT 1"}}`
   - 验证：状态码 200；`result.isError` == false；`result.content[0].type` == "text"；`result.structuredContent` 非空
2. **调用自定义参数化工具**：`tools/call`，name=自定义工具名，arguments 传其必填参数
   - 验证：`result.isError` == false；`result.structuredContent.executed_sql` 含参数绑定占位符；结果行数/内容与参数匹配
3. **未知工具**：name 填不存在的名字 → JSON-RPC error `-32602`
4. **禁用工具**：管理端把某工具 `enabled`=false 并发布后，调用该工具名 → `-32602`（与未知一致）
5. **scope 违规**：execute_sql 的 sql 引用数据范围外的表 → `result.isError` == true（**不是** JSON-RPC error，供 LLM 自纠错）
6. **SQL 策略违规**：sql 含被 SqlPolicy 禁止的操作 → `result.isError` == true

---

## TC-END-004 Prompt 获取 prompts/list + prompts/get
**级别：** P1
**前置：** 已登录，已发布服务（含 enabled prompt，content 含 `{{变量}}` 模板）

1. **列表**：发送 `prompts/list` → `result.prompts` 数组，每项含 `name`/`description`/`arguments`（参数映射 `{name, description, required}`）；无 enabled prompt → 空数组
2. **获取**：`prompts/get`，params `{name: <prompt名>, arguments: {变量: 值}}`
   - 验证：`result.messages[0].role` == "user"；`content.text` == 模板渲染结果（变量已替换）
3. **参数缺失**：缺必填参数 → JSON-RPC error `-32602`
4. **未知/禁用 prompt**：name 不存在或 enabled=false → JSON-RPC error `-32602`

---

## TC-END-005 ping
**级别：** P2
**前置：** 已登录，已发布服务

1. 发送 `ping` → 200，`result` 存在（空对象）

---

## TC-END-006 状态语义与传输校验
**级别：** P0
**前置：** 已登录

| 场景 | 操作 | 预期 |
|---|---|---|
| 未知 code | POST `/ghost/mcp` | **404**（不区分 DRAFT 与不存在） |
| DRAFT 服务 | POST `/<draft-code>/mcp` | **404** |
| DISABLED 服务 | POST `/<disabled-code>/mcp` | **503** + body 含 `error`（JSON-RPC error 结构） |
| PUBLISHED 服务 | POST `/<published-code>/mcp` | **200** |
| 非 POST 方法 | GET `/<published-code>/mcp` | **405** |
| Accept 不含 `text/event-stream` | `Accept: application/json` | **400** |
| 非 initialize 缺 `MCP-Protocol-Version` | 不带该头 | **400** |
| `Accept` 任意值（含缺省） | 任意 Accept / 无 Accept | **200**（不校验） |
| Origin 非 localhost 且不在白名单 | `Origin: http://evil.example.com` | **403** |
| Origin localhost | `Origin: http://localhost:8085` | **200** |
| Origin 在白名单 | `Origin: https://trusted.example.com`（配置 `dati.mcp.allowed-origins`） | **200** |

---

## TC-END-007 认证
**级别：** P0
**前置：** 无

1. **无 token**：POST `/mcp` 不带 Authorization → **401** + `WWW-Authenticate: Bearer` 头
2. **无效 token**：`Authorization: Bearer garbage` → **401** + `WWW-Authenticate: Bearer`
3. 401 响应不泄露服务存在性（body 无 JSON-RPC 信息）

---

## TC-END-008 版本隔离（草稿变更线上不可见）
**级别：** P0
**前置：** 已登录，已发布服务（记下 tools/list 基线）

1. 管理端修改草稿：新增一个自定义工具（不发布）
2. 再次 `tools/list` → **与基线一致**（新工具不可见，版本隔离）
3. 管理端发布 → `tools/list` → 新工具出现（排序正确）
4. **无 enabled 工具的服务**：发布后 `tools/list` 返回空数组（空服务语义）

---

## TC-END-009 停用/启用可见性
**级别：** P1
**前置：** 已登录，已发布服务

1. 管理端 `POST /{id}/disable` → endpoint 请求返回 **503** + JSON-RPC error
2. 管理端 `POST /{id}/enable`（无需重新发布）→ endpoint 请求恢复 **200**，tools/list 内容不变
3. **停用期间发布变更**：disable 后修改草稿并 publish → status 仍为 DISABLED，endpoint 仍 503；enable 后看到的是停用期间发布的新版本（`serverInfo.version` 已递增）

---

## TC-END-010 回滚版本恢复
**级别：** P1
**前置：** 已登录，已发布服务 v2（v1 与 v2 的工具配置不同）

1. 记录 v2 的 `tools/list` 内容
2. 管理端 `POST /{id}/rollback`（`{"target_version_number": 1}`）→ 成功，`active_version_number` 递增
3. `tools/list` → **内容恢复为 v1 的工具集**（差异工具消失/恢复）
4. `serverInfo.version` == 回滚后的新版本号

---

## TC-END-011 Conformance 协议合规验收
**级别：** P1
**前置：** 已登录，已发布服务（含同名测试实体：`test_simple_text` 工具、`test_simple_prompt`、`test_prompt_with_arguments` prompt——用于满足 Conformance fixture 场景）

1. 运行验收脚本（自动完成 Inspector CLI + Conformance 全流程，脚本位于 e2e-tester skill）：
   ```bash
   MCP_CODE=<code> MCP_TOKEN=<jwt> MCP_DS_ID=<种子数据源id> \
     .agents/skills/e2e-tester/scripts/mcp-verify.sh
   ```
2. 脚本输出检查：
   - Inspector CLI：`tools/list` 返回工具列表；`tools/call`（execute_sql + 自定义工具）`isError` == false；`prompts/get` 渲染正确
   - Conformance 汇总：**9 passed / 21 expected failures**（基线内），输出 **"Baseline check passed: all failures are expected"**
3. **0 unexpected failures** —— 若出现 unexpected，视为协议回归，需排查后再提交

> 基线文件：`e2e-tests/conformance/baseline.yml`（预期失败清单，含分类注释）。
> 结果文件（`results/server-*/checks.json`）为可再生产物，不入库（.gitignore）。

---

## TC-END-012 工具发现：元数据写入工具 title/annotations/inputSchema
**级别：** P0
**前置：** 已登录，已发布服务（全部预置工具 enabled，数据范围绑定种子数据源）

1. 发送 `tools/list`（无 params）
2. 验证响应：
   - 状态码 200，`result.tools` 前 6 个为 prebuilt，顺序确定：`search_metadata` → `get_table_info` → `execute_sql` → `update_table_info` → `update_column_info` → `upsert_term`
3. **update_table_info（写工具）**：
   - `title` == `Update Table Metadata`
   - `annotations`：`readOnlyHint`=false、`destructiveHint`=false、`idempotentHint`=true、`openWorldHint`=true
   - `inputSchema.properties.tables.items`：`required` == ["data_source_id", "table"]；`properties` 含 `data_source_id`/`schema`/`table`/`description`/`aliases`；`description.maxLength`==500；`aliases.maxItems`==20（全量替换语义）；`aliases.items.maxLength`==100
4. **update_column_info**：`title`==`Update Column Metadata`；`items.required` 与 `["data_source_id", "table", "column"]` 集合相等（victools 按字母序输出，如 `["column", "data_source_id", "table"]`，顺序非契约）
5. **upsert_term**：`title`==`Upsert Business Term`；`items.required`==["name", "subject_name"]；`subject_name.maxLength`==200、`name.maxLength`==200
6. **只读工具对照**：`search_metadata`/`get_table_info` 的 `annotations.readOnlyHint`==true；`execute_sql` **无** `annotations` 字段
7. **GET_TABLE_INFO schema 回归锚点**：`tables.items.properties` 含 `data_source_id`（decision 12：`data_source_id` 在每行内）

---

## TC-END-013 工具调用：元数据写入工具（部分失败、幂等 upsert、参数错误）
**级别：** P0
**前置：** 已登录，两个已发布服务：服务 A 数据范围绑定种子数据源（步骤 1/3）、服务 B 数据范围绑定种子主题（步骤 2，`scope_type`=SUBJECT）
**数据：** `chinook.e2e.{seeded_datasource_name, seeded_subject_name, mcp.write_tool}`

> **注意**：UPSERT_TERM 通过 `subject_name` 在服务 scope 内定位主题——服务 B 必须绑定种子主题（DATA_SOURCE scope 的服务会对术语返回 SCOPE_ERROR）。

1. **update_table_info 部分失败语义**：`tools/call`，params `{name: "update_table_info", arguments: {tables: [{data_source_id: <seedDsId>, table: "genre", description: "<write_tool.table_desc>"}, {data_source_id: <seedDsId>, table: "<write_tool.ghost_table>", description: "x"}]}}`
   - 状态码 200；`result.isError` == **false**（部分失败不置 error，供 LLM 自纠错）
   - `result.structuredContent.type` == `METADATA_UPDATE`；`results` 长度 2
   - `results[0].success`==true、`change_type`==UPDATE、`new.description`==写入值；`results[1].success`==false、`error.error_category`==PARAM_ERROR
   - 回读验证：`tools/call` get_table_info → genre 表 description==写入值（写入经协议链路生效）
   - **恢复**：`tools/call` update_table_info 写回 `description`=""、`aliases`=[] → success=true
2. **upsert_term 幂等 upsert（CREATE→UPDATE）**：`tools/call`，params `{name: "upsert_term", arguments: {terms: [{subject_name: "<seeded_subject_name>", name: "<write_tool.term_name>", description: "<write_tool.term_desc>", aliases: ["<write_tool.term_aliases[0]>"]}]}}`
   - 状态码 200；`result.isError`==false；`structuredContent.results[0].change_type`==**CREATE**、`old`==null
   - **再次调用同参数** → `change_type`==**UPDATE**、`old.description`==`term_desc`（幂等 upsert，不重复创建）
3. **参数错误 → isError 结果（既有 ToolExecuteException 语义）**：`tools/call` update_table_info，arguments.description 为 501 字符 → 状态码 200，`result.isError`==**true**，`result.content[0].text` 含 `size must be between 0 and 500`（binder 层 PARAM_INVALID 走 isError 供 LLM 自纠错，与 TC-END-003 的 SQL 策略违规一致；非 JSON-RPC error）
4. **清理**：平台 API DELETE `/v1/terms/{id}` 删除测试术语（按步骤 2 术语名查询 id）；删除两个测试服务
