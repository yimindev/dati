package com.dati.mcp.server.controller;

import com.dati.base.exception.GlobalExceptionHandler;
import com.dati.common.template.HandlebarsStyleParser;
import com.dati.common.template.SqlRenderer;
import com.dati.common.template.TextRenderer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import com.dati.auth.authentication.User;
import com.dati.base.RequestContext;
import com.dati.mcp.domain.service.SystemVariableResolver;
import org.junit.jupiter.api.AfterEach;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("TemplatePreviewController unit tests")
class TemplatePreviewControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        RequestContext.clear();
        TemplatePreviewController controller = new TemplatePreviewController(
                new HandlebarsStyleParser(),
                new TextRenderer(),
                new SqlRenderer(),
                new SystemVariableResolver()
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
    }

    @AfterEach
    void tearDown() {
        RequestContext.clear();
    }

    // ===== Text 模式 =====

    @Test
    @DisplayName("Text - simple variable substitution")
    void testTextModeSimple() throws Exception {
        Map<String, Object> body = Map.of(
                "mode", "TEXT",
                "template", "请分析 {{table}} 表的数据。",
                "values", Map.of("table", "tasks")
        );

        mockMvc.perform(post("/v1/template/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rendered").value("请分析 tasks 表的数据。"));
    }

    @Test
    @DisplayName("Text - {{#if}} condition true")
    void testTextModeIfBlock() throws Exception {
        Map<String, Object> body = Map.of(
                "mode", "TEXT",
                "template", "{{#if ctx}}补充：{{ctx}}{{/if}}",
                "values", Map.of("ctx", "华东区关注")
        );

        mockMvc.perform(post("/v1/template/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rendered").value("补充：华东区关注"));
    }

    @Test
    @DisplayName("Text - {{#if}} condition false, skipped")
    void testTextModeIfBlockSkipped() throws Exception {
        Map<String, Object> body = Map.of(
                "mode", "TEXT",
                "template", "前 {{#if ctx}}中{{ctx}}后{{/if}} 尾",
                "values", Map.of()
        );

        mockMvc.perform(post("/v1/template/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rendered").value("前  尾"));
    }

    // ===== SQL 模式 — 按值类型格式化 =====

    @Test
    @DisplayName("SQL - string values quoted")
    void testSqlStringValueQuoted() throws Exception {
        Map<String, Object> body = Map.of(
                "mode", "SQL",
                "template", "SELECT * FROM tasks WHERE status = {{status}}",
                "values", Map.of("status", "todo")
        );

        mockMvc.perform(post("/v1/template/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rendered").value("SELECT * FROM tasks WHERE status = 'todo'"));
    }

    @Test
    @DisplayName("SQL - numbers not quoted")
    void testSqlNumberValueUnquoted() throws Exception {
        Map<String, Object> body = Map.of(
                "mode", "SQL",
                "template", "SELECT * FROM tasks WHERE id = {{id}}",
                "values", Map.of("id", 42)
        );

        mockMvc.perform(post("/v1/template/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rendered").value("SELECT * FROM tasks WHERE id = 42"));
    }

    @Test
    @DisplayName("SQL - booleans not quoted")
    void testSqlBooleanValueUnquoted() throws Exception {
        Map<String, Object> body = Map.of(
                "mode", "SQL",
                "template", "SELECT * FROM tasks WHERE is_active = {{active}}",
                "values", Map.of("active", true)
        );

        mockMvc.perform(post("/v1/template/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rendered").value("SELECT * FROM tasks WHERE is_active = true"));
    }

    @Test
    @DisplayName("SQL - null value renders NULL")
    void testSqlNullValue() throws Exception {
        Map<String, Object> body = Map.of(
                "mode", "SQL",
                "template", "SELECT * FROM tasks WHERE assignee = {{assignee}}",
                "values", Collections.singletonMap("assignee", null)
        );

        mockMvc.perform(post("/v1/template/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rendered").value("SELECT * FROM tasks WHERE assignee = NULL"));
    }

    @Test
    @DisplayName("SQL - numeric arrays inlined without quotes")
    void testSqlArrayNumbers() throws Exception {
        Map<String, Object> body = Map.of(
                "mode", "SQL",
                "template", "SELECT * FROM tasks WHERE dept_id IN ({{ids}})",
                "values", Map.of("ids", List.of(1, 2, 3))
        );

        mockMvc.perform(post("/v1/template/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rendered").value("SELECT * FROM tasks WHERE dept_id IN (1, 2, 3)"));
    }

    @Test
    @DisplayName("SQL - string arrays, each element quoted")
    void testSqlArrayStrings() throws Exception {
        Map<String, Object> body = Map.of(
                "mode", "SQL",
                "template", "SELECT * FROM tasks WHERE status IN ({{statuses}})",
                "values", Map.of("statuses", List.of("active", "pending"))
        );

        mockMvc.perform(post("/v1/template/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rendered").value("SELECT * FROM tasks WHERE status IN ('active', 'pending')"));
    }

    // ===== SQL 模式 — {{{var}}} 原始变量 =====

    @Test
    @DisplayName("SQL - {{{var}}} raw variable inlined directly")
    void testSqlRawVar() throws Exception {
        Map<String, Object> body = Map.of(
                "mode", "SQL",
                "template", "SELECT * FROM {{{table}}} WHERE status = {{status}}",
                "values", Map.of("table", "orders", "status", "active")
        );

        mockMvc.perform(post("/v1/template/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rendered").value("SELECT * FROM orders WHERE status = 'active'"));
    }

    @Test
    @DisplayName("SQL - {{{var:default}}} uses default when missing")
    void testSqlRawDefault() throws Exception {
        Map<String, Object> body = Map.of(
                "mode", "SQL",
                "template", "SELECT * FROM tasks ORDER BY {{{sort:created_at}}}",
                "values", Map.of()
        );

        mockMvc.perform(post("/v1/template/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rendered").value("SELECT * FROM tasks ORDER BY created_at"));
    }

    @Test
    @DisplayName("SQL — {{{var}}} Array → 400")
    void testSqlRawArrayBadRequest() throws Exception {
        Map<String, Object> body = Map.of(
                "mode", "SQL",
                "template", "SELECT {{{ids}}}",
                "values", Map.of("ids", List.of(1, 2, 3))
        );

        mockMvc.perform(post("/v1/template/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CM001"));
    }

    // ===== SQL 模式 — 完整模板 =====

    @Test
    @DisplayName("SQL - full template: table({{{}}}) + where + if + sort({{{}}}) + limit")
    void testSqlModeFullTemplate() throws Exception {
        Map<String, Object> body = Map.of(
                "mode", "SQL",
                "template", "SELECT * FROM {{{table}}}\n{{#where}}\n  {{#if status}}AND status = {{status}}{{/if}}\n  {{#if priority}}AND priority = {{priority}}{{/if}}\n{{/where}}\nORDER BY {{{sort:created_at}}}\nLIMIT {{limit:20}}",
                "values", Map.of(
                        "table", "tasks",
                        "status", "todo",
                        "priority", "high"
                )
        );

        mockMvc.perform(post("/v1/template/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rendered").value(
                        "SELECT * FROM tasks\nWHERE status = 'todo'\n  AND priority = 'high'\nORDER BY created_at\nLIMIT 20"));
    }

    // ===== 错误处理 =====

    @Test
    @DisplayName("Template syntax error → 400")
    void testSyntaxError() throws Exception {
        Map<String, Object> body = Map.of(
                "mode", "TEXT",
                "template", "{{unclosed",
                "values", Map.of()
        );

        mockMvc.perform(post("/v1/template/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CM001"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("Empty mode → 400")
    void testModeEmpty() throws Exception {
        Map<String, Object> body = Map.of(
                "mode", "",
                "template", "hello",
                "values", Map.of()
        );

        mockMvc.perform(post("/v1/template/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Unknown mode → 400")
    void testUnknownMode() throws Exception {
        Map<String, Object> body = Map.of(
                "mode", "json",
                "template", "hello",
                "values", Map.of()
        );

        mockMvc.perform(post("/v1/template/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    // ===== 参数提取 =====

    @Test
    @DisplayName("Extract params - includes plain, default and raw variables")
    void testExtractVariables() throws Exception {
        Map<String, Object> body = Map.of(
                "template", "SELECT * FROM {{{table}}} WHERE id = {{id}} AND name = {{name:default}}"
        );

        mockMvc.perform(post("/v1/template/extract")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.variables").isArray())
                .andExpect(jsonPath("$.variables.length()").value(3))
                .andExpect(jsonPath("$.variables[?(@ == 'table')]").exists())
                .andExpect(jsonPath("$.variables[?(@ == 'id')]").exists())
                .andExpect(jsonPath("$.variables[?(@ == 'name')]").exists());
    }

    @Test
    @DisplayName("Extract params - handles variables in if/where blocks")
    void testExtractVariablesInBlocks() throws Exception {
        Map<String, Object> body = Map.of(
                "template", "SELECT * FROM tasks {{#where}}{{#if status}}AND status = {{status}}{{/if}}{{/where}}"
        );

        mockMvc.perform(post("/v1/template/extract")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.variables").isArray())
                .andExpect(jsonPath("$.variables.length()").value(1))
                .andExpect(jsonPath("$.variables[0]").value("status"));
    }

    @Test
    @DisplayName("Extract params - syntax error → 400")
    void testExtractVariablesSyntaxError() throws Exception {
        Map<String, Object> body = Map.of(
                "template", "{{unclosed"
        );

        mockMvc.perform(post("/v1/template/extract")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Extract params - empty template → 400")
    void testExtractVariablesEmptyTemplate() throws Exception {
        Map<String, Object> body = Map.of(
                "template", ""
        );

        mockMvc.perform(post("/v1/template/extract")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Extract params - static template (no variables) → empty set")
    void testExtractVariablesStaticTemplate() throws Exception {
        Map<String, Object> body = Map.of(
                "template", "SELECT 1"
        );

        mockMvc.perform(post("/v1/template/extract")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.variables").isArray())
                .andExpect(jsonPath("$.variables.length()").value(0));
    }

    @Test
    @DisplayName("Extract params - blank template (whitespace only) → 400")
    void testExtractVariablesBlankTemplate() throws Exception {
        Map<String, Object> body = Map.of(
                "template", "   "
        );

        mockMvc.perform(post("/v1/template/extract")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Extract params - filters out system variables (_user.*, _now, _date)")
    void testExtractVariablesWithSystemVariables() throws Exception {
        Map<String, Object> body = Map.of(
                "template", "SELECT * FROM tasks WHERE owner_id = {{_user.id}} AND name = {{_user.name}} AND created_at <= {{_now}} AND status = {{status}} AND id = {{id}}"
        );

        mockMvc.perform(post("/v1/template/extract")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.variables").isArray())
                .andExpect(jsonPath("$.variables.length()").value(2))
                .andExpect(jsonPath("$.variables[?(@ == 'status')]").exists())
                .andExpect(jsonPath("$.variables[?(@ == 'id')]").exists())
                .andExpect(jsonPath("$.variables[?(@ == '_user.id')]").doesNotExist())
                .andExpect(jsonPath("$.variables[?(@ == '_user.name')]").doesNotExist())
                .andExpect(jsonPath("$.variables[?(@ == '_now')]").doesNotExist());
    }

    @Test
    @DisplayName("Preview - auto-injects system variables from RequestContext")
    void testPreviewWithSystemVariables() throws Exception {
        User user = new User();
        user.setId("usr-456");
        user.setName("bob");
        user.setDisplayName("Bob Builder");
        RequestContext.setUser(user);

        Map<String, Object> body = Map.of(
                "mode", "SQL",
                "template", "SELECT * FROM tasks WHERE owner = {{_user.id}} AND status = {{status}}",
                "values", Map.of("status", "open", "_user.id", "fake-injected-id")
        );

        mockMvc.perform(post("/v1/template/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rendered").value("SELECT * FROM tasks WHERE owner = 'usr-456' AND status = 'open'"));
    }
}
