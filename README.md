# DatI - Database Semantic Gateway for AI Agents

[English](README.md) | [简体中文](README_zh.md)

DatI (Data Intelligence) is a lightweight semantic gateway connecting **AI Agents with enterprise databases**. Simply connect a database, configure semantic metadata, and enable prebuilt or parameterized SQL tools to publish an MCP service that connects with your agents or any MCP host.

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

## Why DatI?

1. **Multiple Databases**: Supports MySQL, PostgreSQL, ClickHouse, Doris, and other relational and analytical databases
2. **Semantic Enhancement**: Supports business terms, column aliases, and automatic enum dictionary extraction. Combined with semantic search, it helps models understand business jargon and find the right tables
3. **Flexible Integration**: Based on standard [MCP](https://modelcontextprotocol.io/) (Streamable HTTP), easily integrates into your existing agents or workflows
4. **Fast to Build**: Out-of-the-box prebuilt tools (metadata inspection, SQL execution) and parameterized SQL tools to publish MCP services without extra deployment
5. **Access Control**: Centrally manages credentials with support for user-level permission isolation

## Use Cases

- **Natural Language Data Analysis**: Connect business databases and support NL2SQL analysis workflows with metadata configuration and prebuilt tools
- **Lightweight App Development**: Wrap databases as MCP services so agents can query and update data through conversation to build lightweight applications

## Tech Stack

- **Backend**: Spring Boot 3.5.x + Java 21 + JPA
- **Frontend**: Vue 3 + TypeScript + Vite + Element Plus + TailwindCSS 4
- **Database**: H2 (Development) / MySQL / PostgreSQL (Production)
- **Search Engine**: Elasticsearch (Semantic Retrieval)

## Documentation

- [Local Development Guide](docs/development.md): Environment setup, startup, common commands, and development conventions
- [Server Deployment Guide](docs/deployment.md): Single-node Docker Compose deployment (no registry needed)
- **Agent Skills** ([Agent Skills Open Standard](https://agentskills.io), auto-discovered by repository agents):
  - [dati-ops](skills/dati-ops/SKILL.md): **User Skill** — Configure and operate the platform via HTTP APIs (data sources, subjects & terms, MCP services); self-contained with built-in openapi.json and query tools, independently distributable; connected in-repo via `.agents/skills/dati-ops/`
  - [e2e-tester](.agents/skills/e2e-tester/SKILL.md): **Developer Skill** — E2E HTTP integration tests and API behavior validation (see test cases in [e2e-tests/test-cases/](e2e-tests/test-cases))
- **Examples**:
  - [Family Finance Assistant](examples/family-finance/README.md): Multi-user collaborative bookkeeping, parameterized permission control, transparent SQL queries, and self-healing agent workflows
- **Architecture & Design** (Maintained continuously alongside code):
  - [Overview](docs/architecture/overview.md)
  - [Authentication](docs/architecture/authentication.md)
  - [Authorization](docs/architecture/permission.md)
  - [Data Source Module](docs/architecture/datasource.md)
  - [Semantic Module](docs/architecture/semantic.md)
  - [MCP Service Management](docs/architecture/mcp-service-management.md)
  - [Template Engine](docs/architecture/template-engine.md)
  - [Editor Architecture](docs/architecture/editor.md)
- **User Guide**: [docs/user-guide](docs/user-guide/index.md) (VitePress site, bilingual)
- **API Specification**: [docs/api/openapi.json](docs/api/openapi.json) (Used by E2E test toolchains)
- **AI Coding Assistant Guidelines**: [AGENTS.md](AGENTS.md) and [.agents/rules/](.agents/rules) (Backend, frontend, and design system rules)
