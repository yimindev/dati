# 语义管理模块架构（Semantic Management）

## 业务概述

语义管理模块为 DatI 提供**数据语义元数据**能力，帮助用户将数据库中的表、字段赋予业务含义：

- **Subject（主题）**：归属于某个数据源的业务主题，如"客户分析"、"销售报表"
- **Term（术语）**：主题下的业务词汇，如"客户"、"营收"
- **TermRelation（术语关联）**：将术语绑定到具体的表/字段/字段值

```
Subject (1) ─────< SubjectTable (M:N) >───── (1) TableInfo
   │
   └────< Term (M:1)
            │
            └────< TermRelation (M:1)
                        ├── TABLE
                        ├── FIELD
```

同时维护 Elasticsearch 索引（`SemanticSearchDocument`）实现跨语义实体的全文搜索。

## 后端架构

### 目录结构

```
backend/src/main/java/com/dati/semantic/
├── domain/
│   ├── SemanticEntityType.java          # 枚举：SUBJECT/TABLE/FIELD/FIELD_VALUE/TERM
│   ├── model/
│   │   ├── Subject.java                 # 主题领域模型
│   │   ├── Term.java                    # 术语领域模型
│   │   └── TermRelation.java            # 术语关联领域模型
│   └── service/
│       ├── SubjectService.java          # Subject 业务逻辑
│       ├── TermService.java             # Term 业务逻辑
│       └── SemanticIndexService.java    # ES 索引封装
│
├── repository/
│   ├── po/                              # 持久化对象（JPA 实体）
│   │   ├── SubjectPO.java
│   │   ├── TermPO.java
│   │   ├── TermRelationPO.java
│   │   ├── SubjectTablePO.java          # 主题-表关联
│   │   └── SemanticSearchDocument.java  # ES 文档
│   ├── dao/                             # 数据访问
│   │   ├── SubjectDAO.java
│   │   ├── TermDAO.java
│   │   ├── TermRelationDAO.java
│   │   ├── SubjectTableDAO.java
│   │   └── SemanticSearchDAO.java
│   └── mapper/                          # PO ↔ Model 转换
│       ├── SubjectMapper.java
│       ├── TermMapper.java
│       └── TermRelationMapper.java
│
└── server/
    ├── controller/
    │   ├── SubjectController.java        # /v1/subjects
    │   └── TermController.java           # /v1/terms
    ├── request/                          # 请求 DTO
    │   ├── CreateSubjectRequest.java
    │   ├── UpdateSubjectRequest.java
    │   ├── AddTableToSubjectRequest.java
    │   ├── CreateTermRequest.java
    │   ├── UpdateTermRequest.java
    │   └── LinkTermRelationRequest.java
    ├── vo/                               # 响应 VO
    │   ├── SubjectVO.java
    │   ├── TermVO.java
    │   ├── SubjectTableVO.java
    │   └── SubjectAvailableTableVO.java
    └── assembler/
        ├── SubjectAssembler.java         # Model → VO
        └── TermAssembler.java
```

### API 列表

**Subject API**（`/v1/subjects`）：

| Method | Path | 说明 |
|--------|------|------|
| GET | `/v1/subjects?datasourceId=&page=&size=` | 分页查询主题 |
| GET | `/v1/subjects/{id}` | 获取主题详情 |
| POST | `/v1/subjects` | 创建主题 |
| PUT | `/v1/subjects/{id}` | 更新主题 |
| DELETE | `/v1/subjects/{id}` | 删除主题 |
| GET | `/v1/subjects/{id}/tables` | 获取已关联的表 |
| GET | `/v1/subjects/{id}/tables/available` | 获取可关联的表 |
| POST | `/v1/subjects/{id}/tables` | 关联表到主题 |
| DELETE | `/v1/subjects/{id}/tables/{tableId}` | 取消关联 |

**Term API**（`/v1/terms`）：

| Method | Path | 说明 |
|--------|------|------|
| GET | `/v1/subjects/{subjectId}/terms` | 查询主题下的术语 |
| GET | `/v1/terms/{id}` | 获取术语详情（含关联） |
| POST | `/v1/subjects/{subjectId}/terms` | 创建术语 |
| PUT | `/v1/terms/{id}` | 更新术语 |
| DELETE | `/v1/terms/{id}` | 删除术语 |
| POST | `/v1/terms/{id}/relations` | 关联术语到表/字段 |
| DELETE | `/v1/terms/{id}/relations/{tableId}/{fieldNameOr_}` | 取消关联 |

## 前端架构

### 页面结构

```
frontend/src/pages/subjects/
├── index.vue         # 主题列表页（搜索、创建、分页）
└── [id].vue          # 主题详情页（基本信息/表管理/术语管理 三标签页）
```

### 核心组件

| 组件 | 位置 | 职责 |
|------|------|------|
| `SubjectCard` | `components/` | 主题卡片展示 |
| `SubjectDialog` | `components/` | 创建/编辑主题弹窗 |
| `SubjectTableList` | `components/` | 主题关联的表管理（含 schema 选择） |
| `TermManager` | `components/` | 术语的增删改及关联管理 |

### 前端 API 封装（`frontend/src/api/subject.ts`）

```typescript
// Subject
listSubjects(page, size, datasourceId): PageResponse<SubjectVO>
getSubject(id): SubjectVO
createSubject(body): IdResponse
updateSubject(id, body): void
deleteSubject(id): void
getSubjectTables(subjectId): SubjectTableVO[]
getAvailableTables(subjectId, schema): SubjectAvailableTableVO[]
addTableToSubject(subjectId, body): void
removeTableFromSubject(subjectId, tableId): void

// Term
getTermsBySubject(subjectId): TermVO[]
getTermDetail(id): TermVO
createTerm(subjectId, body): IdResponse
updateTerm(id, body): void
deleteTerm(id): void
linkTermRelation(termId, body): void
unlinkTermRelation(termId, tableId, fieldNameOrNull): void
```

## 技术要点

1. **ES 索引同步**：Subject/Term 创建/更新/删除时同步写 Elasticsearch，支持全文检索
2. **级联删除**：删除 Subject 会清理 ES 索引；删除 Term 会清理关联的 TermRelation
3. **表归属校验**：关联表到主题时校验表必须属于同一数据源
4. **DTO 命名**：后端使用 `camelCase`，前端 API 层转换为 `snake_case`（后端统一按 dev profile 的 snake_case 处理）
