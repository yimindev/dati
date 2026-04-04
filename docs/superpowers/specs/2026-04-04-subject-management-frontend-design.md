# 主题管理前端页面设计方案

## 1. 概述

本方案描述主题管理模块的前端页面实现，包含主题列表页和主题详情页。

## 2. 侧边栏菜单

### 2.1 菜单结构调整

现有「语义模型」菜单升级为父级「语义管理」，结构如下：

```
语义管理
└─ 主题管理   ← 唯一入口，承载原有语义模型 + 新增主题管理
```

**说明：**
- 原「语义模型」菜单项保留，功能移入「主题管理」详情页的 Tab 中
- 术语管理不设独立入口，集成在主题详情页的 Tab 中（术语归属主题）

## 3. 主题列表页

**路由：** `/subjects`

### 3.1 页面结构

```
┌─────────────────────────────────────────────┐
│  搜索框 [__________] 🔍   [+ 创建主题]      │
├─────────────────────────────────────────────┤
│  ┌─────────────┐  ┌─────────────┐          │
│  │ 主题 A       │  │ 主题 B       │          │
│  │ 描述...      │  │ 描述...      │          │
│  │ 数据源: MySQL│  │ 数据源: PG   │          │
│  │ 关联 5 张表  │  │ 关联 3 张表  │          │
│  │ [编辑][删除] │  │ [编辑][删除] │          │
│  └─────────────┘  └─────────────┘          │
│  ...                                        │
└─────────────────────────────────────────────┘
```

### 3.2 卡片信息

| 字段 | 说明 |
|------|------|
| 名称 | 主题名称（大字突出） |
| 描述 | 主题描述（截断显示） |
| 数据源 | 关联的数据源名称（只读） |
| 关联表数量 | 该主题关联的表数量 |
| 更新时间 | 最后更新时间 |

### 3.3 组件结构

| 组件 | 路径 | 说明 |
|------|------|------|
| 页面 | `pages/subjects/index.vue` | 列表页容器 |
| 卡片组件 | `components/subject/SubjectCard.vue` | 单个主题卡片 |
| 创建/编辑弹窗 | `components/subject/SubjectDialog.vue` | 主题表单弹窗 |

### 3.4 API 调用

| 操作 | API | 说明 |
|------|-----|------|
| 列表 | `GET /v1/subjects?datasourceId=` | 按数据源筛选（可选） |
| 详情 | `GET /v1/subjects/{id}` | 获取主题详情（包含关联表） |
| 创建 | `POST /v1/subjects` | 创建主题（需指定 datasourceId） |
| 更新 | `PUT /v1/subjects/{id}` | 更新主题信息 |
| 删除 | `DELETE /v1/subjects/{id}` | 删除主题 |

## 4. 主题详情页

**路由：** `/subjects/[id]`

### 4.1 页面结构

```
┌─────────────────────────────────────────────────┐
│  面包屑：语义管理 > 主题管理 > {主题名称}         │
├─────────────────────────────────────────────────┤
│  [基本信息]  [表管理]  [术语管理]  ← Tab 切换   │
├─────────────────────────────────────────────────┤
│  Tab 内容区                                      │
└─────────────────────────────────────────────────┘
```

### 4.2 Tab 1：基本信息

- **名称**：可编辑
- **描述**：可编辑（多行文本）
- **关联数据源**：只读显示，不可更改（创建时选定）

### 4.3 Tab 2：表管理

已关联的表列表 + 添加表按钮

**添加表交互：**
- 点击「添加表」弹出 Transfer 穿梭框
- 左侧：可选表列表（来自该主题关联的数据源）
- 右侧：已选表列表
- 只能选择该数据源下的表
- 确认后调用 `POST /v1/subjects/{id}/tables`

**API 调用：**

| 操作 | API |
|------|-----|
| 获取已关联表 | `GET /v1/subjects/{id}/tables` |
| 添加表 | `POST /v1/subjects/{id}/tables` |
| 移除表 | `DELETE /v1/subjects/{id}/tables/{tableId}` |

### 4.4 Tab 3：术语管理

术语列表，每行可展开显示关联的表/字段。

**行内展开交互：**

```
┌────────────────────────────────────────────────┐
│ 术语名称    描述              关联数  操作      │
├────────────────────────────────────────────────┤
│ ▶ 订单金额   订单相关金额术语   2      [编辑][删除]
│   ├─ 关联表: orders (整表)              [删除]│
│   └─ 关联字段: orders.amount           [删除]│
└────────────────────────────────────────────────┘
```

**添加关联流程：**
1. 点击「添加关联」按钮
2. 弹出选择器：先选表（只能选主题已关联的表）
3. 再选字段（可选，不选表示关联整张表）
4. 确认添加

**API 调用：**

| 操作 | API |
|------|-----|
| 术语列表 | `GET /v1/subjects/{subjectId}/terms` |
| 创建术语 | `POST /v1/subjects/{subjectId}/terms` |
| 更新术语 | `PUT /v1/terms/{id}` |
| 删除术语 | `DELETE /v1/terms/{id}` |
| 关联表/字段 | `POST /v1/terms/{id}/relations` |
| 取消关联 | `DELETE /v1/terms/{id}/relations/{tableId}/{fieldName}` |

## 5. 创建主题弹窗

**表单字段：**
| 字段 | 类型 | 说明 |
|------|------|------|
| 名称 | 输入框 | 必填 |
| 描述 | 多行文本 | 可选 |
| 数据源 | 下拉选择 | 必填，创建后不可更改 |

## 6. 类型定义

### 6.1 SubjectVO

```ts
interface SubjectVO {
  id: string
  name: string
  description?: string
  datasourceId: string
  datasourceName?: string  // 关联查询
  tableCount?: number      // 关联表数量
  created_by?: string
  created_at?: string
  updated_by?: string
  updated_at?: string
}
```

### 6.2 SubjectDetailVO

```ts
interface SubjectDetailVO extends SubjectVO {
  tables: SubjectTableVO[]  // 关联的表列表
}

interface SubjectTableVO {
  id: string
  subjectId: string
  tableId: string
  tableName: string         // 表名称
  tableDisplayName?: string // 表显示名称
  schema?: string           // 所属 schema
  description?: string      // 表描述
}
```

### 6.3 TermVO

```ts
interface TermVO {
  id: string
  subjectId: string
  name: string
  description?: string
  created_by?: string
  created_at?: string
  updated_by?: string
  updated_at?: string
}
```

### 6.4 TermRelationVO

```ts
interface TermRelationVO {
  id: string
  termId: string
  entityType: 'TABLE' | 'FIELD'
  tableId: string
  tableName?: string
  fieldName?: string
}
```

## 7. 文件结构

```
frontend/src/
├── api/
│   └── subject.ts           # 主题相关 API
├── components/
│   └── subject/
│       ├── SubjectCard.vue      # 主题卡片
│       ├── SubjectDialog.vue    # 创建/编辑弹窗
│       ├── SubjectTableList.vue # 主题关联的表列表
│       └── TermManager.vue      # 术语管理组件（行内展开）
├── pages/
│   └── subjects/
│       └── index.vue            # 主题列表页
│       └── [id].vue              # 主题详情页
└── locales/
    └── zh.ts                    # 国际化（新增 subject 词条）
```

## 8. 状态

- [ ] 主题列表页
- [ ] 创建/编辑主题弹窗
- [ ] 主题详情页（基本信息 Tab）
- [ ] 主题详情页（表管理 Tab）
- [ ] 主题详情页（术语管理 Tab）
- [ ] 术语关联管理（行内展开）
