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

| 字段 | 类型 | 说明 |
|------|------|------|
| id | String | 主键，格式 subject:{uuid} |
| name | String | 主题名称 |
| description | String | 主题描述 |
| datasourceId | String | 关联的数据源ID（创建时选定，不可更改） |
| createdAt | DateTime | 创建时间 |
| updatedAt | DateTime | 更新时间 |

### 3.2 SubjectTable（主题-表关联）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | String | 主键 |
| subjectId | String | 关联主题ID |
| tableId | String | 关联的表ID |
| createdAt | DateTime | 创建时间 |

**约束**：tableId 对应的表必须属于 Subject.datasourceId

### 3.3 Term（术语）- 主题私有

| 字段 | 类型 | 说明 |
|------|------|------|
| id | String | 主键，格式 term:{uuid} |
| subjectId | String | 所属主题ID |
| name | String | 术语名称 |
| description | String | 术语说明/定义 |
| createdAt | DateTime | 创建时间 |
| updatedAt | DateTime | 更新时间 |

### 3.4 TermRelation（术语-实体关联）- 可选

| 字段 | 类型 | 说明 |
|------|------|------|
| id | String | 主键 |
| termId | String | 关联术语ID |
| entityType | Enum | 关联类型：TABLE（关联整张表）或 FIELD（关联具体字段） |
| tableId | String | 关联的表ID |
| fieldName | String | 字段名（仅 FIELD 类型时填写） |

**关联类型说明**：

| entityType | tableId | fieldName | 含义 |
|------------|---------|-----------|------|
| TABLE | ✓ | 空 | 术语关联到整张表 |
| FIELD | ✓ | ✓ | 术语关联到某个字段 |

**约束**：tableId 必须在该术语所属主题关联的表范围内

## 4. ES 索引设计

### 4.1 文档类型扩展

新增 `SUBJECT` 和 `SUBJECT_TABLE` 类型，复用现有 `semantic_search` 索引：

| type | id格式 | keywords来源 | description来源 |
|------|--------|--------------|-----------------|
| SUBJECT | subject:{id} | 主题名称 | 主题描述 |
| SUBJECT_TABLE | subject_table:{subjectId}:{tableId} | 表名、表别名 | 表描述 |
| TERM | term:{id} | 术语名 | 术语描述 |

### 4.2 EntityReference 扩展

现有 `EntityReference` 已预留 `subjectId` 字段，无需修改。

## 5. API 设计

### 5.1 SubjectController

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /subjects | 创建主题（需指定datasourceId） |
| PUT | /subjects/{id} | 更新主题信息 |
| DELETE | /subjects/{id} | 删除主题（级联删除关联的SubjectTable、Term、TermField） |
| GET | /subjects/{id} | 获取主题详情 |
| POST | /subjects/{id}/tables | 添加表到主题 |
| DELETE | /subjects/{id}/tables/{tableId} | 从主题移除表 |
| GET | /subjects/{id}/tables | 获取主题下的表列表 |

### 5.2 TermController

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /subjects/{subjectId}/terms | 创建术语 |
| PUT | /terms/{id} | 更新术语 |
| DELETE | /terms/{id} | 删除术语（级联删除关联的 TermRelation） |
| GET | /terms/{id} | 获取术语详情 |
| POST | /terms/{id}/relations | 关联术语到表/字段 |
| DELETE | /terms/{id}/relations/{tableId}/{fieldName} | 取消关联（fieldName 为空表示取消表级别关联） |
| GET | /subjects/{subjectId}/terms | 获取主题下的术语列表 |

## 6. Service 层设计

### 6.1 SubjectService

```
SubjectService {
  createSubject(dto): Subject
    - 校验 datasourceId 存在且有效
    - 保存主题
    - 构建 SemanticSearchDocument(type=SUBJECT) 并索引

  updateSubject(id, dto): Subject
    - 校验 id 存在
    - 更新主题
    - 更新 ES 索引

  deleteSubject(id): void
    - 校验 id 存在
    - 删除主题
    - 删除关联的 SubjectTable、Term、TermField
    - 从 ES 删除该主题相关的所有文档

  addTableToSubject(subjectId, tableId): SubjectTable
    - 校验 subjectId 存在
    - 校验 tableId 存在
    - 校验 table 属于 subject.datasourceId
    - 保存关联
    - 构建 SemanticSearchDocument(type=SUBJECT_TABLE) 并索引

  removeTableFromSubject(subjectId, tableId): void
    - 校验关联存在
    - 删除关联
    - 从 ES 删除对应的 SUBJECT_TABLE 文档
    - 删除关联到该表的 TermField

  getSubjectWithTables(id): SubjectDetailVO
    - 返回主题信息 + 关联的表列表
}
```

### 6.2 TermService

```
TermService {
  createTerm(subjectId, dto): Term
    - 校验 subjectId 存在
    - 保存术语
    - 构建 SemanticSearchDocument(type=TERM) 并索引

  updateTerm(id, dto): Term
    - 校验 id 存在
    - 更新术语
    - 更新 ES 索引

  deleteTerm(id): void
    - 校验 id 存在
    - 删除术语
    - 删除关联的 TermRelation
    - 从 ES 删除对应的 TERM 文档

  linkEntity(termId, entityType, tableId, fieldName): TermRelation
    - 校验 termId 存在，获取 subjectId
    - 校验 tableId 属于该 subject 关联的表
    - 如果 entityType=FIELD，还需校验 fieldName 属于该表
    - 保存关联

  unlinkEntity(termId, tableId, fieldName): void
    - 校验关联存在
    - 删除关联（fieldName 为空时删除表级别关联）

  getTermsBySubject(subjectId): List<Term>
    - 返回该主题下的所有术语
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

```sql
-- 主题表
CREATE TABLE subject (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    description TEXT,
    datasource_id VARCHAR(64) NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_datasource (datasource_id)
);

-- 主题-表关联表
CREATE TABLE subject_table (
    id VARCHAR(64) PRIMARY KEY,
    subject_id VARCHAR(64) NOT NULL,
    table_id VARCHAR(64) NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE INDEX uk_subject_table (subject_id, table_id),
    INDEX idx_subject (subject_id)
);

-- 术语表
CREATE TABLE term (
    id VARCHAR(64) PRIMARY KEY,
    subject_id VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL,
    description TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_subject (subject_id)
);

-- 术语-实体关联表
CREATE TABLE term_relation (
    id VARCHAR(64) PRIMARY KEY,
    term_id VARCHAR(64) NOT NULL,
    entity_type VARCHAR(16) NOT NULL COMMENT 'TABLE or FIELD',
    table_id VARCHAR(64) NOT NULL,
    field_name VARCHAR(128),
    UNIQUE INDEX uk_term_relation (term_id, table_id, field_name),
    INDEX idx_term (term_id)
);
```

## 10. 实现计划

### Phase 1: 主题基础管理
1. Subject 实体、Repository、Service、Controller
2. SubjectTable 关联管理
3. ES 索引同步（SUBJECT、SUBJECT_TABLE）
4. 前端：主题 CRUD + 表管理

### Phase 2: 主题私有术语
1. Term 实体、Repository、Service、Controller
2. TermRelation 关联管理（支持 TABLE/FIELD 两种类型）
3. ES 索引同步（TERM）
4. 前端：术语 CRUD + 表/字段关联

### Phase 3: NL2SQL 集成
1. 搜索时支持 subjectId 过滤
2. 统一语义查询接口适配