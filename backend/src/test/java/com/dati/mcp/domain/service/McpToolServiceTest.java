package com.dati.mcp.domain.service;

import com.dati.TestFixtures;
import com.dati.base.exception.DatiException;
import com.dati.base.exception.ErrorCode;
import com.dati.common.template.CompiledTemplate;
import com.dati.common.template.TemplateParseException;
import com.dati.common.template.TemplateParser;
import com.dati.mcp.domain.model.McpCustomTool;
import com.dati.mcp.domain.model.McpPrebuiltToolConfig;
import com.dati.mcp.domain.model.McpToolType;
import com.dati.mcp.domain.model.ToolConfig;
import com.dati.mcp.repository.dao.McpCustomToolDAO;
import com.dati.mcp.repository.dao.McpPrebuiltToolConfigDAO;
import com.dati.mcp.repository.dao.McpServiceDAO;
import com.dati.mcp.repository.po.McpCustomToolPO;
import com.dati.mcp.repository.po.McpPrebuiltToolConfigPO;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("McpToolService 单元测试")
class McpToolServiceTest {

    @Mock
    private McpPrebuiltToolConfigDAO prebuiltDAO;

    @Mock
    private McpCustomToolDAO customToolDAO;

    @Mock
    private McpServiceDAO mcpServiceDAO;

    @Mock
    private TemplateParser templateParser;

    @InjectMocks
    private McpToolService mcpToolService;

    private McpCustomTool testCustomTool;
    private McpCustomToolPO testCustomToolPO;

    @BeforeEach
    void setUp() {
        testCustomTool = TestFixtures.createTestCustomTool();
        testCustomToolPO = new McpCustomToolPO();
        testCustomToolPO.setId(TestFixtures.TEST_MCP_CUSTOM_TOOL_ID);
        testCustomToolPO.setServiceId(TestFixtures.TEST_MCP_SERVICE_ID);
        testCustomToolPO.setName("list_tasks");
        testCustomToolPO.setTitle("查询任务列表");
        testCustomToolPO.setDescription("按状态查询所有任务");
        testCustomToolPO.setEnabled(true);
    }

    // ── 列表 ──

    @Test
    @DisplayName("列表 - 无 DB 记录时返回预置工具默认值 + 空自定义列表")
    void listTools_noConfig_returnsDefaults() {
        when(prebuiltDAO.findAllByServiceId(TestFixtures.TEST_MCP_SERVICE_ID)).thenReturn(List.of());
        when(customToolDAO.findAllByServiceIdOrderByCreatedAtDesc(TestFixtures.TEST_MCP_SERVICE_ID)).thenReturn(List.of());

        ToolsResult result = mcpToolService.listTools(TestFixtures.TEST_MCP_SERVICE_ID);

        assertThat(result.prebuilt()).hasSize(3);
    }

    @Test
    @DisplayName("列表 - 包含 DB 中已配置的预置工具和自定义工具")
    void listTools_withConfig_returnsAll() {
        McpPrebuiltToolConfigPO esPO = new McpPrebuiltToolConfigPO();
        esPO.setId("pre-cfg-001");
        esPO.setServiceId(TestFixtures.TEST_MCP_SERVICE_ID);
        esPO.setToolType(McpToolType.EXECUTE_SQL);
        esPO.setEnabled(false);
        esPO.setConfig("{\"timeout\":60,\"max_rows\":500,\"confirm_required\":true,\"sql_policy\":{\"allow_select\":true,\"allow_insert\":false,\"allow_update\":false,\"allow_delete\":false,\"allow_ddl\":false,\"allow_multi\":false}}");

        when(prebuiltDAO.findAllByServiceId(TestFixtures.TEST_MCP_SERVICE_ID)).thenReturn(List.of(esPO));
        when(customToolDAO.findAllByServiceIdOrderByCreatedAtDesc(TestFixtures.TEST_MCP_SERVICE_ID)).thenReturn(List.of(testCustomToolPO));

        ToolsResult result = mcpToolService.listTools(TestFixtures.TEST_MCP_SERVICE_ID);

        assertThat(result.prebuilt()).hasSize(3);
        assertThat(result.custom()).hasSize(1);
    }

    // ── 更新预置工具 ──

    @Test
    @DisplayName("更新预置工具 - 无记录时创建新行")
    void updatePrebuiltTool_noRecord_createsNew() {
        when(prebuiltDAO.findByServiceIdAndToolType(TestFixtures.TEST_MCP_SERVICE_ID, McpToolType.EXECUTE_SQL))
            .thenReturn(Optional.empty());

        ToolConfig.ExecuteSqlConfig execCfg = new ToolConfig.ExecuteSqlConfig();
        execCfg.setTimeout(60);
        execCfg.setMaxRows(500);

        McpPrebuiltToolConfig input = new McpPrebuiltToolConfig();
        input.setEnabled(false);
        input.setConfig(execCfg);

        mcpToolService.updatePrebuiltTool(TestFixtures.TEST_MCP_SERVICE_ID, McpToolType.EXECUTE_SQL, input);

        ArgumentCaptor<McpPrebuiltToolConfigPO> captor = ArgumentCaptor.forClass(McpPrebuiltToolConfigPO.class);
        verify(prebuiltDAO).save(captor.capture());
        McpPrebuiltToolConfigPO saved = captor.getValue();
        assertThat(saved.getServiceId()).isEqualTo(TestFixtures.TEST_MCP_SERVICE_ID);
        assertThat(saved.getToolType()).isEqualTo(McpToolType.EXECUTE_SQL);
        assertThat(saved.getEnabled()).isFalse();
    }

    @Test
    @DisplayName("更新预置工具 - 有记录时更新")
    void updatePrebuiltTool_existing_updates() {
        McpPrebuiltToolConfigPO existing = new McpPrebuiltToolConfigPO();
        existing.setId("pre-cfg-001");
        existing.setServiceId(TestFixtures.TEST_MCP_SERVICE_ID);
        existing.setToolType(McpToolType.EXECUTE_SQL);
        existing.setEnabled(true);
        when(prebuiltDAO.findByServiceIdAndToolType(TestFixtures.TEST_MCP_SERVICE_ID, McpToolType.EXECUTE_SQL))
            .thenReturn(Optional.of(existing));

        McpPrebuiltToolConfig input = new McpPrebuiltToolConfig();
        input.setEnabled(false);
        mcpToolService.updatePrebuiltTool(TestFixtures.TEST_MCP_SERVICE_ID, McpToolType.EXECUTE_SQL, input);

        verify(prebuiltDAO).save(existing);
        assertThat(existing.getEnabled()).isFalse();
    }

    // ── 创建自定义工具 ──

    @Test
    @DisplayName("创建自定义工具 - 成功")
    void createCustomTool_success() {
        when(mcpServiceDAO.existsById(TestFixtures.TEST_MCP_SERVICE_ID)).thenReturn(true);
        when(customToolDAO.existsByServiceIdAndName(TestFixtures.TEST_MCP_SERVICE_ID, "list_tasks")).thenReturn(false);
        CompiledTemplate compiled = mock(CompiledTemplate.class);
        when(compiled.getVariables()).thenReturn(Set.of());
        when(templateParser.parse(anyString())).thenReturn(compiled);
        when(customToolDAO.save(any(McpCustomToolPO.class))).thenReturn(testCustomToolPO);

        String result = mcpToolService.createCustomTool(TestFixtures.TEST_MCP_SERVICE_ID, testCustomTool);

        assertThat(result).isEqualTo(TestFixtures.TEST_MCP_CUSTOM_TOOL_ID);
        verify(customToolDAO).save(any(McpCustomToolPO.class));
    }

    @Test
    @DisplayName("创建自定义工具 - name 格式不合法抛出异常")
    void createCustomTool_invalidName_throws() {
        when(mcpServiceDAO.existsById(TestFixtures.TEST_MCP_SERVICE_ID)).thenReturn(true);
        testCustomTool.setName("invalid name!");

        DatiException ex = assertThrows(DatiException.class, () ->
            mcpToolService.createCustomTool(TestFixtures.TEST_MCP_SERVICE_ID, testCustomTool)
        );
        assertThat(ex.getCode()).isEqualTo(ErrorCode.MS_TOOL_NAME_INVALID);
        verify(customToolDAO, never()).save(any());
    }

    @Test
    @DisplayName("创建自定义工具 - name 重复抛出异常")
    void createCustomTool_nameExists_throws() {
        when(mcpServiceDAO.existsById(TestFixtures.TEST_MCP_SERVICE_ID)).thenReturn(true);
        when(customToolDAO.existsByServiceIdAndName(TestFixtures.TEST_MCP_SERVICE_ID, "list_tasks")).thenReturn(true);

        DatiException ex = assertThrows(DatiException.class, () ->
            mcpToolService.createCustomTool(TestFixtures.TEST_MCP_SERVICE_ID, testCustomTool)
        );
        assertThat(ex.getCode()).isEqualTo(ErrorCode.MS_TOOL_NAME_EXISTS);
    }

    @Test
    @DisplayName("创建自定义工具 - 服务不存在抛出异常")
    void createCustomTool_serviceNotFound_throws() {
        when(mcpServiceDAO.existsById(TestFixtures.TEST_MCP_SERVICE_ID)).thenReturn(false);

        DatiException ex = assertThrows(DatiException.class, () ->
            mcpToolService.createCustomTool(TestFixtures.TEST_MCP_SERVICE_ID, testCustomTool)
        );
        assertThat(ex.getCode()).isEqualTo(ErrorCode.MS_SERVICE_NOT_FOUND);
    }

    @Test
    @DisplayName("创建 Parameterized SQL — 模板语法错误（{{ 不闭合）→ 拒绝")
    void createCustomTool_templateSyntaxError_throws() {
        when(mcpServiceDAO.existsById(TestFixtures.TEST_MCP_SERVICE_ID)).thenReturn(true);
        when(customToolDAO.existsByServiceIdAndName(TestFixtures.TEST_MCP_SERVICE_ID, "list_tasks")).thenReturn(false);

        ToolConfig.ParamSqlConfig badCfg = new ToolConfig.ParamSqlConfig();
        badCfg.setDataSourceId(TestFixtures.TEST_DATASOURCE_ID);
        badCfg.setSqlTemplate("SELECT * FROM tasks WHERE status = {{status");
        testCustomTool.setConfig(badCfg);
        when(templateParser.parse(anyString()))
            .thenThrow(new TemplateParseException("Unclosed '{{'"));

        DatiException ex = assertThrows(DatiException.class, () ->
            mcpToolService.createCustomTool(TestFixtures.TEST_MCP_SERVICE_ID, testCustomTool)
        );
        assertThat(ex.getCode()).isEqualTo(ErrorCode.MS_TEMPLATE_SYNTAX_ERROR);
        verify(customToolDAO, never()).save(any());
    }

    @Test
    @DisplayName("创建 Parameterized SQL — 参数名不一致：模板引用未定义参数 → 拒绝")
    void createCustomTool_undefinedParameter_throws() {
        when(mcpServiceDAO.existsById(TestFixtures.TEST_MCP_SERVICE_ID)).thenReturn(true);
        when(customToolDAO.existsByServiceIdAndName(TestFixtures.TEST_MCP_SERVICE_ID, "list_tasks")).thenReturn(false);

        ToolConfig.ParamSqlConfig cfg = new ToolConfig.ParamSqlConfig();
        cfg.setDataSourceId(TestFixtures.TEST_DATASOURCE_ID);
        cfg.setSqlTemplate("SELECT * FROM tasks WHERE status = {{status}}");
        cfg.setParameters(List.of());
        testCustomTool.setConfig(cfg);
        CompiledTemplate compiled = mock(CompiledTemplate.class);
        when(compiled.getVariables()).thenReturn(Set.of("status"));
        when(templateParser.parse(anyString())).thenReturn(compiled);

        DatiException ex = assertThrows(DatiException.class, () ->
            mcpToolService.createCustomTool(TestFixtures.TEST_MCP_SERVICE_ID, testCustomTool)
        );
        assertThat(ex.getCode()).isEqualTo(ErrorCode.MS_TOOL_ARG_MISMATCH);
        assertThat(ex.getArgs()[0].toString()).contains("status");
        verify(customToolDAO, never()).save(any());
    }

    @Test
    @DisplayName("创建 Parameterized SQL — 有效模板语法通过校验")
    void createCustomTool_validTemplate_succeeds() {
        when(mcpServiceDAO.existsById(TestFixtures.TEST_MCP_SERVICE_ID)).thenReturn(true);
        when(customToolDAO.existsByServiceIdAndName(TestFixtures.TEST_MCP_SERVICE_ID, "list_tasks")).thenReturn(false);

        ToolConfig.ParamSqlConfig cfg = new ToolConfig.ParamSqlConfig();
        cfg.setDataSourceId(TestFixtures.TEST_DATASOURCE_ID);
        cfg.setSqlTemplate("SELECT * FROM tasks {{#where}}{{#if status}}AND status = {{status}}{{/if}}{{/where}}");
        cfg.setParameters(List.of());
        testCustomTool.setConfig(cfg);
        CompiledTemplate compiled = mock(CompiledTemplate.class);
        when(compiled.getVariables()).thenReturn(Set.of());
        when(templateParser.parse(anyString())).thenReturn(compiled);
        when(customToolDAO.save(any(McpCustomToolPO.class))).thenReturn(testCustomToolPO);

        String result = mcpToolService.createCustomTool(TestFixtures.TEST_MCP_SERVICE_ID, testCustomTool);
        assertThat(result).isEqualTo(TestFixtures.TEST_MCP_CUSTOM_TOOL_ID);
    }

    @Test
    @DisplayName("创建 Parameterized SQL — 空模板允许")
    void createCustomTool_emptyTemplate_allowed() {
        when(mcpServiceDAO.existsById(TestFixtures.TEST_MCP_SERVICE_ID)).thenReturn(true);
        when(customToolDAO.existsByServiceIdAndName(TestFixtures.TEST_MCP_SERVICE_ID, "empty_tool")).thenReturn(false);

        ToolConfig.ParamSqlConfig cfg = new ToolConfig.ParamSqlConfig();
        cfg.setDataSourceId(TestFixtures.TEST_DATASOURCE_ID);
        cfg.setSqlTemplate(null);
        cfg.setParameters(List.of());
        testCustomTool.setName("empty_tool");
        testCustomTool.setConfig(cfg);
        testCustomToolPO.setName("empty_tool");
        when(customToolDAO.save(any(McpCustomToolPO.class))).thenReturn(testCustomToolPO);

        String result = mcpToolService.createCustomTool(TestFixtures.TEST_MCP_SERVICE_ID, testCustomTool);
        assertThat(result).isEqualTo(TestFixtures.TEST_MCP_CUSTOM_TOOL_ID);
    }

    // ── 更新自定义工具 ──

    @Test
    @DisplayName("更新自定义工具 - 成功（含 enabled 开关）")
    void updateCustomTool_success() {
        when(customToolDAO.findByServiceIdAndId(TestFixtures.TEST_MCP_SERVICE_ID, TestFixtures.TEST_MCP_CUSTOM_TOOL_ID))
            .thenReturn(Optional.of(testCustomToolPO));
        CompiledTemplate compiled = mock(CompiledTemplate.class);
        when(compiled.getVariables()).thenReturn(Set.of());
        when(templateParser.parse(anyString())).thenReturn(compiled);

        testCustomTool.setEnabled(false);
        testCustomTool.setTitle("新标题");
        mcpToolService.updateCustomTool(testCustomTool);

        verify(customToolDAO).save(testCustomToolPO);
        assertThat(testCustomToolPO.getEnabled()).isFalse();
        assertThat(testCustomToolPO.getTitle()).isEqualTo("新标题");
    }

    @Test
    @DisplayName("更新自定义工具 - 不存在抛出异常")
    void updateCustomTool_notFound_throws() {
        when(customToolDAO.findByServiceIdAndId(TestFixtures.TEST_MCP_SERVICE_ID, TestFixtures.TEST_MCP_CUSTOM_TOOL_ID))
            .thenReturn(Optional.empty());

        DatiException ex = assertThrows(DatiException.class, () ->
            mcpToolService.updateCustomTool(testCustomTool)
        );
        assertThat(ex.getCode()).isEqualTo(ErrorCode.MS_TOOL_NOT_FOUND);
    }

    // ── 删除自定义工具 ──

    @Test
    @DisplayName("删除自定义工具 - 成功")
    void deleteCustomTool_success() {
        when(customToolDAO.findByServiceIdAndId(TestFixtures.TEST_MCP_SERVICE_ID, TestFixtures.TEST_MCP_CUSTOM_TOOL_ID))
            .thenReturn(Optional.of(testCustomToolPO));

        mcpToolService.deleteCustomTool(TestFixtures.TEST_MCP_SERVICE_ID, TestFixtures.TEST_MCP_CUSTOM_TOOL_ID);

        verify(customToolDAO).delete(testCustomToolPO);
    }

    @Test
    @DisplayName("删除自定义工具 - 不存在抛出异常")
    void deleteCustomTool_notFound_throws() {
        when(customToolDAO.findByServiceIdAndId(TestFixtures.TEST_MCP_SERVICE_ID, TestFixtures.TEST_MCP_CUSTOM_TOOL_ID))
            .thenReturn(Optional.empty());

        DatiException ex = assertThrows(DatiException.class, () ->
            mcpToolService.deleteCustomTool(TestFixtures.TEST_MCP_SERVICE_ID, TestFixtures.TEST_MCP_CUSTOM_TOOL_ID)
        );
        assertThat(ex.getCode()).isEqualTo(ErrorCode.MS_TOOL_NOT_FOUND);
    }

    // ── 计数 ──

    @Test
    @DisplayName("countToolsByServiceId - 返回预置+自定义的已启用总数")
    void countToolsByServiceId_returnsTotal() {
        when(customToolDAO.countByServiceId(TestFixtures.TEST_MCP_SERVICE_ID)).thenReturn(2L);

        long result = mcpToolService.countToolsByServiceId(TestFixtures.TEST_MCP_SERVICE_ID);

        assertThat(result).isEqualTo(5L);
    }
}
