# Quick Start

You can get started with DatI in two ways:
- **Option 1: Automated Operations via Agent** — Use the `dati-ops` skill to let an AI assistant configure data sources and publish services via conversation;
- **Option 2: Visual Configuration via Web Console** — Step through data source onboarding, subject scoping, and MCP service publishing in the browser console.

---

## Option 1: Automated Operations via dati-ops Skill

DatI natively provides the official **`dati-ops`** operational skill compliant with the [Agent Skills Open Standard](https://agentskills.io). You can mount this skill into AI coding assistants (such as OpenCode, Claude Code, Antigravity, etc.) to let agents manage data sources, semantic modeling, and MCP service publishing automatically via natural language instructions.

### 1. Prerequisites & API Key Configuration

Before using `dati-ops`, obtain an API Key and ideally export it as an environment variable:

1. **Generate API Key**: Log in to DatI, click **User Avatar -> API Key Management** in the top right, and click "New API Key";
2. **Configure Connection (Recommended)**: Export the environment variables in your terminal or shell profile (`~/.zshrc`, `~/.bashrc`, or workspace `.env`):
   ```bash
   export DATI_BASE_URL="http://localhost:8085"  # DatI backend URL (default: http://localhost:8085)
   export DATI_API_KEY="sk_your_api_key_here"    # Your generated API Key
   ```
   When environment variables are set, the Agent will automatically read them without needing you to pass credentials in every conversation.

> **Tip**: If environment variables are not set, you can also provide `baseUrl` and `apiKey` directly in your prompt to the Agent.

### 2. Skill Retrieval & Installation

* **Local Development**: Directly reference `skills/dati-ops/SKILL.md` in the repository;
* **Remote / Managed Environment**: Access the <a :href="$frontmatter.skillUrl" target="_blank" rel="noopener noreferrer">dati-ops Skill Repository</a> (or browse the <a :href="$frontmatter.skillsUrl" target="_blank" rel="noopener noreferrer">Skills Directory</a>) for the complete skill definition and utility scripts.

### 3. One-Prompt Automated Operations

Send your requirement directly to the Agent loaded with `dati-ops`:

> *"Connect my local MySQL order database (root/123456@localhost:3306/shop), sync the orders and order_items tables, create an 'E-Commerce Analytics' subject, and publish an MCP service."*

The Agent will automatically inspect OpenAPI specifications, test connectivity, batch sync tables and columns, extract enum values, establish subjects and terms, and publish a production-ready service!

---

## Option 2: Visual Configuration via Web Console

If you prefer configuring via the graphical interface, follow this standard workflow:

### 1. Connect Data Source & Metadata Extraction

Go to the **Data Sources** page and click **Create Data Source**:

1. Fill in connection details (MySQL, PostgreSQL, ClickHouse, Doris, etc.) and click **Test Connection**;
2. Save, then click **Add Tables** in the data source detail page to batch import physical tables;
3. Click **Sync Columns** on imported tables to fetch the latest table schema.

::: tip Core Concept: Why extract enum values?
LLMs cannot reliably guess business enums from raw column names (like `status`, `pay_type`).
Clicking **Extract Values** on enum columns enables DatI to inspect distinct values and record business mappings (e.g., `1 -> Pending`, `2 -> Completed`), drastically improving NL2SQL accuracy during semantic retrieval.
:::

### 2. Model Business Subjects & Terms

Go to the **Subjects** page and click **Create Subject**:

1. **Create Subject**: Select the associated data source, enter the subject name (e.g., "E-Commerce Analytics", "Financial Reconciliation"), and write a description;
2. **Add Tables (Narrow Down Scope)**: In the subject detail page under "Tables", click **Add Tables** to select only the core business tables relevant to this subject (e.g., selecting only `orders` and `order_items`);
3. **Define Terms & Relations**: In the "Terms" tab, click **New Term**, enter business terms or metrics (e.g., `GMV`, `Average Order Value`, `Active Users`), and link them to tables or specific fields (so matching terms return relevant schemas).

::: tip Core Concept: Why use subjects for "table scoping" and "semantic modeling"?
- **Precise Table Scoping**: An enterprise data source typically contains dozens or hundreds of tables (logs, system tables, sensitive tables). Exposing the entire database directly to an LLM consumes massive Prompt Tokens and leads to table-selection hallucinations. Subjects narrow down the scope to only the tables needed for a specific analytics domain, saving cost and increasing accuracy;
- **Decoupling Business Language from Physical Schema**: Users query using business language (e.g., "Check this month's GMV"), whereas physical database columns might be named `total_amount`. Term mapping connects business concepts to physical columns, allowing agents to align intent accurately via semantic search without guessing.
:::

### 3. Build & Publish MCP Service

Go to the **MCP Services** page and click **New Service**:

1. **Basic Info**: Enter a service name and unique `service_code` (used as the MCP endpoint URL path);
2. **Configure Data Scope**: Select the data sources and subjects accessible by this service;
3. **Configure Tools**:
   - **Prebuilt Tools**: Enable `inspect_schema` (metadata inspection) and `execute_sql` (safe read-only SQL execution);
   - **Custom Parameterized Tools**: Write template-driven parameterized SQL using context variables like `{{user_id}}` to prevent unauthorized cross-tenant data access;
4. **Publish**: Click **Publish** to deploy the service.

::: tip Core Concept: Data Scope Isolation & Version Snapshots
- **Data Scope Isolation**: An MCP service can strictly access only data sources and subjects explicitly authorized in its scope. Unauthorized tables remain completely invisible to agents.
- **Drafts & Version Snapshots**: Edits take place in draft mode without affecting production; clicking "Publish Changes" creates an immutable snapshot that takes effect immediately, with one-click rollback support.
:::

### 4. Connect from Agent Clients

DatI exposes endpoints using the standard **Model Context Protocol (Streamable HTTP)**. Once published, you can copy the client JSON config directly from the **Access Configuration Card** on the service detail page.

#### Client Configuration Example

For **OpenCode**, **Claude Code**, or **Cursor**, add the service to your MCP config:

```json
{
  "mcpServers": {
    "dati-order-service": {
      "url": "http://localhost:8085/order-analysis/mcp",
      "headers": {
        "Authorization": "Bearer <YOUR_API_KEY>"
      }
    }
  }
}
```

::: tip API Key Credential
Replace `<YOUR_API_KEY>` with your actual token. You can generate one via **Manage API Keys** at the bottom of the access card, or by clicking **User Avatar -> API Key Management** in the top right.
:::

Once configured, the agent will automatically discover tools for schema retrieval and data querying during conversations.
