# 模板引擎（Template Engine）

## 概述

`com.dati.common.template` 是一个零外部依赖的模板引擎，采用自定义 Handlebars 风格语法（`{{ }}`）。核心特点是**解析与渲染分离**，同一套 AST 支持两种渲染模式：文本替换（Prompt）和参数化 SQL 绑定。

## 包结构

```
com.dati.common.template/
├── TemplateParser.java          // 解析接口（public）
├── TextRenderer.java            // 文本渲染接口（public）
├── SqlRenderer.java             // SQL 渲染接口（public）
├── CompiledTemplate.java        // 编译结果接口，getVariables()（public）
├── PreparedSql.java             // 参数化 SQL 产物（public record）
├── ParamBinding.java            // 单个参数绑定（public record）
├── ParsedTemplate.java          // CompiledTemplate 实现（package-private）
│
├── Node.java                    // AST 根类型（sealed interface）
├── TextNode.java                // 文本节点
├── VarNode.java                 // 变量节点
├── IfNode.java                  // 条件块节点
├── WhereNode.java               // 智能 WHERE 块节点
│
├── HandlebarsStyleParser.java   // 解析器实现（@Component）
├── TextRendererImpl.java        // 文本渲染实现（@Component）
├── SqlRendererImpl.java         // SQL 渲染实现（@Component）
│
├── TemplateParseException.java  // 解析异常
└── TemplateRenderException.java // 渲染异常
```

## 核心流程

```
模板文本
    │
    ▼
HandlebarsStyleParser.parse()
    │
    ▼
CompiledTemplate (AST: List<Node>)
    │
    ├──► TextRenderer.render() → String
    │
    └──► SqlRenderer.render()  → PreparedSql(sql, bindings)
```

## 语法支持（V1）

| 语法 | 示例 | 说明 |
|------|------|------|
| 变量 | `{{var}}` `{{var:default}}` | 有值→替换，无值→空字符串/null |
| 条件块 | `{{#if var}}...{{/if}}` | 仅 `== null` 判断，不支持嵌套 |
| 智能 WHERE | `{{#where}}...{{/where}}` | 全部跳过→消失；首个 AND/OR→裁剪 |
| Array 展开 | `AND id IN ({{ids}})` | SQL 模式自动展开为 `?, ?, ?` |
| 转义 | `\{{` | 字面量 `{{` |

## 渲染行为对比

| 场景 | TextRenderer | SqlRenderer |
|------|-------------|-------------|
| `{{var}}` 值存在 | `value.toString()` 替换 | `?` 占位，值加入 bindings |
| `{{var}}` 值为 null | 输出空字符串 | `?` 占位，绑定 null |
| `{{var:default}}` 值为 null | 替换为 default 字符串 | `?` 占位，绑定 default |
| `{{#if var}}` 值为 null | 内容丢弃 | 内容丢弃 |
| `{{#where}}` 全部跳过 | block 消失 | block 消失（无 WHERE） |
| `{{#where}}` 有内容 | 原样输出 body | `WHERE` + body（首个 AND/OR 裁剪） |
| Array 值 | `.toString()` | 展开为 `?, ?, ...` |

## 关键设计决策

1. **零外部依赖**：语法元素少（4 个），Parser 约 150 行，不需要引入 Handlebars.java 或 MyBatis。
2. **Parser + Renderer 分离**：同一套 AST 支持 Text/SQL 两种模式。
3. **Lenient**：参数缺失不抛异常（等同于 null），`{{#if}}` 只做 `== null` 判断（无 falsy 语义）。
4. **Parser 不感知 SQL**：单一职责，引号/注释内的 `{{ }}` 同样解析。
5. **不缓存 CompiledTemplate**：模板规模小，解析耗时远低于 DB IO。
6. **PreparedSql 不碰 JDBC**：消费方自行 `prepareStatement()` + 遍历 bindings 绑定。
7. **`{{#where}}` 智能行为仅限 SQL 模式**：Text 模式下只输出 body 内容，不处理 WHERE 前缀和 AND/OR 裁剪。

## 错误处理

| 错误 | 异常 |
|------|------|
| `{{` 不闭合 | TemplateParseException |
| `{{#if}}` 缺少 `{{/if}}` | TemplateParseException |
| 多余 `{{/if}}` / `{{/where}}` | TemplateParseException |
| 未知块指令（`{{#foo}}`） | TemplateParseException |
| 变量名不合法（`{{a b}}`） | TemplateParseException |
| 参数缺失 | 不抛异常，等同于 null |

## 测试

- 测试类：`com.dati.common.template.*`（共 145+ 个测试用例）
- 全部为纯 JUnit 单元测试，不依赖数据库、不启动 Spring 容器
- 覆盖 Parser 语法元素、TextRenderer/SqlRenderer 渲染行为、验收标准
