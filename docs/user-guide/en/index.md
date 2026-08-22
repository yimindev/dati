# Quick Start

DatI (Data Intelligence) is a platform that provides unified data access capabilities for LLMs. Connect to various data sources and automatically generate MCP (Model Context Protocol) compliant interfaces.

![DatI Architecture](/images/dati-visual-positioning.svg)

## Core Concepts

| Concept | Description |
|---------|-------------|
| **Data Source** | Database connection, supports MySQL, PostgreSQL, etc. |
| **Subject** | Business semantic modeling for tables, defining terms and relationships |
| **MCP Service** | Service exposing MCP interfaces, including Tools and Prompts |

## Getting Started

### 1. Create a Data Source

Go to "Data Source Management", click "Create Data Source", and fill in the connection details.

### 2. Create a Subject

Go to "Subjects", click "Create Subject", associate a data source, and select the tables you need.

### 3. Publish an MCP Service

Go to "MCP Services" and click "New Service":
1. Fill in name and service code
2. Configure data scope (select accessible data sources and subjects)
3. Configure Tools or Prompts
4. Click "Publish"

Once published, it can be called by LLMs via the MCP endpoint.

## Next Steps

- Learn about [Template Syntax](/en/template-syntax)
- Check [FAQ](/en/faq)
