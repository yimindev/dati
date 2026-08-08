package com.dati.mcp.server.converter;

import com.dati.TestFixtures;
import com.dati.mcp.domain.model.McpToolType;
import com.dati.mcp.domain.model.McpServiceSnapshot;
import com.dati.mcp.domain.model.ToolConfig;
import com.dati.mcp.domain.model.ToolParameter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ToolDefinitionConverter tests")
class ToolDefinitionConverterTest {

    private final ToolDefinitionConverter converter = new ToolDefinitionConverter();

    @Test
    @DisplayName("converts prebuilt and custom tools with deterministic order")
    void convertsToolsWithOrder() {
        var content = TestFixtures.createTestSnapshotContent();
        List<Map<String, Object>> tools = converter.convert(content);
        assertEquals(2, tools.size());
        assertEquals("search_metadata", tools.get(0).get("name"));
        assertEquals("list_tasks", tools.get(1).get("name"));
        assertEquals("查询任务列表", tools.get(1).get("title"));
        @SuppressWarnings("unchecked")
        Map<String, Object> schema = (Map<String, Object>) tools.get(1).get("inputSchema");
        assertEquals("object", schema.get("type"));
        assertEquals(List.of("status"), schema.get("required"));
    }

    @Test
    @DisplayName("filters out disabled tools and returns empty list when none enabled")
    void filtersDisabledTools() {
        var content = new McpServiceSnapshot.SnapshotContent();
        content.setPrebuiltTools(List.of(
            TestFixtures.createTestPrebuiltToolDraft(McpToolType.SEARCH_METADATA, false, new ToolConfig.SearchMetadataConfig())));
        content.setCustomTools(List.of(
            TestFixtures.createTestCustomToolDraft("x", McpToolType.PARAMETERIZED_SQL, false, new ToolConfig.ParamSqlConfig())));
        assertTrue(converter.convert(content).isEmpty());
    }

    @Test
    @DisplayName("prebuilt order is fixed: SEARCH_METADATA, GET_TABLE_INFO, EXECUTE_SQL")
    void prebuiltOrderIsFixed() {
        var content = new McpServiceSnapshot.SnapshotContent();
        content.setPrebuiltTools(List.of(
            TestFixtures.createTestPrebuiltToolDraft(McpToolType.EXECUTE_SQL, true, new ToolConfig.ExecuteSqlConfig()),
            TestFixtures.createTestPrebuiltToolDraft(McpToolType.GET_TABLE_INFO, true, new ToolConfig.GetTableInfoConfig()),
            TestFixtures.createTestPrebuiltToolDraft(McpToolType.SEARCH_METADATA, true, new ToolConfig.SearchMetadataConfig())));
        List<Map<String, Object>> tools = converter.convert(content);
        assertEquals(List.of("search_metadata", "get_table_info", "execute_sql"),
            tools.stream().map(t -> t.get("name")).toList());
    }

    @Test
    @DisplayName("skips custom tool whose name collides with prebuilt or another custom")
    void skipsNameCollisions() {
        var content = new McpServiceSnapshot.SnapshotContent();
        content.setPrebuiltTools(List.of(
            TestFixtures.createTestPrebuiltToolDraft(McpToolType.SEARCH_METADATA, true, new ToolConfig.SearchMetadataConfig())));
        content.setCustomTools(List.of(
            TestFixtures.createTestCustomToolDraft("search_metadata", McpToolType.PARAMETERIZED_SQL, true, new ToolConfig.ParamSqlConfig()),
            TestFixtures.createTestCustomToolDraft("dup", McpToolType.PARAMETERIZED_SQL, true, new ToolConfig.ParamSqlConfig()),
            TestFixtures.createTestCustomToolDraft("dup", McpToolType.PARAMETERIZED_SQL, true, new ToolConfig.ParamSqlConfig())));
        List<Map<String, Object>> tools = converter.convert(content);
        assertEquals(List.of("search_metadata", "dup"), tools.stream().map(t -> t.get("name")).toList());
    }

    @Test
    @DisplayName("parameterized sql inputSchema is generated from ToolParameter list")
    void generatesParamSqlInputSchema() {
        ToolConfig.ParamSqlConfig cfg = new ToolConfig.ParamSqlConfig();
        cfg.setSqlTemplate("SELECT * FROM tasks WHERE status = {{status}}");
        ToolParameter p = new ToolParameter();
        p.setName("status");
        p.setType("String");
        p.setRequired(true);
        p.setDescription("task status");
        cfg.setParameters(List.of(p));
        var content = new McpServiceSnapshot.SnapshotContent();
        content.setCustomTools(List.of(
            TestFixtures.createTestCustomToolDraft("list_tasks", McpToolType.PARAMETERIZED_SQL, true, cfg)));
        List<Map<String, Object>> tools = converter.convert(content);
        @SuppressWarnings("unchecked")
        Map<String, Object> schema = (Map<String, Object>) tools.getFirst().get("inputSchema");
        @SuppressWarnings("unchecked")
        Map<String, Object> props = (Map<String, Object>) schema.get("properties");
        assertTrue(props.containsKey("status"));
        assertEquals("string", ((Map<?, ?>) props.get("status")).get("type"));
        assertEquals(List.of("status"), schema.get("required"));
    }

    @Test
    @DisplayName("null content sections are tolerated")
    void toleratesNullSections() {
        var content = new McpServiceSnapshot.SnapshotContent();
        assertTrue(converter.convert(content).isEmpty());
    }
}
