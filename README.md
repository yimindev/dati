# DatI - Database Semantic Gateway for AI Agents

[English](README.md) | [简体中文](README_zh.md)

DatI (Data Intelligence) is a lightweight semantic gateway connecting **AI Agents with enterprise databases**. Simply connect your database, enrich semantic metadata, and configure built-in or parameterized SQL tools to publish an MCP service — ready for your agents or any MCP host.

```text
┌────────────────────┐  ┌────────────────────┐  ┌────────────────────┐
│  User A: OpenCode  │  │  User B: WorkBuddy │  │  User N: DataAgent │
└──────────┬─────────┘  └──────────┬─────────┘  └──────────┬─────────┘
           └───────────────────────┼───────────────────────┘
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

## Try It Online

Demo URL: http://47.99.122.223:18085/

Account / Password: `demo` / `demo123`

The demo instance is preloaded with sample data and reset periodically.

### Demo

Data and configuration reference the [AdventureWorks sample](examples/adventureworks-dw) and can be automated via the [dati-ops skill](skills/dati-ops).

#### 1. Data Source Metadata Configuration (configurable tables, columns, and values)

<details>
<summary><b>View Demo (GIF)</b></summary>

![Data Source Metadata Configuration](docs/images/datasource_config.gif)

</details>

#### 2. Business Subject Configuration (select relevant tables, define business terms)

<details>
<summary><b>View Demo (GIF)</b></summary>

![Subject Configuration](docs/images/subject-config.gif)

</details>

#### 3. MCP Service Configuration (choose business subjects, enable built-in tools, add parameterized SQL tools)

<details>
<summary><b>View Demo (GIF)</b></summary>

![MCP Service Configuration](docs/images/mcp-service-config.gif)

</details>

#### 4. Configure MCP in Agent (e.g., Antigravity)

<details>
<summary><b>View Screenshot</b></summary>

![Configure MCP in Agent](docs/images/antigravity-mcp-config.jpg)

</details>

#### 5. Query Data in Agent

<details>
<summary><b>View Demo (GIF)</b></summary>

![Query Data in Agent](docs/images/agy-analysis.gif)

</details>


## Why DatI?

1. **Multi-Database Support**: Supports MySQL, PostgreSQL, ClickHouse, Doris, and other relational and analytical databases
2. **Semantic Enhancement**: Supports business terms, column aliases, and automatic enum dictionary extraction. Combined with semantic search, it helps models understand business jargon and find the right tables
3. **Flexible Integration**: Based on standard [MCP](https://modelcontextprotocol.io/) (Streamable HTTP), easily integrates into your existing agents or workflows
4. **Effortless MCP Publishing**: Ready-to-use built-in tools (schema inspection, SQL execution) and parameterized SQL tools to publish MCP services with zero extra deployment
5. **Security & Governance**: Centrally managed database credentials with user-level permission and data scope isolation

## Use Cases

- **Conversational Data Analysis (NL2SQL)**: Connect business databases and support NL2SQL analysis workflows with semantic metadata and built-in tools
- **Lightweight App Development**: Wrap databases as MCP services so agents can query and update data through conversation to build lightweight applications

## Tech Stack

- **Backend**: Spring Boot 3.5.x + Java 21 + JPA
- **Frontend**: Vue 3 + TypeScript + Vite + Element Plus + TailwindCSS 4
- **Database**: H2 (Development) / MySQL / PostgreSQL (Production)
- **Search Engine**: Elasticsearch (Semantic Retrieval)

## Documentation

- [Local Development Guide](docs/development.md): Environment setup, backend/frontend startup, and project conventions
- [Server Deployment Guide](docs/deployment.md): Architecture overview, Docker Compose deployment, and operational maintenance
- [Architecture Overview](docs/architecture/overview.md): High-level architecture, module index, and key design conventions
- **Examples**:
  - [Enterprise BI & Retail Analytics (AdventureWorks DW)](examples/adventureworks-dw/README.md): Star schema Text-to-SQL, metric governance, multi-table joins, and parameterized acceleration
  - [Family Finance Assistant](examples/family-finance/README.md): Multi-user collaborative bookkeeping, parameterized permission control, transparent SQL queries, and self-healing agent workflows
- **Agent Skills** ([Agent Skills Open Standard](https://agentskills.io), auto-discovered by repository agents):
  - [dati-ops](skills/dati-ops/SKILL.md): **User Skill** — Configure and operate the platform via HTTP APIs (data sources, subjects & terms, MCP services); self-contained with built-in openapi.json and query tools, independently distributable; connected in-repo via `.agents/skills/dati-ops/`
  - [e2e-tester](.agents/skills/e2e-tester/SKILL.md): **Developer Skill** — E2E HTTP integration tests and API behavior validation (see test cases in [e2e-tests/test-cases/](e2e-tests/test-cases))
- **User Guide**: [docs/user-guide](docs/user-guide/index.md) (VitePress site, bilingual)
- **API Specification**: [docs/api/openapi.json](docs/api/openapi.json) (Used by E2E test toolchains)
- **AI Coding Assistant Guidelines**: [AGENTS.md](AGENTS.md) and [.agents/rules/](.agents/rules) (Backend, frontend, and design system rules)

## Acknowledgments

Thanks to the [LINUX DO](https://linux.do/) community for discussions and support.

