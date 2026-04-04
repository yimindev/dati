# 主题管理模块设计方案

## 1. 需求概述

### 1.1 背景

当前语义层已完成表、列、术语的语义建模，但缺少主题（Subject）这一层级。主题是 NL2SQL 元数据服务的基本单位，用于：
- 数据分类/分组，方便用户查找
- 权限控制（预留）
- 提供服务的最小单位

### 1.2 新增功能点

| 功能 | 说明 |
|-----|-----|
| 主题管理 | 创建/编辑/删除主题，关联数据源和表 |
| 主题-表关联 | 主题关联的表只能来自同一数据源 |
| 主题私有术语 | 术语归属主题，可选关联表/字段 |

## 2. 设计决策

| 决策项 | 结论 |
|-------|-----|
| 主题关联的表 | 只能来自同一数据源 |
| 数据源-主题 | 一对多关系 |
| 术语归属 | 主题私有 |
| 术语关联 | 可选关联表/字段，或仅主题级别 |
| 主题状态 | 暂不需要（草稿/发布） |
| 表添加方式 | 创建主题时选定数据源，后续只能添加该数据源下的表 |

## 3. 数据模型设计

### 3.1 Subject（主题）

**JPA 实体：** `SubjectPO` 继承 `BaseResourcePO`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | String | 主键（UUID，由 BaseResourcePO 提供） |
| name | String | 主题名称（BaseResourcePO） |
| description | String | 主题描述（BaseResourcePO） |
| datasourceId | String | 关联的数据源ID（创建时选定，不可更改） |
| deleted | Boolean | 软删除标记（BaseResourcePO） |
| createdAt/updatedAt | Instant | 时间戳（BaseResourcePO） |

### 3.2 SubjectTable（主题-表关联）

**JPA 实体：** `SubjectTablePO` 继承 `BaseResourcePO`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | String | 主键（BaseResourcePO） |
| subjectId | String | 关联主题ID |
| tableId | String | 关联的表ID |
| deleted | Boolean | 软删除标记（BaseResourcePO） |

**约束**：tableId 对应的表必须属于 Subject.datasourceId

### 3.3 Term（术语）- 主题私有

**JPA 实体：** `TermPO` 继承 `BaseResourcePO`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | String | 主键（BaseResourcePO） |
| subjectId | String | 所属主题ID |
| name | String | 术语名称（BaseResourcePO） |
| description | String | 术语说明/定义（BaseResourcePO） |
| deleted | Boolean | 软删除标记（BaseResourcePO） |

### 3.4 TermRelation（术语-实体关联）- 可选

**JPA 实体：** `TermRelationPO` 继承 `BaseResourcePO`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | String | 主键（BaseResourcePO） |
| termId | String | 关联术语ID |
| entityType | Enum | 关联类型：TABLE（关联整张表）或 FIELD（关联具体字段） |
| tableId | String | 关联的表ID |
| fieldName | String | 字段名（仅 FIELD 类型时填写，可为空） |
| deleted | Boolean | 软删除标记（BaseResourcePO） |

**关联类型说明**：

| entityType | tableId | fieldName | 含义 |
|------------|---------|-----------|------|
| TABLE | ✓ | 空 | 术语关联到整张表 |
| FIELD | ✓ | ✓ | 术语关联到某个字段 |

**约束**：tableId 必须在该术语所属主题关联的表范围内

### 3.5 BaseResourcePO 公共字段

所有 PO 继承 `BaseResourcePO`，包含以下公共字段：

| 字段 | 类型 | 说明 |
|------|------|------|
| id | String | 主键，UUID |
| name | String | 名称 |
| description | String | 描述 |
| deleted | Boolean | 软删除标记 |
| createdAt | Instant | 创建时间（@CreationTimestamp） |
| updatedAt | Instant | 更新时间（@UpdateTimestamp） |
| createdBy | String | 创建人 |
| updatedBy | String | 更新人 |

## 4. ES 索引设计

### 4.1 文档类型扩展

新增 `SUBJECT`、`SUBJECT_TABLE` 类型，复用现有 `semantic_search` 索引：

| type | id格式 | keywords来源 | description来源 |
|------|--------|--------------|-----------------|
| SUBJECT | subject:{id} | 主题名称 | 主题描述 |
| SUBJECT_TABLE | subject_table:{subjectId}:{tableId} | 表名、表别名 | 表描述 |
| TERM | term:{id} | 术语名 | 术语描述 |

### 4.2 EntityReference 扩展

现有 `EntityReference` 已预留 `subjectId` 字段，无需修改。

## 5. API 设计

### 5.1 SubjectController

**基础路径：** `/v1/subjects`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /v1/subjects | 创建主题（需指定datasourceId） |
| PUT | /v1/subjects/{id} | 更新主题信息 |
| DELETE | /v1/subjects/{id} | 删除主题 |
| GET | /v1/subjects/{id} | 获取主题详情（包含关联的表） |
| GET | /v1/subjects/{id}/tables | 获取主题下的表列表 |
| POST | /v1/subjects/{id}/tables | 添加表到主题 |
| DELETE | /v1/subjects/{id}/tables/{tableId} | 从主题移除表 |
| GET | /v1/subjects?datasourceId= | 按数据源获取主题列表 |

### 5.2 TermController

**基础路径：** `/v1`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /v1/subjects/{subjectId}/terms | 创建术语 |
| PUT | /v1/terms/{id} | 更新术语 |
| DELETE | /v1/terms/{id} | 删除术语 |
| GET | /v1/terms/{id} | 获取术语详情 |
| GET | /v1/subjects/{subjectId}/terms | 获取主题下的术语列表 |
| POST | /v1/terms/{id}/relations | 关联术语到表/字段 |
| DELETE | /v1/terms/{id}/relations/{tableId}/{fieldName} | 取消关联（fieldName 为空表示取消表级别关联） |

## 6. Service 层设计

### 6.1 SubjectService

```java
SubjectService {
  createSubject(name, description, datasourceId): Subject
    - 校验 datasourceId 存在且有效
    - 保存 SubjectPO（继承 BaseResourcePO，自动设置时间戳）
    - 构建 SemanticSearchDocument(type=SUBJECT) 并索引

  updateSubject(id, name, description): Subject
    - 校验 id 存在
    - 更新 SubjectPO
    - 更新 ES 索引

  deleteSubject(id): void
    - 校验 id 存在
    - 软删除 SubjectPO（设置 deleted=true）
    - 从 ES 删除该主题相关的所有文档

  addTableToSubject(subjectId, tableId): void
    - 校验 subjectId 存在
    - 校验 tableId 存在
    - 校验 table 属于 subject.datasourceId
    - 保存 SubjectTablePO
    - 构建 SemanticSearchDocument(type=SUBJECT_TABLE) 并索引

  removeTableFromSubject(subjectId, tableId): void
    - 校验关联存在
    - 软删除 SubjectTablePO
    - 从 ES 删除对应的 SUBJECT_TABLE 文档

  getSubjectWithTables(id): SubjectDetailVO
    - 返回主题信息 + 关联的表列表（含表详情）

  getSubjectsByDatasource(datasourceId): List<Subject>
    - 返回该数据源下的所有主题
}
```

### 6.2 TermService

```java
TermService {
  createTerm(subjectId, name, description): Term
    - 校验 subjectId 存在
    - 保存 TermPO
    - 构建 SemanticSearchDocument(type=TERM) 并索引

  updateTerm(id, name, description): Term
    - 校验 id 存在
    - 更新 TermPO
    - 更新 ES 索引

  deleteTerm(id): void
    - 校验 id 存在
    - 软删除 TermPO
    - 软删除关联的 TermRelationPO
    - 从 ES 删除对应的 TERM 文档

  linkEntity(termId, entityType, tableId, fieldName): void
    - 校验 termId 存在，获取 subjectId
    - 校验 tableId 属于该 subject 关联的表
    - 如果 entityType=FIELD，还需校验 fieldName 有效
    - 保存 TermRelationPO

  unlinkEntity(termId, tableId, fieldName): void
    - 校验关联存在
    - 软删除关联（fieldName 为空时删除表级别关联）

  getTermsBySubject(subjectId): List<Term>
    - 返回该主题下的所有术语

  getTermById(id): Term
    - 返回指定 id 的术语

  getTermRelations(termId): List<TermRelation>
    - 返回术语的所有关联
}
```

## 7. 前端菜单结构

```
语义管理
├── 主题管理
│   └── 主题列表页
│       └── 主题详情页
│           ├── 基本信息（名称、描述、数据源）
│           ├── 表管理 Tab（添加/移除表）
│           └── 术语管理 Tab（添加术语、关联字段）
└── 术语管理（可选：独立入口，按主题筛选）
```

## 8. NL2SQL 集成

NL2SQL 服务调用语义搜索时：

1. **指定 subjectId** 获取该主题的完整上下文
2. **搜索语义索引**时自动带上 `entity.subjectId` 过滤
3. 主题提供了术语和表结构的统一视图

搜索流程：
```
NL2SQL 请求: keyword="订单金额", subjectId="xxx"
    ↓
SemanticIndexService.search(keyword, types=[TABLE, FIELD, TERM], subjectId="xxx")
    ↓
ES 查询: multi_match + filter by entity.subjectId
    ↓
返回匹配结果（仅该主题内的语义实体）
```

## 9. 数据库表设计

所有表继承 BaseResourcePO 的公共字段（id, name, description, deleted, created_at, updated_at, created_by, updated_by）。

### 9.1 subject（主题表）

```sql
CREATE TABLE subject (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    description TEXT,
    datasource_id VARCHAR(64) NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted BIT DEFAULT 0
);

CREATE INDEX idx_subject_datasource ON subject(datasource_id);
```

### 9.2 subject_table（主题-表关联表）

```sql
CREATE TABLE subject_table (
    id VARCHAR(64) PRIMARY KEY,
    subject_id VARCHAR(64) NOT NULL,
    table_id VARCHAR(64) NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted BIT DEFAULT 0,
    CONSTRAINT uk_subject_table UNIQUE (subject_id, table_id)
);

CREATE INDEX idx_subject_table_subject ON subject_table(subject_id);
```

### 9.3 term（术语表）

```sql
CREATE TABLE term (
    id VARCHAR(64) PRIMARY KEY,
    subject_id VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL,
    description TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted BIT DEFAULT 0
);

CREATE INDEX idx_term_subject ON term(subject_id);
```

### 9.4 term_relation（术语-实体关联表）

```sql
CREATE TABLE term_relation (
    id VARCHAR(64) PRIMARY KEY,
    term_id VARCHAR(64) NOT NULL,
    entity_type VARCHAR(16) NOT NULL COMMENT 'TABLE or FIELD',
    table_id VARCHAR(64) NOT NULL,
    field_name VARCHAR(128),
    deleted BIT DEFAULT 0,
    CONSTRAINT uk_term_relation UNIQUE (term_id, table_id, field_name)
);

CREATE INDEX idx_term_relation_term ON term_relation(term_id);
```

## 10. 实现状态

### 已完成

- [x] 后端 PO/DAO/Mapper 层（继承 BaseResourcePO）
- [x] SubjectService / TermService 业务逻辑
- [x] SubjectController / TermController REST API
- [x] ES 索引同步（SUBJECT、SUBJECT_TABLE、TERM）
- [x] 数据库 Migration 脚本
- [x] 单元测试（101 tests passing）

### 待完成

- [ ] 前端页面开发
- [ ] NL2SQL 集成（subjectId 过滤）

## 11. 文件结构

```
backend/src/main/java/com/dati/semantic/
├── domain/
│   ├── SemanticEntityType.java        # 实体类型枚举（已存在）
│   └── model/
│       ├── Subject.java               # 主题领域模型
│       ├── SubjectTable.java          # 主题-表关联领域模型
│       ├── SubjectDetailVO.java       # 主题详情VO
│       ├── Term.java                  # 术语领域模型
│       └── TermRelation.java          # 术语-实体关联领域模型
├── repository/
│   ├── dao/
│   │   ├── SubjectDAO.java           # 主题数据访问
│   │   ├── SubjectTableDAO.java     # 主题-表关联数据访问
│   │   ├── TermDAO.java             # 术语数据访问
│   │   └── TermRelationDAO.java      # 术语-实体关联数据访问
│   ├── mapper/
│   │   ├── SubjectMapper.java       # Subject 映射
│   │   ├── TermMapper.java          # Term 映射
│   │   └── TermRelationMapper.java   # TermRelation 映射
│   └── po/
│       ├── SubjectPO.java           # 主题持久化对象（继承BaseResourcePO）
│       ├── SubjectTablePO.java      # 主题-表关联持久化对象
│       ├── TermPO.java              # 术语持久化对象
│       └── TermRelationPO.java      # 术语-实体关联持久化对象
└── server/
    ├── assembler/
    │   ├── SubjectAssembler.java    # Subject 组装器
    │   └── TermAssembler.java       # Term 组装器
    ├── controller/
    │   ├── SubjectController.java   # 主题 REST API
    │   └── TermController.java      # 术语 REST API
    └── pojo/
        ├── request/
        │   ├── CreateSubjectRequest.java
        │   ├── UpdateSubjectRequest.java
        │   ├── AddTableToSubjectRequest.java
        │   ├── CreateTermRequest.java
        │   ├── UpdateTermRequest.java
        │   └── LinkTermRelationRequest.java
        └── vo/
            ├── SubjectVO.java
            ├── SubjectTableVO.java
            └── TermVO.java
```