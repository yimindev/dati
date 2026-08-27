# 主题与术语模块操作要点

> 接口定义以 `docs/api/openapi.json` 为准,本文件只补充 openapi 之外的业务知识。

## 概念

- **主题(Subject)**:业务域,绑定一个数据源(`datasource_id` 必填),从该数据源选表组织业务口径
- **术语(Term)**:主题下的业务术语定义,可关联到具体表/字段,用于语义层

## 主题

- 创建:`POST /v1/subjects`,必填 `name` + `datasource_id`;可选 `description` / `aliases`
- 更新:`PUT /v1/subjects/{id}` 用 `UpdateSubjectRequest`
- 列表:`GET /v1/subjects` 分页(page/size/keyword)

## 主题选表

```text
GET  /v1/subjects/{id}/available-tables?schema=<必填>   → 可选表列表(该 schema 下未添加的表)
POST /v1/subjects/{id}/tables                            → body {table_id}
DELETE /v1/subjects/{id}/tables/{tableId}                → 移除表
```

- **`schema` query 参数必填**,必须先知道数据源里有哪些 schema
- `GET /v1/subjects/{id}/tables` 查看已添加的表(分页)

## 术语

- 创建:`POST /v1/subjects/{subjectId}/terms`,`name` 必填,可选 `description` / `aliases`(别名数组)
- 列表:`GET /v1/subjects/{subjectId}/terms`(分页,keyword 可搜索)
- 详情/更新/删除:`GET / PUT / DELETE /v1/terms/{id}`,更新用 `UpdateTermRequest`

## 术语关联

- 术语关联到具体业务对象,`entity_type` 为 `TABLE`(表)或 `FIELD`(字段):

```json
{
  "entity_type": "FIELD",
  "table_id": "tbl_xxx",
  "field_name": "order_amount"
}
```

- 建立:`POST /v1/terms/{id}/relations`(返回 relation id)
- 解除:`DELETE /v1/terms/{id}/relations/{tableId}/{fieldName}`
  - **坑:`entity_type=TABLE` 时 `fieldName` 路径段为空**(`.../relations/{tableId}/`),传任何值都会 404;`FIELD` 类型才传字段名
- 关联是术语语义落地的关键步骤;删除术语前先解除其关联
