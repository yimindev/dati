# 平台介绍

DatI(Data Intelligence) 是连接 **AI Agent 与企业数据库** 的轻量级语义网关 —— 仅需接入数据库、配置语义信息、按需启用预置工具与参数化 SQL 工具，即可发布 MCP 服务，灵活地接入用户的 Agent 或任意 MCP Host。

![DatI 架构示意图](/images/dati-visual-positioning.svg)

## 为什么选择 DatI？

1. **多数据库**：支持 MySQL、PostgreSQL、ClickHouse、Doris 等多种关系型与分析型数据库
2. **语义增强**：支持业务术语、字段别名与枚举字典值自动抽取，结合语义检索，让模型能够理解业务黑话、快速找对表
3. **灵活集成**：基于标准 [MCP](https://modelcontextprotocol.io/) 协议（Streamable HTTP），灵活集成至用户现有 Agent 或工作流
4. **高效构建**：提供开箱即用的预置工具（元数据探查、SQL 执行）与参数化 SQL 工具，免部署发布 MCP 服务
5. **安全管控**：凭据集中托管，支持按用户权限隔离

## 适用场景

- **智能问数**：接入业务数据库，通过业务元数据配置以及通用预置工具即可支持 NL2SQL 分析工作流
- **轻应用搭建**：将数据库封装为 MCP 服务，Agent 通过对话即可直接对业务数据增删改查，快速构建轻量级应用


## 下一步

- 前往 [快速上手](/quickstart) 开启你的第一个 DatI 服务
- 深入了解 [模板语法详解](/template-syntax)
- 查阅 [常见问题](/faq)
