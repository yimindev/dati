# MCP 服务管理 - 端到端测试

使用 test-data.yaml 中配置的测试数据集。所有具体值引用数据集 `e2e` 段下的路径。
共享种子数据源（`seeded_datasource_name`）和种子主题（`seeded_subject_name`）由 TC-SEM-000 创建，本模块用例按名称查找复用。

---

## TC-MCP-001 创建、查询、更新、删除 MCP 服务
**级别：** P0
**前置：** 已登录

1. **创建 MCP 服务**：name 自定，description 自定，`code` 自定（必填）
2. 验证返回 200，`id` 为 UUID
3. 在服务列表中搜该 name，确认能找到
4. 验证列表项：name/description/code 与创建一致，`status`=DRAFT
   - `created_by`/`updated_by`/`created_user_name`/`updated_user_name` 不为 null
   - `created_at`/`updated_at` 合理
5. **部分更新**：仅改 description（不传 name/code 等其他字段）
6. 查详情验证 description 已更新，`updated_at > created_at`，name/code/status 未变
7. **删除服务**，查列表确认已删

---

## TC-MCP-002 配置数据范围
**级别：** P1
**前置：** 已登录，种子数据源已就绪（TC-SEM-000）
**数据：** `chinook.e2e.{seeded_datasource_name, mcp.data_scope_type}`

1. 搜索 `seeded_datasource_name` 获取 datasourceId，创建 MCP 服务
2. **配置数据范围**：使用 `mcp.data_scope_type` 类型，reference_id 填 datasourceId
   - 注意：请求 body 字段使用 snake_case（`scope_type`、`reference_id`）
3. 验证返回 200
4. **查数据范围**：验证 items 含刚配置的记录
   - `scope_type` 匹配，`reference_id` 匹配，`reference_name` 非空
   - `resolved_data_sources` 包含数据源名称
5. 删除服务（清理）

---

## TC-MCP-003 Prompt CRUD
**级别：** P1
**前置：** 已登录

1. 创建 MCP 服务
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

1. 搜索 `seeded_datasource_name` 获取 datasourceId，创建 MCP 服务
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

1. 搜索种子数据源 → 创建 MCP 服务 → 配置 data scope（绑定数据源）
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
