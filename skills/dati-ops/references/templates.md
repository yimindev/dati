# 模板语法参考(Handlebars 风格)

> 用于**自定义工具 SQL 模板**与 **prompt 内容**,两者共用同一套模板引擎(HandlebarsStyleParser + SqlRenderer)。

## 语法一览

| 语法 | 含义 | 示例 |
|---|---|---|
| `{{name}}` | 变量插值(双花括号) | `WHERE type = {{type}}` |
| `{{name:default}}` | 变量插值,参数缺失时用默认值 | `{{limit:100}}` |
| `{{{name}}}` | **raw 原样拼接**(三花括号,不做参数绑定) | `{{{table_name}}}` |
| `{{#if name}}...{{/if}}` | 条件块:参数有值(truthy)才包含 | `{{#if note}}note = {{note}}{{/if}}` |
| `{{#where}}...{{/where}}` | 智能 WHERE 块(见下) | `{{#where}}AND a.name = {{account}}{{/where}}` |
| `\{{` / `\{{{` | 转义为字面 `{{` / `{{{` | — |

变量名规则:`[A-Za-z0-9_.]+`(支持点号,如 `_user.name`)。

## 双花括号 vs 三花括号(安全关键)

- **`{{name}}`:参数化绑定** → 渲染为 PreparedStatement 占位符 `?`,值通过参数绑定传入。**防 SQL 注入,默认都应该用它**
- **`{{{name}}}`:raw 拼接** → 值直接拼进 SQL 文本。**仅用于可信值**(如服务端已知的标识符);参数来自 LLM 时使用有注入风险
- **数组值**:`{{tags}}` 传数组时展开为 `?, ?, ?`(配合 `IN ({{ids}})` 使用);`{{{tags}}}` 遇数组直接报错

## 条件块与 WHERE 块

- `{{#if name}}`:`name` 有值(非 null、非空串)时渲染块内内容;用于可选字段的 INSERT/UPDATE
- `{{#where}}...{{/where}}`:**智能 WHERE**——块内容非空时自动加 `WHERE` 前缀,并自动裁剪开头的 `AND`/`OR`;块内全部变量为空时整个 WHERE 消失。适合动态查询条件:

```sql
SELECT * FROM transaction
{{#where}}
  AND type = {{type}}
  {{#if account}}AND account_id = (SELECT id FROM account WHERE name = {{account}}){{/if}}
{{/where}}
```

## 系统变量(服务端自动注入)

| 变量 | 含义 | 示例值 |
|---|---|---|
| `_user.id` | 当前认证用户 ID | `c90c07a5-...` |
| `_user.name` | 当前用户名 | `zhangsan` |
| `_user.display_name` | 显示名(可能为 null) | `张三` |
| `_now` | 当前时间,`yyyy-MM-dd HH:mm:ss` | `2026-08-27 10:30:00` |
| `_date` | 当前日期,`yyyy-MM-dd` | `2026-08-27` |

**典型用途**:数据归属绑定(`WHERE user_name = {{_user.name}}`)、审计字段(`created_by = {{_user.name}}`)、时间戳(`updated_at = {{_now}}`)。实现"责任到人、防越权"的关键机制。

## 使用注意

- 参数**缺失且无默认值**时:双花括号渲染为 `?` 并绑定 null(可能匹配不到行);三花括号渲染为空
- 写 SQL 模板前用 `POST /v1/template/preview` 预览渲染结果、`POST /v1/template/extract` 提取变量列表,验证模板正确
- prompt 模板语法相同,`{{parameter}}` 由客户端调用时传入;prompt 参数声明见 `McpPromptRequest.parameters`(name/description/required)
