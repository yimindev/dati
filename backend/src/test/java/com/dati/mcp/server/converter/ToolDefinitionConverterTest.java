package com.dati.mcp.server.converter;

import com.dati.TestFixtures;
import com.dati.mcp.domain.model.McpToolType;
import com.dati.mcp.domain.model.McpServiceSnapshot;
import com.dati.mcp.domain.model.ToolConfig;
import com.dati.mcp.domain.model.ToolParameter;
import com.dati.mcp.domain.service.McpParameterSchemaGenerator;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ToolDefinitionConverter tests")
class ToolDefinitionConverterTest {

    private final ToolDefinitionConverter converter =
        new ToolDefinitionConverter(new McpParameterSchemaGenerator());

    @Test
    @DisplayName("converts prebuilt and custom tools with deterministic order")
    void convertsToolsWithOrder() {
        var content = TestFixtures.createTestSnapshotContent();
        List<McpSchema.Tool> tools = converter.convert(content);
        assertEquals(2, tools.size());
        assertEquals("search_metadata", tools.get(0).name());
        assertEquals("list_tasks", tools.get(1).name());
        assertEquals("查询任务列表", tools.get(1).title());
        assertEquals("object", tools.get(1).inputSchema().get("type"));
        assertEquals(List.of("status"), tools.get(1).inputSchema().get("required"));
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
    @DisplayName("prebuilt order is fixed: SEARCH_METADATA, GET_TABLE_INFO, LIST_TABLES, EXECUTE_SQL")
    void prebuiltOrderIsFixed() {
        var content = new McpServiceSnapshot.SnapshotContent();
        content.setPrebuiltTools(List.of(
            TestFixtures.createTestPrebuiltToolDraft(McpToolType.EXECUTE_SQL, true, new ToolConfig.ExecuteSqlConfig()),
            TestFixtures.createTestPrebuiltToolDraft(McpToolType.GET_TABLE_INFO, true, new ToolConfig.GetTableInfoConfig()),
            TestFixtures.createTestPrebuiltToolDraft(McpToolType.LIST_TABLES, true, new ToolConfig.ListTablesConfig()),
            TestFixtures.createTestPrebuiltToolDraft(McpToolType.SEARCH_METADATA, true, new ToolConfig.SearchMetadataConfig())));
        List<McpSchema.Tool> tools = converter.convert(content);
        assertEquals(List.of("search_metadata", "get_table_info", "list_tables", "execute_sql"),
            tools.stream().map(McpSchema.Tool::name).toList());
    }

    @Test
    @DisplayName("list_tables exposes empty inputSchema, readOnlyHint and fixed position after get_table_info")
    void listTablesPrebuiltSchemaAndAnnotations() {
        var content = new McpServiceSnapshot.SnapshotContent();
        content.setPrebuiltTools(List.of(
            TestFixtures.createTestPrebuiltToolDraft(McpToolType.GET_TABLE_INFO, true, new ToolConfig.GetTableInfoConfig()),
            TestFixtures.createTestPrebuiltToolDraft(McpToolType.LIST_TABLES, true, new ToolConfig.ListTablesConfig())));
        List<McpSchema.Tool> tools = converter.convert(content);

        assertEquals(List.of("get_table_info", "list_tables"), tools.stream().map(McpSchema.Tool::name).toList());
        McpSchema.Tool tool = tools.get(1);
        assertEquals("list_tables", tool.name());
        assertEquals("List Tables", tool.title());
        assertEquals("object", tool.inputSchema().get("type"));
        assertFalse(tool.inputSchema().containsKey("required"));
        assertNotNull(tool.annotations());
        assertTrue(tool.annotations().readOnlyHint());
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
        List<McpSchema.Tool> tools = converter.convert(content);
        assertEquals(List.of("search_metadata", "dup"), tools.stream().map(McpSchema.Tool::name).toList());
    }

    @Test
    @DisplayName("prebuilt schema is generated from the parameter record")
    void prebuiltSchemaComesFromRecord() {
        var content = new McpServiceSnapshot.SnapshotContent();
        content.setPrebuiltTools(List.of(
            TestFixtures.createTestPrebuiltToolDraft(McpToolType.SEARCH_METADATA, true,
                new ToolConfig.SearchMetadataConfig())));
        List<McpSchema.Tool> tools = converter.convert(content);
        @SuppressWarnings("unchecked")
        var props = (java.util.Map<String, Object>) tools.getFirst().inputSchema().get("properties");
        assertTrue(props.containsKey("keywords"));
        assertEquals(List.of("keywords"), tools.getFirst().inputSchema().get("required"));
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
        List<McpSchema.Tool> tools = converter.convert(content);
        @SuppressWarnings("unchecked")
        var props = (java.util.Map<String, Object>) tools.getFirst().inputSchema().get("properties");
        assertTrue(props.containsKey("status"));
        assertEquals("string", ((java.util.Map<?, ?>) props.get("status")).get("type"));
        assertEquals(List.of("status"), tools.getFirst().inputSchema().get("required"));
    }

    @Test
    @DisplayName("null content sections are tolerated")
    void toleratesNullSections() {
        var content = new McpServiceSnapshot.SnapshotContent();
        assertTrue(converter.convert(content).isEmpty());
    }

    @Test
    @DisplayName("prebuilt tools expose protocol title and annotations")
    void prebuiltTitleAndAnnotations() {
        var content = new McpServiceSnapshot.SnapshotContent();
        content.setPrebuiltTools(List.of(
            TestFixtures.createTestPrebuiltToolDraft(McpToolType.UPDATE_TABLE_INFO, true,
                new ToolConfig.UpdateMetadataConfig()),
            TestFixtures.createTestPrebuiltToolDraft(McpToolType.SEARCH_METADATA, true,
                new ToolConfig.SearchMetadataConfig()),
            TestFixtures.createTestPrebuiltToolDraft(McpToolType.EXECUTE_SQL, true,
                new ToolConfig.ExecuteSqlConfig())));
        List<McpSchema.Tool> tools = converter.convert(content);
        McpSchema.Tool read = tools.stream().filter(t -> t.name().equals("search_metadata")).findFirst().orElseThrow();
        McpSchema.Tool write = tools.stream().filter(t -> t.name().equals("update_table_info")).findFirst().orElseThrow();
        McpSchema.Tool sql = tools.stream().filter(t -> t.name().equals("execute_sql")).findFirst().orElseThrow();

        assertEquals("search_metadata", read.name());
        assertEquals("Search Metadata", read.title());
        assertTrue(read.annotations().readOnlyHint());

        assertEquals("Update Table Metadata", write.title());
        assertNotNull(write.annotations());
        assertFalse(write.annotations().readOnlyHint());
        assertFalse(write.annotations().destructiveHint());
        assertTrue(write.annotations().idempotentHint());
        assertTrue(write.annotations().openWorldHint());

        // EXECUTE_SQL declares neither read-only nor write annotations (can run write SQL)
        assertNull(sql.annotations());
    }
}
