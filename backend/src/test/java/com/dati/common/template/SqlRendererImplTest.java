package com.dati.common.template;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.*;

import static java.util.Collections.singletonMap;
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

    // ---- 转义 ----
    @Test @DisplayName("\\{{var}} → 字面量 {{var}}，无 binding")
    void testEscapeVar() {
        PreparedSql r = renderer.render(parser.parse("\\{{var}}"), Map.of());
        assertEquals("{{var}}", r.sql());
        assertTrue(r.bindings().isEmpty());
    }

    @Test @DisplayName("\\{{}} 与真实变量混合")
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
        PreparedSql r = renderer.render(parser.parse("WHERE 1=1 {{#if s}}AND s={{s}}{{/if}}"), singletonMap("s", null));
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

    @Test @DisplayName("{{#if}} 条件为空字符串 → body 跳过")
    void testIfEmptyStringFalsy() {
        PreparedSql r = renderer.render(parser.parse("WHERE 1=1 {{#if s}}AND s={{s}}{{/if}}"), Map.of("s", ""));
        assertEquals("WHERE 1=1 ", r.sql()); assertTrue(r.bindings().isEmpty());
    }

    @Test @DisplayName("{{#if}} 条件为空集合 → body 跳过")
    void testIfEmptyListFalsy() {
        PreparedSql r = renderer.render(parser.parse("WHERE 1=1 {{#if ids}}AND id IN ({{ids}}){{/if}}"), Map.of("ids", List.of()));
        assertEquals("WHERE 1=1 ", r.sql()); assertTrue(r.bindings().isEmpty());
    }

    @Test @DisplayName("{{#if}} 条件为空数组 → body 跳过")
    void testIfEmptyArrayFalsy() {
        PreparedSql r = renderer.render(parser.parse("WHERE 1=1 {{#if ids}}AND id IN ({{ids}}){{/if}}"), Map.of("ids", new int[]{}));
        assertEquals("WHERE 1=1 ", r.sql()); assertTrue(r.bindings().isEmpty());
    }

    @Test @DisplayName("{{#if}} 条件为 0 → truthy")
    void testIfZeroTruthy() {
        PreparedSql r = renderer.render(parser.parse("WHERE 1=1 {{#if n}}AND n={{n}}{{/if}}"), Map.of("n", 0));
        assertEquals("WHERE 1=1 AND n=?", r.sql()); assertEquals(1, r.bindings().size());
        assertEquals(0, r.bindings().getFirst().value());
    }

    @Test @DisplayName("{{#if}} 条件为 false → truthy")
    void testIfFalseTruthy() {
        PreparedSql r = renderer.render(parser.parse("WHERE 1=1 {{#if flag}}AND flag={{flag}}{{/if}}"), Map.of("flag", false));
        assertEquals("WHERE 1=1 AND flag=?", r.sql()); assertEquals(1, r.bindings().size());
        assertEquals(false, r.bindings().getFirst().value());
    }

    // ---- 嵌套 {{#if}} ----
    @Test @DisplayName("嵌套 {{#if}} 两层都成立 → 内层 SQL 渲染")
    void testNestedIfBothTrue() {
        PreparedSql r = renderer.render(parser.parse("{{#if a}}{{#if b}}AND b={{b}}{{/if}}{{/if}}"), Map.of("a", 1, "b", 2));
        assertEquals("AND b=?", r.sql());
        assertEquals(1, r.bindings().size());
        assertEquals(2, r.bindings().getFirst().value());
    }

    @Test @DisplayName("嵌套 {{#if}} 外层成立内层跳过 → 空")
    void testNestedIfInnerSkipped() {
        PreparedSql r = renderer.render(parser.parse("{{#if a}}{{#if b}}AND b={{b}}{{/if}}{{/if}}"), Map.of("a", 1));
        assertEquals("", r.sql());
        assertTrue(r.bindings().isEmpty());
    }

    @Test @DisplayName("嵌套 {{#if}} 外层跳过 → 空")
    void testNestedIfOuterSkipped() {
        PreparedSql r = renderer.render(parser.parse("{{#if a}}{{#if b}}AND b={{b}}{{/if}}{{/if}}"), Map.of());
        assertEquals("", r.sql());
        assertTrue(r.bindings().isEmpty());
    }

    @Test @DisplayName("嵌套 {{#if}} 内层变量绑定")
    void testNestedIfInnerBinding() {
        PreparedSql r = renderer.render(parser.parse("{{#if a}}x={{x}}{{#if b}} AND b={{b}}{{/if}}{{/if}}"), Map.of("a", 1, "x", 10, "b", 20));
        assertEquals("x=? AND b=?", r.sql());
        assertEquals(2, r.bindings().size());
        assertEquals(10, r.bindings().get(0).value());
        assertEquals(20, r.bindings().get(1).value());
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

    @Test @DisplayName("{{#where}} 内多个 {{#if}}，部分跳过不产生空行")
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

    // ---- 原始类型数组展开 ----
    @Test @DisplayName("int[] → 多个 ?")
    void testIntArray() {
        PreparedSql r = renderer.render(parser.parse("IN ({{ids}})"), Map.of("ids", new int[]{1, 2, 3}));
        assertEquals("IN (?, ?, ?)", r.sql());
        assertEquals(3, r.bindings().size());
        assertEquals(1, r.bindings().get(0).value());
        assertEquals(2, r.bindings().get(1).value());
        assertEquals(3, r.bindings().get(2).value());
    }

    @Test @DisplayName("long[] → 多个 ?")
    void testLongArray() {
        PreparedSql r = renderer.render(parser.parse("IN ({{ids}})"), Map.of("ids", new long[]{10L, 20L}));
        assertEquals("IN (?, ?)", r.sql());
        assertEquals(2, r.bindings().size());
        assertEquals(10L, r.bindings().get(0).value());
        assertEquals(20L, r.bindings().get(1).value());
    }

    @Test @DisplayName("double[] → 多个 ?")
    void testDoubleArray() {
        PreparedSql r = renderer.render(parser.parse("IN ({{vals}})"), Map.of("vals", new double[]{1.5, 2.5}));
        assertEquals("IN (?, ?)", r.sql());
        assertEquals(2, r.bindings().size());
        assertEquals(1.5, r.bindings().get(0).value());
        assertEquals(2.5, r.bindings().get(1).value());
    }

    @Test @DisplayName("boolean[] → 多个 ?")
    void testBooleanArray() {
        PreparedSql r = renderer.render(parser.parse("IN ({{flags}})"), Map.of("flags", new boolean[]{true, false}));
        assertEquals("IN (?, ?)", r.sql());
        assertEquals(2, r.bindings().size());
        assertEquals(true, r.bindings().get(0).value());
        assertEquals(false, r.bindings().get(1).value());
    }

    @Test @DisplayName("Integer[]（Object[]）→ 多个 ?")
    void testIntegerArray() {
        PreparedSql r = renderer.render(parser.parse("IN ({{ids}})"), Map.of("ids", new Integer[]{100, 200}));
        assertEquals("IN (?, ?)", r.sql());
        assertEquals(2, r.bindings().size());
        assertEquals(100, r.bindings().get(0).value());
        assertEquals(200, r.bindings().get(1).value());
    }

    @Test @DisplayName("String[] → 多个 ?")
    void testStringArray() {
        PreparedSql r = renderer.render(parser.parse("IN ({{vals}})"), Map.of("vals", new String[]{"a", "b", "c"}));
        assertEquals("IN (?, ?, ?)", r.sql());
        assertEquals(3, r.bindings().size());
        assertEquals("a", r.bindings().get(0).value());
        assertEquals("b", r.bindings().get(1).value());
        assertEquals("c", r.bindings().get(2).value());
    }

    @Test @DisplayName("空 int[] → IN ()，无 binding")
    void testEmptyIntArray() {
        PreparedSql r = renderer.render(parser.parse("IN ({{ids}})"), Map.of("ids", new int[]{}));
        assertEquals("IN ()", r.sql());
        assertTrue(r.bindings().isEmpty());
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

    // ── {{{var}}} 原始变量（三重大括号）──

    @Test @DisplayName("{{{var}}} 值存在 → 直接内联到 SQL，无 binding")
    void testRawVarDirectInline() {
        PreparedSql r = renderer.render(parser.parse("FROM {{{table}}}"), Map.of("table", "users"));
        assertEquals("FROM users", r.sql());
        assertTrue(r.bindings().isEmpty());
    }

    @Test @DisplayName("{{{var}}} 值为 null → 输出空字符串")
    void testRawVarNull() {
        PreparedSql r = renderer.render(parser.parse("FROM {{{table}}}"), singletonMap("table", null));
        assertEquals("FROM ", r.sql());
        assertTrue(r.bindings().isEmpty());
    }

    @Test @DisplayName("{{{var}}} 参数缺失 → 输出空字符串")
    void testRawVarMissing() {
        PreparedSql r = renderer.render(parser.parse("FROM {{{table}}}"), Map.of());
        assertEquals("FROM ", r.sql());
        assertTrue(r.bindings().isEmpty());
    }

    @Test @DisplayName("{{{var:default}}} null → 内联 default")
    void testRawVarDefaultNull() {
        PreparedSql r = renderer.render(parser.parse("ORDER BY {{{sort:id}}}"), singletonMap("sort", null));
        assertEquals("ORDER BY id", r.sql());
        assertTrue(r.bindings().isEmpty());
    }

    @Test @DisplayName("{{{var:default}}} 有值 → 内联实际值")
    void testRawVarDefaultOverridden() {
        PreparedSql r = renderer.render(parser.parse("ORDER BY {{{sort:id}}}"), Map.of("sort", "name"));
        assertEquals("ORDER BY name", r.sql());
        assertTrue(r.bindings().isEmpty());
    }

    @Test @DisplayName("{{{var}}} 值为 Array → TemplateRenderException")
    void testRawVarArrayThrows() {
        assertThrows(TemplateRenderException.class, () ->
            renderer.render(parser.parse("IN ({{{ids}}})"), Map.of("ids", List.of(1, 2, 3))));
    }

    @Test @DisplayName("{{{var}}} 值非 Array → 正常内联")
    void testRawVarNonArray() {
        PreparedSql r = renderer.render(parser.parse("FETCH NEXT {{{limit}}} ROWS ONLY"), Map.of("limit", 50));
        assertEquals("FETCH NEXT 50 ROWS ONLY", r.sql());
        assertTrue(r.bindings().isEmpty());
    }

    @Test @DisplayName("混合 {{{var}}} + {{var}} → 正确的 SQL 和 binding 顺序")
    void testMixedRawAndSafe() {
        PreparedSql r = renderer.render(parser.parse(
            "SELECT {{{col1}}}, {{{col2}}} FROM {{{table}}} WHERE id = {{id}} AND status = {{status}}"),
            Map.of("col1", "name", "col2", "email", "table", "users", "id", 1, "status", "active"));
        assertEquals("SELECT name, email FROM users WHERE id = ? AND status = ?", r.sql());
        assertEquals(2, r.bindings().size());
        assertEquals("id", r.bindings().get(0).name());
        assertEquals("status", r.bindings().get(1).name());
    }

    @Test @DisplayName("{{{var}}} 在 {{#if}} 块内 → 条件成立时内联")
    void testRawVarInIfTrue() {
        PreparedSql r = renderer.render(parser.parse(
            "SELECT * FROM t {{#if sort}}ORDER BY {{{col}}} {{{dir}}}{{/if}}"),
            Map.of("sort", true, "col", "created_at", "dir", "DESC"));
        assertEquals("SELECT * FROM t ORDER BY created_at DESC", r.sql());
        assertTrue(r.bindings().isEmpty());
    }

    @Test @DisplayName("{{{var}}} 在 {{#if}} 块内 → 条件不成立时消失")
    void testRawVarInIfFalse() {
        PreparedSql r = renderer.render(parser.parse(
            "SELECT * FROM t {{#if sort}}ORDER BY {{{col}}} {{{dir}}}{{/if}}"),
            new HashMap<>(Map.of()));
        assertEquals("SELECT * FROM t ", r.sql());
        assertTrue(r.bindings().isEmpty());
    }

    @Test @DisplayName("{{{var}}} 在 {{#where}} 内 → AND/OR 裁剪正常工作")
    void testRawVarInWhere() {
        PreparedSql r = renderer.render(parser.parse(
            "SELECT * FROM t {{#where}}dept_id = {{dept_id}}{{#if status}}AND status = {{status}}{{/if}}{{/where}} ORDER BY {{{col}}} {{{dir}}}"),
            Map.of("dept_id", 10, "col", "name", "dir", "ASC"));
        assertEquals("SELECT * FROM t WHERE dept_id = ? ORDER BY name ASC", r.sql());
        assertEquals(1, r.bindings().size());
    }

    @Test @DisplayName("完整场景：表名 + 列名 + 排序 + 安全变量")
    void testFullScenario() {
        PreparedSql r = renderer.render(parser.parse(
            "SELECT {{{cols}}} FROM {{{table}}} WHERE dept_id = {{dept_id}} ORDER BY {{{sort}}} {{{dir}}} LIMIT {{{limit}}}"),
            Map.of("cols", "id, name, email", "table", "users", "dept_id", 42, "sort", "created_at", "dir", "DESC", "limit", 100));
        assertEquals("SELECT id, name, email FROM users WHERE dept_id = ? ORDER BY created_at DESC LIMIT 100", r.sql());
        assertEquals(1, r.bindings().size());
        assertEquals(42, r.bindings().getFirst().value());
    }

    @Test @DisplayName("{{{var}}} 注入风险：用户显式选择 raw 模式，值直接拼入 SQL")
    void testRawVarInjectionIsUserChoice() {
        String evil = "t; DROP TABLE users;--";
        PreparedSql r = renderer.render(parser.parse("SELECT * FROM {{{table}}}"), Map.of("table", evil));
        assertEquals("SELECT * FROM " + evil, r.sql());
    }
}
