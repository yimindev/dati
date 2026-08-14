# DatI - AI 数据接入基础设施

DatI（Data Intelligence）是一个为 AI 大模型提供**统一数据接入能力**的基础设施平台。它能帮助你：

- **快速接入多种数据源**：支持主流数据库（MySQL、PostgreSQL、ClickHouse、Oracle 等 10 种）
- **灵活构建 MCP 服务**：自动生成符合 [Model Context Protocol](https://modelcontextprotocol.io/) 标准的服务接口
- **即插即用**：让 AI 大模型（如 Claude、GPT 等）能够无缝访问你的业务数据

## 适用场景

- **NL2SQL 应用构建**：接入业务数据库，进行元数据查询、SQL 执行
- **数据分析助手**：通过配置元数据、术语，为 Agent 提供业务数据分析能力
- **快速 MCP 服务**：通过自定义 SQL 快速构建数据查询 MCP 服务

## 技术栈

- **后端**：Spring Boot 3.5.x + Java 21 + JPA + Flyway
- **前端**：Vue 3 + TypeScript + Vite + Element Plus + TailwindCSS 4
- **数据库**：H2（开发）/ MySQL / PostgreSQL（生产）
- **搜索引擎**：Elasticsearch（语义检索）

## 文档导航

- [本地开发指南](docs/development.md)：环境准备、启动、常用命令与开发约定
- **架构与设计**（长期维护，与代码同步）：
  - [架构总览](docs/architecture/overview.md)
  - [认证架构](docs/architecture/authentication.md)
  - [授权架构](docs/architecture/permission.md)
  - [数据源模块](docs/architecture/datasource.md)
  - [语义管理模块](docs/architecture/semantic.md)
  - [MCP 服务管理](docs/architecture/mcp-service-management.md)
  - [模板引擎](docs/architecture/template-engine.md)
  - [编辑器架构](docs/architecture/editor.md)
- **用户帮助中心**：[docs/user-guide](docs/user-guide/index.md)（VitePress 站点，中英双语）
- **API 契约**：[docs/api/openapi.json](docs/api/openapi.json)（E2E 测试工具链使用）
- **AI 编码助手规范**：[AGENTS.md](AGENTS.md) 与 [.agents/rules/](.agents/rules/)（后端/前端/设计系统规范）
