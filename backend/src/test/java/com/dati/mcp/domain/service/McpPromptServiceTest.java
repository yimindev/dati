package com.dati.mcp.domain.service;

import com.dati.TestFixtures;
import com.dati.base.exception.DatiException;
import com.dati.base.exception.ErrorCode;
import com.dati.common.template.CompiledTemplate;
import com.dati.common.template.TemplateParseException;
import com.dati.common.template.TemplateParser;
import com.dati.mcp.domain.model.McpPrompt;
import com.dati.mcp.repository.dao.McpPromptDAO;
import com.dati.mcp.repository.dao.McpServiceDAO;
import com.dati.mcp.repository.po.McpPromptPO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("McpPromptService unit tests")
class McpPromptServiceTest {

    @Mock
    private McpPromptDAO promptDAO;

    @Mock
    private McpServiceDAO mcpServiceDAO;

    @Mock
    private TemplateParser templateParser;

    @InjectMocks
    private McpPromptService promptService;

    private McpPrompt testPrompt;
    private McpPromptPO testPromptPO;

    @BeforeEach
    void setUp() {
        testPrompt = TestFixtures.createTestMcpPrompt();
        testPromptPO = new McpPromptPO();
        testPromptPO.setId(TestFixtures.TEST_MCP_PROMPT_ID);
        testPromptPO.setServiceId(TestFixtures.TEST_MCP_SERVICE_ID);
        testPromptPO.setName("analyze_table");
        testPromptPO.setDescription("分析指定表的数据");
        testPromptPO.setContent("请分析 {{table}} 表的数据。");
        testPromptPO.setParameters("[{\"name\":\"table\",\"description\":\"表名\",\"required\":true}]");
        testPromptPO.setEnabled(true);
    }

    @Test
    @DisplayName("Create Prompt - success")
    void createPrompt_success() {
        when(mcpServiceDAO.existsById(TestFixtures.TEST_MCP_SERVICE_ID)).thenReturn(true);
        when(promptDAO.existsByServiceIdAndName(TestFixtures.TEST_MCP_SERVICE_ID, "analyze_table")).thenReturn(false);
        CompiledTemplate compiled = mock(CompiledTemplate.class);
        when(compiled.getVariables()).thenReturn(Set.of("table"));
        when(templateParser.parse(testPrompt.getContent())).thenReturn(compiled);
        when(promptDAO.save(any(McpPromptPO.class))).thenReturn(testPromptPO);

        String result = promptService.createPrompt(TestFixtures.TEST_MCP_SERVICE_ID, testPrompt);

        assertThat(result).isEqualTo(TestFixtures.TEST_MCP_PROMPT_ID);
    }

    @Test
    @DisplayName("Create Prompt - throws on duplicate name")
    void createPrompt_nameExists_throws() {
        when(mcpServiceDAO.existsById(TestFixtures.TEST_MCP_SERVICE_ID)).thenReturn(true);
        when(promptDAO.existsByServiceIdAndName(TestFixtures.TEST_MCP_SERVICE_ID, "analyze_table")).thenReturn(true);

        DatiException ex = assertThrows(DatiException.class, () ->
            promptService.createPrompt(TestFixtures.TEST_MCP_SERVICE_ID, testPrompt)
        );
        assertThat(ex.getCode()).isEqualTo(ErrorCode.MS_PROMPT_NAME_EXISTS);
    }

    @Test
    @DisplayName("content references {{xxx}} not defined in parameters → rejected")
    void createPrompt_undefined_throws() {
        when(mcpServiceDAO.existsById(TestFixtures.TEST_MCP_SERVICE_ID)).thenReturn(true);
        testPrompt.setContent("请分析 {{table}} 和 {{status}} 的数据。");
        CompiledTemplate compiled = mock(CompiledTemplate.class);
        when(compiled.getVariables()).thenReturn(Set.of("table", "status"));
        when(templateParser.parse(testPrompt.getContent())).thenReturn(compiled);

        DatiException ex = assertThrows(DatiException.class, () ->
            promptService.createPrompt(TestFixtures.TEST_MCP_SERVICE_ID, testPrompt)
        );
        assertThat(ex.getCode()).isEqualTo(ErrorCode.MS_PROMPT_ARG_MISMATCH);
        assertThat(ex.getArgs()[0].toString()).contains("status");
    }

    @Test
    @DisplayName("parameter defined but not referenced in content → rejected (two-way validation)")
    void createPrompt_unusedParameter_throws() {
        when(mcpServiceDAO.existsById(TestFixtures.TEST_MCP_SERVICE_ID)).thenReturn(true);
        testPrompt.setContent("请分析 {{table}} 表的数据。");
        testPrompt.setParameters(java.util.List.of(
            TestFixtures.createTestPromptParameter("table", "表名", true),
            TestFixtures.createTestPromptParameter("limit", "限制条数", false)
        ));
        CompiledTemplate compiled = mock(CompiledTemplate.class);
        when(compiled.getVariables()).thenReturn(Set.of("table"));
        when(templateParser.parse(testPrompt.getContent())).thenReturn(compiled);

        DatiException ex = assertThrows(DatiException.class, () ->
            promptService.createPrompt(TestFixtures.TEST_MCP_SERVICE_ID, testPrompt)
        );
        assertThat(ex.getCode()).isEqualTo(ErrorCode.MS_PROMPT_ARG_MISMATCH);
        assertThat(ex.getArgs()[0].toString()).contains("limit");
    }

    @Test
    @DisplayName("content template syntax error (unclosed {{) → rejected")
    void createPrompt_unclosedBraces_throws() {
        when(mcpServiceDAO.existsById(TestFixtures.TEST_MCP_SERVICE_ID)).thenReturn(true);
        testPrompt.setContent("请分析 {{table 表的数据。");
        when(templateParser.parse(testPrompt.getContent()))
            .thenThrow(new TemplateParseException("Unclosed '{{'"));

        DatiException ex = assertThrows(DatiException.class, () ->
            promptService.createPrompt(TestFixtures.TEST_MCP_SERVICE_ID, testPrompt)
        );
        assertThat(ex.getCode()).isEqualTo(ErrorCode.MS_TEMPLATE_SYNTAX_ERROR);
    }

    @Test
    @DisplayName("content template syntax error (missing {{/if}}) → rejected")
    void createPrompt_unclosedIf_throws() {
        when(mcpServiceDAO.existsById(TestFixtures.TEST_MCP_SERVICE_ID)).thenReturn(true);
        testPrompt.setContent("{{#if table}}请分析 {{table}}");
        when(templateParser.parse(testPrompt.getContent()))
            .thenThrow(new TemplateParseException("Unclosed '{{#if table}}'"));

        DatiException ex = assertThrows(DatiException.class, () ->
            promptService.createPrompt(TestFixtures.TEST_MCP_SERVICE_ID, testPrompt)
        );
        assertThat(ex.getCode()).isEqualTo(ErrorCode.MS_TEMPLATE_SYNTAX_ERROR);
    }

    @Test
    @DisplayName("escaped \\{{ in content is not treated as a variable")
    void createPrompt_escapedPlaceholder_notAVariable() {
        when(mcpServiceDAO.existsById(TestFixtures.TEST_MCP_SERVICE_ID)).thenReturn(true);
        when(promptDAO.existsByServiceIdAndName(TestFixtures.TEST_MCP_SERVICE_ID, "analyze_table")).thenReturn(false);
        testPrompt.setContent("请使用 \\{{table}} 的写法。");
        testPrompt.setParameters(List.of());
        CompiledTemplate compiled = mock(CompiledTemplate.class);
        when(compiled.getVariables()).thenReturn(Set.of());
        when(templateParser.parse(testPrompt.getContent())).thenReturn(compiled);
        when(promptDAO.save(any(McpPromptPO.class))).thenReturn(testPromptPO);

        String result = promptService.createPrompt(TestFixtures.TEST_MCP_SERVICE_ID, testPrompt);
        assertThat(result).isEqualTo(TestFixtures.TEST_MCP_PROMPT_ID);
    }

    @Test
    @DisplayName("null content does not throw")
    void createPrompt_nullContent_allowed() {
        when(mcpServiceDAO.existsById(TestFixtures.TEST_MCP_SERVICE_ID)).thenReturn(true);
        when(promptDAO.existsByServiceIdAndName(TestFixtures.TEST_MCP_SERVICE_ID, "analyze_table")).thenReturn(false);
        testPrompt.setContent(null);
        testPrompt.setParameters(List.of());
        when(promptDAO.save(any(McpPromptPO.class))).thenReturn(testPromptPO);

        String result = promptService.createPrompt(TestFixtures.TEST_MCP_SERVICE_ID, testPrompt);
        assertThat(result).isEqualTo(TestFixtures.TEST_MCP_PROMPT_ID);
    }

    @Test
    @DisplayName("Update Prompt - success (with enabled flag)")
    void updatePrompt_success() {
        when(promptDAO.findByServiceIdAndId(TestFixtures.TEST_MCP_SERVICE_ID, TestFixtures.TEST_MCP_PROMPT_ID))
            .thenReturn(Optional.of(testPromptPO));
        CompiledTemplate compiled = mock(CompiledTemplate.class);
        when(compiled.getVariables()).thenReturn(Set.of("table"));
        when(templateParser.parse(testPrompt.getContent())).thenReturn(compiled);

        testPrompt.setEnabled(false);
        promptService.updatePrompt(testPrompt);

        verify(promptDAO).save(testPromptPO);
        assertThat(testPromptPO.getEnabled()).isFalse();
    }

    @Test
    @DisplayName("Update Prompt - throws when not found")
    void updatePrompt_notFound_throws() {
        when(promptDAO.findByServiceIdAndId(TestFixtures.TEST_MCP_SERVICE_ID, TestFixtures.TEST_MCP_PROMPT_ID))
            .thenReturn(Optional.empty());

        DatiException ex = assertThrows(DatiException.class, () ->
            promptService.updatePrompt(testPrompt)
        );
        assertThat(ex.getCode()).isEqualTo(ErrorCode.MS_PROMPT_NOT_FOUND);
    }

    @Test
    @DisplayName("Delete Prompt - success")
    void deletePrompt_success() {
        when(promptDAO.findByServiceIdAndId(TestFixtures.TEST_MCP_SERVICE_ID, TestFixtures.TEST_MCP_PROMPT_ID))
            .thenReturn(Optional.of(testPromptPO));

        promptService.deletePrompt(TestFixtures.TEST_MCP_SERVICE_ID, TestFixtures.TEST_MCP_PROMPT_ID);

        verify(promptDAO).delete(testPromptPO);
    }

    @Test
    @DisplayName("Query Prompt list - success")
    void listPrompts_returnsList() {
        when(promptDAO.findAllByServiceIdOrderByCreatedAtDesc(TestFixtures.TEST_MCP_SERVICE_ID))
            .thenReturn(List.of(testPromptPO));

        List<McpPrompt> result = promptService.listPrompts(TestFixtures.TEST_MCP_SERVICE_ID);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getName()).isEqualTo("analyze_table");
    }

    @Test
    @DisplayName("replacePrompts - deletes existing and inserts snapshot content")
    void replacePrompts_replacesAll() {
        McpPrompt restoredPrompt = TestFixtures.createTestMcpPrompt();
        restoredPrompt.setName("restored_prompt");

        promptService.replacePrompts(TestFixtures.TEST_MCP_SERVICE_ID, List.of(restoredPrompt));

        verify(promptDAO).deleteAllByServiceId(TestFixtures.TEST_MCP_SERVICE_ID);
        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        verify(promptDAO).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(1);
        assertThat(((McpPromptPO) captor.getValue().getFirst()).getName()).isEqualTo("restored_prompt");
    }
}
