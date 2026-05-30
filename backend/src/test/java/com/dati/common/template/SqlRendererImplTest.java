package com.dati.common.template;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SqlRenderer 单元测试")
class SqlRendererImplTest {

    private HandlebarsStyleParser parser;
    private SqlRenderer renderer;

    @BeforeEach
    void setUp() { parser = new HandlebarsStyleParser(); renderer = new SqlRendererImpl(); }

    // ---- 纯文本 ----
    @Test @DisplayName("纯文本 → 原样，无 binding")
    void testPlainText() {
        PreparedSql r = renderer.render(parser.parse("SELECT * FROM t"), Map.of());
        assertEquals("SELECT * FROM t", r.sql()); assertTrue(r.bindings().isEmpty());
    }

    @Test @DisplayName("空模板 → 空")
    void testEmpty() {
        PreparedSql r = renderer.render(parser.parse(""), Map.of());
        assertEquals("", r.sql()); assertTrue(r.bindings().isEmpty());
    }

    // ---- 简单变量 ----
    @Test @DisplayName("{{var}} → ? + binding")
    void testSimpleVar() {
        PreparedSql r = renderer.render(parser.parse("WHERE id = {{id}}"), Map.of("id", 42));
        assertEquals("WHERE id = ?", r.sql());
        assertEquals(1, r.bindings().size());
        assertEquals("id", r.bindings().getFirst().name());
        assertEquals(42, r.bindings().getFirst().value());
    }

    @Test @DisplayName("多个变量 → bindings 按模板顺序")
    void testMultipleVars() {
        PreparedSql r = renderer.render(parser.parse("{{t}} WHERE a={{a}} AND b={{b}}"), Map.of("t","tasks","a",1,"b",2));
        assertEquals("? WHERE a=? AND b=?", r.sql());
        assertEquals(3, r.bindings().size());
        assertEquals("t", r.bindings().get(0).name());
        assertEquals("a", r.bindings().get(1).name());
        assertEquals("b", r.bindings().get(2).name());
    }

    @Test @DisplayName("同一变量多次 → 每个出现独立 binding")
    void testDuplicateVar() {
        PreparedSql r = renderer.render(parser.parse("a={{x}} AND b={{x}}"), Map.of("x", 99));
        assertEquals("a=? AND b=?", r.sql());
        assertEquals(2, r.bindings().size());
    }

    // ---- null / 缺值 ----
    @Test @DisplayName("{{var}} = null → ? + null binding")
    void testNullVar() {
        PreparedSql r = renderer.render(parser.parse("WHERE c = {{c}}"), mapOf("c", null));
        assertEquals("WHERE c = ?", r.sql()); assertNull(r.bindings().getFirst().value());
    }

    @Test @DisplayName("{{var}} missing → ? + null binding")
    void testMissingVar() {
        PreparedSql r = renderer.render(parser.parse("WHERE c = {{c}}"), Map.of());
        assertEquals("WHERE c = ?", r.sql()); assertNull(r.bindings().getFirst().value());
    }

    // ---- 默认值 ----
    @Test @DisplayName("{{var:default}} null → binding = default")
    void testDefaultNull() {
        PreparedSql r = renderer.render(parser.parse("LIMIT {{limit:20}}"), mapOf("limit", null));
        assertEquals("LIMIT ?", r.sql()); assertEquals("20", r.bindings().getFirst().value());
    }

    @Test @DisplayName("{{var:default}} 有值 → binding = 实际值")
    void testDefaultOverridden() {
        PreparedSql r = renderer.render(parser.parse("LIMIT {{limit:20}}"), Map.of("limit", 50));
        assertEquals("LIMIT ?", r.sql()); assertEquals(50, r.bindings().getFirst().value());
    }

    // ---- {{#if}} ----
    @Test @DisplayName("{{#if}} true → body 出现，变量绑定")
    void testIfTrue() {
        PreparedSql r = renderer.render(parser.parse("WHERE 1=1 {{#if s}}AND s={{s}}{{/if}}"), Map.of("s", "active"));
        assertEquals("WHERE 1=1 AND s=?", r.sql());
        assertEquals("s", r.bindings().getFirst().name());
    }

    @Test @DisplayName("{{#if}} null → body 消失")
    void testIfNull() {
        PreparedSql r = renderer.render(parser.parse("WHERE 1=1 {{#if s}}AND s={{s}}{{/if}}"), mapOf("s", null));
        assertEquals("WHERE 1=1 ", r.sql()); assertTrue(r.bindings().isEmpty());
    }

    @Test @DisplayName("{{#if}} missing → body 消失")
    void testIfMissing() {
        PreparedSql r = renderer.render(parser.parse("WHERE 1=1 {{#if s}}AND s={{s}}{{/if}}"), Map.of());
        assertEquals("WHERE 1=1 ", r.sql()); assertTrue(r.bindings().isEmpty());
    }

    @Test @DisplayName("{{#if}} 决定 binding 是否出现")
    void testVarOnlyIfTrue() {
        PreparedSql r = renderer.render(parser.parse("{{#if a}}{{a}}{{/if}}{{#if b}}{{b}}{{/if}}"), Map.of("a", 1));
        assertEquals("?", r.sql()); assertEquals(1, r.bindings().size());
    }

    // ---- {{#where}} ----
    @Test @DisplayName("{{#where}} 全跳过 → WHERE 消失")
    void testWhereAllSkipped() {
        PreparedSql r = renderer.render(parser.parse("SELECT * FROM t {{#where}}{{#if s}}AND s={{s}}{{/if}}{{/where}}"), Map.of());
        assertEquals("SELECT * FROM t ", r.sql()); assertTrue(r.bindings().isEmpty());
    }

    @Test @DisplayName("{{#where}} 首个 AND 裁剪")
    void testWhereFirstAnd() {
        PreparedSql r = renderer.render(parser.parse("SELECT * FROM t {{#where}}  AND d={{d}} {{#if s}}AND s={{s}}{{/if}}{{/where}}"),
            Map.of("d", 123, "s", "active"));
        assertEquals("SELECT * FROM t WHERE d=? AND s=?", r.sql()); assertEquals(2, r.bindings().size());
    }

    @Test @DisplayName("{{#where}} 首个 OR 裁剪")
    void testWhereFirstOr() {
        PreparedSql r = renderer.render(parser.parse("SELECT {{#where}}  OR a={{a}} OR b={{b}}{{/where}}"), Map.of("a", 1, "b", 2));
        assertEquals("SELECT WHERE a=? OR b=?", r.sql());
    }

    @Test @DisplayName("{{#where}} 首个非 AND/OR → 原样")
    void testWhereNoPrefix() {
        PreparedSql r = renderer.render(parser.parse("SELECT {{#where}}d={{d}}{{/where}}"), Map.of("d", 5));
        assertEquals("SELECT WHERE d=?", r.sql());
    }

    @Test @DisplayName("{{#where}} 混合：直接内容 + {{#if}}，if 跳过")
    void testWhereMixedIfSkipped() {
        PreparedSql r = renderer.render(parser.parse("{{#where}}d={{d}}{{#if s}}AND s={{s}}{{/if}}{{/where}}"), Map.of("d", 10));
        assertEquals("WHERE d=?", r.sql()); assertEquals(1, r.bindings().size());
    }

    // ---- Array 展开 ----
    @Test @DisplayName("Array → 多个 ?")
    void testArray() {
        PreparedSql r = renderer.render(parser.parse("IN ({{ids}})"), Map.of("ids", List.of(1, 2, 3)));
        assertEquals("IN (?, ?, ?)", r.sql()); assertEquals(3, r.bindings().size());
    }

    @Test @DisplayName("空 Array → IN ()，无 binding")
    void testEmptyArray() {
        PreparedSql r = renderer.render(parser.parse("IN ({{ids}})"), Map.of("ids", List.of()));
        assertEquals("IN ()", r.sql()); assertTrue(r.bindings().isEmpty());
    }

    @Test @DisplayName("单元素 Array → 单 ?")
    void testSingleArray() {
        PreparedSql r = renderer.render(parser.parse("IN ({{ids}})"), Map.of("ids", List.of(42)));
        assertEquals("IN (?)", r.sql()); assertEquals(1, r.bindings().size());
    }

    @Test @DisplayName("非 Array 值 → 单 ?")
    void testNonArray() {
        PreparedSql r = renderer.render(parser.parse("WHERE id = {{id}}"), Map.of("id", 100));
        assertEquals("WHERE id = ?", r.sql()); assertEquals(1, r.bindings().size());
    }

    @Test @DisplayName("Array 与普通变量混合 → bindings 顺序正确")
    void testArrayMixed() {
        PreparedSql r = renderer.render(parser.parse("WHERE a={{a}} AND b IN ({{b}})"), Map.of("a", 1, "b", List.of(10,20)));
        assertEquals("WHERE a=? AND b IN (?, ?)", r.sql());
        assertEquals(3, r.bindings().size());
        assertEquals("a", r.bindings().get(0).name());
        assertEquals("b", r.bindings().get(1).name());
        assertEquals("b", r.bindings().get(2).name());
    }

    // ---- 综合 ----
    @Test @DisplayName("完整参数化 SQL：table + where + if + sort + limit")
    void testFullSql() {
        CompiledTemplate t = parser.parse(
            "SELECT * FROM {{table}} {{#where}}  {{#if s}}AND s={{s}}{{/if}}  {{#if p}}AND p={{p}}{{/if}}{{/where}} ORDER BY {{sort:id}} LIMIT {{limit:20}}");
        PreparedSql r = renderer.render(t, Map.of("table", "tasks", "s", "todo"));
        assertEquals("SELECT * FROM ? WHERE s=? ORDER BY ? LIMIT ?", r.sql());
        assertEquals(4, r.bindings().size());
        assertEquals("table", r.bindings().get(0).name());
        assertEquals("s", r.bindings().get(1).name());
        assertEquals("sort", r.bindings().get(2).name());
        assertEquals("id", r.bindings().get(2).value());
        assertEquals("limit", r.bindings().get(3).name());
        assertEquals("20", r.bindings().get(3).value());
    }

    // ---- SQL 注入防护 ----
    @Test @DisplayName("恶意值不在 SQL 字符串中")
    void testInjectionPrevention() {
        String evil = "' OR '1'='1' --";
        PreparedSql r = renderer.render(parser.parse("WHERE s = {{s}}"), Map.of("s", evil));
        assertEquals("WHERE s = ?", r.sql());
        assertFalse(r.sql().contains(evil));
        assertEquals(evil, r.bindings().getFirst().value());
    }

    @Test @DisplayName("表名注入防护")
    void testTableInjection() {
        String evil = "t; DROP TABLE users;--";
        PreparedSql r = renderer.render(parser.parse("SELECT * FROM {{t}}"), Map.of("t", evil));
        assertEquals("SELECT * FROM ?", r.sql());
        assertFalse(r.sql().contains("DROP"));
    }

    static <K, V> Map<K, V> mapOf(K k1, V v1) {
        Map<K, V> m = new HashMap<>();
        m.put(k1, v1);
        return m;
    }
}
