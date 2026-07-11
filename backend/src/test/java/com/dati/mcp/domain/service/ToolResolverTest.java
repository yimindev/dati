package com.dati.mcp.domain.service;

import com.dati.TestFixtures;
import com.dati.mcp.domain.model.McpToolType;
import com.dati.mcp.domain.model.ToolError;
import com.dati.mcp.repository.dao.McpCustomToolDAO;
import com.dati.mcp.repository.dao.McpPrebuiltToolConfigDAO;
import com.dati.mcp.repository.po.McpCustomToolPO;
import com.dati.mcp.repository.po.McpPrebuiltToolConfigPO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ToolResolver 单元测试")
class ToolResolverTest {

    @Mock
    private McpPrebuiltToolConfigDAO prebuiltDAO;

    @Mock
    private McpCustomToolDAO customToolDAO;

    @InjectMocks
    private ToolResolver toolResolver;

    @Test
    @DisplayName("resolve - 预置工具有 DB 配置时返回 DB 记录")
    void resolvePrebuiltWithDbConfig() {
        McpPrebuiltToolConfigPO po = new McpPrebuiltToolConfigPO();
        po.setServiceId(TestFixtures.TEST_MCP_SERVICE_ID);
        po.setToolType(McpToolType.EXECUTE_SQL);
        po.setEnabled(true);

        when(prebuiltDAO.findByServiceIdAndToolType(TestFixtures.TEST_MCP_SERVICE_ID, McpToolType.EXECUTE_SQL))
                .thenReturn(Optional.of(po));

        ToolResolver.ResolvedTool result = toolResolver.resolve(TestFixtures.TEST_MCP_SERVICE_ID, "EXECUTE_SQL");

        assertThat(result.enabled()).isTrue();
        assertThat(result.isPrebuilt()).isTrue();
        assertThat(result.toolType()).isEqualTo(McpToolType.EXECUTE_SQL);
    }

    @Test
    @DisplayName("resolve - 预置工具无 DB 记录时返回默认配置")
    void resolvePrebuiltWithoutDbConfig() {
        when(prebuiltDAO.findByServiceIdAndToolType(TestFixtures.TEST_MCP_SERVICE_ID, McpToolType.EXECUTE_SQL))
                .thenReturn(Optional.empty());

        ToolResolver.ResolvedTool result = toolResolver.resolve(TestFixtures.TEST_MCP_SERVICE_ID, "EXECUTE_SQL");

        assertThat(result.enabled()).isTrue();
        assertThat(result.isPrebuilt()).isTrue();
        assertThat(result.toolType()).isEqualTo(McpToolType.EXECUTE_SQL);
    }

    @Test
    @DisplayName("resolve - 预置工具已禁用时抛出 MS_TOOL_DISABLED")
    void resolvePrebuiltDisabled() {
        McpPrebuiltToolConfigPO po = new McpPrebuiltToolConfigPO();
        po.setServiceId(TestFixtures.TEST_MCP_SERVICE_ID);
        po.setToolType(McpToolType.EXECUTE_SQL);
        po.setEnabled(false);

        when(prebuiltDAO.findByServiceIdAndToolType(TestFixtures.TEST_MCP_SERVICE_ID, McpToolType.EXECUTE_SQL))
                .thenReturn(Optional.of(po));

        ToolExecuteException ex = assertThrows(ToolExecuteException.class, () ->
                toolResolver.resolve(TestFixtures.TEST_MCP_SERVICE_ID, "EXECUTE_SQL"));

        assertThat(ex.getToolError()).isEqualTo(ToolError.TOOL_DISABLED);
    }

    @Test
    @DisplayName("resolve - 自定义工具找到且启用时返回正确信息")
    void resolveCustomFoundAndEnabled() {
        McpCustomToolPO po = new McpCustomToolPO();
        po.setId("ct-001");
        po.setServiceId(TestFixtures.TEST_MCP_SERVICE_ID);
        po.setName("my_tool");
        po.setDescription("A custom tool");
        po.setTitle("My Tool");
        po.setEnabled(true);

        when(customToolDAO.findByServiceIdAndId(TestFixtures.TEST_MCP_SERVICE_ID, "my_tool"))
                .thenReturn(Optional.of(po));

        ToolResolver.ResolvedTool result = toolResolver.resolve(TestFixtures.TEST_MCP_SERVICE_ID, "my_tool");

        assertThat(result.enabled()).isTrue();
        assertThat(result.isPrebuilt()).isFalse();
        assertThat(result.toolType()).isEqualTo(McpToolType.PARAMETERIZED_SQL);
    }

    @Test
    @DisplayName("resolve - 自定义工具未找到时抛出 MS_TOOL_NOT_FOUND")
    void resolveCustomNotFound() {
        when(customToolDAO.findByServiceIdAndId(TestFixtures.TEST_MCP_SERVICE_ID, "unknown_tool"))
                .thenReturn(Optional.empty());

        ToolExecuteException ex = assertThrows(ToolExecuteException.class, () ->
                toolResolver.resolve(TestFixtures.TEST_MCP_SERVICE_ID, "unknown_tool"));

        assertThat(ex.getToolError()).isEqualTo(ToolError.TOOL_NOT_FOUND);
    }

    @Test
    @DisplayName("resolve - 自定义工具已禁用时抛出 ToolExecuteException")
    void resolveCustomDisabled() {
        McpCustomToolPO po = new McpCustomToolPO();
        po.setId("ct-002");
        po.setServiceId(TestFixtures.TEST_MCP_SERVICE_ID);
        po.setName("disabled_tool");
        po.setDescription("A disabled custom tool");
        po.setTitle("Disabled Tool");
        po.setEnabled(false);

        when(customToolDAO.findByServiceIdAndId(TestFixtures.TEST_MCP_SERVICE_ID, "disabled_tool"))
                .thenReturn(Optional.of(po));

        ToolExecuteException ex = assertThrows(ToolExecuteException.class, () ->
                toolResolver.resolve(TestFixtures.TEST_MCP_SERVICE_ID, "disabled_tool"));

        assertThat(ex.getToolError()).isEqualTo(ToolError.TOOL_DISABLED);
    }

    @Test
    @DisplayName("resolve - PARAMETERIZED_SQL 走自定义工具路径，不调用 prebuiltDAO")
    void resolveParameterizedSqlGoesToCustomPath() {
        McpCustomToolPO po = new McpCustomToolPO();
        po.setId("ct-003");
        po.setServiceId(TestFixtures.TEST_MCP_SERVICE_ID);
        po.setName("my_param_sql");
        po.setDescription("A parameterized SQL tool");
        po.setTitle("ParamSQL");
        po.setEnabled(true);

        when(customToolDAO.findByServiceIdAndId(TestFixtures.TEST_MCP_SERVICE_ID, "PARAMETERIZED_SQL"))
                .thenReturn(Optional.of(po));

        ToolResolver.ResolvedTool result = toolResolver.resolve(TestFixtures.TEST_MCP_SERVICE_ID, "PARAMETERIZED_SQL");

        assertThat(result.isPrebuilt()).isFalse();
        assertThat(result.toolType()).isEqualTo(McpToolType.PARAMETERIZED_SQL);
        verify(prebuiltDAO, never()).findByServiceIdAndToolType(any(), any());
        verify(customToolDAO).findByServiceIdAndId(TestFixtures.TEST_MCP_SERVICE_ID, "PARAMETERIZED_SQL");
    }
}
