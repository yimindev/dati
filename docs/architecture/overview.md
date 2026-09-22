# Architecture Overview

[English](overview.md) | [简体中文](overview_zh.md)

This directory contains architecture documentation for DatI modules, maintained continuously alongside the codebase. We recommend reading in the order listed below.

## High-Level Architecture

![DatI Architecture Diagram](../images/dati-visual-positioning.svg)

- **Frontend** (`frontend/`): Vue 3 + Vite + TypeScript, Element Plus component library, TailwindCSS 4 styling, Vitest for unit testing.
- **Backend** (`backend/`): Spring Boot 3.5.x (Java 21), organized by DDD layers (`domain` / `repository` / `server`), exposing REST APIs, with JPA for metadata database access.
- **Database**: Embedded H2 file database for development (`./db/dati`), switchable to MySQL/PostgreSQL in production via configuration profiles.
- **Search Engine**: Elasticsearch powers semantic retrieval (`SemanticSearchDocument`).

## Module & Document Index

| Module | Document | Responsibilities |
|---|---|---|
| Authentication (auth) | [authentication.md](authentication.md) | Pluggable authentication (JWT / API Key), login, and request interception |
| Authorization (permission) | [permission.md](permission.md) | 3-tier permission evaluation (Admin → Owner → ACL), silent list filtering |
| Data Source (datasource) | [datasource.md](datasource.md) | Database connectivity, metadata probing, table/column management, column sample extraction & search |
| Semantic Management (semantic) | [semantic.md](semantic.md) | Business subjects, terms & relationships, Elasticsearch semantic search |
| MCP Services (mcp) | [mcp-service-management.md](mcp-service-management.md) | MCP service lifecycle, data scoping, built-in/parameterized tools, publishing & versioning, JSON-RPC endpoint, usage stats & invocation logging |
| Template Engine (common.template) | [template-engine.md](template-engine.md) | Zero-dependency template engine with TEXT and parameterized SQL rendering modes |
| Editor (frontend) | [editor.md](editor.md) | CodeMirror 6 template & SQL editor architecture and design decisions |

## Key Conventions

- Exceptions follow `DatiException` + `ErrorCode` enum (prefixes: `CM` Common, `DS` DataSource, `SM` Semantic, `MS` MCP, `AUTH` Auth, `PM` Permission).
- JSON naming strategy (dev profile): `SNAKE_CASE`, adapted via frontend API layer.
- MCP Endpoint: `POST /{code}/mcp` (Streamable HTTP JSON-RPC, 2025-11-25 spec). See section 2.2 of [mcp-service-management.md](mcp-service-management.md) for details.

## Local Development

For environment setup, startup commands, and database reset, see the [Local Development Guide](../development.md).
