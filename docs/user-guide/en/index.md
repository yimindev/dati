# Introduction

DatI is a lightweight enterprise semantic gateway connecting **AI Agents with enterprise databases** — by integrating databases, configuring semantic metadata, defining tools and prompts, and publishing standard MCP services to enable LLMs to query and manipulate business data accurately and securely.

![DatI Architecture](/images/dati-visual-positioning.svg)

## Why DatI?

1. **Broad Database Support**: Native support for relational and analytical databases including MySQL, PostgreSQL, ClickHouse, Doris, and more.
2. **Business Semantic Enhancement**: Unified business terms, column aliases, and automated enum dictionary value extraction combined with semantic search, solving the challenge of LLMs failing to understand business jargon or finding the wrong tables.
3. **Flexible Agent Integration**: Built on standard [Model Context Protocol (MCP)](https://modelcontextprotocol.io/) (Streamable HTTP) for seamless integration with various Agents or any MCP Host.
4. **High-Efficiency Building & Multi-User Reuse**: Out-of-the-box prebuilt tools (metadata inspection, SQL execution) and flexible parameterized templates allow publishing functional services within minutes, supporting multi-user and multi-agent high-concurrency reuse.
5. **Enterprise Security Control**: Centrally encrypted and managed database credentials, combined with user context to support fine-grained permission isolation.

## Use Cases

- **Conversational BI & NL2SQL**: Connect operational databases and enable natural-language-to-SQL analytics workflows through business metadata configuration and prebuilt inspection/query tools.
- **Lightweight App Development**: Encapsulate databases as controlled MCP services so agents can perform CRUD operations directly via conversation, rapidly prototyping lightweight data applications.

## Next Steps

- Go to [Quick Start](/en/quickstart) to publish your first DatI service
- Learn about [Template Syntax](/en/template-syntax)
- Browse [FAQ](/en/faq)
