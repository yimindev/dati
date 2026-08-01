package com.dati.common.template;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("HandlebarsStyleParser unit tests")
class HandlebarsStyleParserTest {

    private TemplateParser parser;

    @BeforeEach
    void setUp() { parser = new HandlebarsStyleParser(); }

    // ---- 纯文本 ----
    @Test @DisplayName("empty template → getVariables empty")
    void testEmptyTemplate() { assertTrue(parser.parse("").getVariables().isEmpty()); }

    @Test @DisplayName("plain text → no variables")
    void testPlainText() { assertEquals(Set.of(), parser.parse("SELECT * FROM tasks").getVariables()); }

    @Test @DisplayName("multiline plain text")
    void testMultilinePlainText() { assertEquals(Set.of(), parser.parse("line1\nline2").getVariables()); }

    // ---- 简单变量 ----
    @Test @DisplayName("{{var}} → getVariables contains the variable")
    void testSimpleVariable() { assertEquals(Set.of("task_id"), parser.parse("WHERE id = {{task_id}}").getVariables()); }

    @Test @DisplayName("multiple distinct variables")
    void testMultipleVariables() { assertEquals(Set.of("table", "status"), parser.parse("{{table}} {{status}}").getVariables()); }

    @Test @DisplayName("same variable repeated → deduplicated")
    void testDuplicateVariable() { assertEquals(Set.of("x"), parser.parse("{{x}} AND {{x}}").getVariables()); }

    // ---- 默认值 ----
    @Test @DisplayName("{{var:default}} → variable name is var, default part excluded")
    void testVariableWithDefault() { assertEquals(Set.of("limit"), parser.parse("LIMIT {{limit:20}}").getVariables()); }

    @Test @DisplayName("default value is empty string {{var:}}")
    void testVariableWithEmptyDefault() { assertEquals(Set.of("name"), parser.parse("{{name:}}").getVariables()); }

    // ---- 变量名格式 ----
    @Test @DisplayName("variable name with underscore")
    void testVarNameUnderscore() { assertEquals(Set.of("data_source_id"), parser.parse("{{data_source_id}}").getVariables()); }

    @Test @DisplayName("variable name with dot")
    void testVarNameDot() { assertEquals(Set.of("table.column"), parser.parse("{{table.column}}").getVariables()); }

    @Test @DisplayName("variable name with digits")
    void testVarNameDigits() { assertEquals(Set.of("col_123"), parser.parse("{{col_123}}").getVariables()); }

    @Test @DisplayName("variable name with multiple dots")
    void testVarNameMultiDot() { assertEquals(Set.of("a.b.c"), parser.parse("{{a.b.c}}").getVariables()); }

    // ---- 变量位置 ----
    @Test @DisplayName("variable at start")
    void testVariableAtStart() { assertEquals(Set.of("var"), parser.parse("{{var}} is the value").getVariables()); }

    @Test @DisplayName("variable at end")
    void testVariableAtEnd() { assertEquals(Set.of("var"), parser.parse("value is {{var}}").getVariables()); }

    @Test @DisplayName("two variables adjacent")
    void testAdjacentVariables() { assertEquals(Set.of("a", "b"), parser.parse("{{a}}{{b}}").getVariables()); }

    @Test @DisplayName("single variable with no other text")
    void testOnlyVariable() { assertEquals(Set.of("status"), parser.parse("{{status}}").getVariables()); }

    // ---- {{#if}} 块 ----
    @Test @DisplayName("{{#if var}}...{{/if}} → getVariables contains the variable")
    void testIfBlock() { assertEquals(Set.of("status"), parser.parse("{{#if status}}AND status = {{status}}{{/if}}").getVariables()); }

    @Test @DisplayName("{{#if}} containing multiple variables")
    void testIfBlockMultipleVars() { assertEquals(Set.of("a", "x", "y"), parser.parse("{{#if a}}{{x}} {{y}}{{/if}}").getVariables()); }

    @Test @DisplayName("{{#if}} body with plain text (no variables)")
    void testIfBlockPureText() { assertEquals(Set.of("flag"), parser.parse("{{#if flag}}SHOWN{{/if}}").getVariables()); }

    // ---- {{#where}} 块 ----
    @Test @DisplayName("{{#where}}...{{/where}}")
    void testWhereBlock() { assertEquals(Set.of("status"), parser.parse("{{#where}}{{#if status}}AND s = {{status}}{{/if}}{{/where}}").getVariables()); }

    @Test @DisplayName("{{#where}} with direct variable (not {{#if}})")
    void testWhereBlockDirectVar() { assertEquals(Set.of("dept_id", "status"), parser.parse("{{#where}}dept_id = {{dept_id}}{{#if status}}AND s = {{status}}{{/if}}{{/where}}").getVariables()); }

    @Test @DisplayName("{{#where}} containing plain text only")
    void testWhereBlockPureText() { assertEquals(Set.of(), parser.parse("{{#where}}1=1{{/where}}").getVariables()); }

    @Test @DisplayName("{{#where}} with multiple {{#if}}")
    void testWhereBlockMultipleIfs() { assertEquals(Set.of("a", "b"), parser.parse("{{#where}}{{#if a}}AND a = {{a}}{{/if}}{{#if b}}AND b = {{b}}{{/if}}{{/where}}").getVariables()); }

    // ---- 转义 ----
    @Test @DisplayName("\\{{ → literal {{, not a variable")
    void testEscape() { assertEquals(Set.of("real_var"), parser.parse("\\{{not_var}} {{real_var}}").getVariables()); }

    @Test @DisplayName("multiple escaped sequences")
    void testMultipleEscapes() { assertEquals(Set.of("c"), parser.parse("\\{{a}} \\{{b}} {{c}}").getVariables()); }

    // ---- 错误：未闭合 ----
    @Test @DisplayName("unclosed {{ → TemplateParseException")
    void testUnclosedBraces() { assertThrows(TemplateParseException.class, () -> parser.parse("{{task_id")); }

    @Test @DisplayName("unclosed {{ at end of template")
    void testUnclosedBracesAtEnd() { assertThrows(TemplateParseException.class, () -> parser.parse("text {{")); }

    @Test @DisplayName("{{#if}} missing {{/if}}")
    void testUnclosedIf() { assertThrows(TemplateParseException.class, () -> parser.parse("{{#if status}}AND 1=1")); }

    @Test @DisplayName("{{#where}} missing {{/where}}")
    void testUnclosedWhere() { assertThrows(TemplateParseException.class, () -> parser.parse("{{#where}}dept_id=1")); }

    // ---- 错误：多余闭合 ----
    @Test @DisplayName("extra {{/if}}")
    void testExtraEndIf() { assertThrows(TemplateParseException.class, () -> parser.parse("SELECT * {{/if}}")); }

    @Test @DisplayName("extra {{/where}}")
    void testExtraEndWhere() { assertThrows(TemplateParseException.class, () -> parser.parse("SELECT * {{/where}}")); }

    // ---- 错误：未知指令 ----
    @Test @DisplayName("{{#unknown}} → TemplateParseException")
    void testUnknownDirective() { assertThrows(TemplateParseException.class, () -> parser.parse("{{#foo}}x{{/foo}}")); }

    @Test @DisplayName("{{#each}}（V2）→ TemplateParseException")
    void testEachNotSupported() { assertThrows(TemplateParseException.class, () -> parser.parse("{{#each items}}{{this}}{{/each}}")); }

    // ---- 错误：变量名非法 ----
    @Test @DisplayName("empty variable {{}} → TemplateParseException")
    void testEmptyVar() { assertThrows(TemplateParseException.class, () -> parser.parse("{{}}")); }

    @Test @DisplayName("variable name with space {{a b}} → TemplateParseException")
    void testVarWithSpace() { assertThrows(TemplateParseException.class, () -> parser.parse("{{a b}}")); }

    @Test @DisplayName("variable name with hyphen {{a-b}} → TemplateParseException")
    void testVarWithHyphen() { assertThrows(TemplateParseException.class, () -> parser.parse("{{a-b}}")); }

    @Test @DisplayName("underscore-only variable name {{_}} → valid")
    void testVarOnlyUnderscore() { assertEquals(Set.of("_"), parser.parse("{{_}}").getVariables()); }

    @Test @DisplayName("{{#if}} empty condition → TemplateParseException")
    void testIfEmptyCondition() { assertThrows(TemplateParseException.class, () -> parser.parse("{{#if }}text{{/if}}")); }

    @Test @DisplayName("{{#if}} condition with space → TemplateParseException")
    void testIfConditionWithSpace() { assertThrows(TemplateParseException.class, () -> parser.parse("{{#if a b}}text{{/if}}")); }

    @Test @DisplayName("invalid variable name in default → TemplateParseException")
    void testDefaultVarInvalidName() { assertThrows(TemplateParseException.class, () -> parser.parse("{{a-b:default}}")); }

    // ---- 嵌套 ----
    @Test @DisplayName("nested {{#if}} → getVariables includes both levels")
    void testNestedIf() {
        CompiledTemplate t = parser.parse("{{#if a}}o{{#if b}}i{{/if}}{{/if}}");
        assertEquals(Set.of("a", "b"), t.getVariables());
    }

    @Test @DisplayName("variables in three-level nested {{#if}}")
    void testDeepNestedIf() {
        CompiledTemplate t = parser.parse("{{#if a}}{{#if b}}{{#if c}}deep{{/if}}{{/if}}{{/if}}");
        assertEquals(Set.of("a", "b", "c"), t.getVariables());
    }

    @Test @DisplayName("nested {{#if}} containing variables")
    void testNestedIfWithVar() {
        CompiledTemplate t = parser.parse("{{#if a}}{{#if b}}x={{b}}{{/if}}{{/if}}");
        assertEquals(Set.of("a", "b"), t.getVariables());
    }

    @Test @DisplayName("escaped {{#if}} is not nesting, parses successfully")
    void testEscapedIfNotNested() {
        CompiledTemplate t = parser.parse("{{#if debug}}语法: \\{{#if condition}}...\\{{/if}}{{/if}}");
        assertEquals(Set.of("debug"), t.getVariables());
    }

    // ---- 综合 ----
    @Test @DisplayName("complex template: text + var + if + where + default")
    void testComplexTemplate() {
        CompiledTemplate t = parser.parse(
            "SELECT * FROM {{table}}\n{{#where}}{{#if status}}AND status = {{status}}{{/if}}{{/where}}\nORDER BY {{sort:created_at}}\nLIMIT {{limit:20}}");
        assertEquals(Set.of("table", "status", "sort", "limit"), t.getVariables());
    }

    @Test @DisplayName("{{}} inside SQL comment still parsed (Parser is SQL-agnostic)")
    void testSqlComment() { assertEquals(Set.of("comment", "status"), parser.parse("-- {{comment}}\nWHERE s = {{status}}").getVariables()); }

    @Test @DisplayName("{{}} inside SQL string is still parsed")
    void testSqlStringLiteral() { assertEquals(Set.of("literal", "var"), parser.parse("SELECT '{{literal}}' AS c, {{var}}").getVariables()); }

    @Test @DisplayName("getVariables returns an immutable collection")
    void testGetVariablesImmutable() {
        Set<String> vars = parser.parse("{{a}}").getVariables();
        assertThrows(UnsupportedOperationException.class, () -> vars.add("b"));
    }

    // ── {{{var}}} 原始变量（三重大括号）──

    @Test @DisplayName("{{{var}}} → getVariables returns the variable")
    void testRawVariable() {
        assertEquals(Set.of("table"), parser.parse("FROM {{{table}}}").getVariables());
    }

    @Test @DisplayName("{{{var}}} → marked raw = true")
    void testRawVariableFlagTrue() {
        CompiledTemplate t = parser.parse("{{{table}}}");
        ParsedTemplate pt = (ParsedTemplate) t;
        VarNode node = (VarNode) pt.getNodes().getFirst();
        assertTrue(node.raw());
    }

    @Test @DisplayName("{{var}} marked raw = false")
    void testNormalVariableFlagFalse() {
        CompiledTemplate t = parser.parse("{{table}}");
        ParsedTemplate pt = (ParsedTemplate) t;
        VarNode node = (VarNode) pt.getNodes().getFirst();
        assertFalse(node.raw());
    }

    @Test @DisplayName("{{{var:default}}} → name excludes default, raw=true")
    void testRawVariableWithDefault() {
        CompiledTemplate t = parser.parse("ORDER BY {{{sort:id}}}");
        ParsedTemplate pt = (ParsedTemplate) t;
        VarNode node = null;
        for (Node n : pt.getNodes()) { if (n instanceof VarNode v) { node = v; break; } }
        assertNotNull(node);
        assertEquals("sort", node.name());
        assertEquals("id", node.defaultValue());
        assertTrue(node.raw());
    }

    @Test @DisplayName("mixed {{var}} + {{{var}}} in one template")
    void testMixedNormalAndRaw() {
        CompiledTemplate t = parser.parse("SELECT {{{col1}}}, {{{col2}}} FROM {{table}} WHERE id = {{id}}");
        assertEquals(Set.of("col1", "col2", "table", "id"), t.getVariables());
    }

    @Test @DisplayName("{{{var}}} variable name contains dot")
    void testRawVarWithDot() {
        assertEquals(Set.of("t.column"), parser.parse("{{{t.column}}}").getVariables());
    }

    // ── {{{ 转义 ──

    @Test @DisplayName("\\{{{ → literal {{{, not a variable")
    void testEscapeTripleBraces() {
        assertEquals(Set.of("var"), parser.parse("\\{{{not_var}}} {{var}}").getVariables());
    }

    @Test @DisplayName("escaped \\{{{ followed by normal variable still works")
    void testEscapeTripleBracesWithNormalVar() {
        assertEquals(Set.of("y"), parser.parse("\\{{{x}}} {{{y}}}").getVariables());
    }

    // ── 错误：{{{ 不闭合 ──

    @Test @DisplayName("unclosed {{{ → TemplateParseException")
    void testUnclosedTripleBraces() {
        assertThrows(TemplateParseException.class, () -> parser.parse("{{{table"));
    }

    @Test @DisplayName("{{{ closed with only }} → TemplateParseException (missing third })")
    void testTripleBracesClosedByDouble() {
        assertThrows(TemplateParseException.class, () -> parser.parse("{{{table}}"));
    }

    @Test @DisplayName("{{{ without closing braces")
    void testUnclosedTripleBracesOnly() {
        assertThrows(TemplateParseException.class, () -> parser.parse("{{{"));
    }

    @Test @DisplayName("{{{}}} empty variable name → TemplateParseException")
    void testEmptyTripleBraces() {
        assertThrows(TemplateParseException.class, () -> parser.parse("{{{}}}"));
    }

    @Test @DisplayName("{{{a b}}} variable name with space → TemplateParseException")
    void testTripleBracesInvalidName() {
        assertThrows(TemplateParseException.class, () -> parser.parse("{{{a b}}}"));
    }

    @Test @DisplayName("{{{ with default but empty variable name → TemplateParseException")
    void testTripleBracesEmptyNameWithDefault() {
        assertThrows(TemplateParseException.class, () -> parser.parse("{{{:default}}}"));
    }
}
