# 模板语法

::: v-pre
在 MCP 服务的自定义工具（参数化 SQL）和 Prompt 中，可以使用模板语法动态生成 SQL 或文本内容。

## 基本语法

使用 `{{参数名}}` 作为占位符，运行时替换为实际值。

```sql
SELECT * FROM users WHERE id = {{userId}}
```

当 `userId` = `123` 时，SQL 模式渲染为（参数绑定，防 SQL 注入）：

```sql
SELECT * FROM users WHERE id = ?
```

## 参数类型

| 类型 | 说明 | 示例 |
|------|------|------|
| String | 字符串 | `'华东'` |
| Number | 数值 | `42` |
| Boolean | 布尔 | `true` / `false` |
| DateTime | 日期时间（ISO 8601） | `2026-08-14T10:00:00` |
| Array | 数组 | `[1, 2, 3]` |

SQL 模式下值按类型格式化：字符串自动加引号，数值/布尔不加引号，空值渲染为 `NULL`，数组自动展开为多个占位符。

## 变量语法

### 安全变量 `{{var}}`

- **SQL 模式**：转为 `?` 占位符 + 参数绑定，防止 SQL 注入（推荐用于值）
- **文本模式**（Prompt）：直接替换为字符串；无值 → 空字符串

### 原始变量 `{{{var}}}`

直接内联到输出文本，不做参数绑定。仅用于无法参数化的 SQL 标识符（表名、列名等）：

```sql
SELECT * FROM {{{tableName}}}
```

### 默认值 `{{var:default}}`

参数无值时使用默认值：

```sql
LIMIT {{limit:100}}
```

## 条件渲染

`{{#if 参数名}}...{{/if}}`：参数值为 null 时丢弃内容，否则渲染（仅支持 null 判断，不支持嵌套）：

```sql
SELECT * FROM products
WHERE 1=1
{{#if categoryId}}AND category_id = {{categoryId}}{{/if}}
```

当 `categoryId` 未传值时渲染为：

```sql
SELECT * FROM products
WHERE 1=1
```

## 智能 WHERE

`{{#where}}...{{/where}}`（仅 SQL 模式生效）：块内全部条件被跳过时整块消失；有内容时自动添加 `WHERE` 前缀并裁剪开头的 `AND` / `OR`：

```sql
SELECT * FROM orders
{{#where}}
{{#if status}}AND status = {{status}}{{/if}}
{{#if minPrice}}AND price >= {{minPrice}}{{/if}}
{{/where}}
```

当 `status` 未传值、`minPrice` = `100` 时渲染为：

```sql
SELECT * FROM orders
WHERE price >= ?
```

## 转义

`\{{` 输出字面量 `{{`。

## Prompt 模板

Prompt 模板使用文本模式渲染，语法相同：

```
你是一个数据分析助手。
请分析 {{tableName}} 表的数据：
{{#if focusFields}}
重点关注字段：{{focusFields}}
{{/if}}
```

## 注意事项

- 参数名区分大小写
- 参数缺失不报错，等同于 null
- 可在 Tool 编辑器中点击「测试渲染」验证模板效果
:::
