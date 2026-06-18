# Template Syntax

::: v-pre
In MCP Service Tools and Prompts, template syntax can be used to dynamically generate SQL or Prompt content.

## Basic Syntax

Use `{{parameterName}}` as placeholders, which will be replaced with actual values at runtime.

```sql
SELECT * FROM users WHERE id = {{userId}}
```

When `userId` = `123`, the rendered result is:

```sql
SELECT * FROM users WHERE id = 123
```

## Template Parameters

### Defining Parameters

Define parameters in the Tool configuration's "Parameters" section:

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| userId | INTEGER | Yes | User ID |
| status | STRING | No | Status filter |

### Optional Parameters

When an optional parameter has no value, the corresponding placeholder is replaced with an empty string.

```sql
SELECT * FROM orders WHERE 1=1 {{status}}
```

When `status` is not provided, it renders as:

```sql
SELECT * FROM orders WHERE 1=1
```

## Conditional Rendering

Use `{{#if parameterName}}...{{/if}}` for conditional rendering:

```sql
SELECT * FROM products
WHERE 1=1
{{#if categoryId}} AND category_id = {{categoryId}} {{/if}}
{{#if minPrice}} AND price >= {{minPrice}} {{/if}}
```

## Loops

Use `{{#each parameterName}}...{{/each}}` for iteration:

```sql
SELECT * FROM users WHERE id IN ({{#each ids}}{{this}}{{#unless @last}}, {{/unless}}{{/each}})
```

When `ids` = `[1, 2, 3]`, it renders as:

```sql
SELECT * FROM users WHERE id IN (1, 2, 3)
```

## Prompt Templates

Prompt template syntax is the same as Tool templates, used to dynamically generate prompts for LLMs:

```
You are a data analysis assistant.
Analyze the following {{tableName}} table data:
{{#if focusFields}}
Focus fields: {{focusFields}}
{{/if}}
```

## Notes

- Parameter names are case-sensitive
- String parameters are automatically quoted
- Click "Preview" in the Tool editor to verify template rendering
:::
