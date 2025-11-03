# 架构总览（Architecture Overview）

本文件概述 Data Conn AI（Dati）的系统架构、模块边界与关键技术。

## 高层架构

- 前端（`frontend/`）：Vue 3 + Vite + TypeScript，UI 组件库 Element Plus，样式 TailwindCSS。
- 后端（`backend/`）：Spring Boot 3.5.x（Java 21），暴露 REST API，基于 JPA/JDBC 访问数据库，集成 Flyway（可选）。
- 数据库：开发环境使用 H2 文件库（`./db/dataconnai`）。可切换到 MySQL/PostgreSQL（新增 profile）。

## 运行时视图

- 默认 Profile：`dev`
- 端口与路由：后端 `:8085`，前端本地 dev server 由 Vite 管理。
- H2 Console：`/h2-console/semantic`（后端运行时可访问）。

## 关键约定

- JSON 命名策略（dev）：`SNAKE_CASE`。
- JPA DDL 策略（dev）：`update`，确保研发阶段表结构自动演进；生产请改为受控迁移（Flyway）。
- 数据库迁移：`backend/src/main/resources/db/migration`（V1__*.sql 等）。

## 模块边界

- datasource：数据源连接与元数据探查（catalogs/schemas/tables/columns，SQL 执行等）。
- semantic：语义实体、检索与索引（示例实体 `SemanticSearchDocument`、`EntityReference`）。
- base/db：基础异常、分页、DB 工具封装（`DciException`、`DbClientFactory`、`JdbcUtils` 等）。

## 扩展点

- 数据库接入：通过新增驱动与 profile 支持更多数据库（在 `application-*.yaml` 中配置）。
- API 版本化：当前以 `/v1/...` 暴露，可在路由与包结构内扩展新版本。
- 语义检索：可引入向量数据库/文本嵌入服务，通过 Repository 层扩展。

## 参考

- 详细后端说明：[../backend/README.md](../backend/README.md)
- API 规范与清单：[../api/README.md](../api/README.md)
