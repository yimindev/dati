# 列值抽取功能设计

## 1. 概述

**功能目标**：为列提供值抽取能力，支持 NL2SQL 场景下匹配列的实际值。

**核心流程**：用户手动标记要抽取值的列 → 从数据库抽取唯一值存入 ES → 支持同义词配置用于语义搜索。

---

## 2. 系统级配置

### 2.1 配置项（YAML）

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `system.column-value-sample-limit` | Integer | 1000 | 值抽取默认采样数量上限 |
| `system.column-value-length-limit` | Integer | 256 | 值长度上限（字符） |

### 2.2 说明

- 采样数量上限可按列覆盖，列级别配置为空时使用系统默认
- 值长度上限为系统级统一限制，手工添加和抽取时均需校验

---

## 3. 数据模型

### 3.1 ColumnInfo 变更

| 字段 | 类型 | 说明 |
|------|------|------|
| `extractValueEnabled` | Boolean | 是否启用值抽取 |
| `valueSampleLimit` | Integer | 采样数量上限（为空则用系统默认） |

### 3.2 ES 文档结构

**Type**: `FIELD_VALUE`

```json
{
  "id": "field_value:{columnId}:{hashOfValue}",
  "type": "FIELD_VALUE",
  "keywords": ["原始值", "同义词1", "同义词2"],
  "entity": {
    "tableId": "xxx",
    "field": "column_name",
    "tableName": "table_name"
  }
}
```

**id 生成规则**：UUID，保证唯一性

**keywords**：原始值 + 同义词列表，用于语义搜索匹配

**entity**：关联列信息，不含 subjectId

---

## 4. API 设计

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/v1/data-sources/{dsId}/tables/{tableId}/columns/{columnId}/values` | 获取某列所有值 |
| PUT | `/v1/data-sources/{dsId}/tables/{tableId}/columns/{columnId}/values` | 批量保存（新增/更新/删除） |
| POST | `/v1/data-sources/{dsId}/tables/{tableId}/columns/{columnId}/values/extract` | 触发抽取 |

### 4.1 GET /values 响应

```json
{
  "values": [
    { "id": "xxx", "value": "北京", "synonyms": ["帝都"] },
    { "id": "yyy", "value": "上海", "synonyms": [] }
  ]
}
```

### 4.2 PUT /values 请求体

```json
{
  "values": [
    { "id": "existing_id", "value": "北京", "synonyms": ["帝都"] },
    { "id": null, "value": "天津", "synonyms": [] }
  ],
  "deletedIds": ["id_to_delete"]
}
```

- `values`: 要新增或更新的值，`id` 为 null 表示新增
- `deletedIds`: 要删除的值 ID 列表

### 4.3 POST /values/extract 请求体

```json
{
  "sampleLimit": 1000,
  "lengthLimit": 256
}
```

- 参数可选，不传则使用列配置或系统默认
- 执行 `SELECT DISTINCT column FROM table LIMIT N`，按长度截断后写入 ES

---

## 5. 前端功能

### 5.1 列管理页面变更

**抽取配置区**（在列信息附近）：
- 抽取开关（toggle）
- 采样数量输入（可选，为空用系统默认）

**抽取操作**：
- "抽取值"按钮

**值列表弹窗**：
- 展示已有值及同义词
- 手工添加值（输入框 + 添加按钮）
- 删除已有值（每行删除按钮）
- 批量保存按钮

### 5.2 交互说明

- 抽取/添加/删除均为前端操作，最终用户点击"保存"才调用 PUT 接口
- 长度校验：超过系统限制时前端提示错误，不允许提交

---

## 6. 数据流

### 6.1 抽取值

1. 用户点击"抽取值"
2. 后端执行 `SELECT DISTINCT column FROM table LIMIT N`
3. 值按长度截断
4. 逐个计算 hash 生成 id，写入 ES（type=FIELD_VALUE）

### 6.2 手工添加

1. 用户在弹窗输入值
2. 前端校验长度
3. 调用 PUT 批量保存

### 6.3 编辑同义词

1. 用户修改某值的同义词
2. 调用 PUT 批量保存

### 6.4 删除

1. 用户点击删除按钮
2. 调用 PUT 批量保存带上 deletedIds

---

## 7. 后端职责划分

### 7.1 新增服务

**ColumnValueService**：
- `extractValues()`: 抽取值逻辑
- `saveValues()`: 批量保存（新增/更新/删除）
- `getValues()`: 获取某列所有值

### 7.2 复用现有

- `SemanticIndexService`: 复用 saveBatch/save/delete 方法写入 ES
- `JdbcMetaService`: 复用 executeQuery 执行抽取 SQL

---

## 8. 待明确事项

无
