package com.dati.mcp.server.endpoint;

import com.dati.TestFixtures;
import com.dati.auth.authentication.User;
import com.dati.auth.domain.service.AuthenticationService;
import com.dati.mcp.domain.model.McpServiceStatus;
import com.dati.mcp.domain.service.McpUsageCollector;
import com.dati.mcp.repository.dao.McpServiceDAO;
import com.dati.mcp.repository.dao.McpServiceSnapshotDAO;
import com.dati.mcp.repository.po.McpServicePO;
import com.dati.permission.domain.service.PermissionService;
import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(McpEndpointController.class)
@Import(McpEndpointService.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = "dati.mcp.allowed-origins=https://trusted.example.com")
@DisplayName("McpEndpointController integration tests")
class McpEndpointControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private McpServiceDAO mcpServiceDAO;

    @MockitoBean
    private McpServiceSnapshotDAO snapshotDAO;

    @MockitoBean
    private AuthenticationService authenticationService;

    @MockitoBean
    private McpProtocolHandler protocolHandler;

    @MockitoBean
    private PermissionService permissionService;

    @MockitoBean
    private McpUsageCollector usageCollector;

    private McpServicePO service;

    @BeforeEach
    void setUp() {
        service = TestFixtures.createTestMcpServicePO();
        service.setStatus(McpServiceStatus.PUBLISHED);
        service.setActiveVersionId("snapshot-001");
        service.setActiveVersionNumber(1);
        when(mcpServiceDAO.findByCode("test-mcp-service")).thenReturn(Optional.of(service));
        when(authenticationService.authenticate(any())).thenReturn(Optional.of(new User()));
        when(permissionService.can(any(), any(), any(), any(), any(), any())).thenReturn(true);
        when(snapshotDAO.findById("snapshot-001"))
            .thenReturn(Optional.of(TestFixtures.createTestSnapshotPO()));
    }

    @Test
    @DisplayName("POST /{code}/mcp responds to tools/list with snapshot tools")
    void toolsList() throws Exception {
        McpSchema.JSONRPCRequest req = new McpSchema.JSONRPCRequest("tools/list", 1, java.util.Map.of());
        when(protocolHandler.handle(any(), any(), any())).thenReturn(
            McpSchema.JSONRPCResponse.result(1, java.util.Map.of("tools", java.util.List.of())));
        mockMvc.perform(post("/test-mcp-service/mcp")
                .header("MCP-Protocol-Version", "2025-11-25")
                .header("Accept", "application/json, text/event-stream")
                .header("Authorization", "Bearer abc")
                .contentType(MediaType.APPLICATION_JSON)
                .content(McpJsonDefaults.getMapper().writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.tools").isArray());
    }

    @Test
    @DisplayName("unknown code returns 404")
    void unknownCode() throws Exception {
        when(mcpServiceDAO.findByCode("ghost")).thenReturn(Optional.empty());
        mockMvc.perform(post("/ghost/mcp")
                .header("Accept", "application/json, text/event-stream")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/list\"}"))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DRAFT service returns 404")
    void draftService() throws Exception {
        service.setStatus(McpServiceStatus.DRAFT);
        mockMvc.perform(post("/test-mcp-service/mcp")
                .header("Accept", "application/json, text/event-stream")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/list\"}"))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DISABLED service returns 503 with JSON-RPC error body")
    void disabledService() throws Exception {
        service.setStatus(McpServiceStatus.DISABLED);
        mockMvc.perform(post("/test-mcp-service/mcp")
                .header("Accept", "application/json, text/event-stream")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/list\"}"))
            .andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @DisplayName("missing or invalid Bearer token returns 401")
    void unauthorized() throws Exception {
        when(authenticationService.authenticate(any())).thenReturn(Optional.empty());
        mockMvc.perform(post("/test-mcp-service/mcp")
                .header("Accept", "application/json, text/event-stream")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/list\"}"))
            .andExpect(status().isUnauthorized())
            .andExpect(header().string("WWW-Authenticate", "Bearer"));
    }

    @Test
    @DisplayName("Accept header is not validated (any Accept works)")
    void acceptNotValidated() throws Exception {
        when(protocolHandler.handle(any(), any(), any())).thenReturn(
            McpSchema.JSONRPCResponse.result(1, java.util.Map.of("tools", java.util.List.of())));
        mockMvc.perform(post("/test-mcp-service/mcp")
                .header("Accept", "text/event-stream")
                .header("MCP-Protocol-Version", "2025-11-25")
                .header("Authorization", "Bearer abc")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/list\"}"))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("no Accept header is accepted")
    void noAcceptHeader() throws Exception {
        when(protocolHandler.handle(any(), any(), any())).thenReturn(
            McpSchema.JSONRPCResponse.result(1, java.util.Map.of("tools", java.util.List.of())));
        mockMvc.perform(post("/test-mcp-service/mcp")
                .header("MCP-Protocol-Version", "2025-11-25")
                .header("Authorization", "Bearer abc")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/list\"}"))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("non-initialize request without MCP-Protocol-Version returns 400")
    void missingProtocolVersion() throws Exception {
        mockMvc.perform(post("/test-mcp-service/mcp")
                .header("Accept", "application/json, text/event-stream")
                .header("Authorization", "Bearer abc")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/list\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("non-localhost Origin returns 403 (not whitelisted)")
    void evilOriginRejected() throws Exception {
        mockMvc.perform(post("/test-mcp-service/mcp")
                .header("Accept", "application/json, text/event-stream")
                .header("MCP-Protocol-Version", "2025-11-25")
                .header("Authorization", "Bearer abc")
                .header("Origin", "http://evil.example.com")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/list\"}"))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("whitelisted non-loopback Origin is accepted")
    void whitelistedOriginAccepted() throws Exception {
        when(protocolHandler.handle(any(), any(), any())).thenReturn(
            McpSchema.JSONRPCResponse.result(1, java.util.Map.of("tools", java.util.List.of())));
        mockMvc.perform(post("/test-mcp-service/mcp")
                .header("Accept", "application/json, text/event-stream")
                .header("MCP-Protocol-Version", "2025-11-25")
                .header("Authorization", "Bearer abc")
                .header("Origin", "https://trusted.example.com")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/list\"}"))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("localhost Origin header is accepted (DNS rebinding protection)")
    void localhostOriginAccepted() throws Exception {
        when(protocolHandler.handle(any(), any(), any())).thenReturn(
            McpSchema.JSONRPCResponse.result(1, java.util.Map.of("tools", java.util.List.of())));
        mockMvc.perform(post("/test-mcp-service/mcp")
                .header("Accept", "application/json, text/event-stream")
                .header("MCP-Protocol-Version", "2025-11-25")
                .header("Authorization", "Bearer abc")
                .header("Origin", "http://localhost:8085")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/list\"}"))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("unparseable body with protocol version returns 400 with JSON-RPC parse error")
    void parseErrorBody() throws Exception {
        String invalidJsonContent = "not-a-json-{";
        mockMvc.perform(post("/test-mcp-service/mcp")
                .header("Accept", "application/json, text/event-stream")
                .header("MCP-Protocol-Version", "2025-11-25")
                .header("Authorization", "Bearer abc")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJsonContent))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error.code").value(-32700))
            .andExpect(jsonPath("$.error.message").isString());
    }

    @Test
    @DisplayName("GET returns 405")
    void getMethodNotAllowed() throws Exception {
        mockMvc.perform(get("/test-mcp-service/mcp"))
            .andExpect(status().isMethodNotAllowed());
    }

    @Test
    @DisplayName("protocol requests (tools/list) are not recorded as usage")
    void protocolRequestsNotRecorded() throws Exception {
        when(protocolHandler.handle(any(), any(), any())).thenReturn(
            McpSchema.JSONRPCResponse.result(1, java.util.Map.of("tools", java.util.List.of())));

        clearInvocations(usageCollector);
        mockMvc.perform(post("/test-mcp-service/mcp")
                .header("Accept", "application/json, text/event-stream")
                .header("MCP-Protocol-Version", "2025-11-25")
                .header("Authorization", "Bearer abc")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/list\"}"))
            .andExpect(status().isOk());

        verifyNoInteractions(usageCollector);
    }

    @Test
    @DisplayName("tools/call request should record usage with toolName and client IP")
    void toolsCallRecordsUsage() throws Exception {
        when(protocolHandler.handle(any(), any(), any())).thenReturn(
            McpSchema.JSONRPCResponse.result(1, java.util.Map.of("content", java.util.List.of())));

        mockMvc.perform(post("/test-mcp-service/mcp")
                .header("Accept", "application/json, text/event-stream")
                .header("MCP-Protocol-Version", "2025-11-25")
                .header("Authorization", "Bearer abc")
                .header("X-Forwarded-For", "192.168.1.100")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/call\",\"params\":{\"name\":\"execute_sql\",\"arguments\":{}}}"))
            .andExpect(status().isOk());

        verify(usageCollector).record(
            eq(service.getId()),
            eq("tools/call"),
            eq("execute_sql"),
            any(),
            any(),
            eq("192.168.1.100"),
            eq(true),
            anyLong(),
            isNull()
        );
    }
}
