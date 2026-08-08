package com.dati.mcp.server.endpoint;

import com.dati.TestFixtures;
import com.dati.mcp.domain.model.McpServiceSnapshot;
import com.dati.mcp.domain.service.ToolExecutor;
import com.dati.mcp.domain.service.ToolExecuteException;
import com.dati.mcp.domain.model.ToolError;
import com.dati.mcp.server.converter.ToolDefinitionConverter;
import com.dati.mcp.server.converter.PromptDefinitionConverter;
import com.dati.mcp.server.converter.ToolResultConverter;
import com.dati.mcp.server.resolver.SnapshotToolResolver;
import com.dati.mcp.repository.po.McpServicePO;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("McpProtocolHandler tests")
class McpProtocolHandlerTest {

    @Mock
    private ToolExecutor executor;

    private McpProtocolHandler handler;
    private McpServicePO service;
    private McpServiceSnapshot.SnapshotContent content;

    @BeforeEach
    void setUp() {
        when(executor.getToolType()).thenReturn(com.dati.mcp.domain.model.McpToolType.SEARCH_METADATA);
        handler = new McpProtocolHandler(
            new ToolDefinitionConverter(), new SnapshotToolResolver(),
            new ToolResultConverter(), new PromptDefinitionConverter(), List.of(executor));
        service = TestFixtures.createTestMcpServicePO();
        service.setStatus(com.dati.mcp.domain.model.McpServiceStatus.PUBLISHED);
        service.setActiveVersionId("snapshot-001");
        service.setActiveVersionNumber(1);
        content = TestFixtures.createTestSnapshotContent();
    }

    @Test
    @DisplayName("initialize returns 2025-11-25 with capabilities matching snapshot")
    void initializeReturnsCapabilities() {
        McpSchema.JSONRPCRequest req = new McpSchema.JSONRPCRequest("initialize", 1, Map.of(
            "protocolVersion", "2025-11-25", "capabilities", Map.of(), "clientInfo", Map.of()));
        McpSchema.JSONRPCResponse resp = handler.handle(service, content, req);
        assertNull(resp.error());
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) resp.result();
        assertEquals("2025-11-25", result.get("protocolVersion"));
        assertTrue(((Map<?, ?>) result.get("capabilities")).containsKey("tools"));
        assertTrue(((Map<?, ?>) result.get("capabilities")).containsKey("prompts"));
        assertEquals("Test MCP Service", ((Map<?, ?>) result.get("serverInfo")).get("name"));
        assertEquals("v1", ((Map<?, ?>) result.get("serverInfo")).get("version"));
    }

    @Test
    @DisplayName("initialize omits capabilities when no enabled tools/prompts")
    void initializeOmitsEmptyCapabilities() {
        var empty = new McpServiceSnapshot.SnapshotContent();
        McpSchema.JSONRPCRequest req = new McpSchema.JSONRPCRequest("initialize", 1, Map.of());
        McpSchema.JSONRPCResponse resp = handler.handle(service, empty, req);
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) resp.result();
        assertFalse(((Map<?, ?>) result.get("capabilities")).containsKey("tools"));
        assertFalse(((Map<?, ?>) result.get("capabilities")).containsKey("prompts"));
    }

    @Test
    @DisplayName("ping returns empty result")
    void pingReturnsEmptyResult() {
        McpSchema.JSONRPCRequest req = new McpSchema.JSONRPCRequest("ping", 2, null);
        McpSchema.JSONRPCResponse resp = handler.handle(service, content, req);
        assertNull(resp.error());
        assertNotNull(resp.result());
    }

    @Test
    @DisplayName("tools/list returns enabled tools from snapshot")
    void toolsListReturnsTools() {
        McpSchema.JSONRPCRequest req = new McpSchema.JSONRPCRequest("tools/list", 3, Map.of());
        McpSchema.JSONRPCResponse resp = handler.handle(service, content, req);
        assertNull(resp.error());
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) resp.result();
        assertEquals(2, ((List<?>) result.get("tools")).size());
    }

    @Test
    @DisplayName("tools/call executes via executor and converts result")
    void toolsCallExecutes() {
        when(executor.execute(any())).thenReturn(
            new com.dati.mcp.server.pojo.SqlExecution("SELECT 1",
                List.of(com.dati.mcp.server.pojo.StatementResult.select(List.of("a"), List.of(List.of(1)), 1)), null));
        McpSchema.JSONRPCRequest req = new McpSchema.JSONRPCRequest("tools/call", 4,
            Map.of("name", "search_metadata", "arguments", Map.of("keywords", List.of("orders"))));
        McpSchema.JSONRPCResponse resp = handler.handle(service, content, req);
        assertNull(resp.error());
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) resp.result();
        assertFalse((boolean) result.get("isError"));
    }

    @Test
    @DisplayName("tools/call with unknown tool returns JSON-RPC error -32602")
    void toolsCallUnknownTool() {
        McpSchema.JSONRPCRequest req = new McpSchema.JSONRPCRequest("tools/call", 5,
            Map.of("name", "ghost", "arguments", Map.of()));
        McpSchema.JSONRPCResponse resp = handler.handle(service, content, req);
        assertNotNull(resp.error());
        assertEquals(-32602, resp.error().code());
    }

    @Test
    @DisplayName("tools/call with tool execution exception returns isError result")
    void toolsCallExecutionError() {
        when(executor.execute(any())).thenThrow(new ToolExecuteException(ToolError.SCOPE_VIOLATION, "not in scope"));
        McpSchema.JSONRPCRequest req = new McpSchema.JSONRPCRequest("tools/call", 6,
            Map.of("name", "search_metadata", "arguments", Map.of("keywords", List.of("x"))));
        McpSchema.JSONRPCResponse resp = handler.handle(service, content, req);
        assertNull(resp.error());
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) resp.result();
        assertTrue((boolean) result.get("isError"));
    }

    @Test
    @DisplayName("prompts/list returns enabled prompts from snapshot")
    void promptsListReturnsPrompts() {
        McpSchema.JSONRPCRequest req = new McpSchema.JSONRPCRequest("prompts/list", 8, Map.of());
        McpSchema.JSONRPCResponse resp = handler.handle(service, content, req);
        assertNull(resp.error());
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) resp.result();
        assertEquals(1, ((List<?>) result.get("prompts")).size());
    }

    @Test
    @DisplayName("prompts/get renders the template with arguments")
    void promptsGetRenders() {
        McpSchema.JSONRPCRequest req = new McpSchema.JSONRPCRequest("prompts/get", 9,
            Map.of("name", "analyze_table", "arguments", Map.of("table", "orders")));
        McpSchema.JSONRPCResponse resp = handler.handle(service, content, req);
        assertNull(resp.error());
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) resp.result();
        assertEquals(1, ((List<?>) result.get("messages")).size());
    }

    @Test
    @DisplayName("prompts/get with unknown prompt returns -32602")
    void promptsGetUnknown() {
        McpSchema.JSONRPCRequest req = new McpSchema.JSONRPCRequest("prompts/get", 10,
            Map.of("name", "ghost", "arguments", Map.of()));
        McpSchema.JSONRPCResponse resp = handler.handle(service, content, req);
        assertNotNull(resp.error());
        assertEquals(-32602, resp.error().code());
    }

    @Test
    @DisplayName("unknown method returns -32601")
    void unknownMethod() {
        McpSchema.JSONRPCRequest req = new McpSchema.JSONRPCRequest("nope/method", 7, null);
        McpSchema.JSONRPCResponse resp = handler.handle(service, content, req);
        assertNotNull(resp.error());
        assertEquals(-32601, resp.error().code());
    }
}
