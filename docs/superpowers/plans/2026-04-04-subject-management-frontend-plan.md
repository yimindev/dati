# 主题管理前端实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 实现主题管理模块的前端页面，包括主题列表页、主题详情页（Tab 切换）、术语行内展开管理

**Architecture:** 遵循现有前端架构：Vue 3 + TypeScript + Element Plus + TailwindCSS，API 层分离，组件化实现

**Tech Stack:** Vue 3, TypeScript, Element Plus, TailwindCSS, vue-i18n, vue-router

---

## 文件结构

```
frontend/src/
├── api/
│   └── subject.ts              # 新增
├── components/
│   └── subject/
│       ├── SubjectCard.vue     # 新增
│       ├── SubjectDialog.vue   # 新增
│       ├── SubjectTableList.vue # 新增
│       └── TermManager.vue     # 新增
├── pages/
│   └── subjects/
│       ├── index.vue           # 新增
│       └── [id].vue            # 新增
└── locales/
    └── zh.ts                   # 修改
```

---

## Task 1: 添加 i18n 词条

**Files:** `frontend/src/locales/zh.ts`

添加 `subject` 模块的中文词条（参考设计文档第 7 节）。

---

## Task 2: 创建 subject API 模块

**Files:** `frontend/src/api/subject.ts`

实现完整的 API 函数：
- `listSubjects`, `getSubjectDetail`, `createSubject`, `updateSubject`, `deleteSubject`
- `getSubjectTables`, `addTableToSubject`, `removeTableFromSubject`
- `getTermsBySubject`, `createTerm`, `updateTerm`, `deleteTerm`
- `getTermDetail`, `linkTermRelation`, `unlinkTermRelation`

类型定义：SubjectVO, SubjectDetailVO, SubjectTableVO, TermVO, TermRelationVO

---

## Task 3: 创建 SubjectCard 组件

**Files:** `frontend/src/components/subject/SubjectCard.vue`

卡片布局：名称、描述、数据源名称、关联表数量、更新时间、编辑/删除按钮

---

## Task 4: 创建 SubjectDialog 组件

**Files:** `frontend/src/components/subject/SubjectDialog.vue`

创建/编辑主题弹窗：名称、描述（可编辑）、数据源选择（创建时可选，编辑时禁用）

---

## Task 5: 创建 SubjectTableList 组件

**Files:** `frontend/src/components/subject/SubjectTableList.vue`

表管理组件：已关联表列表、添加表弹窗（Transfer 穿梭框，参考 `pages/datasources/[id]/tables/index.vue`）

---

## Task 6: 创建 TermManager 组件

**Files:** `frontend/src/components/subject/TermManager.vue`

术语管理组件：
- 术语列表，每行可展开
- 展开显示已关联的表/字段
- 添加/移除关联功能

---

## Task 7: 创建主题列表页

**Files:** `frontend/src/pages/subjects/index.vue`

- 卡片网格布局（搜索框 + 创建按钮）
- 使用 SubjectCard 组件
- 分页支持

---

## Task 8: 创建主题详情页

**Files:** `frontend/src/pages/subjects/[id].vue`

三个 Tab：
1. 基本信息（名称、描述可编辑，数据源只读）
2. 表管理（使用 SubjectTableList 组件）
3. 术语管理（使用 TermManager 组件）

---

## Task 9: 更新侧边栏菜单

**Files:** `frontend/src/components/layouts/BaseSide.vue`

将「语义模型」改为「语义管理」父菜单，添加「主题管理」子菜单

---

## Task 10: 验证构建

运行 `cd frontend && pnpm build` 确保无类型错误

---

**Plan complete and saved to `docs/superpowers/plans/2026-04-04-subject-management-frontend-plan.md`.**

**Two execution options:**

**1. Subagent-Driven (recommended)** - I dispatch a fresh subagent per task, review between tasks, fast iteration

**2. Inline Execution** - Execute tasks in this session using executing-plans, batch execution with checkpoints

**Which approach?**
