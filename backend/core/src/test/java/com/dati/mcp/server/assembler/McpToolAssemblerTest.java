package com.dati.mcp.server.assembler;

import com.dati.common.JsonUtils;
import com.dati.mcp.domain.model.McpCustomTool;
import com.dati.mcp.domain.model.McpToolType;
import com.dati.mcp.domain.model.ToolConfig;
import com.dati.mcp.server.pojo.CreateCustomToolRequest;
import com.dati.mcp.server.pojo.UpdateToolRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("McpToolAssembler unit tests")
class McpToolAssemblerTest {

    private final McpToolAssembler assembler = new McpToolAssembler();

    @Test
    @DisplayName("Create model - applies defaults for omitted tool_type and enabled")
    void toModel_create_appliesDefaults() {
        CreateCustomToolRequest request = new CreateCustomToolRequest();
        request.setName("list_tasks");
        request.setDescription("List tasks");

        McpCustomTool tool = assembler.toModel(request);

        assertThat(tool.getToolType()).isEqualTo(McpToolType.PARAMETERIZED_SQL);
        assertThat(tool.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("Create model - keeps explicit enabled=false")
    void toModel_create_keepsExplicitDisabled() {
        CreateCustomToolRequest request = new CreateCustomToolRequest();
        request.setToolType(McpToolType.PARAMETERIZED_SQL);
        request.setEnabled(false);

        assertThat(assembler.toModel(request).isEnabled()).isFalse();
    }

    @Test
    @DisplayName("Update model - omitted fields stay null so the service keeps stored values")
    void toUpdateModel_omittedFieldsStayNull() {
        UpdateToolRequest request = new UpdateToolRequest();
        request.setDescription("d2");

        McpCustomTool tool = assembler.toUpdateModel(request);

        assertThat(tool.getToolType()).isNull();
        assertThat(tool.getName()).isNull();
        assertThat(tool.getTitle()).isNull();
        assertThat(tool.getDescription()).isEqualTo("d2");
        assertThat(tool.getConfig()).isNull();
    }

    @Test
    @DisplayName("Update model - parses provided ParamSql config")
    void toUpdateModel_parsesConfig() {
        ToolConfig.ParamSqlConfig config = new ToolConfig.ParamSqlConfig();
        config.setDataSourceId("ds-1");
        config.setSqlTemplate("SELECT 1");
        config.setParameters(List.of());

        UpdateToolRequest request = new UpdateToolRequest();
        request.setConfig(JsonUtils.toJson(config));

        McpCustomTool tool = assembler.toUpdateModel(request);

        assertThat(tool.getConfig()).isInstanceOf(ToolConfig.ParamSqlConfig.class);
        assertThat(((ToolConfig.ParamSqlConfig) tool.getConfig()).getSqlTemplate()).isEqualTo("SELECT 1");
    }

    @Test
    @DisplayName("Prebuilt config - blank input returns null (keep stored config)")
    void parsePrebuiltConfig_blank_returnsNull() {
        assertThat(assembler.parsePrebuiltConfig(McpToolType.EXECUTE_SQL, null)).isNull();
        assertThat(assembler.parsePrebuiltConfig(McpToolType.EXECUTE_SQL, "  ")).isNull();
    }

    @Test
    @DisplayName("Prebuilt config - parses ExecuteSql config")
    void parsePrebuiltConfig_executeSql() {
        ToolConfig.ExecuteSqlConfig config = new ToolConfig.ExecuteSqlConfig();
        config.setTimeout(60);
        config.setMaxRows(500);

        assertThat(assembler.parsePrebuiltConfig(McpToolType.EXECUTE_SQL, JsonUtils.toJson(config)))
            .isInstanceOfSatisfying(ToolConfig.ExecuteSqlConfig.class, parsed -> {
                assertThat(parsed.getTimeout()).isEqualTo(60);
                assertThat(parsed.getMaxRows()).isEqualTo(500);
            });
    }
}
