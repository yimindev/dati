# DataSource 模块架构文档

## 1. 概述

DataSource 模块负责管理数据源连接配置，支持多种数据库类型（MySQL、PostgreSQL 等），提供数据源的 CRUD、元数据查询（Schema/Table/Column）和 SQL 执行能力。

## 2. 后端架构

### 2.1 目录结构

```
backend/src/main/java/com/dati/datasource/
├── domain/
│   ├── model/           # 领域实体
│   │   ├── DataSource.java
│   │   ├── TableInfo.java
│   │   └── ColumnInfo.java
│   └── service/         # 领域服务
│       ├── DataSourceService.java   # 数据源 CRUD
│       ├── TableService.java       # 表管理
│       ├── ColumnService.java       # 列管理
│       └── JdbcMetaService.java     # JDBC 元数据查询
├── repository/
│   ├── dao/             # JPA 数据访问
│   ├── po/              # 持久化对象
│   ├── mapper/          # PO ↔ Model 映射
│   └── mapper/          # MyBatis 映射（DSMapper）
└── server/
    ├── controller/      # REST 控制器
    │   ├── DataSourceController.java
    │   ├── TableController.java
    │   └── ColumnController.java
    ├── pojo/            # VO 对象
    │   ├── DatasourceVO.java
    │   ├── TableInfoVO.java
    │   └── ColumnInfoVO.java
    └── assembler/       # Model ↔ VO 转换
        ├── DSAssembler.java
        ├── TableAssembler.java
        └── ColumnAssembler.java
```

### 2.2 核心 API

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/v1/data-sources` | 创建数据源 |
| PUT | `/v1/data-sources/{id}` | 更新数据源 |
| DELETE | `/v1/data-sources/{id}` | 删除数据源 |
| GET | `/v1/data-sources` | 分页查询数据源列表 |
| POST | `/v1/data-sources/test-connection` | 测试连接 |
| GET | `/v1/data-sources/{id}/schemas` | 获取 Schema 列表 |
| GET | `/v1/data-sources/{id}/schemas/{schema}/tables` | 获取表列表 |
| GET | `/v1/data-sources/{id}/schemas/{schema}/tables/{table}/columns` | 获取列信息 |
| POST | `/v1/data-sources/{id}/execute-sql` | 执行 SQL |

### 2.3 核心领域模型

**DataSource**
- `id`, `name`, `description`: 基础信息
- `type`: 数据库类型 (DbType enum)
- `jdbcUrl`, `username`, `password`: 连接信息

**TableInfo**
- `id`, `name`, `description`: 基础信息
- `datasourceId`: 所属数据源 ID
- `schema`: 数据库 Schema
- `aliases`: 别名列表（JSON 存储，用户可设置多个搜索关键词）

**ColumnInfo**
- `id`, `name`, `description`: 基础信息
- `tableId`: 所属表 ID
- `columnType`: 列数据类型
- `aliases`: 别名列表（JSON 存储，用户可设置多个搜索关键词）

### 2.4 关键服务

**JdbcMetaService**: 封装 JDBC 元数据查询，通过 `DbClient` 抽象层支持多数据库类型，调用 `HikariPoolManager` 管理连接池。

**DataSourceService**: 核心业务逻辑
- `testConnection()`: 测试数据库连接
- `addDataSource()`: 保存新数据源
- `deleteDataSource()`: 删除时清理关联的表、列和语义索引

## 3. 前端架构

### 3.1 目录结构

```
frontend/src/
├── pages/datasources/
│   ├── index.vue                    # 数据源列表页
│   └── [id]/tables/
│       ├── index.vue                # 表管理页
│       └── [tableId]/columns.vue    # 列管理页
├── components/datasource/
│   ├── DatasourceTable.vue          # 数据源表格组件
│   ├── DatasourceDialog.vue         # 创建/编辑弹窗
│   ├── DatasourceForm.vue           # 表单组件
│   └── DatasourceAction.vue          # 操作按钮组件
└── api/
    └── datasource.ts                # API 接口定义
```

### 3.2 页面路由

| 路由 | 页面 | 功能 |
|------|------|------|
| `/datasources` | index.vue | 数据源列表、搜索、创建、编辑、删除、测试连接 |
| `/datasources/{id}/tables` | tables/index.vue | 表管理、添加表、同步列、配置元数据 |
| `/datasources/{id}/tables/{tableId}/columns` | columns.vue | 列管理 |

### 3.3 API 接口

```typescript
// 数据源管理
testConnection(body)       // POST /v1/data-sources/test-connection
addDataSource(body)        // POST /v1/data-sources
updateDataSource(id, body) // PUT /v1/data-sources/{id}
deleteDataSource(id)       // DELETE /v1/data-sources/{id}
listDataSources(page, size, keyword) // GET /v1/data-sources

// 元数据查询
getSchemas(id)             // GET /v1/data-sources/{id}/schemas
getTables(id, schema)     // GET /v1/data-sources/{id}/schemas/{schema}/tables
getColumns(id, schema, table) // GET /v1/data-sources/{id}/schemas/{schema}/tables/{table}/columns
executeSql(id, sql)        // POST /v1/data-sources/{id}/execute-sql
```

## 4. 数据流

```
┌─────────────────────────────────────────────────────────────┐
│                         Frontend                             │
│  index.vue → DatasourceDialog → DatasourceForm → API        │
└─────────────────────┬───────────────────────────────────────┘
                      │ HTTP /v1/data-sources/*
                      ▼
┌─────────────────────────────────────────────────────────────┐
│                    Backend Controller                        │
│              DataSourceController                            │
└─────────────────────┬───────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────┐
│                    Domain Service                            │
│  DataSourceService / JdbcMetaService                         │
└─────────────────────┬───────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────┐
│                    Repository Layer                          │
│        DataSourceDAO / JdbcMetaService → DbClient            │
└─────────────────────────────────────────────────────────────┘
```

## 5. 与语义模型（ES）的集成

### 5.1 概述

DataSource 模块通过 `SemanticIndexService` 与 Elasticsearch 集成，将表、字段等元数据同步到 ES 索引，支持语义搜索功能。

### 5.2 核心组件

| 组件 | 路径 | 说明 |
|------|------|------|
| `SemanticSearchDocument` | `semantic/repository/po/` | ES 文档结构 |
| `SemanticIndexService` | `semantic/domain/service/` | 语义索引服务 |
| `SemanticSearchDAO` | `semantic/repository/dao/` | ES Repository |
| `EntityReference` | `semantic/repository/po/` | 实体引用（关联表/字段） |

### 5.3 ES 文档结构

```java
@Document(indexName = "semantic_search")
public class SemanticSearchDocument {
    @Id
    private String id;                    // 格式: "table:{id}" 或 "field:{id}"
    
    @Field(type = FieldType.Text, analyzer = "ik_max_word")
    private List<String> keywords;       // [原始名称] + aliases（去重）
    
    @Field(type = FieldType.Text)
    private String description;            // 描述文本（来自数据库 comment）
    
    @Field(type = FieldType.Keyword)
    private SemanticEntityType type;      // SUBJECT / TABLE / FIELD / FIELD_VALUE / TERM
    
    @Field(type = FieldType.Nested)
    private EntityReference entity;       // 关联的实体引用
}
```

### 5.4 实体关联关系

```
EntityReference
├── subjectId     # 关联的主题（Subject）ID
├── tableId       # 关联的表（TableInfo）ID
├── tableName     # 表名
└── field         # 字段名（FIELD 类型时使用）
```

### 5.5 交互流程

**添加表时** (`TableService.batchAddTables`):
1. 保存 `TableInfoPO` 到 MySQL
2. 通过 JDBC 获取表的列信息
3. 保存 `ColumnInfoPO` 到 MySQL
4. 批量构建 `SemanticSearchDocument`（TABLE + FIELD 类型）
5. 调用 `semanticIndexService.saveBatch()` 写入 ES

**同步列时** (`ColumnService.syncColumns`):
1. 从 JDBC 获取最新列信息
2. 删除旧 `ColumnInfoPO`，保存新的
3. 先调用 `semanticIndexService.deleteByEntityTableId()` 删除旧 ES 文档
4. 批量构建 FIELD 类型文档并写入 ES

**删除数据源时** (`DataSourceService.deleteDataSource`):
1. 获取该数据源下所有表 ID
2. 删除关联的 `ColumnInfoPO`
3. 删除 `TableInfoPO`
4. 调用 `semanticIndexService.deleteByEntityTableIds()` 清理 ES 文档
5. 调用 `semanticIndexService.deleteByEntity_SubjectId()` 清理主题关联的 ES 文档

### 5.6 关键代码路径

```
TableService.batchAddTables()
  └─> semanticIndexService.saveBatch(docs)  // TABLE + FIELD 文档

ColumnService.syncColumns()
  └─> semanticIndexService.deleteByEntityTableId(tableId)
  └─> semanticIndexService.saveBatch(docs)  // FIELD 文档

ColumnService.updateColumn()
  └─> semanticIndexService.save(doc)  // 更新单个 FIELD 文档

TableService.updateTable()
  └─> semanticIndexService.save(doc)  // 更新 TABLE 文档

DataSourceService.deleteDataSource()
  └─> semanticIndexService.deleteByEntityTableIds(tableIds)
  └─> semanticIndexService.deleteByEntity_SubjectId(subjectId)
```

## 6. 关键技术点

- **连接池管理**: 使用 HikariCP，通过 `HikariPoolManager` 统一管理
- **多数据库支持**: `DbClientFactory` 抽象不同数据库的 JDBC 操作
- **DDD 架构**: 严格分层 `controller → service → repository/dao`
- **前后端分离**: 前端 Vue 3 + TypeScript，后端 REST API
- **命名转换**: 前端使用 snake_case (API JSON)，内部使用 camelCase
- **ES 集成**: 表/字段元数据自动同步到 ES，支持语义搜索
- **别名系统**: 表/字段支持多个别名（aliases），同步到 ES keywords 去重存储，支持语义搜索匹配