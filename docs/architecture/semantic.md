# 语义管理模块架构（Semantic Management）

## 业务概述

语义管理模块为 DatI 提供**数据语义元数据**能力，帮助用户将数据库中的表、字段赋予业务含义：

- **Subject（主题）**：归属于某个数据源的业务主题，如"客户分析"、"销售报表"
- **Term（术语）**：主题下的业务词汇，如"客户"、"营收"
- **TermRelation（术语关联）**：将术语绑定到具体的表（TABLE 级）/ 字段（FIELD 级）

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
│   ├── SemanticEntityType.java          # 枚举：SUBJECT/TABLE/FIELD/FIELD_VALUE/TERM（ES 文档类型）
│   ├── TermRelationType.java            # 枚举：TABLE/FIELD（术语关联目标类型）
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
| GET | `/v1/subjects?keyword=&page=&size=` | 分页查询主题（按名称搜索） |
| GET | `/v1/subjects/{id}` | 获取主题详情（含数据源名称） |
| POST | `/v1/subjects` | 创建主题 |
| PUT | `/v1/subjects/{id}` | 更新主题 |
| DELETE | `/v1/subjects/{id}` | 删除主题 |
| GET | `/v1/subjects/{id}/tables?keyword=&page=&size=` | 分页查询已关联的表（按表名搜索，DB 层分页） |
| GET | `/v1/subjects/{id}/available-tables?schema=` | 获取可关联的表 |
| POST | `/v1/subjects/{id}/tables` | 关联表到主题 |
| DELETE | `/v1/subjects/{id}/tables/{tableId}` | 取消关联 |

**Term API**（`/v1/terms`）：

| Method | Path | 说明 |
|--------|------|------|
| GET | `/v1/subjects/{subjectId}/terms?keyword=&page=&size=` | 分页查询主题下的术语（DB 层分页） |
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
listSubjects(page, size, keyword?, signal?): PageResponse<SubjectVO>
getSubject(id, signal?): SubjectVO
createSubject(body, signal?): IdResponse
updateSubject(id, body, signal?): IdResponse
deleteSubject(id, signal?): IdResponse
getSubjectTables(subjectId, page?, size?, keyword?, signal?): PageResponse<TableInfoVO>
getAvailableTables(subjectId, schema, signal?): SubjectAvailableTableVO[]
addTableToSubject(subjectId, body, signal?): IdResponse
removeTableFromSubject(subjectId, tableId, signal?): IdResponse

// Term
getTermsBySubject(subjectId, page?, size?, keyword?, signal?): PageResponse<TermVO>
getTermDetail(id, signal?): TermVO & { relations: TermRelationVO[] }
createTerm(subjectId, body, signal?): IdResponse
updateTerm(id, body, signal?): IdResponse
deleteTerm(id, signal?): IdResponse
linkTermRelation(termId, body, signal?): IdResponse
unlinkTermRelation(termId, tableId, fieldName, signal?): IdResponse
```

## 技术要点

1. **ES 索引同步**：Subject/Term 创建/更新/删除时同步写 Elasticsearch，支持全文检索
2. **级联删除**：删除 Subject 会清理 ES 索引；删除 Term 会清理关联的 TermRelation
3. **表归属校验**：关联表到主题时校验表必须属于同一数据源
4. **数据库级分页**：`getSubjectTables` 和 `getTermsBySubject` 通过 JPQL 子查询在 DB 层完成分页/排序/关键词过滤，避免内存分页
5. **关键词搜索**：主题列表按名称搜索，已关联的表按表名搜索，术语按名称/别名搜索；均采用 Java 层分支（keyword 空/非空分别调用不同 DAO 方法），避免 `IS NULL OR` 影响查询计划
6. **数据源名称富集**：`SubjectVO.datasourceName` 由 `SubjectAssembler` 批量解析——`toVO()` 和 `toVOList()` 内部通过 `DataSourceService.getDataSourceNameMap()` 一次查询完成所有 ID→名称映射
7. **DTO 命名**：后端使用 `camelCase`，前端 API 层转换为 `snake_case`（后端统一按 dev profile 的 snake_case 处理）
