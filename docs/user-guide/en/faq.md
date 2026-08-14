# FAQ

## Data Source Connection

### Q: Connection test failed?

1. Check if the JDBC connection string is correct
2. Verify the database is network accessible (use `host.docker.internal` instead of `localhost` if running in Docker)
3. Verify username and password
4. Ensure remote access is enabled on the database

### Q: Which databases are supported?

Supports 10 mainstream databases including MySQL, PostgreSQL, ClickHouse, and Oracle. More are being added.

## MCP Services

### Q: Can I modify a published service?

Modifications to a published service are saved as a draft; the live version keeps running. Click "Publish Changes" to create a new snapshot and apply the changes immediately. You can also disable, enable, or roll back to a previous version at any time.

### Q: What is the MCP endpoint URL format?

The MCP endpoint is a JSON-RPC over HTTP interface (Streamable HTTP, MCP protocol 2025-11-25):

```
POST http://your-domain/{service-code}/mcp
```

For example, with service code `user-analysis`, the endpoint URL is:

```
POST http://your-domain/user-analysis/mcp
```

Requests must carry the `MCP-Protocol-Version: 2025-11-25` header and authenticate at the application layer (JWT or API Key). The full endpoint URL can be copied from the service detail page.

### Q: What is the difference between Tool and Prompt?

| | Tool | Prompt |
|------|------|------|
| **Purpose** | Data query interface invoked by LLMs | Context template retrieved by LLMs |
| **Output** | SQL execution results | Rendered text |
| **Protocol** | `tools/list`, `tools/call` | `prompts/list`, `prompts/get` |

## Template Syntax

### Q: Template rendering result is wrong?

1. Use the "Test Render" button in the Tool editor to preview the result
2. Check that parameter names match those in the template
3. Confirm parameter types match the expected types in the SQL
4. See [Template Syntax](/en/template-syntax) for the behavior of each syntax element

### Q: How to debug complex conditional templates?

Break the complex template into several simple conditional blocks and test each branch separately.
