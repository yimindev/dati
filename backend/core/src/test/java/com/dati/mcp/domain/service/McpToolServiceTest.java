package com.dati.mcp.domain.service;

import com.dati.TestFixtures;
import com.dati.base.exception.DatiException;
import com.dati.base.exception.ErrorCode;
import com.dati.common.template.CompiledTemplate;
import com.dati.common.template.SqlRenderer;
import com.dati.common.template.SqlValidator;
import com.dati.common.template.TemplateParseException;
import com.dati.common.template.TemplateParser;
import com.dati.mcp.domain.model.McpCustomTool;
import com.dati.mcp.domain.model.McpPrebuiltToolConfig;
import com.dati.mcp.domain.model.McpToolType;
import com.dati.mcp.domain.model.ToolConfig;
import com.dati.mcp.repository.dao.McpCustomToolDAO;
import com.dati.mcp.repository.dao.McpPrebuiltToolConfigDAO;
import com.dati.mcp.repository.po.McpCustomToolPO;
import com.dati.mcp.repository.po.McpPrebuiltToolConfigPO;
import com.dati.permission.domain.service.PermissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("McpToolService unit tests")
class McpToolServiceTest {

    @Mock
    private McpPrebuiltToolConfigDAO prebuiltDAO;

    @Mock
    private McpCustomToolDAO customToolDAO;

    @Mock
    private TemplateParser templateParser;

    @Mock
    private SqlRenderer sqlRenderer;

    @Mock
    private SqlValidator sqlValidator;

    @Mock
    private PermissionService permissionService;

    @Captor
    ArgumentCaptor<List<McpCustomToolPO>> captor;

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
    @DisplayName("List - throws MS_SERVICE_NOT_FOUND when service does not exist")
    void listTools_serviceNotFound_shouldThrow() {
        org.mockito.Mockito.doThrow(new DatiException(ErrorCode.MS_SERVICE_NOT_FOUND))
            .when(permissionService).requireMcpService(TestFixtures.TEST_MCP_SERVICE_ID, com.dati.permission.domain.model.Permission.VIEW);

        DatiException e = assertThrows(DatiException.class,
            () -> mcpToolService.listTools(TestFixtures.TEST_MCP_SERVICE_ID));
        assertThat(e.getCode()).isEqualTo(ErrorCode.MS_SERVICE_NOT_FOUND);
    }

    @Test
    @DisplayName("List - returns prebuilt defaults and empty custom list when no DB records")
    void listTools_noConfig_returnsDefaults() {
        when(prebuiltDAO.findAllByServiceId(TestFixtures.TEST_MCP_SERVICE_ID)).thenReturn(List.of());
        when(customToolDAO.findAllByServiceIdOrderByCreatedAtDesc(TestFixtures.TEST_MCP_SERVICE_ID)).thenReturn(List.of());

        ToolsResult result = mcpToolService.listTools(TestFixtures.TEST_MCP_SERVICE_ID);

        assertThat(result.prebuilt()).hasSize(7);
        // Read tools + EXECUTE_SQL default enabled; metadata update tools default disabled
        assertThat(result.prebuilt())
            .filteredOn(McpPrebuiltToolConfig::isEnabled)
            .extracting(McpPrebuiltToolConfig::getToolType)
            .containsExactlyInAnyOrder(McpToolType.SEARCH_METADATA, McpToolType.GET_TABLE_INFO,
                McpToolType.EXECUTE_SQL, McpToolType.LIST_TABLES);
        assertThat(result.prebuilt())
            .filteredOn(cfg -> !cfg.isEnabled())
            .extracting(McpPrebuiltToolConfig::getToolType)
            .containsExactlyInAnyOrder(McpToolType.UPDATE_TABLE_INFO, McpToolType.UPDATE_COLUMN_INFO, McpToolType.UPSERT_TERM);
    }

    @Test
    @DisplayName("List - includes configured prebuilt and custom tools from DB")
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

        assertThat(result.prebuilt()).hasSize(7);
        assertThat(result.custom()).hasSize(1);
    }

    // ── 更新预置工具 ──

    @Test
    @DisplayName("Update prebuilt tool - creates new row when no record")
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
    @DisplayName("Update prebuilt tool - updates existing row")
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
    @DisplayName("Create custom tool - success")
    void createCustomTool_success() {
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
    @DisplayName("Create custom tool - throws on invalid name format")
    void createCustomTool_invalidName_throws() {
        testCustomTool.setName("invalid name!");

        DatiException ex = assertThrows(DatiException.class, () ->
            mcpToolService.createCustomTool(TestFixtures.TEST_MCP_SERVICE_ID, testCustomTool)
        );
        assertThat(ex.getCode()).isEqualTo(ErrorCode.MS_TOOL_NAME_INVALID);
        verify(customToolDAO, never()).save(any());
    }

    @Test
    @DisplayName("Create custom tool - throws on duplicate name")
    void createCustomTool_nameExists_throws() {
        when(customToolDAO.existsByServiceIdAndName(TestFixtures.TEST_MCP_SERVICE_ID, "list_tasks")).thenReturn(true);

        DatiException ex = assertThrows(DatiException.class, () ->
            mcpToolService.createCustomTool(TestFixtures.TEST_MCP_SERVICE_ID, testCustomTool)
        );
        assertThat(ex.getCode()).isEqualTo(ErrorCode.MS_TOOL_NAME_EXISTS);
    }

    @Test
    @DisplayName("Create custom tool - throws when service not found")
    void createCustomTool_serviceNotFound_throws() {
        org.mockito.Mockito.doThrow(new DatiException(ErrorCode.MS_SERVICE_NOT_FOUND))
            .when(permissionService).requireMcpService(TestFixtures.TEST_MCP_SERVICE_ID, com.dati.permission.domain.model.Permission.EDIT);

        DatiException ex = assertThrows(DatiException.class, () ->
            mcpToolService.createCustomTool(TestFixtures.TEST_MCP_SERVICE_ID, testCustomTool)
        );
        assertThat(ex.getCode()).isEqualTo(ErrorCode.MS_SERVICE_NOT_FOUND);
    }

    @Test
    @DisplayName("Create Parameterized SQL - template syntax error (unclosed {{) → rejected")
    void createCustomTool_templateSyntaxError_throws() {
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
    @DisplayName("Create Parameterized SQL - template references undefined parameter → rejected")
    void createCustomTool_undefinedParameter_throws() {
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
    @DisplayName("Create Parameterized SQL - system variables in template are allowed without parameters")
    void createCustomTool_systemVariablesInTemplate_succeeds() {
        when(customToolDAO.existsByServiceIdAndName(TestFixtures.TEST_MCP_SERVICE_ID, "list_tasks")).thenReturn(false);

        ToolConfig.ParamSqlConfig cfg = new ToolConfig.ParamSqlConfig();
        cfg.setDataSourceId(TestFixtures.TEST_DATASOURCE_ID);
        cfg.setSqlTemplate("SELECT * FROM tasks WHERE owner_id = {{_user.id}} AND created_at <= {{_now}}");
        cfg.setParameters(List.of());
        testCustomTool.setConfig(cfg);
        CompiledTemplate compiled = mock(CompiledTemplate.class);
        when(compiled.getVariables()).thenReturn(Set.of("_user.id", "_now"));
        when(templateParser.parse(anyString())).thenReturn(compiled);
        when(customToolDAO.save(any())).thenAnswer(inv -> {
            McpCustomToolPO po = inv.getArgument(0);
            po.setId("tool-123");
            return po;
        });

        String created = mcpToolService.createCustomTool(TestFixtures.TEST_MCP_SERVICE_ID, testCustomTool);
        assertThat(created).isEqualTo("tool-123");
        verify(customToolDAO).save(any());
    }

    @Test
    @DisplayName("Create Parameterized SQL - valid template passes validation")
    void createCustomTool_validTemplate_succeeds() {
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
    @DisplayName("Create Parameterized SQL - empty template rejected")
    void createCustomTool_emptyTemplate_rejected() {
        when(customToolDAO.existsByServiceIdAndName(TestFixtures.TEST_MCP_SERVICE_ID, "empty_tool")).thenReturn(false);

        ToolConfig.ParamSqlConfig cfg = new ToolConfig.ParamSqlConfig();
        cfg.setDataSourceId(TestFixtures.TEST_DATASOURCE_ID);
        cfg.setSqlTemplate(null);
        cfg.setParameters(List.of());
        testCustomTool.setName("empty_tool");
        testCustomTool.setConfig(cfg);

        DatiException ex = assertThrows(DatiException.class,
            () -> mcpToolService.createCustomTool(TestFixtures.TEST_MCP_SERVICE_ID, testCustomTool));
        assertThat(ex.getCode()).isEqualTo(ErrorCode.INVALID_PARAMETER);
        verify(customToolDAO, never()).save(any());
    }

    // ── 更新自定义工具 ──

    @Test
    @DisplayName("Update custom tool - success (with enabled flag)")
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
    @DisplayName("Update custom tool - throws when not found")
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
    @DisplayName("Delete custom tool - success")
    void deleteCustomTool_success() {
        when(customToolDAO.findByServiceIdAndId(TestFixtures.TEST_MCP_SERVICE_ID, TestFixtures.TEST_MCP_CUSTOM_TOOL_ID))
            .thenReturn(Optional.of(testCustomToolPO));

        mcpToolService.deleteCustomTool(TestFixtures.TEST_MCP_SERVICE_ID, TestFixtures.TEST_MCP_CUSTOM_TOOL_ID);

        verify(customToolDAO).delete(testCustomToolPO);
    }

    @Test
    @DisplayName("Delete custom tool - throws when not found")
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
    @DisplayName("countToolsByServiceId - returns total of prebuilt + custom")
    void countToolsByServiceId_returnsTotal() {
        when(customToolDAO.countByServiceId(TestFixtures.TEST_MCP_SERVICE_ID)).thenReturn(2L);

        long result = mcpToolService.countToolsByServiceId(TestFixtures.TEST_MCP_SERVICE_ID);

        assertThat(result).isEqualTo(9L);
    }

    // ── 全量替换（回滚恢复草稿用）──

    @Test
    @DisplayName("replaceCustomTools - deletes existing and inserts restored content")
    void replaceCustomTools_replacesAll() {
        McpCustomTool restoredTool = TestFixtures.createTestCustomTool();
        restoredTool.setName("restored_tool");

        mcpToolService.replaceCustomTools(TestFixtures.TEST_MCP_SERVICE_ID, List.of(restoredTool));

        verify(customToolDAO).deleteAllByServiceId(TestFixtures.TEST_MCP_SERVICE_ID);
        verify(customToolDAO).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(1);
        assertThat(captor.getValue().getFirst().getName()).isEqualTo("restored_tool");
    }

    // ── 智能检测行为标注 ──

    @Test
    @DisplayName("detectAnnotations - blank template throws INVALID_PARAMETER")
    void detectAnnotations_blankTemplate_throwsException() {
        assertThrows(DatiException.class, () -> mcpToolService.detectAnnotations("   ", List.of()));
    }

    @Test
    @DisplayName("detectAnnotations - SELECT statement returns readOnly=true, idempotent=true, destructive=false")
    void detectAnnotations_select_returnsReadOnly() {
        CompiledTemplate compiled = mock(CompiledTemplate.class);
        when(compiled.getVariables()).thenReturn(Set.of("status"));
        when(templateParser.parse("SELECT * FROM tasks WHERE status = {{status}}")).thenReturn(compiled);
        when(sqlRenderer.render(eq(compiled), any())).thenReturn(new com.dati.common.template.PreparedSql("SELECT * FROM tasks WHERE status = ?", List.of()));

        com.dati.mcp.domain.model.DetectedAnnotations result = mcpToolService.detectAnnotations(
                "SELECT * FROM tasks WHERE status = {{status}}",
                List.of(new com.dati.mcp.domain.model.ToolParameter("status", "String", false, null, null))
        );

        assertThat(result.readOnly()).isTrue();
        assertThat(result.idempotent()).isTrue();
        assertThat(result.destructive()).isFalse();
        assertThat(result.detectedOperation()).isEqualTo("SELECT");
    }

    @Test
    @DisplayName("detectAnnotations - DELETE statement returns destructive=true, readOnly=false")
    void detectAnnotations_delete_returnsDestructive() {
        CompiledTemplate compiled = mock(CompiledTemplate.class);
        when(compiled.getVariables()).thenReturn(Set.of());
        when(templateParser.parse("DELETE FROM tasks")).thenReturn(compiled);
        when(sqlRenderer.render(eq(compiled), any())).thenReturn(new com.dati.common.template.PreparedSql("DELETE FROM tasks", List.of()));

        com.dati.mcp.domain.model.DetectedAnnotations result = mcpToolService.detectAnnotations(
                "DELETE FROM tasks",
                List.of()
        );

        assertThat(result.readOnly()).isFalse();
        assertThat(result.idempotent()).isFalse();
        assertThat(result.destructive()).isTrue();
        assertThat(result.detectedOperation()).isEqualTo("DELETE");
    }
}
