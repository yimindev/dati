package com.dati.common.template;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonMap;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SqlRenderer unit tests")
class SqlRendererTest {

    private HandlebarsStyleParser parser;
    private SqlRenderer renderer;

    @BeforeEach
    void setUp() { parser = new HandlebarsStyleParser(); renderer = new SqlRenderer(); }

    // ---- 纯文本 ----
    @Test @DisplayName("plain text → as-is, no binding")
    void testPlainText() {
        PreparedSql r = renderer.render(parser.parse("SELECT * FROM t"), Map.of());
        assertEquals("SELECT * FROM t", r.sql()); assertTrue(r.bindings().isEmpty());
    }

    @Test @DisplayName("empty template → empty")
    void testEmpty() {
        PreparedSql r = renderer.render(parser.parse(""), Map.of());
        assertEquals("", r.sql()); assertTrue(r.bindings().isEmpty());
    }

    // ---- 转义 ----
    @Test @DisplayName("\\{{var}} → literal {{var}}, no binding")
    void testEscapeVar() {
        PreparedSql r = renderer.render(parser.parse("\\{{var}}"), Map.of());
        assertEquals("{{var}}", r.sql());
        assertTrue(r.bindings().isEmpty());
    }

    @Test @DisplayName("escaped \\{{}} mixed with real variables")
    void testEscapeMixed() {
        PreparedSql r = renderer.render(parser.parse("\\{{x}} = {{y}}"), Map.of("y", 42));
        assertEquals("{{x}} = ?", r.sql());
        assertEquals(1, r.bindings().size());
        assertEquals(42, r.bindings().getFirst().value());
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

    @Test @DisplayName("multiple variables → bindings in template order")
    void testMultipleVars() {
        PreparedSql r = renderer.render(parser.parse("{{t}} WHERE a={{a}} AND b={{b}}"), Map.of("t","tasks","a",1,"b",2));
        assertEquals("? WHERE a=? AND b=?", r.sql());
        assertEquals(3, r.bindings().size());
        assertEquals("t", r.bindings().get(0).name());
        assertEquals("a", r.bindings().get(1).name());
        assertEquals("b", r.bindings().get(2).name());
    }

    @Test @DisplayName("same variable repeated → independent binding per occurrence")
    void testDuplicateVar() {
        PreparedSql r = renderer.render(parser.parse("a={{x}} AND b={{x}}"), Map.of("x", 99));
        assertEquals("a=? AND b=?", r.sql());
        assertEquals(2, r.bindings().size());
    }

    // ---- null / 缺值 ----
    @Test @DisplayName("{{var}} = null → ? + null binding")
    void testNullVar() {
        PreparedSql r = renderer.render(parser.parse("WHERE c = {{c}}"), singletonMap("c", null));
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
        PreparedSql r = renderer.render(parser.parse("LIMIT {{limit:20}}"), singletonMap("limit", null));
        assertEquals("LIMIT ?", r.sql()); assertEquals("20", r.bindings().getFirst().value());
    }

    @Test @DisplayName("{{var:default}} with value → binding = actual value")
    void testDefaultOverridden() {
        PreparedSql r = renderer.render(parser.parse("LIMIT {{limit:20}}"), Map.of("limit", 50));
        assertEquals("LIMIT ?", r.sql()); assertEquals(50, r.bindings().getFirst().value());
    }

    // ---- {{#if}} ----
    @Test @DisplayName("{{#if}} true → body appears, variables bound")
    void testIfTrue() {
        PreparedSql r = renderer.render(parser.parse("WHERE 1=1 {{#if s}}AND s={{s}}{{/if}}"), Map.of("s", "active"));
        assertEquals("WHERE 1=1 AND s=?", r.sql());
        assertEquals("s", r.bindings().getFirst().name());
    }

    @Test @DisplayName("{{#if}} null → body gone")
    void testIfNull() {
        PreparedSql r = renderer.render(parser.parse("WHERE 1=1 {{#if s}}AND s={{s}}{{/if}}"), singletonMap("s", null));
        assertEquals("WHERE 1=1 ", r.sql()); assertTrue(r.bindings().isEmpty());
    }

    @Test @DisplayName("{{#if}} missing → body gone")
    void testIfMissing() {
        PreparedSql r = renderer.render(parser.parse("WHERE 1=1 {{#if s}}AND s={{s}}{{/if}}"), Map.of());
        assertEquals("WHERE 1=1 ", r.sql()); assertTrue(r.bindings().isEmpty());
    }

    @Test @DisplayName("{{#if}} decides whether binding appears")
    void testVarOnlyIfTrue() {
        PreparedSql r = renderer.render(parser.parse("{{#if a}}{{a}}{{/if}}{{#if b}}{{b}}{{/if}}"), Map.of("a", 1));
        assertEquals("?", r.sql()); assertEquals(1, r.bindings().size());
    }

    @Test @DisplayName("{{#if}} empty string condition → body skipped")
    void testIfEmptyStringFalsy() {
        PreparedSql r = renderer.render(parser.parse("WHERE 1=1 {{#if s}}AND s={{s}}{{/if}}"), Map.of("s", ""));
        assertEquals("WHERE 1=1 ", r.sql()); assertTrue(r.bindings().isEmpty());
    }

    @Test @DisplayName("{{#if}} empty collection condition → body skipped")
    void testIfEmptyListFalsy() {
        PreparedSql r = renderer.render(parser.parse("WHERE 1=1 {{#if ids}}AND id IN ({{ids}}){{/if}}"), Map.of("ids", List.of()));
        assertEquals("WHERE 1=1 ", r.sql()); assertTrue(r.bindings().isEmpty());
    }

    @Test @DisplayName("{{#if}} empty array condition → body skipped")
    void testIfEmptyArrayFalsy() {
        PreparedSql r = renderer.render(parser.parse("WHERE 1=1 {{#if ids}}AND id IN ({{ids}}){{/if}}"), Map.of("ids", new int[]{}));
        assertEquals("WHERE 1=1 ", r.sql()); assertTrue(r.bindings().isEmpty());
    }

    @Test @DisplayName("{{#if}} condition 0 → truthy")
    void testIfZeroTruthy() {
        PreparedSql r = renderer.render(parser.parse("WHERE 1=1 {{#if n}}AND n={{n}}{{/if}}"), Map.of("n", 0));
        assertEquals("WHERE 1=1 AND n=?", r.sql()); assertEquals(1, r.bindings().size());
        assertEquals(0, r.bindings().getFirst().value());
    }

    @Test @DisplayName("{{#if}} condition false → truthy")
    void testIfFalseTruthy() {
        PreparedSql r = renderer.render(parser.parse("WHERE 1=1 {{#if flag}}AND flag={{flag}}{{/if}}"), Map.of("flag", false));
        assertEquals("WHERE 1=1 AND flag=?", r.sql()); assertEquals(1, r.bindings().size());
        assertEquals(false, r.bindings().getFirst().value());
    }

    // ---- 嵌套 {{#if}} ----
    @Test @DisplayName("nested {{#if}} both true → inner SQL rendered")
    void testNestedIfBothTrue() {
        PreparedSql r = renderer.render(parser.parse("{{#if a}}{{#if b}}AND b={{b}}{{/if}}{{/if}}"), Map.of("a", 1, "b", 2));
        assertEquals("AND b=?", r.sql());
        assertEquals(1, r.bindings().size());
        assertEquals(2, r.bindings().getFirst().value());
    }

    @Test @DisplayName("nested {{#if}} outer true inner skipped → empty")
    void testNestedIfInnerSkipped() {
        PreparedSql r = renderer.render(parser.parse("{{#if a}}{{#if b}}AND b={{b}}{{/if}}{{/if}}"), Map.of("a", 1));
        assertEquals("", r.sql());
        assertTrue(r.bindings().isEmpty());
    }

    @Test @DisplayName("nested {{#if}} outer skipped → empty")
    void testNestedIfOuterSkipped() {
        PreparedSql r = renderer.render(parser.parse("{{#if a}}{{#if b}}AND b={{b}}{{/if}}{{/if}}"), Map.of());
        assertEquals("", r.sql());
        assertTrue(r.bindings().isEmpty());
    }

    @Test @DisplayName("nested {{#if}} inner variable bound")
    void testNestedIfInnerBinding() {
        PreparedSql r = renderer.render(parser.parse("{{#if a}}x={{x}}{{#if b}} AND b={{b}}{{/if}}{{/if}}"), Map.of("a", 1, "x", 10, "b", 20));
        assertEquals("x=? AND b=?", r.sql());
        assertEquals(2, r.bindings().size());
        assertEquals(10, r.bindings().get(0).value());
        assertEquals(20, r.bindings().get(1).value());
    }

    // ---- {{#where}} ----
    @Test @DisplayName("{{#where}} all skipped → WHERE gone")
    void testWhereAllSkipped() {
        PreparedSql r = renderer.render(parser.parse("SELECT * FROM t {{#where}}{{#if s}}AND s={{s}}{{/if}}{{/where}}"), Map.of());
        assertEquals("SELECT * FROM t ", r.sql()); assertTrue(r.bindings().isEmpty());
    }

    @Test @DisplayName("{{#where}} leading AND trimmed")
    void testWhereFirstAnd() {
        PreparedSql r = renderer.render(parser.parse("SELECT * FROM t {{#where}}  AND d={{d}} {{#if s}}AND s={{s}}{{/if}}{{/where}}"),
            Map.of("d", 123, "s", "active"));
        assertEquals("SELECT * FROM t WHERE d=? AND s=?", r.sql()); assertEquals(2, r.bindings().size());
    }

    @Test @DisplayName("{{#where}} leading OR trimmed")
    void testWhereFirstOr() {
        PreparedSql r = renderer.render(parser.parse("SELECT {{#where}}  OR a={{a}} OR b={{b}}{{/where}}"), Map.of("a", 1, "b", 2));
        assertEquals("SELECT WHERE a=? OR b=?", r.sql());
    }

    @Test @DisplayName("{{#where}} first token non-AND/OR → as-is")
    void testWhereNoPrefix() {
        PreparedSql r = renderer.render(parser.parse("SELECT {{#where}}d={{d}}{{/where}}"), Map.of("d", 5));
        assertEquals("SELECT WHERE d=?", r.sql());
    }

    @Test @DisplayName("{{#where}} mixed: direct content + {{#if}}, if skipped")
    void testWhereMixedIfSkipped() {
        PreparedSql r = renderer.render(parser.parse("{{#where}}d={{d}}{{#if s}}AND s={{s}}{{/if}}{{/where}}"), Map.of("d", 10));
        assertEquals("WHERE d=?", r.sql()); assertEquals(1, r.bindings().size());
    }

    @Test @DisplayName("{{#where}} with multiple {{#if}}, partial skips produce no blank lines")
    void testWhereMultipleIfNoBlankLines() {
        String tpl = """
                SELECT * FROM invoice i
                INNER JOIN customer c ON i.customerid = c.customerid
                {{#where}}
                  {{#if city}}
                   and c.city in ({{city}})
                  {{/if}}
                 {{#if num}}
                  and i.customerid > {{num}}
                 {{/if}}
                and i.invoicedate > {{invocedate}}
                {{/where}}""";
        PreparedSql r = renderer.render(
                parser.parse(tpl),
                Map.of("city", List.of("Beijing", "Shanghai"), "invocedate", "2024-01-01"));
        // 核心断言：渲染结果中不能有空行（\n后紧跟空白再\n）
        assertFalse(r.sql().matches("(?s).*\\n\\s*\\n.*"), "不应有空行: " + r.sql());
        assertEquals(3, r.bindings().size());
    }

    // ---- Array 展开 ----
    @Test @DisplayName("Array → multiple ?")
    void testArray() {
        PreparedSql r = renderer.render(parser.parse("IN ({{ids}})"), Map.of("ids", List.of(1, 2, 3)));
        assertEquals("IN (?, ?, ?)", r.sql()); assertEquals(3, r.bindings().size());
    }

    @Test @DisplayName("empty Array → IN () without binding")
    void testEmptyArray() {
        PreparedSql r = renderer.render(parser.parse("IN ({{ids}})"), Map.of("ids", List.of()));
        assertEquals("IN ()", r.sql()); assertTrue(r.bindings().isEmpty());
    }

    @Test @DisplayName("single-element Array → single ?")
    void testSingleArray() {
        PreparedSql r = renderer.render(parser.parse("IN ({{ids}})"), Map.of("ids", List.of(42)));
        assertEquals("IN (?)", r.sql()); assertEquals(1, r.bindings().size());
    }

    @Test @DisplayName("non-Array value → single ?")
    void testNonArray() {
        PreparedSql r = renderer.render(parser.parse("WHERE id = {{id}}"), Map.of("id", 100));
        assertEquals("WHERE id = ?", r.sql()); assertEquals(1, r.bindings().size());
    }

    @Test @DisplayName("Array mixed with plain variables → bindings in correct order")
    void testArrayMixed() {
        PreparedSql r = renderer.render(parser.parse("WHERE a={{a}} AND b IN ({{b}})"), Map.of("a", 1, "b", List.of(10,20)));
        assertEquals("WHERE a=? AND b IN (?, ?)", r.sql());
        assertEquals(3, r.bindings().size());
        assertEquals("a", r.bindings().get(0).name());
        assertEquals("b", r.bindings().get(1).name());
        assertEquals("b", r.bindings().get(2).name());
    }

    // ---- 原始类型数组展开 ----
    @Test @DisplayName("int[] → multiple ?")
    void testIntArray() {
        PreparedSql r = renderer.render(parser.parse("IN ({{ids}})"), Map.of("ids", new int[]{1, 2, 3}));
        assertEquals("IN (?, ?, ?)", r.sql());
        assertEquals(3, r.bindings().size());
        assertEquals(1, r.bindings().get(0).value());
        assertEquals(2, r.bindings().get(1).value());
        assertEquals(3, r.bindings().get(2).value());
    }

    @Test @DisplayName("long[] → multiple ?")
    void testLongArray() {
        PreparedSql r = renderer.render(parser.parse("IN ({{ids}})"), Map.of("ids", new long[]{10L, 20L}));
        assertEquals("IN (?, ?)", r.sql());
        assertEquals(2, r.bindings().size());
        assertEquals(10L, r.bindings().get(0).value());
        assertEquals(20L, r.bindings().get(1).value());
    }

    @Test @DisplayName("double[] → multiple ?")
    void testDoubleArray() {
        PreparedSql r = renderer.render(parser.parse("IN ({{vals}})"), Map.of("vals", new double[]{1.5, 2.5}));
        assertEquals("IN (?, ?)", r.sql());
        assertEquals(2, r.bindings().size());
        assertEquals(1.5, r.bindings().get(0).value());
        assertEquals(2.5, r.bindings().get(1).value());
    }

    @Test @DisplayName("boolean[] → multiple ?")
    void testBooleanArray() {
        PreparedSql r = renderer.render(parser.parse("IN ({{flags}})"), Map.of("flags", new boolean[]{true, false}));
        assertEquals("IN (?, ?)", r.sql());
        assertEquals(2, r.bindings().size());
        assertEquals(true, r.bindings().get(0).value());
        assertEquals(false, r.bindings().get(1).value());
    }

    @Test @DisplayName("Integer[] (Object[]) → multiple ?")
    void testIntegerArray() {
        PreparedSql r = renderer.render(parser.parse("IN ({{ids}})"), Map.of("ids", new Integer[]{100, 200}));
        assertEquals("IN (?, ?)", r.sql());
        assertEquals(2, r.bindings().size());
        assertEquals(100, r.bindings().get(0).value());
        assertEquals(200, r.bindings().get(1).value());
    }

    @Test @DisplayName("String[] → multiple ?")
    void testStringArray() {
        PreparedSql r = renderer.render(parser.parse("IN ({{vals}})"), Map.of("vals", new String[]{"a", "b", "c"}));
        assertEquals("IN (?, ?, ?)", r.sql());
        assertEquals(3, r.bindings().size());
        assertEquals("a", r.bindings().get(0).value());
        assertEquals("b", r.bindings().get(1).value());
        assertEquals("c", r.bindings().get(2).value());
    }

    @Test @DisplayName("empty int[] → IN () without binding")
    void testEmptyIntArray() {
        PreparedSql r = renderer.render(parser.parse("IN ({{ids}})"), Map.of("ids", new int[]{}));
        assertEquals("IN ()", r.sql());
        assertTrue(r.bindings().isEmpty());
    }

    // ---- 综合 ----
    @Test @DisplayName("full parameterized SQL: table + where + if + sort + limit")
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
    @Test @DisplayName("malicious value not present in SQL string")
    void testInjectionPrevention() {
        String evil = "' OR '1'='1' --";
        PreparedSql r = renderer.render(parser.parse("WHERE s = {{s}}"), Map.of("s", evil));
        assertEquals("WHERE s = ?", r.sql());
        assertFalse(r.sql().contains(evil));
        assertEquals(evil, r.bindings().getFirst().value());
    }

    @Test @DisplayName("table name injection guard")
    void testTableInjection() {
        String evil = "t; DROP TABLE users;--";
        PreparedSql r = renderer.render(parser.parse("SELECT * FROM {{t}}"), Map.of("t", evil));
        assertEquals("SELECT * FROM ?", r.sql());
        assertFalse(r.sql().contains("DROP"));
    }

    // ── {{{var}}} 原始变量（三重大括号）──

    @Test @DisplayName("{{{var}}} value present → inlined directly into SQL, no binding")
    void testRawVarDirectInline() {
        PreparedSql r = renderer.render(parser.parse("FROM {{{table}}}"), Map.of("table", "users"));
        assertEquals("FROM users", r.sql());
        assertTrue(r.bindings().isEmpty());
    }

    @Test @DisplayName("{{{var}}} null value → renders empty string")
    void testRawVarNull() {
        PreparedSql r = renderer.render(parser.parse("FROM {{{table}}}"), singletonMap("table", null));
        assertEquals("FROM ", r.sql());
        assertTrue(r.bindings().isEmpty());
    }

    @Test @DisplayName("{{{var}}} param missing → renders empty string")
    void testRawVarMissing() {
        PreparedSql r = renderer.render(parser.parse("FROM {{{table}}}"), Map.of());
        assertEquals("FROM ", r.sql());
        assertTrue(r.bindings().isEmpty());
    }

    @Test @DisplayName("{{{var:default}}} null → inlines default")
    void testRawVarDefaultNull() {
        PreparedSql r = renderer.render(parser.parse("ORDER BY {{{sort:id}}}"), singletonMap("sort", null));
        assertEquals("ORDER BY id", r.sql());
        assertTrue(r.bindings().isEmpty());
    }

    @Test @DisplayName("{{{var:default}}} with value → inlines actual value")
    void testRawVarDefaultOverridden() {
        PreparedSql r = renderer.render(parser.parse("ORDER BY {{{sort:id}}}"), Map.of("sort", "name"));
        assertEquals("ORDER BY name", r.sql());
        assertTrue(r.bindings().isEmpty());
    }

    @Test @DisplayName("{{{var}}} value is Array → TemplateRenderException")
    void testRawVarArrayThrows() {
        assertThrows(TemplateRenderException.class, () ->
            renderer.render(parser.parse("IN ({{{ids}}})"), Map.of("ids", List.of(1, 2, 3))));
    }

    @Test @DisplayName("{{{var}}} non-Array value → inlined normally")
    void testRawVarNonArray() {
        PreparedSql r = renderer.render(parser.parse("FETCH NEXT {{{limit}}} ROWS ONLY"), Map.of("limit", 50));
        assertEquals("FETCH NEXT 50 ROWS ONLY", r.sql());
        assertTrue(r.bindings().isEmpty());
    }

    @Test @DisplayName("mixed {{{var}}} + {{var}} → correct SQL and binding order")
    void testMixedRawAndSafe() {
        PreparedSql r = renderer.render(parser.parse(
            "SELECT {{{col1}}}, {{{col2}}} FROM {{{table}}} WHERE id = {{id}} AND status = {{status}}"),
            Map.of("col1", "name", "col2", "email", "table", "users", "id", 1, "status", "active"));
        assertEquals("SELECT name, email FROM users WHERE id = ? AND status = ?", r.sql());
        assertEquals(2, r.bindings().size());
        assertEquals("id", r.bindings().get(0).name());
        assertEquals("status", r.bindings().get(1).name());
    }

    @Test @DisplayName("{{{var}}} inside {{#if}} block → inlined when condition true")
    void testRawVarInIfTrue() {
        PreparedSql r = renderer.render(parser.parse(
            "SELECT * FROM t {{#if sort}}ORDER BY {{{col}}} {{{dir}}}{{/if}}"),
            Map.of("sort", true, "col", "created_at", "dir", "DESC"));
        assertEquals("SELECT * FROM t ORDER BY created_at DESC", r.sql());
        assertTrue(r.bindings().isEmpty());
    }

    @Test @DisplayName("{{{var}}} inside {{#if}} block → gone when condition false")
    void testRawVarInIfFalse() {
        PreparedSql r = renderer.render(parser.parse(
            "SELECT * FROM t {{#if sort}}ORDER BY {{{col}}} {{{dir}}}{{/if}}"),
            new HashMap<>(Map.of()));
        assertEquals("SELECT * FROM t ", r.sql());
        assertTrue(r.bindings().isEmpty());
    }

    @Test @DisplayName("{{{var}}} inside {{#where}} → AND/OR trimming works")
    void testRawVarInWhere() {
        PreparedSql r = renderer.render(parser.parse(
            "SELECT * FROM t {{#where}}dept_id = {{dept_id}}{{#if status}}AND status = {{status}}{{/if}}{{/where}} ORDER BY {{{col}}} {{{dir}}}"),
            Map.of("dept_id", 10, "col", "name", "dir", "ASC"));
        assertEquals("SELECT * FROM t WHERE dept_id = ? ORDER BY name ASC", r.sql());
        assertEquals(1, r.bindings().size());
    }

    @Test @DisplayName("full scenario: table + column + sort + safe variables")
    void testFullScenario() {
        PreparedSql r = renderer.render(parser.parse(
            "SELECT {{{cols}}} FROM {{{table}}} WHERE dept_id = {{dept_id}} ORDER BY {{{sort}}} {{{dir}}} LIMIT {{{limit}}}"),
            Map.of("cols", "id, name, email", "table", "users", "dept_id", 42, "sort", "created_at", "dir", "DESC", "limit", 100));
        assertEquals("SELECT id, name, email FROM users WHERE dept_id = ? ORDER BY created_at DESC LIMIT 100", r.sql());
        assertEquals(1, r.bindings().size());
        assertEquals(42, r.bindings().getFirst().value());
    }

    @Test @DisplayName("{{{var}}} injection risk: raw mode inlines value directly into SQL")
    void testRawVarInjectionIsUserChoice() {
        String evil = "t; DROP TABLE users;--";
        PreparedSql r = renderer.render(parser.parse("SELECT * FROM {{{table}}}"), Map.of("table", evil));
        assertEquals("SELECT * FROM " + evil, r.sql());
    }
}
