# 语义层（主题 + 术语） - 端到端测试

使用 test-data.yaml 中配置的测试数据集。所有具体值均引用数据集 `e2e` 段下的路径。

**共享种子数据源**：所有用例共用 `chinook.e2e.seeded_datasource_name` 指定的数据源（含 `chinook.e2e.seeded_tables` 中的表，`chinook.e2e.datasource.column_values.table`.`chinook.e2e.datasource.column_values.target_column` 维度值已抽取）。TC-SEM-000 负责首次创建，后续用例按名查找复用，**均不删除该数据源**。

**ES 索引约定**（详见 test-env.yaml）：
- `SUBJECT`：id=`subject:{subjectId}`，keywords=name+aliases，entity.subjectId
- `TERM`：id=`term:{termId}`，keywords=name+aliases，entity.subjectId

---

## TC-SEM-000 种子数据初始化（一次性）
**级别：** P0
**前置：** 已登录
**数据：** `chinook.e2e.{seeded_datasource_name, seeded_subject_name, seeded_tables, default_schema}`，`chinook.e2e.datasource.column_values`
**注意：** 此用例只建不删，后续所有语义层和 MCP 服务用例依赖此数据

### 种子数据源
1. 在数据源列表中搜索 `seeded_datasource_name`
2. 如果已存在，记下 datasourceId，跳到步骤 8
3. 如果不存在，创建 PostgreSQL 数据源（使用 postgres_local），name 设为 `seeded_datasource_name`
4. 将 `seeded_tables` 中列出的表批量添加到数据源（schema 填 `default_schema`）
5. 对 `column_values.table` 表同步列
6. 将 `column_values.target_column` 列的 `extract_value_enabled` 设为 `true`
7. 对该列执行维度值抽取（POST column values/extract）
8. 验证数据源就绪：
   - 查表列表确认包含 `seeded_tables` 中的所有表
   - 查 `column_values.table` 的维度值列表，确认有 `column_values.value_count` 个值

### 种子主题
9. 在主题列表中搜索 `seeded_subject_name`
10. 如果已存在，记下 subjectId，完成
11. 如果不存在，创建主题（绑定种子数据源 datasourceId），name 设为 `seeded_subject_name`
12. 将 `seeded_tables` 中的表添加到主题
13. 在主题下创建一个术语（name 取 `chinook.e2e.semantic.seed_term_name`），关联到 `column_values.table` 的 `column_values.target_column`
14. 验证主题就绪：查主题的表列表包含 `seeded_tables`，查术语列表有术语且有 relation

---

## TC-SEM-001 主题 CRUD
**级别：** P0
**前置：** 已登录，种子数据源已就绪（TC-SEM-000）
**数据：** `chinook.e2e.seeded_datasource_name`

1. 搜索名为 `seeded_datasource_name` 的数据源，获取 datasourceId
2. **创建主题**：name 自定，description 自定，`datasource_id` 填 datasourceId，同时传 `aliases`（自定一组别名）
3. 验证返回 200，`id` 为 UUID
4. 查主题列表搜该 name，确认找到，验证 name/description/aliases/datasource_id 正确，`created_user_name`/`updated_user_name` 不为 null
5. **ES 验证**：`type=SUBJECT, entity.subjectId=subjectId` 有 1 条，keywords 含 name+aliases
6. 查主题详情，与列表一致
7. **更新主题**：改 name、description、aliases
8. 查详情验证更新生效，`updated_at > created_at`；ES 验证 keywords/description 已更新
9. **删除主题**，查列表确认已删；ES 验证 `subject:{id}` 已清理
10. **注意：不删数据源**

---

## TC-SEM-002 主题表管理：查看可用表、添加表、查已添加表、移除表
**级别：** P0
**前置：** 已登录，种子数据源已就绪
**数据：** `chinook.e2e.{seeded_datasource_name, semantic.subject_tables}`

1. 查找种子数据源，获取 datasourceId
2. **创建主题**（绑定 datasourceId）
3. 查 available-tables，验证包含 `subject_tables` 中的表名，每条有 `table_id`/`table_name`/`schema`
4. 从 available-tables 选第一张表添加到主题
5. 查主题表列表，确认已添加
6. 将其余表也添加，确认全部在列表中
7. 移除最后添加的表，确认已不在、其余仍存
8. 删除主题（不删数据源）

---

## TC-SEM-003 术语 CRUD
**级别：** P0
**前置：** 已登录，种子数据源已就绪
**数据：** `chinook.e2e.{seeded_datasource_name, semantic.term_table}`

1. 查找种子数据源，获取 datasourceId
2. 创建主题（绑定 datasourceId），将 `term_table` 添加到主题
3. **创建术语**：name 自定，description 自定，传 `aliases`
4. 查术语列表，验证分页合理（page=1/size/total≥1），name/description/aliases 正确，`relations=[]` 非 null，created_by/updated_by 不为 null
5. **ES 验证**：`type=TERM, entity.subjectId=subjectId` 有 1 条，keywords=name+aliases
6. 查术语详情，与列表一致
7. **更新术语**：改 name/description/aliases（注意 UpdateTermRequest 的 name 必填）
8. 查详情验证更新，ES 验证 keywords 已更新
9. **删除术语**，查列表确认；ES 验证 `term:{id}` 已清理
10. 删除主题（不删数据源）

---

## TC-SEM-004 术语关联关系管理（关联到列和维度值）
**级别：** P1
**前置：** 已登录，种子数据源已就绪（`column_values.table` 维度值已抽取）
**数据：** `chinook.e2e.semantic.term_relation`

1. 查找种子数据源，获取 datasourceId
2. 在数据源中查找 `term_relation.table` 表，获取 tableId；查找 `term_relation.field` 列，获取 columnId
3. 查该列的维度值列表，记下 `term_relation.sample_dim_value` 对应的 valueId
4. 创建主题（绑定 datasourceId），将该表添加到主题
5. **创建术语**，记下 termId
6. **添加 FIELD 关系**：entityType=`FIELD`，tableId，fieldName=`term_relation.field`
7. 验证返回 200
8. **添加 FIELD_VALUE 关系**：entityType=`FIELD_VALUE`，tableId，fieldName=`term_relation.field`
9. 验证返回 200
10. 查术语详情，验证 relations 有 2 条，entity_type 分别为 FIELD 和 FIELD_VALUE，table_name/field_name 正确
11. **删 FIELD 关系**（DELETE /terms/{termId}/relations/{tableId}/{fieldName}）
12. 查术语详情，验证剩 1 条 FIELD_VALUE，非 null
13. 删除术语，删除主题（不删数据源）

---

## TC-SEM-005 级联清理：删除主题时术语及 ES 文档同步清理
**级别：** P1
**前置：** 已登录，种子数据源已就绪
**数据：** `chinook.e2e.{seeded_datasource_name, semantic.term_table}`

1. 查找种子数据源 → 创建主题（绑源+加表）→ 创建 2 个术语
2. 查术语列表确认有 2 条；ES 查 `entity.subjectId` 有 1 SUBJECT + 2 TERM = 3 条
3. **直接删除主题**（不先删术语）
4. 查该主题术语列表，预期 404
5. **ES 验证**：`entity.subjectId=subjectId` 全部 0 条
6. 不删数据源

---

## TC-SEM-006 主题错误场景
**级别：** P2
**前置：** 已登录，种子数据源已就绪

1. 创建主题不填 name → 400
2. 创建主题不填 datasource_id → 400
3. 用不存在的 datasourceId 创建 → 应报错（不能 200）
4. 正常创建主题
5. 更新不存在的主题 → 404
6. 给不存在的主题加表 → 404
7. 删除不存在的主题 → 404
8. 删正常主题（不删数据源）

---

## TC-SEM-007 术语错误场景
**级别：** P2
**前置：** 已登录，种子数据源已就绪

1. 查找种子数据源 → 创建主题（不删）
2. 创建术语不填 name → 400
3. 正常创建术语
4. 重复添加相同 relation → 应报错（400/409，不能 200）
5. 删不存在的 relation → 应 404（不能 200）
6. 给不存在的 termId 加 relation → 404
7. 更新不存在的术语 → 404
8. 删不存在的术语 → 404
9. 删术语、删主题（不删数据源）

---

## TC-SEM-008 主题和术语列表分页与搜索
**级别：** P2
**前置：** 已登录，种子数据源已就绪
**数据：** `chinook.e2e.semantic.search_keyword`

1. 查找种子数据源 → 创建主题
2. 创建 3 个主题（一个 name 含 `search_keyword`），page=1/size=2 查列表
3. 验证 data≤2，total≥3，total_pages 正确；keyword 搜只返回 1 条
4. 创建 3 个术语（一个 name 含 `search_keyword`），同样验证分页+搜索
5. 删术语、删主题（不删数据源）
