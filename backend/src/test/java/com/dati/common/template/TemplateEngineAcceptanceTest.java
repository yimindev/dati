package com.dati.common.template;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("模板引擎验收测试（US-5.5 AC-1 ~ AC-12）")
class TemplateEngineAcceptanceTest {

    private TemplateParser parser;
    private TextRenderer textRenderer;
    private SqlRenderer sqlRenderer;

    @BeforeEach
    void setUp() {
        parser = new HandlebarsStyleParser();
        textRenderer = new TextRendererImpl();
        sqlRenderer = new SqlRendererImpl();
    }

    @Test @DisplayName("AC-1: parse('{{name}}') 成功，getVariables() 返回 ['name']")
    void ac1() { assertEquals(Set.of("name"), parser.parse("{{name}}").getVariables()); }

    @Test @DisplayName("AC-2: parse('{{#if x}}text{{/if}}') 成功，getVariables() 返回 ['x']")
    void ac2() { assertEquals(Set.of("x"), parser.parse("{{#if x}}text{{/if}}").getVariables()); }

    @Test @DisplayName("AC-3: parse('{{unclosed') 抛 TemplateParseException")
    void ac3() { assertThrows(TemplateParseException.class, () -> parser.parse("{{unclosed")); }

    @Test @DisplayName("AC-4: parse('{{#if x}}') 抛 TemplateParseException")
    void ac4() { assertThrows(TemplateParseException.class, () -> parser.parse("{{#if x}}")); }

    @Test @DisplayName("AC-5: parse('{{#unknown}}') 抛 TemplateParseException")
    void ac5() { assertThrows(TemplateParseException.class, () -> parser.parse("{{#unknown}}")); }

    @Test @DisplayName("AC-6: TextRenderer.render('Hello {{name}}', {name:'World'}) → 'Hello World'")
    void ac6() {
        assertEquals("Hello World", textRenderer.render(parser.parse("Hello {{name}}"), Map.of("name", "World")));
    }

    @Test @DisplayName("AC-7: TextRenderer.render('{{#if x}}shown{{/if}}', {}) → ''")
    void ac7() { assertEquals("", textRenderer.render(parser.parse("{{#if x}}shown{{/if}}"), Map.of())); }

    @Test @DisplayName("AC-8: SqlRenderer.render('WHERE id = {{id}}', {id:1}) → 'WHERE id = ?', binding=(id,1)")
    void ac8() {
        PreparedSql r = sqlRenderer.render(parser.parse("WHERE id = {{id}}"), Map.of("id", 1));
        assertEquals("WHERE id = ?", r.sql());
        assertEquals(1, r.bindings().size());
        assertEquals("id", r.bindings().getFirst().name());
        assertEquals(1, r.bindings().getFirst().value());
    }

    @Test @DisplayName("AC-9: SqlRenderer.render('{{#if status}}AND s={{s}}{{/if}}', {}) → 内容消失")
    void ac9() {
        PreparedSql r = sqlRenderer.render(parser.parse("{{#if status}}AND s={{s}}{{/if}}"), Map.of());
        assertEquals("", r.sql()); assertTrue(r.bindings().isEmpty());
    }

    @Test @DisplayName("AC-10: SqlRenderer where 全部跳过 → WHERE 消失")
    void ac10() {
        PreparedSql r = sqlRenderer.render(parser.parse("{{#where}}{{#if s}}AND s={{s}}{{/if}}{{/where}}"), Map.of());
        assertEquals("", r.sql()); assertTrue(r.bindings().isEmpty());
    }

    @Test @DisplayName("AC-11: SqlRenderer where 首个 AND 被裁剪")
    void ac11() {
        PreparedSql r = sqlRenderer.render(parser.parse("{{#where}}AND s={{s}}{{#if a}}AND a={{a}}{{/if}}{{/where}}"), Map.of("a", 1));
        assertEquals("WHERE s=?AND a=?", r.sql());
    }

    @Test @DisplayName("AC-12: SqlRenderer.render('IN ({{ids}})', {ids:[1,2,3]}) → 'IN (?, ?, ?)', 3 bindings")
    void ac12() {
        PreparedSql r = sqlRenderer.render(parser.parse("IN ({{ids}})"), Map.of("ids", List.of(1, 2, 3)));
        assertEquals("IN (?, ?, ?)", r.sql());
        assertEquals(3, r.bindings().size());
        assertEquals(1, r.bindings().get(0).value());
        assertEquals(2, r.bindings().get(1).value());
        assertEquals(3, r.bindings().get(2).value());
    }
}
