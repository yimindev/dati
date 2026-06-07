package com.dati.mcp.server.controller;

import com.dati.base.exception.GlobalExceptionHandler;
import com.dati.common.template.HandlebarsStyleParser;
import com.dati.common.template.SqlRendererImpl;
import com.dati.common.template.TextRendererImpl;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("TemplatePreviewController 单元测试")
class TemplatePreviewControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        TemplatePreviewController controller = new TemplatePreviewController(
                new HandlebarsStyleParser(),
                new TextRendererImpl(),
                new SqlRendererImpl()
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
    }

    // ===== Text 模式 =====

    @Test
    @DisplayName("Text — 简单变量替换")
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
    @DisplayName("Text — {{#if}} 条件成立")
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
    @DisplayName("Text — {{#if}} 条件不成立时跳过")
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
    @DisplayName("SQL — 字符串值加引号")
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
    @DisplayName("SQL — 数值不加引号")
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
    @DisplayName("SQL — 布尔值不加引号")
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
    @DisplayName("SQL — null 值输出 NULL")
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
    @DisplayName("SQL — 数组数值内联，无引号")
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
    @DisplayName("SQL — 数组字符串，每个元素加引号")
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
    @DisplayName("SQL — {{{var}}} 原始变量直接内联")
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
    @DisplayName("SQL — {{{var:default}}} 无值时走默认")
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
    @DisplayName("SQL — 完整模板：table({{{}}}) + where + if + sort({{{}}}) + limit")
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
    @DisplayName("模板语法错误 → 400")
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
    @DisplayName("mode 为空 → 400")
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
    @DisplayName("未知 mode → 400")
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
}
