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

1. 使用 bad_connection 测试数据源连接
2. 预期返回 `false`，不应返回 `true`
3. 预期 HTTP 状态码为 200（接口本身正常，只是连接失败）

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

## TC-DS-004 浏览数据库 Schema（两种方式）
**级别：** P1
**前置：** 已登录
**注意：** 需要真实数据库连接

1. **方式一（无需创建数据源）**：直接用 postgres_local 的连接信息调用 POST /schemas，传入 jdbc_url、username、password、type
2. 验证返回结果：字符串数组，包含 `chinook.e2e.datasource.expected_schemas` 中的所有 schema
3. **方式二（已有数据源）**：创建 PostgreSQL 数据源（使用 postgres_local）
4. 通过 GET /{id}/schemas 获取该数据源的 schema 列表
5. 验证与方式一返回的结果一致（都包含 `expected_schemas` 中的 schema），不包含 null 或空字符串
6. 删除该数据源（清理）

---

## TC-DS-005 浏览 Schema 下的 Tables 和 Columns
**级别：** P1
**前置：** 已登录
**注意：** 需要真实数据库连接
**数据：** `chinook.e2e.datasource.{core_tables, browse_table1, browse_table1_columns, browse_table2, browse_table2_columns}`

1. 创建 PostgreSQL 数据源（使用 postgres_local）
2. 获取 `chinook.e2e.datasource.expected_schema` 的 table 列表
3. 验证返回结果：
   - 是数组，非空，每个 table 对象包含 `name` 字段
   - 包含 `chinook.e2e.datasource.core_tables` 中的所有表名
4. 获取 `chinook.e2e.datasource.browse_table1` 的 column 列表
5. 验证返回结果：
   - 是数组，非空，每个 column 包含 `name` 和类型相关信息
   - 包含 `chinook.e2e.datasource.browse_table1_columns` 中的列名
6. 获取 `chinook.e2e.datasource.browse_table2` 的 column 列表
7. 验证包含 `chinook.e2e.datasource.browse_table2_columns` 中的列名
8. 删除该数据源（清理）

---

## TC-DS-006 执行 SQL 查询
**级别：** P1
**前置：** 已登录
**注意：** 需要真实数据库连接
**数据：** `chinook.sample_queries`，`chinook.e2e.datasource.{album_row_count, join_result_fields}`

1. 创建 PostgreSQL 数据源（使用 postgres_local）
2. 执行 simple_count SQL（参考 chinook.sample_queries.simple_count）
3. 验证返回结果：
   - 是数组，至少 1 行
   - 第一行包含 `total` 字段，值为 `chinook.e2e.datasource.album_row_count`
4. 执行 join_three_tables SQL（参考 chinook.sample_queries.join_three_tables）
5. 验证返回结果：
   - 是数组，每行包含 `chinook.e2e.datasource.join_result_fields` 中的所有字段
   - 数据有意义（非空值、非乱码）
6. 删除该数据源（清理）

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
3. 验证批量添加返回 200
4. **查询已添加表名列表**（GET added-names）
5. 验证返回的表中包含 `table_mgmt.add_tables` 中的所有表名
6. **ES 验证 - TABLE 已同步**：查询 ES，filter `entity.datasourceId` = datasourceId AND `type` = `TABLE`
7. 验证 ES 中有与 `add_tables` 数量相等的 TABLE 文档，每条 `entity.tableName` 对应一个表名
   - 每条 `keywords` 数组至少包含表名本身
   - `entity.datasourceId`、`entity.tableId` 正确
   - `createdTime`、`updatedTime` 非空
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
13. **ES 验证 - TABLE 已更新**：查询 ES 中 `id` = `table:{该表的tableId}` 的文档
    - `keywords` 数组包含 `table_mgmt.update.aliases` 中的所有值
    - `description` 为 `table_mgmt.update.description`
    - `updatedTime` 晚于之前的值
14. **删除一张表**：删除 `table_mgmt.delete_target` 表
15. 验证删除返回 200
16. 查询已添加表名列表，确认 `delete_target` 已不在，其余表仍存在
17. **ES 验证 - 已删表的 TABLE 已清理**：查询 ES 中 `entity.tableName` = `delete_target` AND `entity.datasourceId` = datasourceId 的 TABLE 文档
    - 应返回 0 条（已物理删除）
18. **ES 验证 - 其余表的 TABLE 仍存在**：确认未删除表的 TABLE 文档不受影响
19. 删除数据源（清理）

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
11. **ES 验证 - FIELD 已更新**：查询 ES 中 `id` = `field:{该列的columnId}` 的文档
    - `keywords` 包含 `column_values.column_aliases` 中的所有值
    - `description` 为 `column_values.column_description`
12. **抽取维度值**（POST column values/extract）：对 `target_column` 列触发维度值提取
13. 验证抽取返回 200
14. **查询维度值列表**（GET column values），验证抽取结果：
    - 返回 `column_values.value_count` 个值
    - 每项包含 `id`、`value`、`synonyms` 字段
    - 包含 `column_values.sample_values` 中的值（至少匹配 3 个）
    - `synonyms` 初始为空数组，分页参数合理
15. **ES 验证 - FIELD_VALUE 已抽取**：查询 ES 中 `entity.tableId` = tableId AND `entity.field` = `target_column` AND `type` = `FIELD_VALUE`
    - 共 `value_count` 条文档
    - 每条 `keywords` 数组包含维度值文本本身
    - 能找到 `keywords` 含 `sample_values` 中各值的文档
16. **更新维度值**（PUT column values）：遍历 `column_values.synonym_updates`，为每个 value 设置对应的 synonyms
    - 注意：需要传完整 values 列表（id + value + synonyms），deletedIds 为空数组
17. 再次查询维度值列表，确认更新的 values 的 synonyms 已生效
18. **ES 验证 - FIELD_VALUE 同义词已更新**：查询 ES 中对应的 FIELD_VALUE 文档
    - 每条被更新的 value 的 `keywords` 包含其 synonyms 中的所有值
19. **删除数据源（清理）**
20. **ES 验证 - 全部清理**：查询 ES 中 `entity.datasourceId` = datasourceId 的所有文档，应返回 0 条

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
5. 用 page=1, size=1 查询，验证只返回 1 条，`total_pages` 根据 total 计算正确
6. 依次删除 3 个数据源（清理）
