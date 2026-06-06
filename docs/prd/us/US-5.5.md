## US-5.5：模板引擎基础设施

### 概述

实现一个协议无关、零外部依赖的模板引擎，作为 `com.dati.common.template` 基础设施组件。引擎采用自定义类 Handlebars 语法（`{{ }}`），Parser 与 Renderer 分离，同一套 AST 支持文本渲染（Prompt）和 SQL 渲染（参数化 SQL）两种模式。

提供两种变量语义：
- **`{{var}}`** — 安全变量。Text 模式做字符串替换；SQL 模式转为 `?` 占位符 + `ParamBinding`（PreparedStatement 参数化，防注入）。
- **`{{{var}}}`** — 原始变量。直接内联值到输出字符串，不经过 `?` / `ParamBinding`。用于表名、列名、排序方向等无法参数化的标识符场景。对应 Handlebars 的 triple-stash raw output 语义。

核心原则：
- **解析与渲染分离**：`TemplateParser` 只负责模板文本 → AST，不接触参数值
- **两模式渲染**：`TextRenderer` 做字符串替换，`SqlRenderer` 做 `?` 占位符 + PreparedStatement 参数绑定
- **引擎 lenient**：参数缺失不抛异常，`{{#if}}` 的开关判断仅基于 `== null`
- **不缓存**：每次渲染都从 `parse()` 开始，模板规模小，解析耗时远低于 DB IO

### 语法规范（V1）

```sql
-- 安全变量（有值 → 替换或绑定，无值 → Text 空字符串 / SQL 绑定 null）
{{var}}
{{var:default}}           -- 值为 null 时使用默认值

-- 原始变量（三重大括号，直接内联值到输出，不做 ? 占位）
{{{var}}}
{{{var:default}}}         -- 值为 null 时使用默认值

-- 条件块（不支持嵌套）
{{#if var}}
  ...                     -- var != null 时保留
{{/if}}

-- 智能 WHERE 块
{{#where}}
  dept_id = {{dept_id}}   -- 任意内容（{{#if}} 可选）
  {{#if status}}AND status = {{status}}{{/if}}
{{/where}}
-- 所有子条件都不成立 → WHERE 消失
-- 有内容且首个为 AND/OR → 裁剪

-- Array 展开（仅 SQL 模式，仅 {{var}} 有效，自动检测 List/数组类型）
AND dept_id IN ({{dept_ids}})   -- → IN (?, ?, ?)

-- 转义：\{{  →  字面量 {{ ； \{{{  →  字面量 {{{
```

**约束**：
- 变量名：`[A-Za-z0-9_.]+`
- `{{#if}}` ：不支持嵌套，不支持表达式（仅 `== null` 判断），不支持 falsy 语义
- `{{#where}}`：V1 仅此一个智能块；`{{#set}}` / `{{#trim}}` 放 V2
- 未知块指令 → `TemplateParseException`
- Parser 不感知 SQL 语法（引号/注释内的 `{{}}` 同样被解析）
- `{{{var}}}` 仅在 SQL 模式下行为与 `{{var}}` 不同（直接内联 vs `?` 占位）；Text 模式下两者行为一致
- `{{{var}}}` 值为 Array → `TemplateRenderException`（数组无法有意义地直接拼接）
- `{{{var}}}` 是用户显式选择的安全退让——值直接拼入 SQL，消费方应确保值来源可信

### 包结构

```
com.dati.common.template/
├── TemplateParser.java          // public 接口
├── TextRenderer.java            // public 接口
├── SqlRenderer.java             // public 接口
├── CompiledTemplate.java        // public 接口，getVariables()
├── PreparedSql.java             // public record
├── ParamBinding.java            // public record
│
├── ast/                         // package-private record
│   ├── Node.java                // sealed interface
│   ├── TextNode.java
│   ├── VarNode.java
│   ├── IfNode.java
│   └── WhereNode.java
│
├── parser/
│   └── HandlebarsStyleParser.java  // package-private
│
├── renderer/
│   ├── TextRendererImpl.java       // package-private
│   └── SqlRendererImpl.java        // package-private
│
└── exception/
    ├── TemplateParseException.java
    └── TemplateRenderException.java
```

### 核心 API

```java
public interface TemplateParser {
    CompiledTemplate parse(String template) throws TemplateParseException;
}

public interface CompiledTemplate {
    Set<String> getVariables();
}

public interface TextRenderer {
    String render(CompiledTemplate compiled, Map<String, Object> params);
}

public interface SqlRenderer {
    PreparedSql render(CompiledTemplate compiled, Map<String, Object> params);
}

public record PreparedSql(String sql, List<ParamBinding> bindings) {}
public record ParamBinding(String name, Object value) {}
```

- `TemplateParser` / `TextRenderer` / `SqlRenderer` 均为 Spring `@Component` 单例，构造注入
- `PreparedSql` 是最终产物，引擎不碰 JDBC，消费方自行 `connection.prepareStatement()` + 遍历 `bindings` 绑定

### AST 定义

```java
sealed interface Node permits TextNode, VarNode, IfNode, WhereNode {}

record TextNode(String text) implements Node {}
record VarNode(String name, String defaultValue, boolean raw) implements Node {}
record IfNode(String condition, List<Node> body) implements Node {}
record WhereNode(List<Node> body) implements Node {}
```

- 所有节点不可变
- `IfNode.condition` 和 `VarNode.name` 均为纯 String，不额外封装

### 渲染行为

| 场景 | TextRenderer | SqlRenderer |
|---|---|---|
| `{{var}}`，值存在 | `value.toString()` 替换 | `?` 占位，值加入 `bindings` |
| `{{var}}`，值为 null | 输出空字符串 | `?` 占位，绑定 null |
| `{{var:default}}`，值为 null | 替换为 default 字符串 | `?` 占位，绑定 default（作为值） |
| `{{{var}}}`，值存在 | `value.toString()` 替换 | 直接内联 `value.toString()` 到 SQL 字符串 |
| `{{{var}}}`，值为 null | 输出空字符串 | 输出空字符串 |
| `{{{var:default}}}`，值为 null | 替换为 default 字符串 | 内联 default 字符串 |
| `{{{var}}}` 值为 Array | `.toString()`（如 `[1,2,3]`） | `TemplateRenderException` |
| `{{#if var}}`，值 == null | 内容丢弃 | 内容丢弃 |
| `{{#if var}}`，值 != null | 内容保留并继续渲染 | 内容保留并继续渲染 |
| `{{#where}}`，全部跳过 | 整个块消失 | 整个块消失（无 WHERE） |
| `{{#where}}`，有内容，首个以 AND/OR 开头 | 裁剪首个 AND/OR，输出 `WHERE ...` | 裁剪首个 AND/OR，输出 `WHERE ...` |
| `{{#where}}`，有内容，首个非 AND/OR | 原样输出 `WHERE ...` | 原样输出 `WHERE ...` |
| `{{var}}` 值为 Array | `.toString()`（如 `[1,2,3]`） | 展开为 `?, ?, ...` |
| 参数在 Map 中不存在 | 等同于 null | 等同于 null |

### 错误处理

| 错误类型 | 行为 |
|---|---|
| `{{` 不闭合 | `TemplateParseException` |
| `{{{` 不闭合 | `TemplateParseException` |
| `{{#if}}` 无对应 `{{/if}}` | `TemplateParseException` |
| `{{#where}}` 无对应 `{{/where}}` | `TemplateParseException` |
| 多余 `{{/if}}` / `{{/where}}` | `TemplateParseException` |
| 未知块指令（如 `{{#foo}}`） | `TemplateParseException` |
| 参数缺失（`{{var}}` 在 Map 中找不到） | 不抛异常，等同于 null |
| 参数缺失（`{{{var}}}` 在 Map 中找不到） | 不抛异常，等同于 null（输出空字符串） |
| `{{#if}}` 的 condition 在 Map 中找不到 | 不抛异常，等同于 null（跳过块） |
| `{{{var}}}` 值为 Array（仅在 SqlRenderer） | `TemplateRenderException` |

### 包依赖

```
com.dati.common.template
  依赖：JDK 17+（sealed interface）
  被依赖：com.dati.mcp.server（MCP 适配层）

不依赖 Spring Framework、JDBC、或任何第三方模板库
```

### 测试策略

| 测试层 | 覆盖内容 |
|---|---|
| Parser 单测 | 所有语法元素、`{{var}}` / `{{{var}}}` / `{{{var:default}}}`、未闭合 `{{` / `{{{`、多余 `{{/if}}`、未知指令、`\{{` / `\{{{` 转义、默认值语法、空模板、仅纯文本模板 |
| TextRenderer 单测 | `{{var}}` 替换、`{{{var}}}` 替换（与 `{{var}}` 行为一致）、`{{var:default}}` 回退、`{{#if}}` 开关、`{{#where}}` 块处理、Array `.toString()`、参数缺失容错 |
| SqlRenderer 单测 | `?` 占位符正确性、参数绑定顺序、`{{{var}}}` 内联到 SQL、`{{{var}}}` Array 抛异常、`{{{var}}}` null 输出空字符串、`{{{var:default}}}` 回退、`{{#if}}` 开关对 SQL 片段的影响、`{{#where}}` 裁剪 AND/OR + WHERE 消失、Array 展开为 `?, ?, ...` |

全部为纯 JUnit 单元测试，不依赖数据库、不启动 Spring 容器。

### 对现有 US 的影响

| US | 影响 |
|---|---|
| **US-03（Tool 配置）** | Parameterized SQL 模板语法从 `:paramName` 改为 `{{paramName}}`，保存时可用 `TemplateParser.parse()` 做语法校验，执行时用 `SqlRenderer.render()` 替代手写参数绑定逻辑 |
| **US-05（Prompt 配置）** | 后端校验改用 `TemplateParser.parse()` + `getVariables()` 做一致性检查，`prompts/get` 渲染改用 `TextRenderer.render()` |
| **PRD 正文** | 7.7 节「`:paramName`」和 12 节「`:paramName`」更新为「`{{paramName}}`」 |

### 验收标准

1. `TemplateParser.parse("{{name}}")` 成功，`getVariables()` 返回 `["name"]`
2. `TemplateParser.parse("{{{name}}}")` 成功，`getVariables()` 返回 `["name"]`
3. `TemplateParser.parse("{{#if x}}text{{/if}}")` 成功，`getVariables()` 返回 `["x"]`
4. `TemplateParser.parse("{{unclosed")` 抛 `TemplateParseException`
5. `TemplateParser.parse("{{{unclosed")` 抛 `TemplateParseException`
6. `TemplateParser.parse("{{#if x}}")` 抛 `TemplateParseException`（缺少 `{{/if}}`）
7. `TemplateParser.parse("{{#unknown}}")` 抛 `TemplateParseException`
8. `TextRenderer.render("Hello {{name}}", {"name": "World"})` → `"Hello World"`
9. `TextRenderer.render("Hello {{{name}}}", {"name": "World"})` → `"Hello World"`（与 `{{name}}` 行为一致）
10. `TextRenderer.render("{{#if x}}shown{{/if}}", {})` → `""` （x 缺失 → 跳过）
11. `SqlRenderer.render("WHERE id = {{id}}", {"id": 1})` → `PreparedSql{sql="WHERE id = ?", bindings=[ParamBinding("id", 1)]}`
12. `SqlRenderer.render("WHERE id = {{{id}}}", {"id": 1})` → `PreparedSql{sql="WHERE id = 1", bindings=[]}`
13. `SqlRenderer.render("FROM {{{table}}}", {"table": "users"})` → `PreparedSql{sql="FROM users", bindings=[]}`
14. `SqlRenderer.render("ORDER BY {{{col}}} {{{dir}}}", {"col": "name", "dir": "DESC"})` → `PreparedSql{sql="ORDER BY name DESC", bindings=[]}`
15. `SqlRenderer.render("{{#if status}}AND status = {{status}}{{/if}}", {})` → 内容消失
16. `SqlRenderer.render("{{#where}}{{#if s}}AND s = {{s}}{{/if}}{{/where}}", {})` → `PreparedSql{sql="", bindings=[]}` (WHERE 消失)
17. `SqlRenderer.render("{{#where}}AND s = {{s}}{{#if a}}AND a = {{a}}{{/if}}{{/where}}", {"a": 1})` → 首个 AND 被裁剪
18. `SqlRenderer.render("IN ({{ids}})", {"ids": [1,2,3]})` → `PreparedSql{sql="IN (?, ?, ?)", bindings=[ParamBinding("ids", 1), ...]}`
19. `SqlRenderer.render("IN ({{{ids}}})", {"ids": [1,2,3]})` → 抛 `TemplateRenderException`

---

### 设计决策记录

| # | 决策 | 理由 |
|---|---|---|
| 1 | 自定义引擎，零外部依赖 | V1 语法子集很小（4 个语法元素），Parser 约 200 行，不需要引入 Handlebars.java 或 MyBatis |
| 2 | Parser + Renderer 分离 | 同一套 AST 支持 Text/SQL 两种渲染模式，引擎对上层协议无感知 |
| 3 | 语法统一 `{{ }}` | Prompt 和 SQL 用户只学一种占位语法；Monaco/CodeMirror 原生支持 Mustache 模式高亮 |
| 4 | `{{#if}}` 仅 `== null` 判断 | SQL 场景中 `0`、`""`、`false` 都是合法查询条件；falsy 语义会导致非预期行为 |
| 5 | 参数缺失不抛异常 | 引擎不知道哪些参数是「必填」的——这是 US-03/US-05 校验层的职责 |
| 6 | Parser 不感知 SQL 语法 | 单一职责：Parser 只管 `{{ }}` 模板语法；加 SQL 感知等于绑定到一种外层语言 |
| 7 | Array 引擎自动检测 | 用户不需要额外语法标记；行为由渲染模式自然区分 |
| 8 | 不缓存 CompiledTemplate | 模板规模小，解析耗时远低于 DB IO；缓存带来的失效/内存管理复杂度远超收益 |
| 9 | `PreparedSql` 不碰 JDBC | 引擎是纯文本处理层，JDBC 连接管理是消费方的职责 |
| 10 | `{{#where}}` 内部允许任意内容 | 必填参数裸写在 `{{#where}}` 内是合理场景 |
| 11 | `{{{var}}}` 直接内联到 SQL | SQL 中的表名、列名、排序方向等标识符无法通过 `?` 占位（JDBC 限制），必须直接拼接。对齐 Handlebars triple-stash 语义，用户显式选择三重大括号即接受 SQL 注入风险 |
