# Template Syntax

::: v-pre
In MCP service custom tools (parameterized SQL) and Prompts, you can use template syntax to dynamically generate SQL or text content.

## Basic Syntax

Use `{{paramName}}` as a placeholder, replaced with the actual value at runtime.

```sql
SELECT * FROM users WHERE id = {{userId}}
```

When `userId` = `123`, the SQL-mode render becomes (parameter binding, SQL-injection safe):

```sql
SELECT * FROM users WHERE id = ?
```

## Parameter Types

| Type | Description | Example |
|------|-------------|---------|
| String | Text value | `'East China'` |
| Number | Numeric value | `42` |
| Boolean | Boolean value | `true` / `false` |
| DateTime | Date-time (ISO 8601) | `2026-08-14T10:00:00` |
| Array | Array value | `[1, 2, 3]` |

In SQL mode, values are formatted by type: strings are quoted, numbers/booleans are not, null renders as `NULL`, and arrays expand into multiple placeholders.

## Variable Syntax

### Safe Variable `{{var}}`

- **SQL mode**: converted to a `?` placeholder + parameter binding, preventing SQL injection (recommended for values)
- **Text mode** (Prompt): replaced directly with the string; no value → empty string

### Raw Variable `{{{var}}}`

Inlined directly into the output text without parameter binding. Use only for SQL identifiers that cannot be parameterized (table names, column names, etc.):

```sql
SELECT * FROM {{{tableName}}}
```

### Default Value `{{var:default}}`

The default is used when the parameter has no value:

```sql
LIMIT {{limit:100}}
```

## Built-in System Parameters

In parameterized SQL, you can use built-in system parameters starting with `_`. These are securely injected by the server at runtime (no need to define them in parameter lists; LLMs cannot manipulate them):

| System Parameter | Description | Example |
| :--- | :--- | :--- |
| `{{_user.id}}` | Current caller's user ID | `usr_0194a2b3...` |
| `{{_user.name}}` | Current caller's username | `admin` |
| `{{_user.display_name}}` | Current caller's display name | `Alice` |
| `{{_now}}` | Current timestamp (`yyyy-MM-dd HH:mm:ss`) | `2026-08-22 15:30:00` |
| `{{_date}}` | Current date (`yyyy-MM-dd`) | `2026-08-22` |

**Example:**
```sql
SELECT * FROM orders
WHERE owner_id = {{_user.id}}
  AND created_at <= {{_now}}
```

> [!TIP]
> - System parameters also support default values, e.g. `{{_user.id:anonymous}}`. If the user is unauthenticated or context is missing, it falls back to the default value.
> - Typing `{{_` in the SQL editor automatically triggers autocompletion for system parameters.

## Conditional Rendering

`{{#if paramName}}...{{/if}}`: the content is dropped when the parameter is null, otherwise rendered (null check only, no nesting):

```sql
SELECT * FROM products
WHERE 1=1
{{#if categoryId}}AND category_id = {{categoryId}}{{/if}}
```

When `categoryId` is not provided, it renders as:

```sql
SELECT * FROM products
WHERE 1=1
```

## Smart WHERE

`{{#where}}...{{/where}}` (SQL mode only): if all conditions inside are skipped, the whole block disappears; otherwise a `WHERE` prefix is added and the leading `AND` / `OR` is trimmed:

```sql
SELECT * FROM orders
{{#where}}
{{#if status}}AND status = {{status}}{{/if}}
{{#if minPrice}}AND price >= {{minPrice}}{{/if}}
{{/where}}
```

When `status` is not provided and `minPrice` = `100`, it renders as:

```sql
SELECT * FROM orders
WHERE price >= ?
```

## Escaping

`\{{` outputs a literal `{{`.

## Prompt Templates

Prompt templates use text-mode rendering with the same syntax:

```
You are a data analysis assistant.
Analyze the data in the {{tableName}} table:
{{#if focusFields}}
Focus fields: {{focusFields}}
{{/if}}
```

## Notes

- Parameter names are case-sensitive
- Missing parameters do not raise errors; they are treated as null
- Use the "Test Render" button in the Tool editor to verify template output
:::
