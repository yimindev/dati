# FAQ

## Data Source Connection

### Q: Connection test failed?

1. Check if the JDBC connection string is correct
2. Verify the database is network accessible (use `host.docker.internal` instead of `localhost` if running in Docker)
3. Verify username and password
4. Ensure remote access is enabled on the database

### Q: Which databases are supported?

Currently supports MySQL, PostgreSQL, and H2. More databases are being added.

## MCP Services

### Q: Can I modify a published service?

Modifications to a published service create a draft copy. The original service continues running. Republish to apply changes.

### Q: What is the MCP endpoint format?

```
http://your-domain/mcp/{service-code}
```

For example, with service code `user-analysis`:

```
http://your-domain/mcp/user-analysis
```

### Q: What's the difference between Tools and Prompts?

| | Tool | Prompt |
|---|---|---|
| **Purpose** | Data query interface for LLMs | Context template for LLMs |
| **Output** | SQL execution result | Rendered text |
| **Protocol** | `tools/list`, `tools/call` | `prompts/get` |

## Template Syntax

### Q: Template rendering result is incorrect?

1. Use the "Preview" button in the Tool editor
2. Check that parameter names match those in the template
3. Verify parameter types match the expected SQL types

### Q: How to debug complex conditional templates?

Consider breaking complex templates into multiple simple Tools, testing each conditional branch separately.
