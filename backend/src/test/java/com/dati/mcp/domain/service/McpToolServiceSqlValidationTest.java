package com.dati.mcp.domain.service;

import com.dati.TestFixtures;
import com.dati.base.exception.DatiException;
import com.dati.base.exception.ErrorCode;
import com.dati.common.template.HandlebarsStyleParser;
import com.dati.common.template.SqlValidator;
import com.dati.mcp.domain.model.McpCustomTool;
import com.dati.mcp.domain.model.ToolConfig;
import com.dati.mcp.domain.model.ToolParameter;
import com.dati.mcp.repository.dao.McpCustomToolDAO;
import com.dati.mcp.repository.dao.McpPrebuiltToolConfigDAO;
import com.dati.mcp.repository.po.McpCustomToolPO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("McpToolService SQL template validation tests")
class McpToolServiceSqlValidationTest {

    @Mock
    private McpPrebuiltToolConfigDAO prebuiltDAO;

    @Mock
    private McpCustomToolDAO customToolDAO;

    @Mock
    private com.dati.common.template.SqlRenderer sqlRenderer;

    @Mock
    private com.dati.permission.domain.service.PermissionService permissionService;

    private McpToolService mcpToolService;

    @BeforeEach
    void setUp() {
        // Real parser + real validator: quoted-placeholder check needs a real ParsedTemplate
        mcpToolService = new McpToolService(prebuiltDAO, customToolDAO,
                new HandlebarsStyleParser(), sqlRenderer, new SqlValidator(), permissionService);
    }

    private McpCustomTool toolWithTemplate(String sqlTemplate) {
        McpCustomTool tool = TestFixtures.createTestCustomTool();
        ToolConfig.ParamSqlConfig cfg = new ToolConfig.ParamSqlConfig();
        cfg.setDataSourceId(TestFixtures.TEST_DATASOURCE_ID);
        cfg.setSqlTemplate(sqlTemplate);
        ToolParameter p = new ToolParameter();
        p.setName("genre_name");
        p.setType("String");
        cfg.setParameters(List.of(p));
        tool.setConfig(cfg);
        return tool;
    }

    @Test
    @DisplayName("Create tool with quoted variable is rejected with MS020")
    void createCustomTool_quotedVariable_throwsMs020() {
        when(customToolDAO.existsByServiceIdAndName(anyString(), anyString())).thenReturn(false);
        McpCustomTool tool = toolWithTemplate("SELECT * FROM genre WHERE name = '{{genre_name}}'");

        assertThatThrownBy(() -> mcpToolService.createCustomTool(TestFixtures.TEST_MCP_SERVICE_ID, tool))
                .isInstanceOf(DatiException.class)
                .satisfies(e -> assertThat(((DatiException) e).getCode())
                        .isEqualTo(ErrorCode.MS_TEMPLATE_QUOTED_VAR));

        verify(customToolDAO, never()).save(any(McpCustomToolPO.class));
    }

    @Test
    @DisplayName("Create tool with unquoted template is accepted")
    void createCustomTool_validTemplate_saved() {
        when(customToolDAO.existsByServiceIdAndName(anyString(), anyString())).thenReturn(false);
        McpCustomTool tool = toolWithTemplate("SELECT * FROM genre WHERE name = {{genre_name}}");
        McpCustomToolPO saved = new McpCustomToolPO();
        saved.setId("tool-1");
        when(customToolDAO.save(any(McpCustomToolPO.class))).thenReturn(saved);

        String id = mcpToolService.createCustomTool(TestFixtures.TEST_MCP_SERVICE_ID, tool);

        assertThat(id).isEqualTo("tool-1");
    }

    @Test
    @DisplayName("Update tool with quoted variable is rejected with MS020")
    void updateCustomTool_quotedVariable_throwsMs020() {
        McpCustomToolPO po = new McpCustomToolPO();
        po.setId("tool-1");
        po.setServiceId(TestFixtures.TEST_MCP_SERVICE_ID);
        po.setName("list_tasks");
        when(customToolDAO.findByServiceIdAndId(anyString(), anyString())).thenReturn(Optional.of(po));

        McpCustomTool tool = toolWithTemplate("SELECT * FROM genre WHERE name = '{{genre_name}}'");
        tool.setId("tool-1");

        assertThatThrownBy(() -> mcpToolService.updateCustomTool(tool))
                .isInstanceOf(DatiException.class)
                .satisfies(e -> assertThat(((DatiException) e).getCode())
                        .isEqualTo(ErrorCode.MS_TEMPLATE_QUOTED_VAR));
    }
}
