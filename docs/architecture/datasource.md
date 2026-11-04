# DataSource 模块架构文档

> **更新时间**: 2026-07-27

## 1. 概述

DataSource 模块负责管理数据源连接配置，支持多种数据库类型（MySQL、PostgreSQL 等），提供数据源的 CRUD、元数据查询（Catalog/Schema/Table/Column）、SQL 执行、列值抽取与管理等能力。

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
│       ├── ColumnValueService.java  # 列值抽取与管理
│       └── JdbcMetaService.java     # JDBC 元数据查询
├── repository/
│   ├── dao/             # JPA 数据访问
│   │   ├── DataSourceDAO.java
│   │   ├── TableInfoDAO.java
│   │   └── ColumnInfoDAO.java
│   ├── po/              # 持久化对象
│   │   ├── DataSourcePO.java
│   │   ├── TableInfoPO.java
│   │   └── ColumnInfoPO.java
│   └── mapper/          # PO ↔ Model 静态映射工具（含加解密）
│       ├── DSMapper.java
│       ├── TableMapper.java
│       └── ColumnMapper.java
└── server/
    ├── controller/      # REST 控制器
    │   ├── DataSourceController.java
    │   ├── TableController.java
    │   └── ColumnController.java
    ├── pojo/            # VO / 请求体对象
    │   ├── DatasourceVO.java
    │   ├── TableInfoVO.java
    │   ├── ColumnInfoVO.java
    │   ├── AddTableRequest.java
    │   ├── SqlExecuteRequest.java
    │   ├── ColumnValueListRequest.java
    │   └── ColumnValueVO.java
    └── assembler/       # Model ↔ VO 转换
        ├── DSAssembler.java
        ├── TableAssembler.java
        └── ColumnAssembler.java
```

### 2.2 核心 API

#### 2.2.1 数据源管理（DataSourceController，前缀 `/v1/data-sources`）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/v1/data-sources/test-connection` | 测试连接（不存储）；body 可带已存数据源 `id`，密码留空时复用其已保存密码 |
| POST | `/v1/data-sources` | 创建数据源 |
| PUT | `/v1/data-sources/{id}` | 更新数据源（null 字段不覆盖） |
| DELETE | `/v1/data-sources/{id}` | 删除数据源（级联清理表/列/ES索引） |
| GET | `/v1/data-sources` | 分页查询数据源列表，keyword 匹配名称 / ID；`%` / `_` 按字面匹配 |
| GET | `/v1/data-sources/{id}/schemas` | 获取数据库 Schema 列表（校验 VIEW 权限） |
| GET | `/v1/data-sources/{id}/schemas/{schema}/tables` | 获取表列表（校验 VIEW 权限） |

#### 2.2.2 表管理（TableController，前缀 `/v1/data-sources/{datasourceId}`）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/v1/data-sources/{datasourceId}/tables` | 分页查询已添加的表列表，keyword 匹配表名 / 表别名（字面匹配，校验 VIEW 权限） |
| GET | `/v1/data-sources/{datasourceId}/tables/added-names` | 获取已添加的表名列表（校验 VIEW 权限） |
| POST | `/v1/data-sources/{datasourceId}/tables/batch` | 批量添加表（从数据库同步表+列+ES索引，校验 EDIT 权限） |
| DELETE | `/v1/data-sources/{datasourceId}/tables/{tableId}` | 删除表（级联删列+ES索引，校验 EDIT 权限） |
| PUT | `/v1/data-sources/{datasourceId}/tables/{tableId}` | 更新表元数据（别名、描述）并同步 ES（校验 EDIT 权限） |

#### 2.2.3 列管理（ColumnController，前缀 `/v1/data-sources/{datasourceId}/tables/{tableId}/columns`）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `.../columns` | 分页查询列列表，keyword 匹配列名 / 列别名（字面匹配，校验 VIEW 权限） |
| PUT | `.../columns/{id}` | 更新列元数据（别名、描述、值匹配开关，校验 EDIT 权限） |
| POST | `.../columns/sync` | 从数据库同步最新列信息（校验 EDIT 权限） |
| POST | `.../columns/{columnId}/values/extract` | 从数据库抽取列去重值写入 ES（强校验所属数据源一致性 + EDIT 权限） |
| GET | `.../columns/{columnId}/values` | 分页查询列值列表，keyword 匹配值与同义词（ES keywords，校验 VIEW 权限） |
| PUT | `.../columns/{columnId}/values` | 保存列值（增/删/改同义词，校验 EDIT 权限） |

### 2.3 核心领域模型

**DataSource**
- `id`, `name`, `description`: 基础信息（继承 BaseResource）
- `type`: 数据库类型 (DbType enum：MYSQL / MARIADB / POSTGRESQL / CLICKHOUSE / DORIS / UNKNOWN)
- `jdbcUrl`, `username`, `password`: 连接信息（Model 中为明文，PO 中 password 加密存储）
- `defaultSchema`: 默认 Schema，由服务端在创建/连接信息变更时实际探测写入；请求 DTO 不再接收该字段（传了也会被忽略）

**TableInfo**
- `id`, `name`, `description`: 基础信息
- `datasourceId`: 所属数据源 ID
- `schema`: 数据库 Schema
- `aliases`: 别名列表（用户可设置多个搜索关键词，用于语义搜索匹配）

**ColumnInfo**
- `id`, `name`, `description`: 基础信息
- `tableId`: 所属表 ID
- `columnType`: 列数据类型（来自 JDBC TYPE_NAME）
- `aliases`: 别名列表（用户可设置多个搜索关键词）
- `extractValueEnabled`: 是否启用列值抽取（Boolean，默认 false）。开启后可将该列的 DISTINCT 值抽取到 ES，支持值匹配搜索。关闭时自动清理已抽取的 FIELD_VALUE 文档。

### 2.4 关键服务

**JdbcMetaService**: 封装 JDBC 元数据查询
- 通过 `DbClient` 抽象层支持多数据库类型
- 通过 `DbClientFactory.getDbClient(DbType)` 获取对应的 DbClient 实现
- 调用 `HikariPoolManager` 管理 HikariCP 连接池
- `executeSql()`: 使用 `JdbcTemplate` 执行 SQL 并返回 `List<Map<String, Object>>`
- `resolveCurrentSchema(connector, dbType)`: 探测连接的默认 schema。先由 `JdbcUrl.parse()` 校验 URL 前缀是否与所选类型匹配、host/port 是否合法，再由 `JdbcConnections.withConnection()` 依次做 DNS 解析、TCP 可达性检查、DriverManager 建连并执行 `DbClient.getCurrentSchema(Connection)`；整段探测有 10s 上限，虚线程执行，避免黑洞地址挂住 HTTP 请求。连接失败由 `JdbcFailures.translate()` 映射为可操作的错误码（见 2.5）

**DataSourceService**: 数据源核心业务逻辑与双通道访问
- `testConnection(DataSource, existingId)`: 与创建/更新共用同一探测流程，成功返回 true；失败抛出与保存时完全一致的错误。请求带 `id` 且未填密码时，用**已存连接**探测：此时请求的 type/jdbc_url/username 必须与已存值一致，否则返回 `DS020` 要求填密码——已存密码绝不会被发往请求方指定的地址；该路径校验 `DATA_SOURCE (VIEW)` 权限
- `addDataSource(DataSource)`: **持久化前**探测真实 `defaultSchema`（忽略客户端传入值），探测失败转换为明确业务异常不落库；保存成功密码加密存储
- `updateDataSource(id, DataSource)`: 校验 `DATA_SOURCE (EDIT)`；非 null 覆盖；仅连接变更时重新探测并关闭旧连接池。**url/username/type 任一变化时必须显式提供 password**（否则 `DS021`），避免把已存密码发往请求方指定的新地址
- `deleteDataSource(id)`: 校验 `DATA_SOURCE (EDIT)`；关闭连接池 → 清理关联 Column → Table → ES 语义索引
- `listDataSources(keyword, pageable)`: 分页查询（SQL 层按 owner / ACL 静默过滤）
- `getDataSource(id)`: **用户通道** — 查询并强制执行当前用户针对数据源的 `VIEW` 鉴权，密码脱敏返回
- `getDataSourceInternal(id)`: **内部通道** — 供 MCP 工具执行引擎等内部组件使用，解耦用户级数据源直属权限，由 MCP 服务层 DataScope 统一管控访问范围
- `getSchemas(id, catalog)` / `getTables(id, catalog, schema)`: 校验 `DATA_SOURCE (VIEW)` 权限后查询元数据
- `getDataSourceNameMap(ids)` / `getDataSourceBriefs(ids)`: 批量轻量查询

**TableService**: 表管理 + ES 语义索引同步（级联校验所属数据源权限）
- `getTables(pageReq, datasourceId, keyword)`: 分页查询表列表（校验 `requireDataSource(dsId, VIEW)`）
- `getAddedTableNames(datasourceId)`: 获取已添加表名（校验 `requireDataSource(dsId, VIEW)`）
- `batchAddTables(datasourceId, tables)`: **事务方法** — 校验 `requireDataSource(dsId, EDIT)` → 保存 Table PO → JDBC 获取列信息 → 保存 Column PO → 批量构建 TABLE + FIELD 写入 ES
- `deleteTable(tableId)` / `deleteTables(tableIds)`: 校验所属数据源 `EDIT` 权限 → 级联删除列 + 清理 ES
- `updateTable(tableId, tableInfo)`: 校验所属数据源 `EDIT` 权限 → 更新别名/描述 → 同步 ES

**ColumnService**: 列管理（级联校验所属数据源权限）
- `getColumns(pageReq, tableId, keyword)`: 分页查询列列表（反查 table 所属数据源校验 `VIEW` 权限）
- `updateColumn(id, columnInfo)`: 更新列元数据 → 校验所属数据源 `EDIT` 权限 → 同步 ES
- `syncColumns(datasourceId, tableId, overwriteExisting)`: 校验 `requireDataSource(dsId, EDIT)` → 从 JDBC 获取最新列 → 增量同步 PO 与 ES 文档（保留未消失列的用户编辑同义词与配置）

**ColumnValueService**: 列值抽取与管理（NEW）
- `extractValues(datasourceId, columnId, overwrite)`: 强校验 `columnId` 所属表与传入 `datasourceId` 一致性 → 校验所属数据源 `EDIT` 权限 → 执行 `SELECT DISTINCT {column} FROM {table} LIMIT N` → 写入 ES `FIELD_VALUE` 文档
- `saveValues(columnId, values, deletedIds)`: 校验所属数据源 `EDIT` 权限 → 手动增/删/改列值同义词并更新 ES
- `getValues(columnId, pageReq, keyword)`: 校验所属数据源 `VIEW` 权限 → 分页查询列值同义词

### 2.5 连接探测与错误分类

`POST /v1/data-sources/test-connection`、创建和更新（连接信息变更时）共用同一条探测链路：

```
JdbcUrl.parse()              前缀与所选类型是否匹配、host/port 是否合法        DS009 / DS010 / DS005
   ↓
JdbcConnections               DNS 解析 + TCP 可达性检查（5s）+ 建连 + 探测 schema（整段 10s 上限）
   ↓
JdbcFailures.translate()     按 SQLState / 厂商错误码 / cause 链归类
```

| 错误码 | 触发条件 | 用户看到的信息 |
|--------|----------|----------------|
| DS005 | 类型为空或 UNKNOWN | 不支持的数据库类型 |
| DS009 | URL 缺 host / 端口非法 | 无效的 JDBC URL + 期望格式 |
| DS010 | URL 前缀与所选类型不一致 | 类型不匹配 + 期望前缀 |
| DS011 | DNS 无法解析 host | 主机无法解析 |
| DS012 | 端口拒绝连接 | 连接被拒绝，检查服务是否启动/端口 |
| DS013 | TCP 或整段探测超时 | 连接超时，检查主机/端口/网络 |
| DS014 | 认证失败（28xxx / 1045） | 用户认证失败，检查用户名密码 |
| DS015 | 数据库不存在（3D000 / 1049） | 数据库在 host:port 上不存在 |
| DS016 | 库级权限不足（42501 / 1044） | 用户无权访问数据库 |
| DS017 | TLS 握手失败 | TLS 失败，检查服务端 SSL 配置 |
| DS018 | 无驱动接受该 URL | 选择与 URL 前缀匹配的类型 |
| DS019 | URL 里没有数据库名 | 显式填写数据库名（驱动默认库名通常是登录用户，容易踩坑） |
| DS020 | 带 id 测试连接但连接信息与已存值不一致 | 填入密码后再测试新连接（已存密码不会发往新地址） |
| DS021 | 更新时改了 url/username/type 但未提供密码 | 重新填写密码后再保存（已存密码只会用于已存地址） |
| DS022 | TCP 已连通但驱动层建会话失败（如代理/SSL/驱动问题） | 检查代理、SSL 与驱动配置 |
| DS001 | 其它驱动异常 | 清洗后的驱动原始信息（去掉连接池包装前缀） |
| DS006 | 连接成功但探测不到 schema | 检查 currentSchema/search_path 或库名 |

探测使用独立的短连接（`DriverManager`）而不是 Hikari 池：失败消息不再被 `Failed to initialize connection pool` 包装，也不会因反复测试连接积累连接池。`JdbcConnector.toString()` 与所有错误消息中的 URL 都会脱敏密码。

## 3. 前端架构

### 3.1 目录结构

```
frontend/src/
├── pages/datasources/
│   ├── index.vue                    # 数据源列表页
│   └── [id]/tables/
│       ├── index.vue                # 表管理页（含批量添加、别名配置、列同步）
│       └── [tableId]/columns.vue    # 列管理页（含别名、值匹配开关、列值管理）
├── components/datasource/
│   ├── DatasourceTable.vue          # 数据源表格组件
│   ├── DatasourceDialog.vue         # 创建/编辑弹窗（含测试连接）
│   └── DatasourceForm.vue           # 表单组件（校验+数据库类型选择）
└── api/
    ├── datasource.ts                # 数据源 API + 元数据查询 API
    ├── tableinfo.ts                 # 表管理 API
    └── column.ts                    # 列管理 + 列值管理 API
```

### 3.2 页面路由

| 路由 | 页面 | 功能 |
|------|------|------|
| `/datasources` | index.vue | 数据源列表、搜索、创建、编辑、删除、测试连接 |
| `/datasources/{id}/tables` | tables/index.vue | 表管理：批量添加（Transfer 穿梭框选择）、配置元数据（别名+描述）、同步列、删除表 |
| `/datasources/{id}/tables/{tableId}/columns` | columns.vue | 列管理：别名配置、描述编辑、值匹配开关（仅字符串类型可用）、列值抽取与管理 |

### 3.3 API 接口

```typescript
// === 数据源管理 (datasource.ts) ===
testConnection(body)              // POST /v1/data-sources/test-connection
addDataSource(body)               // POST /v1/data-sources
updateDataSource(id, body)        // PUT /v1/data-sources/{id}
deleteDataSource(id)              // DELETE /v1/data-sources/{id}
listDataSources(page, size, keyword) // GET /v1/data-sources

// 元数据查询
getSchemas(id)                    // GET /v1/data-sources/{id}/schemas
getTables(id, schema)            // GET /v1/data-sources/{id}/schemas/{schema}/tables

// === 表管理 (tableinfo.ts) ===
listTableInfos(datasourceId, page, size, keyword)  // GET .../tables
getAddedTableNames(datasourceId)                    // GET .../tables/added-names
batchAddTables(datasourceId, tables)               // POST .../tables/batch
deleteTable(datasourceId, tableId)                 // DELETE .../tables/{tableId}
updateTable(datasourceId, tableId, data)           // PUT .../tables/{tableId}
syncColumns(datasourceId, tableId, overwrite)      // POST .../columns/sync

// === 列管理 (column.ts) ===
listTableColumns(datasourceId, tableId, page, size, keyword)  // GET .../columns
saveColumnMetadata(datasourceId, tableId, column)             // PUT .../columns/{id}

// 列值管理
extractColumnValues(datasourceId, tableId, columnId, overwrite)  // POST .../values/extract
getColumnValues(datasourceId, tableId, columnId, page, size)     // GET .../values
saveColumnValues(datasourceId, tableId, columnId, values, deletedIds) // PUT .../values
```

## 4. 数据流

```
┌─────────────────────────────────────────────────────────────┐
│                         Frontend                             │
│  index.vue → DatasourceDialog → DatasourceForm → API        │
│  tables/index.vue → el-transfer 穿梭框 → batchAddTables     │
│  columns.vue → 别名/值匹配开关/列值管理弹窗                   │
└─────────────────────┬───────────────────────────────────────┘
                      │ HTTP /v1/data-sources/*
                      ▼
┌─────────────────────────────────────────────────────────────┐
│                    Backend Controllers                       │
│  DataSourceController / TableController / ColumnController   │
└─────────────────────┬───────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────┐
│                    Domain Services                           │
│  DataSourceService / TableService / ColumnService            │
│  ColumnValueService / JdbcMetaService                        │
└─────────────────────┬───────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────┐
│               Repository / Infrastructure                    │
│  JPA DAO → MySQL        DbClient → JDBC → 外部数据库          │
│  SemanticIndexService → Elasticsearch                        │
│  HikariPoolManager → HikariCP 连接池                          │
└─────────────────────────────────────────────────────────────┘
```

## 5. 与语义模型（ES）的集成

### 5.1 概述

DataSource 模块通过 `SemanticIndexService` 与 Elasticsearch 集成，将表、字段等元数据同步到 ES 索引，支持语义搜索功能。此外，`ColumnValueService` 可将列的 DISTINCT 值抽取到 ES，实现字段值级别的搜索匹配。

### 5.2 核心组件

| 组件 | 路径 | 说明 |
|------|------|------|
| `SemanticSearchDocument` | `semantic/repository/po/` | ES 文档结构 |
| `SemanticIndexService` | `semantic/domain/service/` | 语义索引服务 |
| `SemanticSearchDAO` | `semantic/repository/dao/` | ES Repository |
| `EntityReference` | `semantic/repository/po/` | 实体引用（关联表/字段） |
| `ColumnValueConfig` | `config/` | 列值抽取配置（采样数量限制、值长度限制） |

### 5.3 ES 文档结构

```java
@Document(indexName = "semantic_search", createIndex = false)
public class SemanticSearchDocument {
    @Id
    private String id;                    // 格式: "table:{id}"、"field:{id}" 或 UUID（FIELD_VALUE）

    // 分词器别名，实际类型由配置注入（默认 standard，可配 ik_max_word/ik_smart）
    @Field(type = FieldType.Text,
           analyzer = SemanticIndexSettings.INDEX_ANALYZER,
           searchAnalyzer = SemanticIndexSettings.SEARCH_ANALYZER)
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
└── field         # 字段名（FIELD / FIELD_VALUE 类型时使用）
```

### 5.5 交互流程

**添加表时** (`TableService.batchAddTables`):
1. 保存 `TableInfoPO` 到 MySQL
2. 通过 JDBC 获取表的列信息和表注释
3. 保存 `ColumnInfoPO` 到 MySQL
4. 批量构建 `SemanticSearchDocument`（TABLE + FIELD 类型）
5. 调用 `semanticIndexService.saveBatch()` 写入 ES

**同步列时** (`ColumnService.syncColumns`):
1. 从 JDBC 获取最新列信息
2. 删除旧 `ColumnInfoPO`，保存新的（保留已有别名和描述）
3. 先调用 `semanticIndexService.deleteByEntityTableId()` 删除旧 ES FIELD 文档
4. 批量构建 FIELD 类型文档并写入 ES

**更新列时** (`ColumnService.updateColumn`):
1. 更新列 PO 的别名、描述、`extractValueEnabled`
2. 若 `extractValueEnabled` 从 true 变为 false，调用 `semanticIndexService.deleteByTableFieldAndType()` 清理 FIELD_VALUE 文档
3. 更新 ES 中的 FIELD 文档

**抽取列值时** (`ColumnValueService.extractValues`):
1. 执行 `SELECT DISTINCT {column} FROM {table} LIMIT N`（N 由 ColumnValueConfig 配置）
2. 每个值截断到配置的最大长度
3. 去重后写入 ES（`SemanticEntityType.FIELD_VALUE`）

**删除数据源时** (`DataSourceService.deleteDataSource`):
1. 获取该数据源下所有表 ID
2. 删除关联的 `ColumnInfoPO`
3. 删除 `TableInfoPO`
4. 调用 `semanticIndexService.deleteByEntityTableIds()` 清理 ES 文档
5. 调用 `semanticIndexService.deleteByEntity_SubjectId()` 清理主题关联的 ES 文档
6. 关闭 HikariCP 连接池

### 5.6 关键代码路径

```
TableService.batchAddTables()
  └─> semanticIndexService.saveBatch(docs)  // TABLE + FIELD 文档

ColumnService.syncColumns()
  └─> semanticIndexService.deleteByEntityTableId(tableId)
  └─> semanticIndexService.saveBatch(docs)  // FIELD 文档

ColumnService.updateColumn()
  └─> semanticIndexService.save(doc)  // 更新单个 FIELD 文档
  └─> semanticIndexService.deleteByTableFieldAndType()  // extractValueEnabled 从 true→false 时

ColumnValueService.extractValues()
  └─> jdbcMetaService.executeSql() → SELECT DISTINCT
  └─> semanticIndexService.saveBatch(docs)  // FIELD_VALUE 文档

ColumnValueService.saveValues()
  └─> semanticIndexService.deleteById() / saveBatch()  // 手动管理列值

TableService.updateTable()
  └─> semanticIndexService.save(doc)  // 更新 TABLE 文档

DataSourceService.deleteDataSource()
  └─> semanticIndexService.deleteByEntityTableIds(tableIds)
  └─> semanticIndexService.deleteByEntity_SubjectId(subjectId)
```

## 6. 关键技术点

- **连接池管理**: 使用 HikariCP，通过 `HikariPoolManager`（ConcurrentHashMap）统一管理，ShutdownHook 自动清理；预期的连接池初始化失败（如认证失败、网络不通）会被 `HikariPoolManager.getDataSource()` 捕获并转换为 `SQLException`，由上层统一处理为业务异常，而不是让未处理的 `PoolInitializationException` 直接抛出
- **多数据库支持**: `DbClientFactory` 简单工厂模式，通过 `DbClient` 接口 + `AbstractDbClient` 模板方法抽象不同数据库的 JDBC 操作。已实现 `MysqlDbClient` / `MariaDbClient` / `DorisDbClient`（Schema = Catalog，`SELECT DATABASE()`）、`PostgresqlDbClient`（`SELECT current_schema()`）、`ClickhouseDbClient`（`SELECT currentDatabase()`）。新增类型步骤见[第 7 章](#7-新增数据源类型)
- **密码安全**: PO 中存储 `encryptedPassword`（`EncryptionUtils.encrypt()`），Mapper 层做加解密转换，VO 层不返回密码
- **DDD 架构**: 严格分层 `controller → service → repository/dao`
- **前后端分离**: 前端 Vue 3 + TypeScript + Element Plus，后端 REST API
- **命名转换**: 前端 API JSON 使用 snake_case（如 `jdbc_url`），前端脚本内部使用 camelCase
- **ES 集成**: 表/字段元数据自动同步到 ES，支持语义搜索；列值可抽取到 ES 实现值级别匹配
- **别名系统**: 表/字段支持多个别名（aliases），同步到 ES keywords 去重存储，支持语义搜索匹配；管理端列表搜索同样匹配 aliases（DB 层 `CAST(aliases AS String)` + `LOCATE`）
- **列表搜索字面匹配**: 所有管理端 keyword 搜索使用 `LOCATE` 而非 SQL `LIKE`，`%` / `_` 不再作为通配符，避免搜索特殊字符命中全量数据
- **列值抽取**: 通过 `ColumnValueService` 执行 `SELECT DISTINCT` 抽取列的去重值到 ES，支持覆盖/追加两种模式，受 `ColumnValueConfig` 限制采样数量和值长度
- **值匹配开关**: 仅字符串类型列（varchar/char/text）支持，开启后可管理列值；关闭时自动清理 ES 中的 FIELD_VALUE 数据

## 7. 新增数据源类型

以 ClickHouse / Doris / MariaDB 接入为模板，新增数据库类型需同步修改后端与前端（编码规范见 `.agents/rules/backend.md` → Adding a New Datasource Type）。

### 7.1 后端步骤（TDD）

1. **添加 JDBC 驱动依赖**：`backend/core/pom.xml` 添加 `runtime` 依赖；若版本不在 Spring Boot BOM 中，在根 `pom.xml` 增加版本属性 + `dependencyManagement` 条目（参考 `clickhouse-jdbc.version`）。
2. **扩展 `DbType` 枚举**：新增类型值。
3. **先写测试**（`backend/src/test/java/com/dati/db/client/`）：
   - `XxxDbClientTest`：mock `DatabaseMetaData` / `Statement` / `ResultSet`，并用 `mockStatic(HikariPoolManager.class)` 拦截连接获取（参考 `ClickhouseDbClientTest`）。
   - 扩展 `DbClientFactoryTest` 断言新类型映射。
   - 可选：真实服务集成测试，用 `Assumptions.assumeTrue(isReachable(host, port))` 在服务未启动时自动跳过（参考 `ClickhouseAndDorisIntegrationTest`）。
4. **实现 `XxxDbClient extends AbstractDbClient`**，只覆写与 `DatabaseMetaData` 默认行为不同的方法：
   - `getDbType()`：必覆写。
   - `getSchemas(connector, catalog)`：MySQL 系（schema 即 catalog）委托 `super.getCatalogs()`。
   - `getCurrentSchema(connector)`：数据库方言 SQL，如 MySQL/MariaDB/Doris `SELECT DATABASE()`、PostgreSQL `SELECT current_schema()`、ClickHouse `SELECT currentDatabase()`。
   - `getTables` / `getColumns`：仅当驱动元数据语义不同（如 catalog/schema 参数位置）时覆写。
5. **注册工厂**：在 `DbClientFactory` 静态块注册 `DbType → DbClient`。

### 7.2 前端步骤

1. `DatasourceForm.vue`：`<el-option>` 增加类型选项（value 与 `DbType` 枚举名一致）。
2. `SearchHitResult.vue` / `TableListResult.vue`：`dbTypeLabel` 增加标签，`dbTypeColor` 增加颜色映射（仅使用 `var(--ep-color-*)`）。

### 7.3 驱动选择规则（重要）

`HikariPoolManager` 与 `JdbcUtils` 依赖 `DriverManager` 自动探测，**不显式设置 `driverClassName`**，因此 classpath 上每个 URL 前缀必须恰好只有一个驱动接受：

| URL 前缀 | 驱动 | 说明 |
|---|---|---|
| `jdbc:mysql://` | mysql-connector-j（`com.mysql.cj.jdbc.Driver`） | MySQL 与 Doris 共用；MariaDB 驱动不接受此前缀（除非 URL 含 `permitMysqlScheme` 标记） |
| `jdbc:mariadb://` | mariadb-java-client（`org.mariadb.jdbc.Driver`） | 该前缀下也可连 MySQL 服务器 |
| `jdbc:postgresql://` | postgresql（`org.postgresql.Driver`） | |
| `jdbc:clickhouse://` | clickhouse-jdbc（`com.clickhouse.jdbc.ClickHouseDriver`） | |

新增驱动时须确认它不会同时注册到已有 URL 前缀，否则连接选择将变得不确定。

### 7.4 验证清单

- [ ] `mvn -Dtest='XxxDbClientTest,DbClientFactoryTest' test` 通过
- [ ] 有真实服务时集成测试通过（无服务时自动跳过）
- [ ] 前端 `pnpm build` 通过
- [ ] `DbType` 枚举、`DatasourceForm.vue`、两个结果组件标签映射同步更新
- [ ] 确认存量数据中不存在已删除的 `DbType` 值（否则 `DbType.valueOf` 抛异常）

## 8. 参考

- 语义模型集成（ES）：[semantic.md](semantic.md)
- 模板引擎：[template-engine.md](template-engine.md)
