package com.dati.mcp.repository.mapper;

import com.dati.TestFixtures;
import com.dati.mcp.domain.model.McpDataScopeType;
import com.dati.mcp.domain.model.McpServiceSnapshot;
import com.dati.mcp.domain.model.McpToolType;
import com.dati.mcp.domain.model.ToolConfig;
import com.dati.mcp.repository.po.McpServiceSnapshotPO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("McpServiceSnapshotMapper snapshot content format tests")
class McpServiceSnapshotMapperTest {

    private McpServiceSnapshot.SnapshotContent sampleContent() {
        McpServiceSnapshot.SnapshotContent content = new McpServiceSnapshot.SnapshotContent();

        McpServiceSnapshot.ServiceInfo info = new McpServiceSnapshot.ServiceInfo();
        info.setId(TestFixtures.TEST_MCP_SERVICE_ID);
        info.setName("Test MCP Service");
        info.setDescription("desc");
        info.setCode(TestFixtures.TEST_MCP_SERVICE_CODE);
        content.setServiceInfo(info);

        content.setDataScopes(List.of(
                new McpServiceSnapshot.DataScopeDraft(TestFixtures.TEST_MCP_SERVICE_ID,
                        McpDataScopeType.DATA_SOURCE, TestFixtures.TEST_DATASOURCE_ID)));

        ToolConfig.SearchMetadataConfig config = new ToolConfig.SearchMetadataConfig();
        content.setPrebuiltTools(List.of(
                new McpServiceSnapshot.PrebuiltToolDraft(TestFixtures.TEST_MCP_SERVICE_ID,
                        McpToolType.SEARCH_METADATA, true, config)));

        content.setCustomTools(List.of(
                new McpServiceSnapshot.CustomToolDraft(TestFixtures.TEST_MCP_SERVICE_ID,
                        "list_tasks", McpToolType.PARAMETERIZED_SQL, "查询任务列表", "desc",
                        true, TestFixtures.createTestParamSqlConfig())));

        content.setPrompts(List.of(
                new McpServiceSnapshot.PromptDraft(TestFixtures.TEST_MCP_SERVICE_ID,
                        "analyze_table", "desc", true,
                        "SELECT * FROM {{table}}", List.of())));
        return content;
    }

    @Test
    @DisplayName("Snapshot JSON contains business fields only - no id or audit fields")
    void toPO_serializesBusinessFieldsOnly() {
        McpServiceSnapshot snapshot = new McpServiceSnapshot();
        snapshot.setServiceId(TestFixtures.TEST_MCP_SERVICE_ID);
        snapshot.setVersionNumber(1);
        snapshot.setContent(sampleContent());

        String json = McpServiceSnapshotMapper.toPO(snapshot).getSnapshotContent();

        assertThat(json).contains("data_scopes", "prebuilt_tools", "custom_tools", "prompts");
        // 业务字段保留
        assertThat(json).contains(TestFixtures.TEST_DATASOURCE_ID, "list_tasks", "analyze_table");
        // 运行时字段必须不存在：id、service_id 之外的审计字段（created_by/created_at/updated_by/updated_at）
        assertThat(json)
                .doesNotContain("created_by", "created_at", "updated_by", "updated_at");
    }

    @Test
    @DisplayName("Entity ids are not stored in snapshot content")
    void toPO_snapshotJsonHasNoEntityIds() {
        McpServiceSnapshot snapshot = new McpServiceSnapshot();
        snapshot.setServiceId(TestFixtures.TEST_MCP_SERVICE_ID);
        snapshot.setVersionNumber(1);
        snapshot.setContent(sampleContent());

        String json = McpServiceSnapshotMapper.toPO(snapshot).getSnapshotContent();

        // 快照 JSON 中除 service_info.id（= serviceId）外不应出现子实体 id
        assertThat(json).doesNotContain("mcp-ct-001", "mcp-pt-001");
    }

    @Test
    @DisplayName("Legacy snapshot JSON with ids and audit fields still parses (backward compatible)")
    void parseContent_legacyJsonWithRuntimeFields_ignoresUnknown() {
        // 旧格式：data scope/tool/prompt 携带 id 与审计字段（BUG-20260801-001 之前的格式）
        String legacyJson = """
                {
                  "service_info": {"id": "mcp-svc-001", "name": "Test MCP Service", "description": "desc", "code": "test-mcp-service"},
                  "data_scopes": [
                    {"id": "scope-old-1", "service_id": "mcp-svc-001", "scope_type": "DATA_SOURCE", "reference_id": "ds-001",
                     "created_by": "u1", "created_at": "2026-01-01T00:00:00Z", "updated_by": "u1", "updated_at": "2026-01-01T00:00:00Z"}
                  ],
                  "prebuilt_tools": [
                    {"id": "pre-old-1", "service_id": "mcp-svc-001", "tool_type": "SEARCH_METADATA", "enabled": true,
                     "config": {"timeout": 60}, "created_at": "2026-01-01T00:00:00Z"}
                  ],
                  "custom_tools": [
                    {"id": "ct-old-1", "service_id": "mcp-svc-001", "name": "list_tasks", "tool_type": "PARAMETERIZED_SQL",
                     "title": "查询任务列表", "description": "desc", "enabled": true,
                     "config": {"data_source_id": "ds-001", "sql_template": "SELECT 1"}, "created_at": "2026-01-01T00:00:00Z"}
                  ],
                  "prompts": [
                    {"id": "pt-old-1", "service_id": "mcp-svc-001", "name": "analyze_table", "description": "desc", "enabled": true,
                     "content": "SELECT * FROM {{table}}", "parameters": [], "created_at": "2026-01-01T00:00:00Z"}
                  ]
                }
                """;

        McpServiceSnapshotPO po = new McpServiceSnapshotPO();
        po.setSnapshotContent(legacyJson);
        McpServiceSnapshot snapshot = McpServiceSnapshotMapper.toModel(po);

        McpServiceSnapshot.SnapshotContent content = snapshot.getContent();
        assertThat(content).isNotNull();
        assertThat(content.getDataScopes()).hasSize(1);
        assertThat(content.getDataScopes().getFirst().scopeType()).isEqualTo(McpDataScopeType.DATA_SOURCE);
        assertThat(content.getDataScopes().getFirst().referenceId()).isEqualTo("ds-001");
        assertThat(content.getCustomTools()).hasSize(1);
        assertThat(content.getCustomTools().getFirst().name()).isEqualTo("list_tasks");
        assertThat(content.getCustomTools().getFirst().config()).isInstanceOf(ToolConfig.ParamSqlConfig.class);
        assertThat(content.getPrebuiltTools()).hasSize(1);
        assertThat(content.getPrebuiltTools().getFirst().toolType()).isEqualTo(McpToolType.SEARCH_METADATA);
        assertThat(content.getPrompts()).hasSize(1);
        assertThat(content.getPrompts().getFirst().name()).isEqualTo("analyze_table");
    }

    @Test
    @DisplayName("Draft to model mapping rebuilds entities without ids (safe for rollback restore)")
    void toModel_rebuildsEntitiesWithoutIds() {
        McpServiceSnapshot.DataScopeDraft draft = new McpServiceSnapshot.DataScopeDraft(
                TestFixtures.TEST_MCP_SERVICE_ID, McpDataScopeType.DATA_SOURCE, TestFixtures.TEST_DATASOURCE_ID);

        var scope = McpServiceSnapshotMapper.toDataScope(draft);
        assertThat(scope.getId()).isNull();
        assertThat(scope.getServiceId()).isEqualTo(TestFixtures.TEST_MCP_SERVICE_ID);
        assertThat(scope.getScopeType()).isEqualTo(McpDataScopeType.DATA_SOURCE);
        assertThat(scope.getReferenceId()).isEqualTo(TestFixtures.TEST_DATASOURCE_ID);

        McpServiceSnapshot.CustomToolDraft toolDraft = new McpServiceSnapshot.CustomToolDraft(
                TestFixtures.TEST_MCP_SERVICE_ID, "list_tasks", McpToolType.PARAMETERIZED_SQL,
                "查询任务列表", "desc", true, TestFixtures.createTestParamSqlConfig());
        var tool = McpServiceSnapshotMapper.toCustomTool(toolDraft);
        assertThat(tool.getId()).isNull();
        assertThat(tool.getServiceId()).isEqualTo(TestFixtures.TEST_MCP_SERVICE_ID);
        assertThat(tool.getName()).isEqualTo("list_tasks");

        McpServiceSnapshot.PromptDraft promptDraft = new McpServiceSnapshot.PromptDraft(
                TestFixtures.TEST_MCP_SERVICE_ID, "analyze_table", "desc", true,
                "SELECT * FROM {{table}}", List.of());
        var prompt = McpServiceSnapshotMapper.toPrompt(promptDraft);
        assertThat(prompt.getId()).isNull();
        assertThat(prompt.getServiceId()).isEqualTo(TestFixtures.TEST_MCP_SERVICE_ID);
        assertThat(prompt.getName()).isEqualTo("analyze_table");

        McpServiceSnapshot.PrebuiltToolDraft prebuiltDraft = new McpServiceSnapshot.PrebuiltToolDraft(
                TestFixtures.TEST_MCP_SERVICE_ID, McpToolType.SEARCH_METADATA, true, new ToolConfig.SearchMetadataConfig());
        var prebuilt = McpServiceSnapshotMapper.toPrebuiltTool(prebuiltDraft);
        assertThat(prebuilt.getId()).isNull();
        assertThat(prebuilt.getServiceId()).isEqualTo(TestFixtures.TEST_MCP_SERVICE_ID);
        assertThat(prebuilt.getToolType()).isEqualTo(McpToolType.SEARCH_METADATA);
        assertThat(prebuilt.getConfig()).isInstanceOf(ToolConfig.SearchMetadataConfig.class);
    }
}
