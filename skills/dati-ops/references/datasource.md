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

- `GET .../columns` → 列列表(含 id、名称、类型、描述等)
- `PUT .../columns/{id}` → 保存列的业务元数据(中文描述、业务含义)
- 字典值链路:
  - `POST .../values/extract` → 从库中提取该列的实际取值(如枚举值),query 参数 `overwrite`(默认 false,true 覆盖已有值)
  - `GET .../values` → 查已保存的字典值(分页,`keyword` 可搜索)
  - `PUT .../values` → 保存/覆盖字典值列表(`ColumnValueListRequest`)

## 列表查询

`GET /v1/data-sources` 分页参数 `page` / `size`(PageReq),`keyword` 可选;响应为 `PageResponse{data, total}`。
