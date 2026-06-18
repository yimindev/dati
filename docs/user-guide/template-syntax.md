# 模板语法

::: v-pre
在 MCP 服务的 Tool 和 Prompt 中，可以使用模板语法来动态生成 SQL 或 Prompt 内容。

## 基本语法

使用 `{{参数名}}` 作为占位符，运行时会替换为实际值。

```sql
SELECT * FROM users WHERE id = {{userId}}
```

当 `userId` = `123` 时，渲染结果为：

```sql
SELECT * FROM users WHERE id = 123
```

## 模板参数

### 定义参数

在 Tool 配置的「参数列表」中定义参数：

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| userId | INTEGER | 是 | 用户 ID |
| status | STRING | 否 | 状态过滤 |

### 可选参数

可选参数未传值时，对应的占位符会被替换为空字符串。

```sql
SELECT * FROM orders WHERE 1=1 {{status}}
```

当 `status` 未传值时渲染为：

```sql
SELECT * FROM orders WHERE 1=1
```

## 条件渲染

使用 `{{#if 参数名}}...{{/if}}` 实现条件渲染：

```sql
SELECT * FROM products
WHERE 1=1
{{#if categoryId}} AND category_id = {{categoryId}} {{/if}}
{{#if minPrice}} AND price >= {{minPrice}} {{/if}}
```

## 循环渲染

使用 `{{#each 参数名}}...{{/each}}` 实现循环：

```sql
SELECT * FROM users WHERE id IN ({{#each ids}}{{this}}{{#unless @last}}, {{/unless}}{{/each}})
```

当 `ids` = `[1, 2, 3]` 时渲染为：

```sql
SELECT * FROM users WHERE id IN (1, 2, 3)
```

## Prompt 模板

Prompt 模板语法与 Tool 相同，用于动态生成给 LLM 的提示词：

```
你是一个数据分析助手。
请分析以下 {{tableName}} 表的数据：
{{#if focusFields}}
重点关注字段：{{focusFields}}
{{/if}}
```

## 注意事项

- 参数名区分大小写
- 字符串类型参数会自动添加引号
- 请在 Tool 编辑器中点击「测试渲染」验证模板效果
:::
