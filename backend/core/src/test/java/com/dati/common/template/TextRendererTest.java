package com.dati.common.template;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import static java.util.Collections.singletonMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;



@DisplayName("TextRenderer unit tests")
class TextRendererTest {

    private HandlebarsStyleParser parser;
    private TextRenderer renderer;

    @BeforeEach
    void setUp() {
        parser = new HandlebarsStyleParser();
        renderer = new TextRenderer();
    }



    // ---- 纯文本 ----
    @Test @DisplayName("plain text → output as-is")
    void testPlainText() { assertEquals("Hello", renderer.render(parser.parse("Hello"), Map.of())); }

    // ---- 转义 ----
    @Test @DisplayName("\\{{not_var}} {{real_var}} → literal {{not_var}}")
    void testEscapeLiteral() {
        assertEquals("{{not_var}} hello",
            renderer.render(parser.parse("\\{{not_var}} {{real_var}}"), Map.of("real_var", "hello")));
    }

    @Test @DisplayName("multiple escaped \\{{")
    void testMultipleEscapes() {
        assertEquals("{{a}} {{b}} c",
            renderer.render(parser.parse("\\{{a}} \\{{b}} {{c}}"), Map.of("c", "c")));
    }

    @Test @DisplayName("empty template → empty string")
    void testEmpty() { assertEquals("", renderer.render(parser.parse(""), Map.of())); }

    // ---- 简单变量替换 ----
    @Test @DisplayName("{{var}} → value substituted")
    void testSimpleVar() { assertEquals("Hello World", renderer.render(parser.parse("Hello {{name}}"), Map.of("name", "World"))); }

    @Test @DisplayName("{{var}} Number → toString()")
    void testNumberVar() { assertEquals("LIMIT 100", renderer.render(parser.parse("LIMIT {{n}}"), Map.of("n", 100))); }

    @Test @DisplayName("{{var}} Boolean → toString()")
    void testBooleanVar() { assertEquals("flag=true", renderer.render(parser.parse("flag={{f}}"), Map.of("f", true))); }

    // ---- null / 缺值 ----
    @Test @DisplayName("{{var}} = null → empty string")
    void testNullVar() { assertEquals("Hello ", renderer.render(parser.parse("Hello {{name}}"), Collections.singletonMap("name", null))); }

    @Test @DisplayName("{{var}} missing from params → empty string")
    void testMissingVar() { assertEquals("Hello ", renderer.render(parser.parse("Hello {{name}}"), Map.of())); }

    // ---- 默认值 ----
    @Test @DisplayName("{{var:default}} null → default")
    void testDefaultNull() { assertEquals("LIMIT 20", renderer.render(parser.parse("LIMIT {{limit:20}}"), Collections.singletonMap("limit", null))); }

    @Test @DisplayName("{{var:default}} missing → default")
    void testDefaultMissing() { assertEquals("LIMIT 20", renderer.render(parser.parse("LIMIT {{limit:20}}"), Map.of())); }

    @Test @DisplayName("{{var:default}} with value → uses actual value")
    void testDefaultOverridden() { assertEquals("LIMIT 50", renderer.render(parser.parse("LIMIT {{limit:20}}"), Map.of("limit", 50))); }

    // ---- {{#if}} ----
    // ---- 嵌套 {{#if}} ----
    @Test @DisplayName("nested {{#if}} both true → inner rendered")
    void testNestedIfBothTrue() {
        assertEquals("inner",
            renderer.render(parser.parse("{{#if a}}{{#if b}}inner{{/if}}{{/if}}"), Map.of("a", 1, "b", 1)));
    }

    @Test @DisplayName("nested {{#if}} outer true inner skipped → empty")
    void testNestedIfInnerSkipped() {
        assertEquals("",
            renderer.render(parser.parse("{{#if a}}{{#if b}}inner{{/if}}{{/if}}"), Map.of("a", 1)));
    }

    @Test @DisplayName("nested {{#if}} outer skipped → whole block gone")
    void testNestedIfOuterSkipped() {
        assertEquals("",
            renderer.render(parser.parse("{{#if a}}{{#if b}}inner{{/if}}{{/if}}"), Map.of()));
    }

    @Test @DisplayName("nested {{#if}} inner variable substituted")
    void testNestedIfInnerVar() {
        assertEquals("x=5",
            renderer.render(parser.parse("{{#if a}}{{#if b}}x={{b}}{{/if}}{{/if}}"), Map.of("a", 1, "b", 5)));
    }

    @Test @DisplayName("nested {{#if}} three levels")
    void testNestedIfThreeLevels() {
        assertEquals("deep",
            renderer.render(parser.parse("{{#if a}}{{#if b}}{{#if c}}deep{{/if}}{{/if}}{{/if}}"),
                Map.of("a", 1, "b", 1, "c", 1)));
    }

    @Test @DisplayName("{{#if}} true → body rendered")
    void testIfTrue() { assertEquals("Hello World", renderer.render(parser.parse("{{#if name}}Hello {{name}}{{/if}}"), Map.of("name", "World"))); }

    @Test @DisplayName("{{#if}} null → block gone")
    void testIfNull() { assertEquals("pre  suf", renderer.render(parser.parse("pre {{#if f}}SHOWN{{/if}} suf"), Collections.singletonMap("f", null))); }

    @Test @DisplayName("{{#if}} missing → block gone")
    void testIfMissing() { assertEquals("pre  suf", renderer.render(parser.parse("pre {{#if f}}SHOWN{{/if}} suf"), Map.of())); }

    @Test @DisplayName("{{#if}} condition 0 (non-null) → body rendered")
    void testIfZeroIsTruthy() { assertEquals("p=0", renderer.render(parser.parse("{{#if p}}p={{p}}{{/if}}"), Map.of("p", 0))); }

    @Test @DisplayName("{{#if}} condition false (non-null) → body rendered")
    void testIfFalseIsTruthy() { assertEquals("shown", renderer.render(parser.parse("{{#if f}}shown{{/if}}"), Map.of("f", false))); }

    @Test @DisplayName("{{#if}} empty string condition → body skipped (falsy)")
    void testIfEmptyStringFalsy() { assertEquals("", renderer.render(parser.parse("{{#if s}}shown{{/if}}"), Map.of("s", ""))); }

    @Test @DisplayName("{{#if}} empty collection condition → body skipped")
    void testIfEmptyListFalsy() { assertEquals("", renderer.render(parser.parse("{{#if ids}}shown{{/if}}"), Map.of("ids", List.of()))); }

    @Test @DisplayName("{{#if}} condition 0 → truthy (SQL semantics)")
    void testIfZeroTruthy() { assertEquals("shown", renderer.render(parser.parse("{{#if n}}shown{{/if}}"), Map.of("n", 0))); }

    @Test @DisplayName("{{#if}} condition false → truthy (SQL semantics)")
    void testIfFalseTruthy() { assertEquals("shown", renderer.render(parser.parse("{{#if flag}}shown{{/if}}"), Map.of("flag", false))); }

    // ---- {{#where}} ----
    @Test @DisplayName("{{#where}} all skipped → block gone")
    void testWhereAllSkipped() {
        CompiledTemplate t = parser.parse("SELECT * FROM t {{#where}}{{#if s}}AND s={{s}}{{/if}}{{/where}}");
        assertEquals("SELECT * FROM t ", renderer.render(t, Map.of()));
    }

    @Test @DisplayName("{{#where}} with content → body output as-is, no WHERE prefix")
    void testWhereBodyRendered() {
        CompiledTemplate t = parser.parse("SELECT * FROM t {{#where}}  AND d={{d}} {{#if s}}AND s={{s}}{{/if}}{{/where}}");
        assertEquals("SELECT * FROM t AND d=123 AND s=active", renderer.render(t, Map.of("d", 123, "s", "active")));
    }

    @Test @DisplayName("{{#where}} leading OR in body not trimmed")
    void testWhereOrNotStripped() {
        CompiledTemplate t = parser.parse("SELECT {{#where}}  OR a={{a}} OR b={{b}}{{/where}}");
        assertEquals("SELECT OR a=1 OR b=2", renderer.render(t, Map.of("a", 1, "b", 2)));
    }

    @Test @DisplayName("{{#where}} without AND/OR prefix → body as-is")
    void testWhereNoPrefix() {
        CompiledTemplate t = parser.parse("SELECT {{#where}}d={{d}}{{/where}}");
        assertEquals("SELECT d=5", renderer.render(t, Map.of("d", 5)));
    }

    @Test @DisplayName("{{#where}} with multiple {{#if}}, partial skips produce no blank lines")
    void testWhereMultipleIfNoBlankLines() {
        CompiledTemplate t = parser.parse("""
                查询结果如下：
                {{#where}}
                  {{#if city}}
                    城市：{{city}}
                  {{/if}}
                 {{#if num}}
                    数量：{{num}}
                 {{/if}}
                日期：{{invocedate}}
                {{/where}}""");
        String result = renderer.render(t, Map.of("city", "Beijing", "invocedate", "2024-01-01"));
        assertFalse(result.matches("(?s).*\\n\\s*\\n.*"), "不应有空行: " + result);
    }

    // ---- Array → toString() ----
    @Test @DisplayName("Array → toString()")
    void testArrayToString() {
        assertEquals("ids: [1, 2, 3]", renderer.render(parser.parse("ids: {{ids}}"), Map.of("ids", List.of(1, 2, 3))));
    }

    // ---- 综合 ----
    @Test @DisplayName("full Prompt template")
    void testFullPrompt() {
        CompiledTemplate t = parser.parse("请分析 {{table}} 表。{{#if ctx}}补充：{{ctx}}{{/if}} 最多 {{limit:100}} 条。");
        String result = renderer.render(t, Map.of("table", "tasks", "ctx", "华东区"));
        assertEquals("请分析 tasks 表。补充：华东区 最多 100 条。", result);
    }

    // ---- 边界 ----
    @Test @DisplayName("only {{#if}} true → body")
    void testOnlyIfTrue() { assertEquals("hi", renderer.render(parser.parse("{{#if x}}hi{{/if}}"), Map.of("x", 1))); }

    @Test @DisplayName("only {{#if}} false → empty")
    void testOnlyIfFalse() { assertEquals("", renderer.render(parser.parse("{{#if x}}hi{{/if}}"), Map.of())); }

    @Test @DisplayName("{{#if}} inner variable with false condition → missing value not exposed")
    void testVarInSkippedIf() { assertEquals("", renderer.render(parser.parse("{{#if flag}}{{missing}}{{/if}}"), Map.of())); }

    // ── {{{var}}} 在 Text 模式下与 {{var}} 行为一致 ──

    @Test @DisplayName("{{{var}}} → behaves like {{var}}")
    void testRawVarSameAsNormal() {
        assertEquals("Hello World",
            renderer.render(parser.parse("Hello {{{name}}}"), Map.of("name", "World")));
    }

    @Test @DisplayName("{{{var}}} null → empty string")
    void testRawVarNull() {
        assertEquals("Hello ",
            renderer.render(parser.parse("Hello {{{name}}}"), singletonMap("name", null)));
    }

    @Test @DisplayName("{{{var}}} missing → empty string")
    void testRawVarMissing() {
        assertEquals("Hello ",
            renderer.render(parser.parse("Hello {{{name}}}"), Map.of()));
    }

    @Test @DisplayName("{{{var:default}}} null → default")
    void testRawVarDefault() {
        assertEquals("LIMIT 20",
            renderer.render(parser.parse("LIMIT {{{limit:20}}}"), singletonMap("limit", null)));
    }

    @Test @DisplayName("{{{var}}} Array → .toString()")
    void testRawVarArrayToString() {
        assertEquals("Result: [1, 2, 3]",
            renderer.render(parser.parse("Result: {{{items}}}"), Map.of("items", List.of(1, 2, 3))));
    }
}
