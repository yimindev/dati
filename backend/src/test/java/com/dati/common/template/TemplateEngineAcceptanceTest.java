package com.dati.common.template;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("模板引擎验收测试")
class TemplateEngineAcceptanceTest {

    private TemplateParser parser;
    private TextRenderer textRenderer;
    private SqlRenderer sqlRenderer;

    @BeforeEach
    void setUp() {
        parser = new HandlebarsStyleParser();
        textRenderer = new TextRenderer();
        sqlRenderer = new SqlRenderer();
    }

    // ========== Parser ==========

    @Test @DisplayName("解析简单变量 {{name}}")
    void parseSimpleVar() {
        assertEquals(Set.of("name"), parser.parse("{{name}}").getVariables());
    }

    @Test @DisplayName("解析 {{#if}} 块")
    void parseIfBlock() {
        assertEquals(Set.of("x"), parser.parse("{{#if x}}text{{/if}}").getVariables());
    }

    @Test @DisplayName("未闭合标签抛异常")
    void parseUnclosedBraces() {
        assertThrows(TemplateParseException.class, () -> parser.parse("{{unclosed"));
    }

    @Test @DisplayName("{{#if}} 缺少闭合抛异常")
    void parseUnclosedIf() {
        assertThrows(TemplateParseException.class, () -> parser.parse("{{#if x}}"));
    }

    @Test @DisplayName("未知指令抛异常")
    void parseUnknownDirective() {
        assertThrows(TemplateParseException.class, () -> parser.parse("{{#unknown}}"));
    }

    // ========== TextRenderer ==========

    @Test @DisplayName("变量替换：'Hello {{name}}' → 'Hello World'")
    void renderTextSimpleVar() {
        assertEquals("Hello World",
            textRenderer.render(parser.parse("Hello {{name}}"), Map.of("name", "World")));
    }

    @Test @DisplayName("{{#if}} 缺失时块消失：'前缀{{#if x}}内容{{/if}}后缀' → '前缀后缀'")
    void renderTextIfSkipped() {
        assertEquals("前缀后缀",
            textRenderer.render(parser.parse("前缀{{#if x}}内容{{/if}}后缀"), Map.of()));
    }

    @Test @DisplayName("{{#where}} 全部跳过 → block 消失")
    void renderTextWhereAllSkipped() {
        assertEquals("SELECT * FROM tasks ",
            textRenderer.render(parser.parse("SELECT * FROM tasks {{#where}}{{#if status}}AND status = {{status}}{{/if}}{{/where}}"), Map.of()));
    }

    // ========== SqlRenderer ==========

    @Test @DisplayName("变量绑定：'WHERE id = {{id}}' → 'WHERE id = ?'，binding=(id, 1)")
    void renderSqlSimpleVar() {
        PreparedSql r = sqlRenderer.render(parser.parse("WHERE id = {{id}}"), Map.of("id", 1));
        assertEquals("WHERE id = ?", r.sql());
        assertEquals(1, r.bindings().size());
        assertEquals("id", r.bindings().getFirst().name());
        assertEquals(1, r.bindings().getFirst().value());
    }

    @Test @DisplayName("{{#if}} 缺失时 SQL 片段消失")
    void renderSqlIfSkipped() {
        PreparedSql r = sqlRenderer.render(
            parser.parse("SELECT * FROM tasks {{#if status}}WHERE status = {{status}}{{/if}}"), Map.of());
        assertEquals("SELECT * FROM tasks ", r.sql());
        assertTrue(r.bindings().isEmpty());
    }

    @Test @DisplayName("{{#where}} 全部跳过 → WHERE 消失")
    void renderSqlWhereAllSkipped() {
        PreparedSql r = sqlRenderer.render(
            parser.parse("SELECT * FROM tasks {{#where}}{{#if status}}AND status = {{status}}{{/if}}{{/where}}"), Map.of());
        assertEquals("SELECT * FROM tasks ", r.sql());
        assertTrue(r.bindings().isEmpty());
    }

    @Test @DisplayName("{{#where}} 首个 AND 被裁剪，且保留正常空格")
    void renderSqlWhereAndStripped() {
        PreparedSql r = sqlRenderer.render(
            parser.parse("SELECT * FROM tasks {{#where}}AND status = {{status}} {{#if author}}AND author = {{author}}{{/if}}{{/where}}"),
            Map.of("author", "alice"));
        assertEquals("SELECT * FROM tasks WHERE status = ? AND author = ?", r.sql());
    }

    @Test @DisplayName("{{#where}} 直接条件无 AND，首个非 AND/OR 原样输出")
    void renderSqlWhereNoPrefix() {
        PreparedSql r = sqlRenderer.render(
            parser.parse("SELECT * FROM tasks {{#where}}status = {{status}}{{/where}}"), Map.of("status", "active"));
        assertEquals("SELECT * FROM tasks WHERE status = ?", r.sql());
    }

    @Test @DisplayName("Array 展开：'IN ({{ids}})' → 'IN (?, ?, ?)'，3 bindings")
    void renderSqlArrayExpand() {
        PreparedSql r = sqlRenderer.render(parser.parse("IN ({{ids}})"), Map.of("ids", List.of(1, 2, 3)));
        assertEquals("IN (?, ?, ?)", r.sql());
        assertEquals(3, r.bindings().size());
        assertEquals(1, r.bindings().get(0).value());
        assertEquals(2, r.bindings().get(1).value());
        assertEquals(3, r.bindings().get(2).value());
    }

    // ========== 块标签首尾换行剥离 ==========

    @Test
    @DisplayName("Text — 多行 {{#if}} 不产生多余空行，缩进和体内换行保留")
    void renderTextMultilineIfPreservesIndentAndInnerNewlines() {
        String rendered = textRenderer.render(
            parser.parse("""
                你是一个数据分析助手。
                请根据以下上下文进行分析：
                {{#if context}}
                  - 数据源：{{source}}
                  - 时间范围：{{period}}
                {{/if}}
                开始分析。"""),
            Map.of("context", true, "source", "my_db", "period", "last_7_days"));
        assertEquals("""
                你是一个数据分析助手。
                请根据以下上下文进行分析：
                  - 数据源：my_db
                  - 时间范围：last_7_days
                开始分析。""", rendered);
    }

    @Test
    @DisplayName("SQL — 多行 {{#if}} 不产生多余空行")
    void renderSqlMultilineIfNoBlankLine() {
        PreparedSql r = sqlRenderer.render(
            parser.parse("""
                SELECT * FROM orders
                {{#if status}}
                WHERE status = {{status}}
                {{/if}}
                ORDER BY created_at DESC"""),
            Map.of("status", "active"));
        assertEquals("""
                SELECT * FROM orders
                WHERE status = ?
                ORDER BY created_at DESC""", r.sql());
        assertEquals(1, r.bindings().size());
        assertEquals("active", r.bindings().getFirst().value());
    }

    @Test
    @DisplayName("SQL — 多行 {{#where}} 不产生多余空行，AND 裁剪正常")
    void renderSqlMultilineWhereNoBlankLine() {
        PreparedSql r = sqlRenderer.render(
            parser.parse("""
                SELECT * FROM orders
                {{#where}}
                AND status = {{status}}
                {{/where}}
                ORDER BY created_at DESC"""),
            Map.of("status", "active"));
        assertEquals("""
                SELECT * FROM orders
                WHERE status = ?
                ORDER BY created_at DESC""", r.sql());
    }

    @Test
    @DisplayName("SQL — 纯内联 {{#if}} 不受影响（无前后换行的回归）")
    void renderSqlInlineIfUnchanged() {
        PreparedSql r = sqlRenderer.render(
            parser.parse("SELECT * FROM tasks WHERE status = {{status}}{{#if level}} AND level = {{level}}{{/if}}"),
            Map.of("status", "active", "level", 3));
        assertEquals("SELECT * FROM tasks WHERE status = ? AND level = ?", r.sql());
    }

    // ========== 嵌套 {{#if}} ==========

    @Test @DisplayName("嵌套 {{#if}} 解析成功")
    void parseNestedIf() {
        CompiledTemplate t = parser.parse("SELECT * FROM tasks {{#if status}}{{#if author}}WHERE author = {{author}}{{/if}}{{/if}}");
        assertEquals(Set.of("status", "author"), t.getVariables());
    }

    @Test @DisplayName("TextRenderer 嵌套 if 全部成立")
    void renderTextNestedIfAllTrue() {
        assertEquals("xyz",
            textRenderer.render(parser.parse("{{#if a}}x{{#if b}}y{{#if c}}z{{/if}}{{/if}}{{/if}}"),
                Map.of("a", 1, "b", 1, "c", 1)));
    }

    @Test @DisplayName("TextRenderer 嵌套 if 内层跳过")
    void renderTextNestedIfPartial() {
        assertEquals("x",
            textRenderer.render(parser.parse("{{#if a}}x{{#if b}}y{{/if}}{{/if}}"), Map.of("a", 1)));
    }

    @Test @DisplayName("SqlRenderer 嵌套 if 全部成立")
    void renderSqlNestedIfAllTrue() {
        PreparedSql r = sqlRenderer.render(
            parser.parse("SELECT * FROM tasks {{#if status}}{{#if level}}WHERE level = {{level}}{{/if}}{{/if}}"),
            Map.of("status", "active", "level", 3));
        assertEquals("SELECT * FROM tasks WHERE level = ?", r.sql());
        assertEquals(1, r.bindings().size());
        assertEquals(3, r.bindings().getFirst().value());
    }

    @Test @DisplayName("SqlRenderer 嵌套 if 外层成立、内层跳过")
    void renderSqlNestedIfPartial() {
        PreparedSql r = sqlRenderer.render(
            parser.parse("SELECT * FROM tasks {{#if status}}WHERE status = {{status}}{{#if level}} AND level = {{level}}{{/if}}{{/if}}"),
            Map.of("status", "active"));
        assertEquals("SELECT * FROM tasks WHERE status = ?", r.sql());
        assertEquals(1, r.bindings().size());
    }
}
