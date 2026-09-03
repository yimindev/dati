# DatI - 企业级 Agent 数据库语义网关

[English](README.md) | [简体中文](README_zh.md)

DatI(Data Intelligence) 是连接 **AI Agent 与企业数据库** 的语义网关 —— 集语义建模、细粒度权限控制与标准 MCP 接口于一体，让大模型精准、安全地读写业务数据。主要功能：

- **数据源接入**：支持主流数据库（MySQL、PostgreSQL 等）
- **语义建模**：对表、列、列值、业务术语进行统一管理与检索
- **构建 MCP 服务**：预置元数据检索、SQL 执行等标准工具，支持参数化 SQL 查询，可自动生成符合 [MCP](https://modelcontextprotocol.io/) 标准的服务接口

```text
┌────────────────────┐   ┌────────────────────┐   ┌────────────────────┐
│  User A: OpenCode  │   │  User B: WorkBuddy │   │  User N: DataAgent │
└─────────┬──────────┘   └─────────┬──────────┘   └─────────┬──────────┘
          └────────────────────────┼────────────────────────┘
                                   │ MCP (Streamable HTTP)
                                   ▼
┌─────────────────────────────── DatI ───────────────────────────────┐
│     ┌──────────────┐     ┌──────────────┐     ┌──────────────┐     │
│     │   Semantic   │     │   Security   │     │    Tools     │     │
│     └──────────────┘     └──────────────┘     └──────────────┘     │
└──────────────────────────────────┬─────────────────────────────────┘
                                   │
                                   ▼
┌────────────────────────────────────────────────────────────────────┐
│    MySQL     │   PostgreSQL    │    ClickHouse    │      Doris     │
└────────────────────────────────────────────────────────────────────┘
```

## 为什么选择 DatI？

1. **多数据库支持**：支持 MySQL、PostgreSQL、ClickHouse、Doris 等多种关系型与分析型数据库
2. **业务语义增强**：支持业务术语、字段别名与枚举字典值自动抽取，结合语义检索，解决大模型“不理解业务黑话、找不对表字段”的问题
3. **灵活的 Agent 集成**：基于标准 [MCP](https://modelcontextprotocol.io/) 协议（Streamable HTTP），灵活接入各类 Agent 或任意MCP Host
4. **高效构建与多用户复用**：提供开箱即用的预置工具（元数据探查、SQL 执行）与灵活的自定义参数化模板，几分钟即可发布可用服务，支持多用户与多 Agent 高并发复用
5. **企业级安全管控**：数据库密码集中加密托管，结合用户上下文可支持细粒度权限隔离

## 适用场景

- **智能问数**：接入业务数据库，通过业务元数据配置以及通用预置工具即可支持 NL2SQL 分析工作流
- **轻应用搭建**：将数据库封装为 MCP 服务，Agent 通过对话即可直接对业务数据增删改查，快速构建轻量级应用

## 技术栈

- **后端**：Spring Boot 3.5.x + Java 21 + JPA
- **前端**：Vue 3 + TypeScript + Vite + Element Plus + TailwindCSS 4
- **数据库**：H2（开发）/ MySQL / PostgreSQL（生产）
- **搜索引擎**：Elasticsearch（语义检索）

## 文档导航

- [本地开发指南](docs/development.md)：环境准备、启动、常用命令与开发约定
- **实战案例**：
  - [家庭共享记账助手](examples/family-finance/README.md)：多用户协作记账、参数化防越权、全员透明 SQL 查账与开箱自愈示例
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
- **Agent Skill**（[Agent Skills 开放标准](https://agentskills.io)，仓库内 agent 自动发现）：
  - [dati-ops](skills/dati-ops/SKILL.md)：**用户技能**——通过 HTTP API 完成平台配置与操作（数据源/主题术语/MCP 服务），技能自包含（内置 openapi.json 与查询工具），可独立分发；仓库内通过 `.agents/skills/dati-ops/` 薄壳接入
  - [e2e-tester](.agents/skills/e2e-tester/SKILL.md)：**开发技能**——E2E HTTP 集成测试与 API 行为验证（测试用例见 [e2e-tests/test-cases/](e2e-tests/test-cases/)）
