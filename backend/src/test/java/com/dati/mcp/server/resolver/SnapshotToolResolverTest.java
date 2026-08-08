package com.dati.mcp.server.resolver;

import com.dati.TestFixtures;
import com.dati.mcp.domain.model.McpDataScopeType;
import com.dati.mcp.domain.model.McpServiceDataScope;
import com.dati.mcp.domain.model.McpServiceSnapshot;
import com.dati.mcp.domain.model.McpToolType;
import com.dati.mcp.domain.model.ToolConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SnapshotToolResolver tests")
class SnapshotToolResolverTest {

    private final SnapshotToolResolver resolver = new SnapshotToolResolver();

    @Test
    @DisplayName("resolves prebuilt tool by enum name")
    void resolvesPrebuilt() {
        var content = TestFixtures.createTestSnapshotContent();
        Optional<SnapshotToolResolver.ResolvedTool> tool = resolver.resolve(content, "search_metadata");
        assertTrue(tool.isPresent());
        assertEquals(McpToolType.SEARCH_METADATA, tool.get().toolType());
        assertEquals("search_metadata", tool.get().name());
    }

    @Test
    @DisplayName("resolves custom tool by business name")
    void resolvesCustom() {
        var content = TestFixtures.createTestSnapshotContent();
        Optional<SnapshotToolResolver.ResolvedTool> tool = resolver.resolve(content, "list_tasks");
        assertTrue(tool.isPresent());
        assertEquals(McpToolType.PARAMETERIZED_SQL, tool.get().toolType());
        assertEquals("list_tasks", tool.get().name());
        assertInstanceOf(ToolConfig.ParamSqlConfig.class, tool.get().config());
    }

    @Test
    @DisplayName("disabled or unknown tool resolves to empty")
    void disabledOrUnknownIsEmpty() {
        var content = new McpServiceSnapshot.SnapshotContent();
        content.setPrebuiltTools(List.of(
            TestFixtures.createTestPrebuiltToolDraft(McpToolType.SEARCH_METADATA, false, new ToolConfig.SearchMetadataConfig())));
        content.setCustomTools(List.of(
            TestFixtures.createTestCustomToolDraft("off", McpToolType.PARAMETERIZED_SQL, false, new ToolConfig.ParamSqlConfig())));
        assertTrue(resolver.resolve(content, "search_metadata").isEmpty());
        assertTrue(resolver.resolve(content, "off").isEmpty());
        assertTrue(resolver.resolve(content, "nope").isEmpty());
    }

    @Test
    @DisplayName("builds scope items from snapshot data scopes")
    void buildsScopeItems() {
        var content = TestFixtures.createTestSnapshotContent();
        List<McpServiceDataScope> items = resolver.buildScopeItems(TestFixtures.TEST_MCP_SERVICE_ID, content);
        assertEquals(1, items.size());
        assertEquals(McpDataScopeType.DATA_SOURCE, items.getFirst().getScopeType());
        assertEquals(TestFixtures.TEST_DATASOURCE_ID, items.getFirst().getReferenceId());
        assertEquals(TestFixtures.TEST_MCP_SERVICE_ID, items.getFirst().getServiceId());
    }

    @Test
    @DisplayName("null sections yield empty scope list and empty resolve")
    void toleratesNullSections() {
        var content = new McpServiceSnapshot.SnapshotContent();
        assertTrue(resolver.resolve(content, "anything").isEmpty());
        assertTrue(resolver.buildScopeItems("svc", content).isEmpty());
    }
}
