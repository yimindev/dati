package com.dati.common.template;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("HandlebarsStyleParser 单元测试")
class HandlebarsStyleParserTest {

    private TemplateParser parser;

    @BeforeEach
    void setUp() { parser = new HandlebarsStyleParser(); }

    // ---- 纯文本 ----
    @Test @DisplayName("空模板 → getVariables 空")
    void testEmptyTemplate() { assertTrue(parser.parse("").getVariables().isEmpty()); }

    @Test @DisplayName("纯文本 → 无变量")
    void testPlainText() { assertEquals(Set.of(), parser.parse("SELECT * FROM tasks").getVariables()); }

    @Test @DisplayName("多行纯文本")
    void testMultilinePlainText() { assertEquals(Set.of(), parser.parse("line1\nline2").getVariables()); }

    // ---- 简单变量 ----
    @Test @DisplayName("{{var}} → getVariables 包含该变量")
    void testSimpleVariable() { assertEquals(Set.of("task_id"), parser.parse("WHERE id = {{task_id}}").getVariables()); }

    @Test @DisplayName("多个不同变量")
    void testMultipleVariables() { assertEquals(Set.of("table", "status"), parser.parse("{{table}} {{status}}").getVariables()); }

    @Test @DisplayName("同一变量多次出现 → 去重")
    void testDuplicateVariable() { assertEquals(Set.of("x"), parser.parse("{{x}} AND {{x}}").getVariables()); }

    // ---- 默认值 ----
    @Test @DisplayName("{{var:default}} → 变量名为 var，不含默认值部分")
    void testVariableWithDefault() { assertEquals(Set.of("limit"), parser.parse("LIMIT {{limit:20}}").getVariables()); }

    @Test @DisplayName("默认值为空字符串 {{var:}}")
    void testVariableWithEmptyDefault() { assertEquals(Set.of("name"), parser.parse("{{name:}}").getVariables()); }

    // ---- 变量名格式 ----
    @Test @DisplayName("变量名含下划线")
    void testVarNameUnderscore() { assertEquals(Set.of("data_source_id"), parser.parse("{{data_source_id}}").getVariables()); }

    @Test @DisplayName("变量名含点号")
    void testVarNameDot() { assertEquals(Set.of("table.column"), parser.parse("{{table.column}}").getVariables()); }

    @Test @DisplayName("变量名含数字")
    void testVarNameDigits() { assertEquals(Set.of("col_123"), parser.parse("{{col_123}}").getVariables()); }

    @Test @DisplayName("变量名含多个点号")
    void testVarNameMultiDot() { assertEquals(Set.of("a.b.c"), parser.parse("{{a.b.c}}").getVariables()); }

    // ---- 变量位置 ----
    @Test @DisplayName("变量在开头")
    void testVariableAtStart() { assertEquals(Set.of("var"), parser.parse("{{var}} is the value").getVariables()); }

    @Test @DisplayName("变量在末尾")
    void testVariableAtEnd() { assertEquals(Set.of("var"), parser.parse("value is {{var}}").getVariables()); }

    @Test @DisplayName("两个变量紧邻")
    void testAdjacentVariables() { assertEquals(Set.of("a", "b"), parser.parse("{{a}}{{b}}").getVariables()); }

    @Test @DisplayName("仅一个变量，无其他文本")
    void testOnlyVariable() { assertEquals(Set.of("status"), parser.parse("{{status}}").getVariables()); }

    // ---- {{#if}} 块 ----
    @Test @DisplayName("{{#if var}}...{{/if}} → getVariables 含该变量")
    void testIfBlock() { assertEquals(Set.of("status"), parser.parse("{{#if status}}AND status = {{status}}{{/if}}").getVariables()); }

    @Test @DisplayName("{{#if}} 内含多个变量")
    void testIfBlockMultipleVars() { assertEquals(Set.of("a", "x", "y"), parser.parse("{{#if a}}{{x}} {{y}}{{/if}}").getVariables()); }

    @Test @DisplayName("{{#if}} body 纯文本（无变量）")
    void testIfBlockPureText() { assertEquals(Set.of("flag"), parser.parse("{{#if flag}}SHOWN{{/if}}").getVariables()); }

    // ---- {{#where}} 块 ----
    @Test @DisplayName("{{#where}}...{{/where}}")
    void testWhereBlock() { assertEquals(Set.of("status"), parser.parse("{{#where}}{{#if status}}AND s = {{status}}{{/if}}{{/where}}").getVariables()); }

    @Test @DisplayName("{{#where}} 内直接放变量（非 {{#if}}）")
    void testWhereBlockDirectVar() { assertEquals(Set.of("dept_id", "status"), parser.parse("{{#where}}dept_id = {{dept_id}}{{#if status}}AND s = {{status}}{{/if}}{{/where}}").getVariables()); }

    @Test @DisplayName("{{#where}} 内仅纯文本")
    void testWhereBlockPureText() { assertEquals(Set.of(), parser.parse("{{#where}}1=1{{/where}}").getVariables()); }

    @Test @DisplayName("{{#where}} 内多个 {{#if}}")
    void testWhereBlockMultipleIfs() { assertEquals(Set.of("a", "b"), parser.parse("{{#where}}{{#if a}}AND a = {{a}}{{/if}}{{#if b}}AND b = {{b}}{{/if}}{{/where}}").getVariables()); }

    // ---- 转义 ----
    @Test @DisplayName("\\{{ → 字面量 {{，不算变量")
    void testEscape() { assertEquals(Set.of("real_var"), parser.parse("\\{{not_var}} {{real_var}}").getVariables()); }

    @Test @DisplayName("多个转义")
    void testMultipleEscapes() { assertEquals(Set.of("c"), parser.parse("\\{{a}} \\{{b}} {{c}}").getVariables()); }

    // ---- 错误：未闭合 ----
    @Test @DisplayName("未闭合 {{ → TemplateParseException")
    void testUnclosedBraces() { assertThrows(TemplateParseException.class, () -> parser.parse("{{task_id")); }

    @Test @DisplayName("{{ 在模板末尾不闭合")
    void testUnclosedBracesAtEnd() { assertThrows(TemplateParseException.class, () -> parser.parse("text {{")); }

    @Test @DisplayName("{{#if}} 缺少 {{/if}}")
    void testUnclosedIf() { assertThrows(TemplateParseException.class, () -> parser.parse("{{#if status}}AND 1=1")); }

    @Test @DisplayName("{{#where}} 缺少 {{/where}}")
    void testUnclosedWhere() { assertThrows(TemplateParseException.class, () -> parser.parse("{{#where}}dept_id=1")); }

    // ---- 错误：多余闭合 ----
    @Test @DisplayName("多余 {{/if}}")
    void testExtraEndIf() { assertThrows(TemplateParseException.class, () -> parser.parse("SELECT * {{/if}}")); }

    @Test @DisplayName("多余 {{/where}}")
    void testExtraEndWhere() { assertThrows(TemplateParseException.class, () -> parser.parse("SELECT * {{/where}}")); }

    // ---- 错误：未知指令 ----
    @Test @DisplayName("{{#unknown}} → TemplateParseException")
    void testUnknownDirective() { assertThrows(TemplateParseException.class, () -> parser.parse("{{#foo}}x{{/foo}}")); }

    @Test @DisplayName("{{#each}}（V2）→ TemplateParseException")
    void testEachNotSupported() { assertThrows(TemplateParseException.class, () -> parser.parse("{{#each items}}{{this}}{{/each}}")); }

    // ---- 错误：变量名非法 ----
    @Test @DisplayName("空变量 {{}} → TemplateParseException")
    void testEmptyVar() { assertThrows(TemplateParseException.class, () -> parser.parse("{{}}")); }

    @Test @DisplayName("变量名含空格 {{a b}} → TemplateParseException")
    void testVarWithSpace() { assertThrows(TemplateParseException.class, () -> parser.parse("{{a b}}")); }

    @Test @DisplayName("变量名含连字符 {{a-b}} → TemplateParseException")
    void testVarWithHyphen() { assertThrows(TemplateParseException.class, () -> parser.parse("{{a-b}}")); }

    @Test @DisplayName("变量名仅下划线 {{_}} → 合法")
    void testVarOnlyUnderscore() { assertEquals(Set.of("_"), parser.parse("{{_}}").getVariables()); }

    @Test @DisplayName("{{#if}} 空条件 → TemplateParseException")
    void testIfEmptyCondition() { assertThrows(TemplateParseException.class, () -> parser.parse("{{#if }}text{{/if}}")); }

    @Test @DisplayName("{{#if}} 条件含空格 → TemplateParseException")
    void testIfConditionWithSpace() { assertThrows(TemplateParseException.class, () -> parser.parse("{{#if a b}}text{{/if}}")); }

    @Test @DisplayName("默认值变量名非法 → TemplateParseException")
    void testDefaultVarInvalidName() { assertThrows(TemplateParseException.class, () -> parser.parse("{{a-b:default}}")); }

    // ---- 嵌套 ----
    @Test @DisplayName("嵌套 {{#if}} → getVariables 含两层变量")
    void testNestedIf() {
        CompiledTemplate t = parser.parse("{{#if a}}o{{#if b}}i{{/if}}{{/if}}");
        assertEquals(Set.of("a", "b"), t.getVariables());
    }

    @Test @DisplayName("嵌套 {{#if}} 三层的变量")
    void testDeepNestedIf() {
        CompiledTemplate t = parser.parse("{{#if a}}{{#if b}}{{#if c}}deep{{/if}}{{/if}}{{/if}}");
        assertEquals(Set.of("a", "b", "c"), t.getVariables());
    }

    @Test @DisplayName("嵌套 {{#if}} 内含变量")
    void testNestedIfWithVar() {
        CompiledTemplate t = parser.parse("{{#if a}}{{#if b}}x={{b}}{{/if}}{{/if}}");
        assertEquals(Set.of("a", "b"), t.getVariables());
    }

    @Test @DisplayName("转义后的 {{#if}} 不是嵌套，应解析成功")
    void testEscapedIfNotNested() {
        CompiledTemplate t = parser.parse("{{#if debug}}语法: \\{{#if condition}}...\\{{/if}}{{/if}}");
        assertEquals(Set.of("debug"), t.getVariables());
    }

    // ---- 综合 ----
    @Test @DisplayName("复杂模板：文本 + var + if + where + default")
    void testComplexTemplate() {
        CompiledTemplate t = parser.parse(
            "SELECT * FROM {{table}}\n{{#where}}{{#if status}}AND status = {{status}}{{/if}}{{/where}}\nORDER BY {{sort:created_at}}\nLIMIT {{limit:20}}");
        assertEquals(Set.of("table", "status", "sort", "limit"), t.getVariables());
    }

    @Test @DisplayName("SQL 注释内的 {{}} 仍被解析（Parser 不感知 SQL）")
    void testSqlComment() { assertEquals(Set.of("comment", "status"), parser.parse("-- {{comment}}\nWHERE s = {{status}}").getVariables()); }

    @Test @DisplayName("SQL 字符串内的 {{}} 仍被解析")
    void testSqlStringLiteral() { assertEquals(Set.of("literal", "var"), parser.parse("SELECT '{{literal}}' AS c, {{var}}").getVariables()); }

    @Test @DisplayName("getVariables 返回不可变集合")
    void testGetVariablesImmutable() {
        Set<String> vars = parser.parse("{{a}}").getVariables();
        assertThrows(UnsupportedOperationException.class, () -> vars.add("b"));
    }
}
