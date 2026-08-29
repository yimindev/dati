# 数据源模块操作要点

> 接口定义以 `docs/api/openapi.json` 为准,本文件只补充 openapi 之外的业务知识。

## 数据源类型

`type` 枚举:`MYSQL` / `MARIADB` / `POSTGRESQL` / `ORACLE` / `SQLSERVER` / `CLICKHOUSE` / `DORIS` / `TRINO` / `UNKNOWN`

## 创建/更新数据源

- openapi 里请求体是**全量 DataSource**(含 `id`/`created_by`/`created_at` 等只读字段),实际只需业务字段:

```json
{
  "name": "订单库",
  "type": "MYSQL",
  "jdbc_url": "jdbc:mysql://localhost:3306/orders",
  "username": "root",
  "password": "******",
  "default_schema": "orders"
}
```

- `jdbc_url` / `type` / `username` 必填(见 schema `required`)
- 更新用 `UpdateDataSourceRequest`,同名接口在 `PUT /v1/data-sources/{id}`

## 连接测试

- `POST /v1/data-sources/test-connection`:body 与创建相同,**返回布尔值**而非 IdResponse
- 只测连接不落库;测试成功后才能真正创建

## 建表流程(核心链路)

```text
GET  /v1/data-sources/{id}/schemas                     → schema 列表(可选 catalog 参数)
GET  .../schemas/{schema}/tables                       → 浏览库中真实表
GET  .../tables/added-names                            → 已登记的表名列表(用于排除)
POST .../tables/batch                                  → AddTableRequest 数组 [{name, schema}]
POST .../tables/{tableId}/columns/sync                 → 从库同步列;overwrite_existing 可选
```

**坑:`batchAddTables` 返回的 `IdResponse.id` 是添加的表数量**(`"3"`),不是表 id。需要表 id 时先 `GET .../tables` 按名称找。

- `sync` 的 `overwrite_existing` 参数:true 覆盖已存在列的元数据(默认 false 保留)
- 删除表:`DELETE .../tables/{tableId}`;删除数据源:`DELETE /v1/data-sources/{id}`

## 列元数据与字典值

列相关的路径都挂在数据源+表之下(**需要 datasourceId 与 tableId 两级前缀**):

- `GET /v1/data-sources/{datasourceId}/tables/{tableId}/columns` → 列列表(分页,含 id、名称、类型、描述等)
- `PUT /v1/data-sources/{datasourceId}/tables/{tableId}/columns/{id}` → 保存单列的业务元数据(中文描述、别名 `aliases`,即列元数据增强)
- 字典值链路:
  - `POST /v1/data-sources/{datasourceId}/tables/{tableId}/columns/{columnId}/values/extract` → 从库中提取该列的实际取值(如枚举值),query 参数 `overwrite`(默认 false,true 覆盖已有值)
  - `GET /v1/data-sources/{datasourceId}/tables/{tableId}/columns/{columnId}/values` → 查已保存的字典值(分页,`keyword` 可搜索)
  - `PUT /v1/data-sources/{datasourceId}/tables/{tableId}/columns/{columnId}/values` → 保存/覆盖字典值列表(`ColumnValueListRequest`)

> 列元数据增强是语义层建设的关键步骤:同步列只是拿到原始结构,描述/别名/字典值才让 LLM 理解业务语义(见 [scenario-design.md](scenario-design.md) 第 2 节)。

## 列表查询

`GET /v1/data-sources` 分页参数 `page` / `size`(PageReq),`keyword` 可选;响应为 `PageResponse{data, total}`。
