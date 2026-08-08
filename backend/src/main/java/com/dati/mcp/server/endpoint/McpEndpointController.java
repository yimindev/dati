package com.dati.mcp.server.endpoint;

import com.dati.auth.domain.service.AuthenticationService;
import com.dati.base.RequestContext;
import com.dati.common.JsonUtils;
import com.dati.mcp.domain.model.McpServiceSnapshot;
import com.dati.mcp.domain.model.McpServiceStatus;
import com.dati.mcp.repository.dao.McpServiceDAO;
import com.dati.mcp.repository.dao.McpServiceSnapshotDAO;
import com.dati.mcp.repository.mapper.McpServiceSnapshotMapper;
import com.dati.mcp.repository.po.McpServicePO;
import com.dati.mcp.repository.po.McpServiceSnapshotPO;
import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

/**
 * MCP endpoint HTTP layer. Routes /{code}/mcp, enforces service status semantics,
 * Bearer authentication, and transport-level header validation. JSON-RPC dispatch
 * is delegated to {@link McpProtocolHandler}. Stateless: no Mcp-Session-Id issued.
 */
@Slf4j
@RestController
public class McpEndpointController {

    private final McpServiceDAO mcpServiceDAO;
    private final McpServiceSnapshotDAO snapshotDAO;
    private final AuthenticationService authenticationService;
    private final McpProtocolHandler protocolHandler;
    private final McpJsonMapper jsonMapper = McpJsonDefaults.getMapper();

    public McpEndpointController(McpServiceDAO mcpServiceDAO,
                                 McpServiceSnapshotDAO snapshotDAO,
                                 AuthenticationService authenticationService,
                                 McpProtocolHandler protocolHandler) {
        this.mcpServiceDAO = mcpServiceDAO;
        this.snapshotDAO = snapshotDAO;
        this.authenticationService = authenticationService;
        this.protocolHandler = protocolHandler;
    }

    @PostMapping("/{code}/mcp")
    public ResponseEntity<String> handle(@PathVariable String code, HttpServletRequest request,
                                         @RequestBody String body) {
        // 1. Authentication (before any existence info leaks)
        var user = authenticationService.authenticate(request);
        if (user.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .header("WWW-Authenticate", "Bearer")
                .build();
        }
        // Populate RequestContext so downstream services (permission checks, audit fields)
        // can resolve the current user; clear afterwards to avoid leaking across virtual threads.
        RequestContext.setUser(user.get());
        try {
            return handleAuthenticated(code, request, body);
        } finally {
            RequestContext.clear();
        }
    }

    private ResponseEntity<String> handleAuthenticated(String code, HttpServletRequest request,
                                                       String body) {
        // 2. Route + status semantics
        McpServicePO service = mcpServiceDAO.findByCode(code).orElse(null);
        if (service == null || service.getStatus() == McpServiceStatus.DRAFT) {
            return ResponseEntity.notFound().build();
        }
        if (service.getStatus() == McpServiceStatus.DISABLED) {
            String err = jsonError(body, -32603, "Service is disabled");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .contentType(MediaType.APPLICATION_JSON).body(err);
        }
        // 3. Transport validation
        String accept = request.getHeader("Accept");
        if (accept == null || !(accept.contains("application/json") && accept.contains("text/event-stream"))) {
            return ResponseEntity.badRequest().build();
        }
        String origin = request.getHeader("Origin");
        if (origin != null && !origin.isBlank() && !isTrustedOrigin(origin)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        // initialize is exempt from the protocol version header requirement (2025-11-25 spec)
        String method = parseMethod(body);
        String protocolVersion = request.getHeader("MCP-Protocol-Version");
        if (!"initialize".equals(method) && !McpProtocolHandler.PROTOCOL_VERSION.equals(protocolVersion)) {
            return ResponseEntity.badRequest().build();
        }
        // 4. Snapshot loading (active version only)
        McpServiceSnapshot.SnapshotContent content = loadActiveSnapshot(service);
        if (content == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
        // 5. JSON-RPC dispatch
        try {
            McpSchema.JSONRPCMessage message = McpSchema.deserializeJsonRpcMessage(jsonMapper, body);
            if (message instanceof McpSchema.JSONRPCRequest req) {
                McpSchema.JSONRPCResponse resp = protocolHandler.handle(service, content, req);
                return ResponseEntity.ok(jsonMapper.writeValueAsString(resp));
            }
            // notifications: accept without response (2025-11-25 allows; we have none to handle)
            return ResponseEntity.status(HttpStatus.ACCEPTED).build();
        } catch (Exception e) {
            log.warn("Invalid MCP message from {}: {}", code, e.getMessage());
            return ResponseEntity.badRequest().body(jsonError(body, -32700, "Parse error: " + e.getMessage()));
        }
    }

    /**
     * DNS rebinding protection (MCP 2025-11-25 security best practices):
     * non-localhost Origin headers are rejected; loopback origins are trusted
     * (browser-based MCP clients run locally).
     */
    private boolean isTrustedOrigin(String origin) {
        try {
            String host = new java.net.URI(origin).getHost();
            return host != null
                && (host.equals("localhost") || host.equals("127.0.0.1")
                    || host.equals("[::1]") || host.equals("::1"));
        } catch (Exception e) {
            return false;
        }
    }

    private String parseMethod(String body) {
        try {
            return JsonUtils.parseJson(body).path("method").asText(null);
        } catch (Exception e) {
            return null;
        }
    }

    /** Echoes the request id when present; serializes manually when absent (SDK rejects null ids). */
    private String jsonError(String body, int code, String message) {
        Object id = parseId(body);
        if (id == null) {
            return "{\"jsonrpc\":\"2.0\",\"error\":{\"code\":" + code + ",\"message\":\"" + message + "\"}}";
        }
        McpSchema.JSONRPCResponse resp = McpSchema.JSONRPCResponse.error(id,
            new McpSchema.JSONRPCResponse.JSONRPCError(code, message, null));
        try {
            return jsonMapper.writeValueAsString(resp);
        } catch (Exception e) {
            return "{\"jsonrpc\":\"2.0\",\"error\":{\"code\":" + code + ",\"message\":\"" + message + "\"}}";
        }
    }

    private Object parseId(String body) {
        try {
            var idNode = JsonUtils.parseJson(body).get("id");
            if (idNode == null || idNode.isNull() || idNode.isMissingNode()) {
                return null;
            }
            return idNode.isIntegralNumber() ? idNode.asLong() : idNode.asText();
        } catch (Exception e) {
            return null;
        }
    }

    private McpServiceSnapshot.SnapshotContent loadActiveSnapshot(McpServicePO service) {
        if (service.getActiveVersionId() == null) {
            return null;
        }
        Optional<McpServiceSnapshotPO> po = snapshotDAO.findById(service.getActiveVersionId());
        if (po.isEmpty()) {
            return null;
        }
        McpServiceSnapshot snapshot = McpServiceSnapshotMapper.toModel(po.get());
        return snapshot.getContent();
    }
}
