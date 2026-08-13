# MCP 服务管理 - 端到端测试

使用 test-data.yaml 中配置的测试数据集。所有具体值引用数据集 `e2e` 段下的路径。
共享种子数据源（`seeded_datasource_name`）和种子主题（`seeded_subject_name`）由 TC-SEM-000 创建，本模块用例按名称查找复用。

> **创建服务约定**：创建请求必须携带 `data_scopes`（至少 1 条，绑定种子数据源/主题）：
> `{"code": "...", "name": "...", "data_scopes": [{"scope_type": "DATA_SOURCE", "reference_id": "<datasourceId>"}]}`
> 不带 `data_scopes` → 400；服务数据范围为空时调用 `/publish` → 400
>
> **种子数据源约定**：种子数据源必须已设置 `default_schema`（如 `public`），否则 schema-less 表引用（`SELECT * FROM genre`）无法通过 `ScopeValidator` 表级解析，工具测试会误报 SCOPE_ERROR。
>
> **路径约定**：数据源接口路径为 `/v1/data-sources`（带连字符）、表列表为 `/v1/data-sources/{id}/tables`；术语列表为 `/v1/subjects/{subjectId}/terms`（无全局 `/v1/terms` 端点）。

---

## TC-MCP-001 创建、查询、更新、删除 MCP 服务
**级别：** P0
**前置：** 已登录

1. **创建 MCP 服务**：name 自定，description 自定，`code` 自定（必填），携带 `data_scopes` 绑定种子数据源
2. 验证返回 200，`id` 为 UUID
3. **创建不带数据范围的服务**（body 无 `data_scopes`）→ 预期 400
4. 在服务列表中搜该 name，确认能找到
5. 验证列表项：name/description/code 与创建一致，`status`=DRAFT
   - `created_by`/`updated_by`/`created_user_name`/`updated_user_name` 不为 null
   - `created_at`/`updated_at` 合理
6. **部分更新**：仅改 description（不传 name/code 等其他字段）
7. 查详情验证 description 已更新，`updated_at > created_at`，name/code/status 未变
8. **删除服务**，查列表确认已删

---

## TC-MCP-002 配置数据范围
**级别：** P1
**前置：** 已登录，种子数据源已就绪（TC-SEM-000）
**数据：** `chinook.e2e.{seeded_datasource_name, mcp.data_scope_type}`

1. 搜索 `seeded_datasource_name` 获取 datasourceId，创建 MCP 服务（携带 `data_scopes` 绑定该数据源）
2. **查数据范围**：验证 items 含刚配置的记录
   - `scope_type` 匹配，`reference_id` 匹配，`reference_name` 非空
   - `resolved_data_sources` 包含数据源名称
3. **更新数据范围（PUT 全量替换）**：使用 `mcp.data_scope_type` 类型，reference_id 填另一数据源或同一数据源
   - 注意：请求 body 字段使用 snake_case（`scope_type`、`reference_id`）
4. 验证返回 200，再次查数据范围：items 为替换后的记录
5. 删除服务（清理）

---

## TC-MCP-003 Prompt CRUD
**级别：** P1
**前置：** 已登录

1. 创建 MCP 服务（携带 `data_scopes` 绑定种子数据源）
2. **创建 Prompt**：name 自定，content 自定（如 `chinook.e2e.mcp.prompt_content_example`），`enabled`=true
3. 验证返回 200，含 id
4. 查 Prompt 列表，确认 name/content/enabled 与创建一致
5. **部分更新**：仅改 content（不传 name/enabled）
6. 查列表验证 content 已更新，name/enabled 未变
7. **删除 Prompt**，查列表确认已删
8. 删除服务（清理）

---

## TC-MCP-004 Tool CRUD
**级别：** P1
**前置：** 已登录，种子数据源已就绪
**数据：** `chinook.e2e.{seeded_datasource_name, mcp.tool}`

1. 搜索 `seeded_datasource_name` 获取 datasourceId，创建 MCP 服务（携带 `data_scopes` 绑定该数据源）
2. **创建 Tool**：type=`mcp.tool.type`
   - config 中包含 data_source_id（填种子 datasourceId）、sql_template、parameters（参考 `mcp.tool.config`）
   - 注意：请求 body 使用 snake_case（`tool_type`、`data_source_id`、`sql_template`）
3. 验证返回 200
4. 查 Tool 列表（custom 列表），确认 type/name/title 正确
5. **部分更新**：改 description
6. 查列表验证已更新
7. **删除 Tool**，查列表确认
8. 删除服务（清理）

---

## TC-MCP-005 测试 Tool
**级别：** P1
**前置：** 已登录，种子数据源已就绪
**数据：** `chinook.e2e.{seeded_datasource_name, mcp.tool}`

1. 搜索种子数据源 → 创建 MCP 服务（携带 `data_scopes` 绑定该数据源）
2. 创建 Tool（PARAMETERIZED_SQL，sql_template 使用 `mcp.tool.config.sql_template`）
3. **测试 Tool**：传入参数（如 genre_name=`chinook.e2e.datasource.column_values.sample_values[0]`）
4. 验证：状态码 200，响应含执行结果（非空、非 500）
5. 删 Tool，删服务（清理）

---

## TC-MCP-006 按状态筛选服务列表
**级别：** P2
**前置：** 已登录

1. 创建 2 个 MCP 服务（不同 name）
2. 用 status=DRAFT 筛选，验证返回的记录 status 全为 DRAFT
3. 用 status=PUBLISHED 筛选，验证返回空或全为 PUBLISHED
4. 删服务（清理）

---

## TC-MCP-007 删除不存在的 MCP 服务
**级别：** P2
**前置：** 已登录

1. 用不存在的 UUID 删除 MCP 服务
2. 预期返回错误（404），不能返回 200 或 500

---

## TC-MCP-008 MCP 服务发布、版本号递增与草稿 Diff 对比
**级别：** P0
**前置：** 已登录

1. **创建 MCP 服务**：code 自定，name 自定，携带 `data_scopes` 绑定种子数据源，初始状态 `status` 为 DRAFT
2. **查询未发布 Diff**：调用 `GET /v1/mcp-services/{id}/diff`
   - 验证 `has_changes` 为 `true`
3. **首次发布服务**：调用 `POST /v1/mcp-services/{id}/publish`（Body: `{"release_note": "Initial v1"}`）
   - 验证返回 200，`id` 为生成的快照 UUID
   - 查服务详情 `GET /v1/mcp-services/{id}`，验证 `status` 为 PUBLISHED，`active_version_number` 为 1
   - 调用 `GET /v1/mcp-services/{id}/diff`，验证 `has_changes` 为 `false`
4. **查询快照列表**：调用 `GET /v1/mcp-services/{id}/snapshots`
   - 验证返回快照数组含 1 条记录，`version_number` 为 1，`release_note` 为 "Initial v1"
5. **产生草稿变更**：更新服务 name/description（调用 `PUT /v1/mcp-services/{id}`）或添加 Tool
6. **再次查询 Diff**：调用 `GET /v1/mcp-services/{id}/diff`
   - 验证 `has_changes` 为 `true`，`basic_info_changed` 或 `tools_changed` 为 `true`
7. **发布变更**：调用 `POST /v1/mcp-services/{id}/publish`（Body: `{"release_note": "Release v2"}`）
   - 验证返回 200
   - 查服务详情，验证 `status` 为 PUBLISHED，`active_version_number` 为 2
   - 调用 `GET /v1/mcp-services/{id}/diff`，验证 `has_changes` 为 `false`
8. 删除服务（清理）

---

## TC-MCP-009 服务停用与重新启用
**级别：** P1
**前置：** 已登录

1. **创建并发布服务**：创建 MCP 服务（携带 `data_scopes`），调用 `/publish` 成功发布，确认 `status` 为 PUBLISHED
2. **停用服务**：调用 `POST /v1/mcp-services/{id}/disable`
   - 验证返回 200
   - 查服务详情，验证 `status` 变为 DISABLED，`active_version_number` 保持已有版本号
3. **重新启用服务**：调用 `POST /v1/mcp-services/{id}/enable`
   - 验证返回 200
   - 查服务详情，验证 `status` 恢复为 PUBLISHED
4. 删除服务（清理）

---

## TC-MCP-010 版本回滚完整链路
**级别：** P0
**前置：** 已登录

1. **创建并发布 v1**：创建 MCP 服务（description 记为 `desc_v1`，携带 `data_scopes` 绑定种子数据源），调用 `/publish`（`{"release_note": "Initial v1"}`）
   - 验证 `status`=PUBLISHED，`active_version_number`=1
2. **修改并发布 v2**：`PUT /v1/mcp-services/{id}` 更新 description 为 `desc_v2`，调用 `/publish`（`{"release_note": "Release v2"}`）
   - 验证 `active_version_number`=2
3. **产生未发布草稿**：再次更新 description 为 `desc_v3_draft`
   - 调用 `GET /diff`，验证 `has_changes`=true
4. **回滚到 v1**：调用 `POST /v1/mcp-services/{id}/rollback`（Body: `{"target_version_number": 1}`）
   - 验证返回 200，`id` 为新快照 UUID（≠ v1 快照 id）
   - 查服务详情：`active_version_number`=**3**（回滚生成新版本，不是回到 1），`status` 仍为 PUBLISHED
   - 查详情 description == `desc_v1`（**草稿已被 v1 内容覆盖**）
   - 调用 `GET /diff`，验证 `has_changes`=**false**（回滚后草稿与线上一致，审计字段差异不误报）
5. **快照列表**：调用 `GET /v1/mcp-services/{id}/snapshots`
   - 验证返回 3 条记录，倒序 v3/v2/v1
   - v3 的 `release_note` 以 "Rollback to v1" 开头
   - **验证响应只含版本元信息（版本号/备注/时间），不携带快照正文内容**（列表接口不应暴露草稿配置全文）
6. **回滚到不存在的版本**：调用 `POST /v1/mcp-services/{id}/rollback`（Body: `{"target_version_number": 99}`）
   - 预期返回错误（404 语义），不能返回 200
   - 查服务详情：`active_version_number` 仍为 3，description 仍为 `desc_v1`（线上与草稿均不变）
7. 删除服务（清理）

---

## TC-MCP-011 发布 ≠ 上线 与状态机前置条件
**级别：** P1
**前置：** 已登录

1. **创建服务**：携带 `data_scopes` 绑定种子数据源，初始状态 DRAFT
2. **DRAFT 状态非法操作**：
   - 调用 `POST /v1/mcp-services/{id}/disable` → 预期错误（409）
   - 调用 `POST /v1/mcp-services/{id}/enable` → 预期错误（409）
3. **首次发布**：`POST /publish` → `status`=PUBLISHED，`active_version_number`=1
4. **停用**：`POST /disable` → `status`=DISABLED
5. **停用状态下发布变更**：更新 description，调用 `POST /publish`
   - 验证返回 200
   - 查服务详情：**`status` 仍为 DISABLED**（发布 ≠ 上线，不自动恢复对外）
   - `active_version_number` 递增为 2（快照已更新）
6. **启用**：`POST /enable` → `status` 恢复为 PUBLISHED
   - 线上快照为停用期间发布的新版本（`active_version_number`=2）
7. 删除服务（清理）

---

## TC-MCP-012 diff 业务字段比较：prebuilt 配置变更检测
**级别：** P1
**前置：** 已登录

1. **创建并发布服务**：创建携带 `data_scopes` 的服务，`POST /publish` → PUBLISHED，`active_version_number`=1
2. **修改 prebuilt 工具配置**：调用 `PUT /v1/mcp-services/{id}/tools/SEARCH_METADATA`
   - Body 注意：`config` 为 **JSON 字符串**（`{"tool_type": "SEARCH_METADATA", "enabled": true, "config": "{\"timeout\": 60}"}`）
3. **查询 Diff**：
   - 验证 `has_changes`=true，`tools_changed`=true
   - **`modified_tools` 包含 "SEARCH_METADATA"**（prebuilt 变更能定位到具体工具）
   - `added_tools`/`deleted_tools` 为空
4. **改回原配置**：再次 `PUT /tools/SEARCH_METADATA`，`config` 恢复为 `{"timeout": 30}`
   - 调用 `GET /diff`：验证 `has_changes`=**false**（业务字段一致即无变更；DB 记录 `updated_at` 已变但**审计字段差异不误报**）
5. 删除服务（清理）

---

## TC-MCP-013 删除服务：级联删除（US-10）
**级别：** P1
**前置：** 已登录，种子数据源已就绪（TC-SEM-000）

1. **创建服务**：携带 `data_scopes` 绑定种子数据源
2. **添加自定义工具**：`POST /v1/mcp-services/{id}/tools`，name 自定、`tool_type`=PARAMETERIZED_SQL、`config` 为 JSON 字符串（绑定种子数据源）
3. **添加 Prompt**：`POST /v1/mcp-services/{serviceId}/prompts`，name 自定、content 自定
4. **发布服务**：`POST /publish` → PUBLISHED、`active_version_number`=1（生成快照，验证快照也被级联删除）
5. **级联删除**：`DELETE /v1/mcp-services/{id}` → 200，响应 `id` 与删除的服务 id 一致
6. **验证删除结果**：
   - 查服务详情 → 404
   - 查服务列表（按 name 搜索）→ 无该服务
   - 查询该服务的 prompts / snapshots → 均 404（custom tools 已级联清除，`GET /tools` 返回 200 但 `custom` 为空数组 —— 预置工具为代码内置默认值，属既有行为）
   - 再次 `DELETE` 该 id → 404（幂等）

---

## TC-MCP-014 工具创建校验分支
**级别：** P2
**前置：** 已登录，种子数据源已就绪
**数据：** `chinook.e2e.{seeded_datasource_name, mcp.tool}`

1. 搜索 `seeded_datasource_name` 获取 datasourceId，创建 MCP 服务（携带 `data_scopes` 绑定该数据源）
2. **name 格式非法**：POST `/v1/mcp-services/{id}/tools`，`tool_type`=PARAMETERIZED_SQL，`name`="Invalid Name"（含空格/大写，违反 `^[a-z0-9]([a-z0-9_-]{0,62}[a-z0-9])?$`），config 为合法 JSON 字符串（可复用 `mcp.tool.config` 结构）→ 预期 400（MS004 工具名称格式无效）
3. **工具重名**：先创建合法工具（name=`qa_tool_dup`，config 复用 `mcp.tool.config`），再创建同名工具 → 预期 409（MS003 工具名称已存在）
4. **模板语法错误**：创建工具，config.sql_template 为 `mcp.tool.config.sql_template` 的未闭合变体（去掉结尾 `}}`）→ 预期 400（MS008 模板语法错误）
5. **模板引用未定义参数**：sql_template 含 `{{undef_var}}` 但 parameters 为空 → 预期 400（MS009 工具参数不匹配）
6. **空模板**：sql_template 为空字符串或缺失 → 预期 400（实际 CM001 "sql_template is required in config"）
7. **缺 tool_type**：请求体不传 `tool_type` → 预期 400（@NotNull 校验）
8. 删除服务（清理）

---

## TC-MCP-015 预置工具开关与 EXECUTE_SQL 配置
**级别：** P1
**前置：** 已登录，种子数据源已就绪

1. 搜索种子数据源 → 创建 MCP 服务（携带 `data_scopes` 绑定该数据源）
2. **懒初始化默认配置**：GET `/v1/mcp-services/{id}/tools` → prebuilt **6 个**，均 `enabled`=true；SEARCH_METADATA / GET_TABLE_INFO 的 config 为 `{"timeout": 30}`；EXECUTE_SQL config 含 `sql_policy.allow_select`=true（其余 false）、`timeout`=30、`max_rows`=1000；UPDATE_TABLE_INFO / UPDATE_COLUMN_INFO / UPSERT_TERM 的 config 为 `{}`（无 per-service 配置，无 DB 记录时使用代码默认值）
3. **更新 EXECUTE_SQL 配置**：PUT `/v1/mcp-services/{id}/tools/EXECUTE_SQL`，body：`{"tool_type": "EXECUTE_SQL", "enabled": true, "config": "{\"sql_policy\":{\"allow_select\":true,\"allow_update\":true,\"allow_multi\":true},\"timeout\":60,\"max_rows\":500}"}`（config 为 JSON 字符串）→ 200
   - GET `/tools` 回读：EXECUTE_SQL config 与提交一致（`allow_update`=true、`allow_multi`=true、`timeout`=60、`max_rows`=500）
4. **预置工具开关**：PUT `/tools/SEARCH_METADATA`，body：`{"tool_type": "SEARCH_METADATA", "enabled": false}` → 200；GET `/tools` 回读 SEARCH_METADATA `enabled`=false
5. **disabled 工具测试被拦**：POST `/tools/SEARCH_METADATA/test`，args `{"keywords": ["Rock"]}` → HTTP 200（工具测试错误走 200 + error 结构，非 HTTP 错误码），`success`=false，`error.error_category`=PARAM_ERROR（TOOL_DISABLED）
6. 删除服务（清理）

---

## TC-MCP-016 EXECUTE_SQL 工具测试
**级别：** P0
**前置：** 已登录，种子数据源已就绪
**数据：** `chinook.e2e.{seeded_datasource_name, mcp.tool.exec_sql}`

1. 搜索种子数据源 → 创建 MCP 服务（携带 `data_scopes` 绑定该数据源）
2. **SELECT 成功**：POST `/tools/EXECUTE_SQL/test`，args `{"data_source_id": "<seedDsId>", "sql": "<mcp.tool.exec_sql.select>"}` → HTTP 200
   - `success`=true，`execution_time_ms` 非负
   - `data.type`=SQL_EXECUTION，`data.executed_sql` 与输入一致，`data.results` 非空
   - results[0]：`type`=SELECT、`success`=true、`columns` 与 `<mcp.tool.exec_sql.select_columns>` 一致、`rows` 非空（种子数据 25 条流派）
3. **策略拦截（DML）**：args sql 改为 `<mcp.tool.exec_sql.update>`（默认 policy 仅允许 SELECT）→ HTTP 200，`success`=false，`error.error_category`=PERMISSION_DENIED（策略在 SQL 分析层拦截，未实际执行，数据未变）
4. **多语句拦截**：sql = `<mcp.tool.exec_sql.multi>`（默认 allow_multi=false，type=MULTI）→ HTTP 200，`success`=false，`error.error_category`=PERMISSION_DENIED
5. **缺参**：args 仅 `{"data_source_id": "<seedDsId>"}`（缺 sql）→ HTTP 200，`success`=false，`error.error_category`=PARAM_ERROR（PARAM_MISSING）
6. 删除服务（清理）

---

## TC-MCP-017 GET_TABLE_INFO 与 SEARCH_METADATA 测试
**级别：** P1
**前置：** 已登录，种子数据源已就绪（TC-SEM-000），ES 已索引（ES 查询前 `es_refresh`）
**数据：** `chinook.e2e.{seeded_datasource_name, mcp.subject, datasource.nonexistent_table}`

1. 搜索种子数据源 → 创建 MCP 服务（携带 `data_scopes` 绑定该数据源）
2. **GET_TABLE_INFO（平台元数据路径，decision 12：`data_source_id` 在每个 `tables[]` 项内）**：POST `/tools/GET_TABLE_INFO/test`，args `{"tables": [{"data_source_id": "<seedDsId>", "table": "<mcp.subject.term.relation_table>"}]}`（注意：参数为 `tables` 数组，`data_source_id` 在每行内，非顶层；顶层 `data_source_id` 会导致 PARAM_INVALID）→ HTTP 200
   - `success`=true，`data.type`=TABLE_METADATA，`data.tables` 非空
   - tables[0]：`table`=`mcp.subject.term.relation_table`，`columns` 与 `mcp.subject.table_columns.<同表>` 一致（来自平台同步元数据，非 JDBC 直查）
   - **不存在的表**：args tables 改为 `[{"data_source_id": "<seedDsId>", "table": "<datasource.nonexistent_table>"}]` → HTTP 200，`success`=true，`data.tables` 为空数组（静默跳过，不报错）
3. **SEARCH_METADATA（ES 搜索路径）**：POST `/tools/SEARCH_METADATA/test`，args `{"keywords": ["<mcp.subject.field_value_search>"]}` → HTTP 200
   - `success`=true，`data.type`=SEARCH_HIT，`data.keywords`=["<mcp.subject.field_value_search>"]
   - `data.data_sources` 或 `data.terms` 至少一个非空（`mcp.subject.table_columns` 中对应表的维度值命中，按数据源分组返回）
4. **空关键词**：args `{"keywords": []}` → HTTP 200，`success`=false，`error.error_category`=PARAM_ERROR（PARAM_MISSING）
5. 删除服务（清理）

---

## TC-MCP-018 工具测试异常路径
**级别：** P1
**前置：** 已登录，种子数据源已就绪（TC-SEM-000）
**数据：** `chinook.e2e.{seeded_datasource_name, mcp.subject}`

> **注意**：种子主题（`seeded_subject_name`）包含全部种子表，无法触发表级违规。需**新建专用主题**（只添加 `<mcp.subject.dedicated_subject_table>` 一张表，测后删除）作为服务数据范围。

1. 搜索种子数据源 → **创建专用主题**（name 自定，绑定种子数据源，只添加 `<mcp.subject.dedicated_subject_table>` 表）
2. **创建 MCP 服务**，`data_scopes` 绑定该专用主题（`scope_type`=SUBJECT）
3. **SUBJECT scope 展开**：GET `/v1/mcp-services/{id}/data-scope` → `resolved_data_sources` 含种子数据源（SUBJECT 展开为实际数据源 ID）
4. **表级 scope 违规**：创建自定义工具（sql_template=`SELECT * FROM <mcp.subject.term.relation_table>`，config.data_source_id=种子 dsId）→ POST `/tools/{toolId}/test` → HTTP 200，`success`=false，`error.error_category`=SCOPE_ERROR（该表不在专用主题表集合内）
5. **工具不存在**：POST `/tools/00000000-0000-0000-0000-000000000000/test` → HTTP 200，`success`=false，`error.error_category`=PARAM_ERROR（TOOL_NOT_FOUND）
6. **disabled 自定义工具**：再创建一工具 → PUT `/tools/{id}` 设 `enabled`=false → POST `/tools/{id}/test` → HTTP 200，`success`=false，`error.error_category`=PARAM_ERROR（TOOL_DISABLED）
7. 删除服务 + 删除专用主题（清理）

---

## TC-MCP-019 PARAMETERIZED_SQL 无策略拦截（回归锚点）
**级别：** P1
**前置：** 已登录，种子数据源已就绪
**数据：** `chinook.e2e.{seeded_datasource_name, mcp.tool.update_template, mcp.tool.update_params}`

> **背景**：PARAMETERIZED_SQL 的 SQL 模板由作者配置时编写，运行时仅注入参数值，无运行时 sqlPolicy。UPDATE 模板直接执行（不被策略拦截）。本用例锚定该行为，防止策略校验回归。

1. 搜索种子数据源 → 创建 MCP 服务（携带 `data_scopes` 绑定该数据源）
2. **创建 UPDATE 模板工具**：POST `/v1/mcp-services/{id}/tools`，name 自定，`tool_type`=PARAMETERIZED_SQL，config（JSON 字符串）：`data_source_id`=种子 dsId，`sql_template`=`mcp.tool.update_template`（`UPDATE genre SET name = name WHERE genreid = {{id}}`，no-op 不污染种子数据），`parameters`=`mcp.tool.update_params`（id/Number/required）
3. **测试执行**：POST `/tools/{id}/test`，args `{"id": -1}` → HTTP 200
   - `success`=**true**（不再被策略拦截），`execution_time_ms` 非负
   - `data.type`=SQL_EXECUTION，`data.executed_sql` 含 `UPDATE genre`，`data.results` 非空
   - results[0]：`type`=WRITE、`success`=true、`affected_rows`=0（WHERE genreid=-1 无匹配行，数据未变；注意 chinook 表列为 `genreid` 而非 `genre_id`）
4. 删除服务（清理）

---

## TC-MCP-020 基于主题的 MCP 服务主路径：术语检索与关联展开
**级别：** P0
**前置：** 已登录，种子数据源/主题已就绪（TC-SEM-000），ES 索引完整（TABLE 3/FIELD 7/FIELD_VALUE 25/TERM 1/SUBJECT 1；索引缺失时按环境修复流程重建：表 PUT + 列同步 + 值抽取）
**数据：** `chinook.e2e.{seeded_datasource_name, seeded_subject_name, semantic.seed_term_name, mcp.subject}`

> **背景**：MCP 服务的主路径是**关联主题**——把领域术语暴露给 LLM。核心链路：搜关键词 → ES 命中 TERM → `terms` 返回术语（含主题归属）→ TermRelation 展开关联表 → 表出现在 `data_sources`（关键词不含表名）。

1. 搜索种子主题（`seeded_subject_name`）获取 subjectId，搜索种子数据源获取 datasourceId
2. **创建 MCP 服务绑定主题**：`data_scopes` 携带 `{"scope_type": "SUBJECT", "reference_id": "<subjectId>"}` → 200
3. **SUBJECT scope 展开**：GET `/v1/mcp-services/{id}/data-scope` → `resolved_data_sources` 含种子数据源
4. **搜术语名（核心）**：POST `/tools/SEARCH_METADATA/test`，args `{"keywords": ["<seed_term_name>"]}` → HTTP 200
   - `success`=true，`data.type`=SEARCH_HIT
   - **`data.terms` 非空**：terms[0].`name`=`mcp.subject.term.name`、`description`=`mcp.subject.term.description`、`subject_name`=`seeded_subject_name`（术语归属主题正确）
   - **`data.data_sources` 含关联表**：`mcp.subject.term.relation_table` 出现在 tables 中——**关键词不含表名，靠 TermRelation（`mcp.subject.term.relation_field`）展开**
5. **搜术语别名**：args `{"keywords": ["<mcp.subject.term.aliases[0]>"]}` → terms 含该术语，data_sources 含 `mcp.subject.term.relation_table`（TERM 文档 keywords 含别名）
6. **搜维度值（FIELD_VALUE 路径）**：args `{"keywords": ["<mcp.subject.field_value_search>"]}` → success=true，data_sources 含 `mcp.subject.term.relation_table`，**terms 为空**（值命中 ≠ 术语命中）
7. **搜表名**：args `{"keywords": ["<mcp.subject.table_search>"]}` → data_sources 含该表
8. **主题外关键词（scope 过滤）**：args `{"keywords": ["<mcp.subject.outside_keyword>"]}` → success=true，`data.data_sources` 与 `data.terms` 均为空数组（不报错）
9. **GET_TABLE_INFO 主题内表（正向）**：args `{"tables": [{"data_source_id": "<dsId>", "table": "<mcp.subject.term.relation_table>"}]}` → success=true，`data.tables` 非空，columns 与 `mcp.subject.table_columns.<同表>` 一致
10. **EXECUTE_SQL 主题内表（正向）**：args `{"data_source_id": "<dsId>", "sql": "<mcp.subject.select_sql>"}` → success=true，results[0] SELECT、rows 非空（表级 scope 通过）
11. **服务详情**：GET `/v1/mcp-services/{id}` → `tool_count`=6（6 个预置工具，无自定义）
12. 删除服务（清理）

---

## TC-MCP-021 UPDATE_TABLE_INFO 元数据写入（含部分失败与恢复）
**级别：** P0
**前置：** 已登录，种子数据源已就绪（TC-SEM-000）
**数据：** `chinook.e2e.{seeded_datasource_name, mcp.write_tool, datasource.nonexistent_table}`

> **背景**：LLM 把学到的表描述/别名写回共享元数据存储。写入立即可见（GET_TABLE_INFO 回读），单条失败不阻塞其他条目（部分失败语义）。测试后恢复基线（genre 表基线：`description`=""、`aliases`=[]），不污染种子数据。

1. 搜索种子数据源 → 创建 MCP 服务（携带 `data_scopes` 绑定该数据源）
2. **基线读取**：POST `/tools/GET_TABLE_INFO/test`，args `{"tables": [{"data_source_id": "<seedDsId>", "table": "<write_tool.table>"}]}` → 记录 `data.tables[0].description` 与 `aliases` 为基线（预期 `""` 与 `[]`）
3. **写入表描述与别名**：POST `/tools/UPDATE_TABLE_INFO/test`，args `{"tables": [{"data_source_id": "<seedDsId>", "table": "<write_tool.table>", "description": "<write_tool.table_desc>", "aliases": ["<write_tool.table_aliases[0]>"]}]}` → HTTP 200
   - `success`=true，`data.type`=**METADATA_UPDATE**
   - `data.results[0]`：`entity_type`=TABLE、`entity`=`write_tool.table`、`success`=true、`change_type`=UPDATE
   - `results[0].old.description`/`aliases` 与基线一致；`results[0].new.description`=写入值
4. **回读验证（写入立即生效）**：GET_TABLE_INFO 同参数 → `data.tables[0].description`=写入值，`aliases` 含写入别名
5. **部分失败**：UPDATE_TABLE_INFO 一次传两行——genre（合法）+ `write_tool.ghost_table`（不存在）→ HTTP 200
   - `data.results` 长度 2；`results[0].success`=true（合法行写入生效，GET_TABLE_INFO 回读确认）
   - `results[1].success`=false，`results[1].error.error_category`=**PARAM_ERROR**（ENTITY_NOT_FOUND），`error.message` 含表名
6. **scope 违规（ds 级）**：args `tables[0].data_source_id`=随机 UUID → HTTP 200，`results[0].success`=false，`error.error_category`=**SCOPE_ERROR**（写工具仅做 ds 级校验，不做表级）
7. **参数超长（binder 层整体失败）**：args `description`=501 字符 → HTTP 200，`success`=false，`error.error_category`=PARAM_ERROR，`data` 不存在（非逐条失败）
8. **恢复基线**：UPDATE_TABLE_INFO 写回基线（`description`=""、`aliases`=[]）→ success=true；GET_TABLE_INFO 回读确认已恢复
9. 删除服务（清理）

---

## TC-MCP-022 UPDATE_COLUMN_INFO 元数据写入（含失败分支与恢复）
**级别：** P1
**前置：** 已登录，种子数据源已就绪（TC-SEM-000）
**数据：** `chinook.e2e.{seeded_datasource_name, mcp.write_tool}`

> **背景**：列描述/别名写入（列值语义是最高价值知识）。写后 GET_TABLE_INFO 回读验证（ColumnDef.comment=列描述）。测试后恢复基线。

1. 搜索种子数据源 → 创建 MCP 服务（携带 `data_scopes` 绑定该数据源）
2. **基线读取**：GET_TABLE_INFO，args `{"tables": [{"data_source_id": "<seedDsId>", "table": "<write_tool.table>"}]}` → 记录列 `<write_tool.column>` 的 `comment` 与 `aliases` 为基线
3. **写入列描述与别名**：POST `/tools/UPDATE_COLUMN_INFO/test`，args `{"columns": [{"data_source_id": "<seedDsId>", "table": "<write_tool.table>", "column": "<write_tool.column>", "description": "<write_tool.column_desc>", "aliases": ["<write_tool.column_aliases[0]>"]}]}` → HTTP 200
   - `data.results[0]`：`entity_type`=COLUMN、`entity`=`write_tool.column`、`success`=true、`change_type`=UPDATE
   - `results[0].old.comment` 语义（description）与基线一致；`results[0].new.description`=写入值
4. **回读验证**：GET_TABLE_INFO → 列 `write_tool.column` 的 `comment`=写入值，`aliases` 含写入别名
5. **不存在的列**：args `column`=`write_tool.ghost_column` → HTTP 200，`results[0].success`=false，`error.error_category`=PARAM_ERROR（ENTITY_NOT_FOUND），`error.message` 含列名
6. **恢复基线**：UPDATE_COLUMN_INFO 写回基线 → success=true；GET_TABLE_INFO 回读确认已恢复
7. 删除服务（清理）

---

## TC-MCP-023 UPSERT_TERM 术语写入（CREATE→UPDATE 幂等 + 平台可见性）
**级别：** P0
**前置：** 已登录，种子主题已就绪（TC-SEM-000）
**数据：** `chinook.e2e.{seeded_subject_name, mcp.write_tool}`

> **背景**：LLM 写入的业务术语须在平台侧可见（跨工具一致性：MCP 写入 → 平台术语 API 可查），重复调用同 subject+name 走 UPDATE（幂等 upsert）。术语通过平台 API 清理（无 v1 管理 UI）。

1. 搜索种子主题（`seeded_subject_name`）获取 subjectId，创建 MCP 服务（`data_scopes` 绑定该主题，`scope_type`=SUBJECT）
2. **创建术语**：POST `/tools/UPSERT_TERM/test`，args `{"terms": [{"subject_name": "<seeded_subject_name>", "name": "<write_tool.term_name>", "description": "<write_tool.term_desc>", "aliases": ["<write_tool.term_aliases[0]>"]}]}` → HTTP 200
   - `data.results[0]`：`entity_type`=TERM、`entity`=`write_tool.term_name`、`success`=true、`change_type`=**CREATE**、`old`=null、`new.description`=写入值
3. **平台 API 验证（跨工具一致性）**：GET `/v1/subjects/{subjectId}/terms`（keyword=术语名）→ 术语存在，`name`/`description`/`aliases` 与写入一致
4. **更新术语（幂等 upsert）**：再次调用 UPSERT_TERM（同 subject+name，`description`=`write_tool.term_desc_v2`）→ `results[0].change_type`=**UPDATE**，`old.description`=步骤 2 写入值，`new.description`=新值
5. 平台 API 回读 → `description` 已更新为 `term_desc_v2`
6. **主题不在范围**：args `terms[0].subject_name`=`write_tool.outside_subject` → HTTP 200，`results[0].success`=false，`error.error_category`=**SCOPE_ERROR**
7. **清理**：平台 API DELETE `/v1/terms/{id}` 删除测试术语（按步骤 3 查到的 id），删除服务
