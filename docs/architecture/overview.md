# 架构总览（Architecture Overview）

本目录收录 DatI 各模块的架构文档，与代码保持同步维护。建议按以下顺序阅读。

## 高层架构

![DatI 架构示意图](../images/dati-visual-positioning.svg)

- **前端**（`frontend/`）：Vue 3 + Vite + TypeScript，组件库 Element Plus，样式 TailwindCSS 4，单元测试 Vitest。
- **后端**（`backend/`）：Spring Boot 3.5.x（Java 21），按 DDD 分层（`domain` / `repository` / `server`），暴露 REST API，JPA 访问元数据库，Flyway 迁移。
- **数据库**：开发环境 H2 文件库（`./db/dataconnai`），可切换 MySQL/PostgreSQL 等（新增 profile）。
- **搜索引擎**：Elasticsearch 承载语义检索（`SemanticSearchDocument`）。

## 模块与文档索引

| 模块 | 文档 | 职责 |
|------|------|------|
| 认证（auth） | [authentication.md](authentication.md) | 可插拔认证（JWT / API Key）、登录与请求拦截 |
| 授权（permission） | [permission.md](permission.md) | 三层权限判定（管理员 → 创建者 → ACL）、列表静默过滤 |
| 数据源（datasource） | [datasource.md](datasource.md) | 数据源连接、元数据探查、表/列管理、列值抽取、SQL 执行 |
| 语义管理（semantic） | [semantic.md](semantic.md) | 主题（Subject）、术语（Term）与关联、ES 语义检索 |
| MCP 服务（mcp） | [mcp-service-management.md](mcp-service-management.md) | MCP 服务生命周期、数据范围、工具/Prompt、发布与版本管理、JSON-RPC Endpoint |
| 模板引擎（common.template） | [template-engine.md](template-engine.md) | 零依赖模板引擎，TEXT / 参数化 SQL 双渲染模式 |
| 编辑器（前端） | [editor.md](editor.md) | CodeMirror 6 模板/SQL 编辑器架构与设计决策 |

## 关键约定

- 异常统一走 `DatiException` + `ErrorCode` 枚举（前缀：`CM` 通用、`DS` 数据源、`SM` 语义）。
- JSON 命名策略（dev）：`SNAKE_CASE`，前端 API 层适配。
- MCP Endpoint：`POST /{code}/mcp`（JSON-RPC over HTTP，2025-11-25 协议），详细语义见 [mcp-service-management.md](mcp-service-management.md) 2.2 节。

## 本地开发

环境准备、启动命令、数据库与迁移等见 [本地开发指南](../development.md)。
