# Introduction

DatI (Data Intelligence) is a lightweight semantic gateway connecting **AI Agents with enterprise databases**. Simply connect a database, configure semantic metadata, and enable prebuilt or parameterized SQL tools to publish an MCP service that connects with your agents or any MCP host.

![DatI Architecture](/images/dati-visual-positioning.svg)

## Why DatI?

1. **Multiple Databases**: Supports MySQL, PostgreSQL, ClickHouse, Doris, and other relational and analytical databases
2. **Semantic Enhancement**: Supports business terms, column aliases, and automatic enum dictionary extraction. Combined with semantic search, it helps models understand business jargon and find the right tables
3. **Flexible Integration**: Based on standard [MCP](https://modelcontextprotocol.io/) (Streamable HTTP), easily integrates into your existing agents or workflows
4. **Fast to Build**: Out-of-the-box prebuilt tools (metadata inspection, SQL execution) and parameterized SQL tools to publish MCP services without extra deployment
5. **Access Control**: Centrally manages credentials with support for user-level permission isolation

## Use Cases

- **Natural Language Data Analysis**: Connect business databases and support NL2SQL analysis workflows with metadata configuration and prebuilt tools
- **Lightweight App Development**: Wrap databases as MCP services so agents can query and update data through conversation to build lightweight applications

## Next Steps

- Go to [Quick Start](/en/quickstart) to publish your first DatI service
- Learn about [Template Syntax](/en/template-syntax)
- Browse [FAQ](/en/faq)
