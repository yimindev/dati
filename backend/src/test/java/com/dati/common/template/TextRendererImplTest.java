package com.dati.common.template;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertEquals;



@DisplayName("TextRenderer 单元测试")
class TextRendererImplTest {

    private HandlebarsStyleParser parser;
    private TextRenderer renderer;

    @BeforeEach
    void setUp() {
        parser = new HandlebarsStyleParser();
        renderer = new TextRendererImpl();
    }

    static <K, V> Map<K, V> mapOf(K k1, V v1) {
        Map<K, V> m = new HashMap<>();
        m.put(k1, v1);
        return m;
    }

    // ---- 纯文本 ----
    @Test @DisplayName("纯文本 → 原样输出")
    void testPlainText() { assertEquals("Hello", renderer.render(parser.parse("Hello"), Map.of())); }

    @Test @DisplayName("空模板 → 空字符串")
    void testEmpty() { assertEquals("", renderer.render(parser.parse(""), Map.of())); }

    // ---- 简单变量替换 ----
    @Test @DisplayName("{{var}} → 值替换")
    void testSimpleVar() { assertEquals("Hello World", renderer.render(parser.parse("Hello {{name}}"), Map.of("name", "World"))); }

    @Test @DisplayName("{{var}} Number → toString()")
    void testNumberVar() { assertEquals("LIMIT 100", renderer.render(parser.parse("LIMIT {{n}}"), Map.of("n", 100))); }

    @Test @DisplayName("{{var}} Boolean → toString()")
    void testBooleanVar() { assertEquals("flag=true", renderer.render(parser.parse("flag={{f}}"), Map.of("f", true))); }

    // ---- null / 缺值 ----
    @Test @DisplayName("{{var}} = null → 空字符串")
    void testNullVar() { assertEquals("Hello ", renderer.render(parser.parse("Hello {{name}}"), mapOf("name", null))); }

    @Test @DisplayName("{{var}} 不在 params → 空字符串")
    void testMissingVar() { assertEquals("Hello ", renderer.render(parser.parse("Hello {{name}}"), Map.of())); }

    // ---- 默认值 ----
    @Test @DisplayName("{{var:default}} null → default")
    void testDefaultNull() { assertEquals("LIMIT 20", renderer.render(parser.parse("LIMIT {{limit:20}}"), mapOf("limit", null))); }

    @Test @DisplayName("{{var:default}} missing → default")
    void testDefaultMissing() { assertEquals("LIMIT 20", renderer.render(parser.parse("LIMIT {{limit:20}}"), Map.of())); }

    @Test @DisplayName("{{var:default}} 有值 → 用实际值")
    void testDefaultOverridden() { assertEquals("LIMIT 50", renderer.render(parser.parse("LIMIT {{limit:20}}"), Map.of("limit", 50))); }

    // ---- {{#if}} ----
    @Test @DisplayName("{{#if}} true → body 渲染")
    void testIfTrue() { assertEquals("Hello World", renderer.render(parser.parse("{{#if name}}Hello {{name}}{{/if}}"), Map.of("name", "World"))); }

    @Test @DisplayName("{{#if}} null → 块消失")
    void testIfNull() { assertEquals("pre  suf", renderer.render(parser.parse("pre {{#if f}}SHOWN{{/if}} suf"), mapOf("f", null))); }

    @Test @DisplayName("{{#if}} missing → 块消失")
    void testIfMissing() { assertEquals("pre  suf", renderer.render(parser.parse("pre {{#if f}}SHOWN{{/if}} suf"), Map.of())); }

    @Test @DisplayName("{{#if}} 条件为 0（非 null）→ body 渲染")
    void testIfZeroIsTruthy() { assertEquals("p=0", renderer.render(parser.parse("{{#if p}}p={{p}}{{/if}}"), Map.of("p", 0))); }

    @Test @DisplayName("{{#if}} 条件为 false（非 null）→ body 渲染")
    void testIfFalseIsTruthy() { assertEquals("shown", renderer.render(parser.parse("{{#if f}}shown{{/if}}"), Map.of("f", false))); }

    @Test @DisplayName("{{#if}} 条件为空字符串（非 null）→ body 渲染")
    void testIfEmptyStringTruthy() { assertEquals("shown", renderer.render(parser.parse("{{#if s}}shown{{/if}}"), Map.of("s", ""))); }

    // ---- {{#where}} ----
    @Test @DisplayName("{{#where}} 全跳过 → block 消失")
    void testWhereAllSkipped() {
        CompiledTemplate t = parser.parse("SELECT * FROM t {{#where}}{{#if s}}AND s={{s}}{{/if}}{{/where}}");
        assertEquals("SELECT * FROM t ", renderer.render(t, Map.of()));
    }

    @Test @DisplayName("{{#where}} 有内容 → body 原样输出，无 WHERE 前缀")
    void testWhereBodyRendered() {
        CompiledTemplate t = parser.parse("SELECT * FROM t {{#where}}  AND d={{d}} {{#if s}}AND s={{s}}{{/if}}{{/where}}");
        assertEquals("SELECT * FROM t AND d=123 AND s=active", renderer.render(t, Map.of("d", 123, "s", "active")));
    }

    @Test @DisplayName("{{#where}} body 首 OR 不裁剪")
    void testWhereOrNotStripped() {
        CompiledTemplate t = parser.parse("SELECT {{#where}}  OR a={{a}} OR b={{b}}{{/where}}");
        assertEquals("SELECT OR a=1 OR b=2", renderer.render(t, Map.of("a", 1, "b", 2)));
    }

    @Test @DisplayName("{{#where}} 无 AND/OR 前缀 → body 原样")
    void testWhereNoPrefix() {
        CompiledTemplate t = parser.parse("SELECT {{#where}}d={{d}}{{/where}}");
        assertEquals("SELECT d=5", renderer.render(t, Map.of("d", 5)));
    }

    // ---- Array → toString() ----
    @Test @DisplayName("Array → toString()")
    void testArrayToString() {
        assertEquals("ids: [1, 2, 3]", renderer.render(parser.parse("ids: {{ids}}"), Map.of("ids", List.of(1, 2, 3))));
    }

    // ---- 综合 ----
    @Test @DisplayName("完整 Prompt 模板")
    void testFullPrompt() {
        CompiledTemplate t = parser.parse("请分析 {{table}} 表。{{#if ctx}}补充：{{ctx}}{{/if}} 最多 {{limit:100}} 条。");
        String result = renderer.render(t, Map.of("table", "tasks", "ctx", "华东区"));
        assertEquals("请分析 tasks 表。补充：华东区 最多 100 条。", result);
    }

    // ---- 边界 ----
    @Test @DisplayName("仅 {{#if}} 成立 → body")
    void testOnlyIfTrue() { assertEquals("hi", renderer.render(parser.parse("{{#if x}}hi{{/if}}"), Map.of("x", 1))); }

    @Test @DisplayName("仅 {{#if}} 不成立 → 空")
    void testOnlyIfFalse() { assertEquals("", renderer.render(parser.parse("{{#if x}}hi{{/if}}"), Map.of())); }

    @Test @DisplayName("{{#if}} 内变量，但 if 不成立 → 变量的缺值不暴露")
    void testVarInSkippedIf() { assertEquals("", renderer.render(parser.parse("{{#if flag}}{{missing}}{{/if}}"), Map.of())); }
}
