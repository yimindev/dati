# MCP 服务管理 - 端到端测试

使用 test-data.yaml 中配置的测试数据集。所有具体值引用数据集 `e2e` 段下的路径。
共享种子数据源（`seeded_datasource_name`）和种子主题（`seeded_subject_name`）由 TC-SEM-000 创建，本模块用例按名称查找复用。

> **创建服务约定**：创建请求必须携带 `data_scopes`（至少 1 条，绑定种子数据源/主题）：
> `{"code": "...", "name": "...", "data_scopes": [{"scope_type": "DATA_SOURCE", "reference_id": "<datasourceId>"}]}`
> 不带 `data_scopes` → 400；服务数据范围为空时调用 `/publish` → 400

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
