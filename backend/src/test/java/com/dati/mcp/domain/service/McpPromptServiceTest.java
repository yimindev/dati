package com.dati.mcp.domain.service;

import com.dati.TestFixtures;
import com.dati.base.exception.DatiException;
import com.dati.base.exception.ErrorCode;
import com.dati.mcp.domain.model.McpPrompt;
import com.dati.mcp.repository.dao.McpPromptDAO;
import com.dati.mcp.repository.dao.McpServiceDAO;
import com.dati.mcp.repository.po.McpPromptPO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("McpPromptService 单元测试")
class McpPromptServiceTest {

    @Mock
    private McpPromptDAO promptDAO;

    @Mock
    private McpServiceDAO mcpServiceDAO;

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
    @DisplayName("创建 Prompt - 成功")
    void createPrompt_success() {
        when(mcpServiceDAO.existsById(TestFixtures.TEST_MCP_SERVICE_ID)).thenReturn(true);
        when(promptDAO.existsByServiceIdAndName(TestFixtures.TEST_MCP_SERVICE_ID, "analyze_table")).thenReturn(false);
        when(promptDAO.save(any(McpPromptPO.class))).thenReturn(testPromptPO);

        String result = promptService.createPrompt(TestFixtures.TEST_MCP_SERVICE_ID, testPrompt);

        assertThat(result).isEqualTo(TestFixtures.TEST_MCP_PROMPT_ID);
    }

    @Test
    @DisplayName("创建 Prompt - name 重复抛出异常")
    void createPrompt_nameExists_throws() {
        when(mcpServiceDAO.existsById(TestFixtures.TEST_MCP_SERVICE_ID)).thenReturn(true);
        when(promptDAO.existsByServiceIdAndName(TestFixtures.TEST_MCP_SERVICE_ID, "analyze_table")).thenReturn(true);

        DatiException ex = assertThrows(DatiException.class, () ->
            promptService.createPrompt(TestFixtures.TEST_MCP_SERVICE_ID, testPrompt)
        );
        assertThat(ex.getCode()).isEqualTo(ErrorCode.MS_PROMPT_NAME_EXISTS);
    }

    @Test
    @DisplayName("content 中 {{xxx}} 未在 parameters 中定义 → 拒绝")
    void createPrompt_undefined_throws() {
        when(mcpServiceDAO.existsById(TestFixtures.TEST_MCP_SERVICE_ID)).thenReturn(true);
        testPrompt.setContent("请分析 {{table}} 和 {{status}} 的数据。");

        DatiException ex = assertThrows(DatiException.class, () ->
            promptService.createPrompt(TestFixtures.TEST_MCP_SERVICE_ID, testPrompt)
        );
        assertThat(ex.getCode()).isEqualTo(ErrorCode.MS_PROMPT_ARG_MISMATCH);
        assertThat(ex.getArgs()[0].toString()).contains("status");
    }

    @Test
    @DisplayName("parameters 中多余定义不阻塞保存")
    void createPrompt_unusedParam_allowed() {
        when(mcpServiceDAO.existsById(TestFixtures.TEST_MCP_SERVICE_ID)).thenReturn(true);
        when(promptDAO.existsByServiceIdAndName(TestFixtures.TEST_MCP_SERVICE_ID, "analyze_table")).thenReturn(false);
        testPrompt.setParameters(java.util.List.of(
            TestFixtures.createTestPromptParameter("table", "表名", true),
            TestFixtures.createTestPromptParameter("limit", "限制", false)
        ));
        when(promptDAO.save(any(McpPromptPO.class))).thenReturn(testPromptPO);

        String result = promptService.createPrompt(TestFixtures.TEST_MCP_SERVICE_ID, testPrompt);

        assertThat(result).isEqualTo(TestFixtures.TEST_MCP_PROMPT_ID);
    }

    @Test
    @DisplayName("更新 Prompt - 成功（含 enabled 开关）")
    void updatePrompt_success() {
        when(promptDAO.findByServiceIdAndId(TestFixtures.TEST_MCP_SERVICE_ID, TestFixtures.TEST_MCP_PROMPT_ID))
            .thenReturn(Optional.of(testPromptPO));

        testPrompt.setEnabled(false);
        promptService.updatePrompt(TestFixtures.TEST_MCP_SERVICE_ID, TestFixtures.TEST_MCP_PROMPT_ID, testPrompt);

        assertThat(testPromptPO.getEnabled()).isFalse();
    }

    @Test
    @DisplayName("更新 Prompt - 不存在抛出异常")
    void updatePrompt_notFound_throws() {
        when(promptDAO.findByServiceIdAndId(TestFixtures.TEST_MCP_SERVICE_ID, TestFixtures.TEST_MCP_PROMPT_ID))
            .thenReturn(Optional.empty());

        DatiException ex = assertThrows(DatiException.class, () ->
            promptService.updatePrompt(TestFixtures.TEST_MCP_SERVICE_ID, TestFixtures.TEST_MCP_PROMPT_ID, testPrompt)
        );
        assertThat(ex.getCode()).isEqualTo(ErrorCode.MS_PROMPT_NOT_FOUND);
    }

    @Test
    @DisplayName("删除 Prompt - 成功")
    void deletePrompt_success() {
        when(promptDAO.findByServiceIdAndId(TestFixtures.TEST_MCP_SERVICE_ID, TestFixtures.TEST_MCP_PROMPT_ID))
            .thenReturn(Optional.of(testPromptPO));

        promptService.deletePrompt(TestFixtures.TEST_MCP_SERVICE_ID, TestFixtures.TEST_MCP_PROMPT_ID);

        verify(promptDAO).delete(testPromptPO);
    }

    @Test
    @DisplayName("查询 Prompt 列表 - 成功")
    void listPrompts_returnsList() {
        when(promptDAO.findAllByServiceIdOrderByCreatedAtDesc(TestFixtures.TEST_MCP_SERVICE_ID))
            .thenReturn(List.of(testPromptPO));

        List<McpPrompt> result = promptService.listPrompts(TestFixtures.TEST_MCP_SERVICE_ID);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getName()).isEqualTo("analyze_table");
    }
}
