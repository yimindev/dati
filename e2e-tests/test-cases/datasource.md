# 数据源管理 - 端到端测试

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

1. 使用 bad_connection（指向 192.0.2.1）测试数据源连接
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

## TC-DS-004 浏览数据库 Schema
**级别：** P1
**前置：** 已登录
**注意：** 需要真实数据库连接

1. 创建 PostgreSQL 数据源（使用 postgres_local）
2. 获取该数据源的 schema 列表
3. 全面验证返回结果：
   - 返回的是字符串数组
   - 包含 PostgreSQL 的标准 schema：`public`、`information_schema`、`pg_catalog`
   - 不包含明显无意义的值（null、空字符串）
4. 删除该数据源（清理）

---

## TC-DS-005 浏览 Tables 和 Columns
**级别：** P1
**前置：** 已登录
**注意：** 需要真实数据库连接

1. 创建 PostgreSQL 数据源（使用 postgres_local）
2. 获取 `public` schema 的 table 列表
3. 验证返回结果：
   - 是数组，非空
   - 每个 table 对象包含 `name` 字段
   - 包含已知的 Chinook 表（album、artist、track 中至少一个）
4. 获取 `album` 表的 column 列表
5. 验证返回结果：
   - 是数组，非空
   - 每个 column 包含 `name` 和类型相关信息
   - 包含常见列名（如 `albumid`、`title`）
6. 删除该数据源（清理）

---

## TC-DS-006 执行 SQL 查询
**级别：** P1
**前置：** 已登录
**注意：** 需要真实数据库连接

1. 创建 PostgreSQL 数据源（使用 postgres_local）
2. 执行 SQL：`SELECT count(*) AS total FROM album`
3. 验证返回结果：
   - 是数组，至少 1 行
   - 第一行包含 `total` 字段，值为正整数
4. 执行 SQL：`SELECT albumid, title FROM album LIMIT 3`
5. 验证返回结果：
   - 是数组，至少 1 行（如果表非空）
   - 每行包含 `albumid` 和 `title` 字段
   - `title` 是字符串，非空
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
