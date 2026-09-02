# DatI - Enterprise-Grade Database Semantic Gateway for AI Agents

[English](README.md) | [简体中文](README_zh.md)

DatI is a semantic gateway connecting **AI Agents with enterprise databases** — combining semantic modeling, fine-grained access control, and standard MCP interfaces to enable LLMs to query and manipulate business data accurately and securely. Key features:

- **Data Source Connectivity**: Native support for mainstream databases (MySQL, PostgreSQL, ClickHouse, Doris, etc.)
- **Semantic Modeling**: Unified management and retrieval across tables, columns, sample values, and business terminology
- **MCP Service Generation**: Prebuilt standard tools for metadata inspection and SQL execution, support for parameterized SQL queries, and automated generation of service endpoints conforming to the [Model Context Protocol (MCP)](https://modelcontextprotocol.io/) standard

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

## Use Cases

- **Conversational BI & NL2SQL**: Connect to business databases and enable natural-language-to-SQL analytics workflows through business metadata configuration and prebuilt inspection/query tools.
- **Lightweight App Development**: Encapsulate databases as MCP services so agents can perform controlled CRUD operations via conversation, rapidly building lightweight data applications.

## Tech Stack

- **Backend**: Spring Boot 3.5.x + Java 21 + JPA
- **Frontend**: Vue 3 + TypeScript + Vite + Element Plus + TailwindCSS 4
- **Database**: H2 (Development) / MySQL / PostgreSQL (Production)
- **Search Engine**: Elasticsearch (Semantic Retrieval)

## Documentation

- [Local Development Guide](docs/development.md): Environment setup, launch instructions, common commands, and development conventions
- **Showcase & Examples**:
  - [Family Finance Assistant](examples/family-finance/README.md): Multi-user collaborative bookkeeping, parameterized permission isolation, transparent SQL auditing, and self-healing agent workflows
- **Architecture & Design** (Maintained continuously alongside code):
  - [Overview](docs/architecture/overview.md)
  - [Authentication](docs/architecture/authentication.md)
  - [Authorization & Permission](docs/architecture/permission.md)
  - [Data Source Module](docs/architecture/datasource.md)
  - [Semantic Management Module](docs/architecture/semantic.md)
  - [MCP Service Management](docs/architecture/mcp-service-management.md)
  - [Template Engine](docs/architecture/template-engine.md)
  - [Editor Architecture](docs/architecture/editor.md)
- **User Guide**: [docs/user-guide](docs/user-guide/index.md) (VitePress site, Bilingual)
- **API Specification**: [docs/api/openapi.json](docs/api/openapi.json) (Used by E2E test toolchains)
- **AI Coding Assistant Guidelines**: [AGENTS.md](AGENTS.md) and [.agents/rules/](.agents/rules/) (Backend / Frontend / Design System rules)
- **Agent Skills** ([Agent Skills Open Standard](https://agentskills.io), auto-discovered by repository agents):
  - [dati-ops](skills/dati-ops/SKILL.md): **User Skill** — Configure and operate the platform via HTTP APIs (data sources / subjects & terms / MCP services); self-contained with built-in openapi.json and query tools, independently distributable; integrated via thin shell `.agents/skills/dati-ops/`.
  - [e2e-tester](.agents/skills/e2e-tester/SKILL.md): **Developer Skill** — E2E HTTP integration testing and API behavior validation (see test cases in [e2e-tests/test-cases/](e2e-tests/test-cases/)).
