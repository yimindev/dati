# 数据源管理 - 端到端测试

使用 test-data.yaml 中配置的测试数据集（当前为 Chinook）。所有具体值（表名、列名、行数等）均引用数据集 `e2e` 段下的路径，不硬编码。

---

## TC-DS-001 创建、查询、删除数据源
**级别：** P0
**前置：** 已登录
**数据：** test-data.yaml `datasources.postgres_local`

1. 创建一个 PostgreSQL 数据源（使用 postgres_local 的连接信息）
   - 注意：`type` 用大写 `POSTGRESQL`，连接信息用 `jdbc_url` 字段
2. 预期创建成功返回 200。检查返回的完整响应：
   - 包含 `id` 字段且为 UUID 格式
3. 在数据源列表中用默认分页参数搜索该数据源，确认能找到
4. 检查列表中的该条记录是否完整合理：
   - `name`、`type`、`jdbc_url`、`username` 与创建时一致
   - `page`、`size`、`total` 分页参数存在且合理
   - `created_at`、`updated_at` 是合理的时间戳（非空、非未来时间）
   - `created_by`、`updated_by` **不为 null、不为空**
   - `created_user_name`、`updated_user_name` **不为 null、不为空**
   - `password` **不应明文暴露**在列表中
5. 删除该数据源，预期返回 200
6. 再次搜索该名称，确认已找不到（清理验证）

---

## TC-DS-002 测试连接 - 失败场景
**级别：** P1
**前置：** 已登录
**数据：** test-data.yaml `datasources.bad_connection`
1. 使用 bad_connection（`jdbc:mysql://192.0.2.1:9999/nothing`）测试数据源连接
2. 预期 HTTP 状态码为 400（测试连接失败是业务结果，通过统一错误响应返回）
3. 预期 `code` 为 `DS013`（连接超时）或 `DS012`（连接被拒绝）；`message` 必须包含实际尝试的端点 `192.0.2.1:9999`，并提示检查主机/端口/网络，不能出现 `Failed to initialize connection pool` 之类的内部包装文案
4. 预期 15s 内返回（后端对整段探测有 10s 上限），不得让前端 axios 先超时
5. 中文界面（`Accept-Language: zh-CN`）返回中文提示
6. 不应再返回 HTTP 200 + `false` 的旧契约

---

## TC-DS-003 更新数据源并验证完整性
**级别：** P0
**前置：** 已登录

1. 创建一个 PostgreSQL 数据源（使用 postgres_local），记录创建时间
2. 仅更新 name（部分更新，不传其他字段）
3. 查询详情，验证 name 已更新，`updated_at` 晚于 `created_at`，其他字段未被意外修改
4. 更新 description + name（完整更新）
5. 查询详情，验证所有更新字段生效
6. 删除数据源（清理）

---

## TC-DS-004 浏览数据库 Schema
**级别：** P1
**前置：** 已登录
**注意：** 需要真实数据库连接

1. 创建 PostgreSQL 数据源（使用 postgres_local）
2. 通过 GET /{id}/schemas 获取该数据源的 schema 列表
3. 验证返回结果：字符串数组，包含 `chinook.e2e.datasource.expected_schemas` 中的所有 schema，不包含 null 或空字符串
4. 删除该数据源（清理）

> 注：原「方式一（POST /schemas 无需创建数据源）」已移除。后端与前端流程均无此设计——创建表单仅提供"测试连接"，schema 浏览在数据源详情页（已有数据源 id）进行，属过时用例。见 BUG-20260820-003。

---

## TC-DS-005 浏览 Schema 下的 Tables
**级别：** P1
**前置：** 已登录
**注意：** 需要真实数据库连接
**数据：** `chinook.e2e.datasource.core_tables`

1. 创建 PostgreSQL 数据源（使用 postgres_local）
2. 通过 GET `/v1/data-sources/{id}/schemas/{schema}/tables` 获取 `chinook.e2e.datasource.expected_schema` 的 table 列表
3. 验证返回结果：
   - 是数组，非空，每个 table 对象包含 `name` 字段
   - 包含 `chinook.e2e.datasource.core_tables` 中的所有表名
4. 删除该数据源（清理）

> 注：原始的未加表直接探查 JDBC 列接口（`GET .../tables/{table}/columns`）及直接执行 SQL 接口（`POST .../execute-sql`，原 TC-DS-006）已全部从 DataSourceController 移除。
> - 表列元数据管理统一通过「添加表 → 同步列」工作流执行（见 TC-DS-010 / TC-DS-011）；
> - SQL 执行统一在 MCP 服务与工具层通过受控快照及 DataScope 范围约束执行（见 TC-MCP-005 / TC-END-003）。

---

## TC-DS-007 缺少必填字段创建
**级别：** P2
**前置：** 已登录

1. 尝试创建一个不填 `jdbc_url` 的数据源
2. 预期返回 400，错误信息明确指出 `jdbc_url` 缺失
3. 尝试创建一个不填 `type` 的数据源
4. 预期返回 400，错误信息明确指出 `type` 缺失

---

## TC-DS-008 删除不存在的数据源
**级别：** P2
**前置：** 已登录

1. 用一个明显不存在的 UUID 尝试删除数据源
2. 预期返回错误（应为 404，带明确的错误信息），不能返回 200
3. 预期错误信息有意义（不是空响应或 500）

---

## TC-DS-009 测试连接 - 成功场景
**级别：** P1
**前置：** 已登录
**数据：** test-data.yaml `datasources.postgres_local`
**注意：** 需要真实数据库连接

1. 使用 postgres_local 的连接信息测试数据源连接
2. 预期返回 `true`
3. 预期 HTTP 状态码为 200

---

## TC-DS-010 表管理全流程：批量添加、查列表、更新元数据、删除表
**级别：** P0
**前置：** 已登录
**数据：** `chinook.e2e.datasource.table_mgmt`，`chinook.e2e.default_schema`
**注意：** 需要真实数据库连接，需验证 ES 同步

1. 创建 PostgreSQL 数据源（使用 postgres_local），记下 datasourceId
2. **批量添加表**：将 `chinook.e2e.datasource.table_mgmt.add_tables` 中列出的表批量添加到数据源
   - body 是数组，每项包含 `name`（表名）和 `schema`（填 `chinook.e2e.default_schema`）
3. 验证批量添加返回 200，响应包含 `added_count`（等于添加数量）和 `table_ids`（每个新增表的 ID）
4. **查询已添加表列表**（GET added-names）
5. 验证返回的列表中包含 `table_mgmt.add_tables` 中的所有表：每项包含 `schema` 和 `name`
6. **ES 验证 - TABLE 已同步**：查询 ES，filter `entity.datasourceId` = datasourceId AND `type` = `TABLE`
7. 验证 ES 中有与 `add_tables` 数量相等的 TABLE 文档，每条 `entity.tableName` 对应一个表名
   - 每条 `keywords` 数组至少包含表名本身
   - `entity.datasourceId`、`entity.tableId` 正确
   - `createdTime`、`updatedTime` 非空；记录该 TABLE 文档的 `createdTime`（后续更新不得改变）
8. **查询表列表**（GET tables，用 `table_mgmt.update.target` 作为 keyword 搜索）
9. 验证分页列表中的表记录：
   - `name`、`schema` 正确
   - `id` 为 UUID 格式，`aliases` 字段存在
   - `created_by`、`updated_by` **不为 null、不为空**
   - `created_user_name`、`updated_user_name` **不为 null、不为空**
10. **更新表元数据**：更新 `table_mgmt.update.target` 表，设置：
    - `aliases` = `table_mgmt.update.aliases`
    - `description` = `table_mgmt.update.description`
11. 验证更新返回 200，且响应 `id` 不为 null
12. 再次查表列表，验证更新后的 aliases 和 description 已生效
13. **按别名搜索表**：GET tables，keyword 分别取 `table_mgmt.update.aliases` 中的值 → 只返回 `table_mgmt.update.target` 表（列表搜索同时匹配 name 与 aliases）
14. **通配符按字面匹配**：keyword=`%25`（即 `%`）→ 返回 0 条；keyword=`_` → 只返回 name/aliases 中真正含下划线的表，不得返回全量
15. **ES 验证 - TABLE 已更新**：查询 ES 中 `id` = `table:{该表的tableId}` 的文档
    - `keywords` 数组包含 `table_mgmt.update.aliases` 中的所有值
    - `description` 为 `table_mgmt.update.description`
    - `updatedTime` 晚于之前的值
    - `createdTime` 与步骤 7 记录的值一致（不得被重置为更新时刻）
16. **删除一张表**：删除 `table_mgmt.delete_target` 表
17. 验证删除返回 200
18. 查询已添加表列表，确认 `delete_target` 已不在（按 `schema`+`name` 匹配），其余表仍存在
19. **ES 验证 - 已删表的 TABLE 已清理**：查询 ES 中 `entity.tableName` = `delete_target` AND `entity.datasourceId` = datasourceId 的 TABLE 文档
    - 应返回 0 条（已物理删除）
20. **ES 验证 - 其余表的 TABLE 仍存在**：确认未删除表的 TABLE 文档不受影响
21. 删除数据源（清理）

---

## TC-DS-011 列同步与维度值抽取全流程
**级别：** P0
**前置：** 已登录
**数据：** `chinook.e2e.datasource.column_values`，`chinook.e2e.default_schema`
**注意：** 需要真实数据库连接，需验证 ES 同步

1. 创建 PostgreSQL 数据源（使用 postgres_local），记下 datasourceId
2. 批量添加 `column_values.table` 到数据源（schema 填 `default_schema`），记下 tableId
3. **同步列**（POST columns/sync）：触发从数据库同步列定义
4. 验证同步返回 200
5. **查询列列表**（GET columns），验证同步结果：
   - 返回的列中包含 `column_values.expected_columns` 中的所有列名
   - 每列有 `id`（UUID）、`column_type`、`aliases`
   - `extract_value_enabled` 字段存在（初始为 false）
   - `created_by`、`updated_by` **不为 null、不为空**
   - `created_user_name`、`updated_user_name` **不为 null、不为空**
   - `table_id` 正确
6. **ES 验证 - FIELD 已同步**：查询 ES 中 `entity.tableId` = tableId AND `type` = `FIELD`
7. 验证有与 `expected_columns` 数量相等的 FIELD 文档，`entity.field` 分别对应各列名
   - 每条 `keywords` 至少包含列名本身
   - `entity.datasourceId`、`entity.tableId`、`entity.tableName` 正确
   - `createdTime`、`updatedTime` 非空；记录目标列的 `createdTime`（后续更新/同步不得改变）
8. **部分更新列元数据（不含 name）**：将 `column_values.target_column` 列进行部分更新：
   - body 中 **只传** `description`、`aliases`、`extract_value_enabled` 三个字段
   - **不传 `name`、不传 `column_type`**
   - `description` = `column_values.column_description`
   - `aliases` = `column_values.column_aliases`
   - `extract_value_enabled` = `true`
   - 使用 PUT /columns/{columnId}
9. 验证更新返回 200
10. 再次查询列列表确认更新生效：
    - aliases、description 已更新
    - `extract_value_enabled` 为 `true`
    - **`name` 未被清空**，仍为原来的列名（验证部分更新语义正确）
    - **`column_type` 未被清空**，仍为原来的类型
11. **按别名搜索列**：GET columns，keyword 分别取 `column_values.column_aliases` 中的值 → 只返回 `column_values.target_column` 列（列表搜索同时匹配 name 与 aliases）
12. **通配符按字面匹配**：keyword=`%25`（即 `%`）→ 返回 0 条；keyword=`_` → 只返回 name/aliases 中真正含下划线的列，不得返回全量
13. **ES 验证 - FIELD 已更新**：查询 ES 中 `id` = `field:{该列的columnId}` 的文档
    - `keywords` 包含 `column_values.column_aliases` 中的所有值
    - `description` 为 `column_values.column_description`
    - `createdTime` 与步骤 7 记录的值一致（更新不得重置创建时间）
14. **抽取维度值**（POST column values/extract）：对 `target_column` 列触发维度值提取
15. 验证抽取返回 200
16. **查询维度值列表**（GET column values），验证抽取结果：
    - 返回 `column_values.value_count` 个值
    - 每项包含 `id`、`value`、`synonyms` 字段
    - 包含 `column_values.sample_values` 中的值（至少匹配 3 个）
    - `synonyms` 初始为空数组，分页参数合理
17. **ES 验证 - FIELD_VALUE 已抽取**：查询 ES 中 `entity.tableId` = tableId AND `entity.field` = `target_column` AND `type` = `FIELD_VALUE`
    - 共 `value_count` 条文档
    - 每条 `keywords` 数组包含维度值文本本身
    - 能找到 `keywords` 含 `sample_values` 中各值的文档
18. **更新维度值**（PUT column values）：遍历 `column_values.synonym_updates`，为每个 value 设置对应的 synonyms
    - 注意：需要传完整 values 列表（id + value + synonyms），deletedIds 为空数组
19. 再次查询维度值列表，确认更新的 values 的 synonyms 已生效
20. **ES 验证 - FIELD_VALUE 同义词已更新**：查询 ES 中对应的 FIELD_VALUE 文档
    - 每条被更新的 value 的 `keywords` 包含其 synonyms 中的所有值
21. **再次同步列（回归锚点：同步不得重置值抽取配置）**：再次 POST columns/sync（不传 overwrite_existing）
    - **预期**：重新查询列列表，`extract_value_enabled` 仍为 **true**（用户配置而非数据库结构，应与 aliases 一样被保留）
    - **预期**：`aliases`、`description` 仍为步骤 8 写入的值（既有保留语义不回归）
    - **预期**：ES 中该列 FIELD_VALUE 文档数仍为 `value_count`（同步不应删除已抽取的维度值）
    - **预期**：ES 中该列 FIELD 文档的 `createdTime` 仍为步骤 7 记录的值（同步重建不得重置创建时间）
    - **注**：当前实现（2026-08-20 前）会静默重置 `extract_value_enabled=false` 并删除全部 FIELD_VALUE 文档，本步骤为回归验证锚点
22. **跨数据源路径校验**：创建第二个空数据源，记下 secondDatasourceId
    - 用 secondDatasourceId + 当前 tableId 调用 POST columns/sync → 预期 404 `DS007`
    - 用 secondDatasourceId + 当前 tableId + 当前 columnId 调用 PUT columns/{columnId} → 预期 404 `DS008`
    - 用同样的错误路径调用 POST values/extract → 预期 404 `DS008`
    - 再查当前列列表，确认 aliases/description/extract_value_enabled 均未被修改
23. 删除第二个数据源（清理）
24. 删除数据源（清理）
25. **ES 验证 - 全部清理**：查询 ES 中 `entity.datasourceId` = datasourceId 的所有文档，应返回 0 条

---

## TC-DS-012 表管理的错误场景
**级别：** P2
**前置：** 已登录
**数据：** `chinook.e2e.datasource.nonexistent_table`

1. 创建 PostgreSQL 数据源（使用 postgres_local）
2. **添加不存在的表**：尝试批量添加 `nonexistent_table`
3. 预期返回 404，错误信息指明表在数据源中不存在
4. **删除不存在的表**：用一个不存在的 UUID（如 `00000000-0000-0000-0000-000000000000`）删除表
5. 预期返回 404，错误信息指明表不存在
6. **对不存在的表同步列**：用一个不存在的 tableId 触发列同步
7. 预期返回 404，错误信息指明表不存在
8. 删除数据源（清理）

---

## TC-DS-013 数据源列表分页与搜索
**级别：** P2
**前置：** 已登录

1. 创建 `chinook.e2e.datasource.pagination_count` 个数据源（使用 postgres_local 连接信息，仅 name 不同）
2. 用默认分页参数（page=1）查询数据源列表
3. 验证分页信息合理：`page`=1、`size` 有默认值、`total` ≥ `pagination_count`
4. 用 keyword 搜索其中一个的名称，确认只返回匹配的
5. **通配符按字面匹配**：keyword=`%25`（即 `%`）→ total=0（不得返回全量）；keyword=`_` → 只返回名称中真正含下划线的数据源
6. 用 page=1, size=1 查询，验证只返回 1 条，`total_pages` 根据 total 计算正确
7. 依次删除 3 个数据源（清理）

---

## TC-DS-014 测试连接 - 默认 schema 不可用
**级别：** P1
**前置：** 已登录
**数据：** test-data.yaml `datasources.postgres_local`
**注意：** 需要真实 PostgreSQL 连接

1. 将 postgres_local 的 jdbc_url 追加/替换 `currentSchema` 为不存在的 schema（如 `missing_schema_<timestamp>`）
2. 调用 `POST /v1/data-sources/test-connection`
3. 预期 HTTP 状态码为 400，`code=DS006`，`message` 说明「连接成功但没有可用的默认 schema」，并提示检查 `currentSchema`/`search_path`
4. 使用同一 URL 调用 `POST /v1/data-sources`
5. 预期同样返回 400 `DS006`，且数据源未落库
6. 清理：按名称搜索确认没有创建成功


---

## TC-DS-015 测试连接 - 错误分类可自诊断
**级别：** P1
**前置：** 已登录
**注意：** 需要真实 PostgreSQL 连接

对每种错误分别调用 `POST /v1/data-sources/test-connection`，断言状态码、错误码与 message：

1. 密码错误 → 400 `DS014`，message 含用户名与「认证失败/检查用户名和密码」
2. 数据库不存在（`jdbc:postgresql://localhost:5432/no_such_db_<ts>`）→ 400 `DS015`，message 含库名与 `host:port`
3. `type=MYSQL` + PostgreSQL URL → 400 `DS010`，message 说明期望前缀 `jdbc:mysql://`
4. `jdbc_url=jdbc:postgresql://` → 400 `DS009`，message 含 URL 与期望格式
5. 缺少 `type` → 400 `CM001`，message 指向字段 `type`
6. `type=UNKNOWN` → 400 `DS005`
7. URL 不含数据库名（`jdbc:postgresql://localhost:5432` 或结尾 `/`）→ 400 `DS019`，message 含期望的 URL 示例；不得出现驱动的 `Unable to parse URL` 原文
7. 所有场景的 message 不得出现堆栈、换行或连接池内部前缀；不得回显密码
8. 若 TCP 可达但驱动建会话失败（如本地代理导致 IP 字面量异常）→ 400 `DS022`，提示检查代理/SSL/驱动

---

## TC-DS-016 创建/编辑数据源 - 连接校验
**级别：** P0
**前置：** 已登录
**数据：** test-data.yaml `datasources.postgres_local`
**注意：** 需要真实 PostgreSQL 连接

1. 不先调用测试连接，直接用 postgres_local 创建数据源 → 200，返回 id
2. 用错误密码创建另一个数据源 → 400 `DS014`，且该名称未落库
3. `PUT /v1/data-sources/{id}` 仅传 `{"name":"renamed"}`（不带 password/jdbc_url）→ 200
4. 查询详情：name 已更新，jdbc_url/username/default_schema 未变
5. 再次 `PUT` 只改 description 同样成功（证明编辑不需要重填密码）
6. `POST /v1/data-sources/test-connection` 传 `{"id":"<id>","type":"POSTGRESQL","jdbc_url":"<与已存一致>","username":"<与已存一致>","password":""}` → 200 `true`（无需重输密码）
7. 同一请求把 `jdbc_url` 改成另一个地址、`password` 留空 → 400 `DS020`，且**不得**用已存密码去连该地址
8. 用不存在的 id + 空密码 → 404 `DS002`；用无 VIEW 权限的账号 → 403 `PM001`
9. `PUT /v1/data-sources/{id}` 只改 `jdbc_url`（指向另一个地址）且不传 password → 400 `DS021`，且不得用已存密码去连新地址
10. 同一请求补上 `password` → 200（以新密码探测新地址）
11. 清理：删除数据源

---

## TC-DS-017 测试连接 - 不可达地址的时间上限
**级别：** P1
**前置：** 已登录

1. 用黑洞地址（如 `jdbc:postgresql://192.0.2.1:5432/postgres`）调用测试连接
2. 预期 15s 内返回 400，`code=DS013`
3. 重复 3 次，确认耗时稳定（不会因连接池累积而变慢）
