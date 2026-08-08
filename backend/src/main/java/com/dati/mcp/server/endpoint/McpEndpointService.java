package com.dati.mcp.server.endpoint;

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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * MCP endpoint orchestration: service status semantics (404/503), transport-level
 * validation (Origin DNS-rebinding protection, MCP-Protocol-Version), active snapshot
 * loading and JSON-RPC dispatch delegation. HTTP concerns (routing, headers, response
 * mapping) stay in {@link McpEndpointController}. Stateless: no Mcp-Session-Id issued.
 */
@Slf4j
@Service
public class McpEndpointService {

    private final McpServiceDAO mcpServiceDAO;
    private final McpServiceSnapshotDAO snapshotDAO;
    private final McpProtocolHandler protocolHandler;
    private final Set<String> allowedOrigins;
    private final McpJsonMapper jsonMapper = McpJsonDefaults.getMapper();

    public McpEndpointService(McpServiceDAO mcpServiceDAO,
                              McpServiceSnapshotDAO snapshotDAO,
                              McpProtocolHandler protocolHandler,
                              @Value("${dati.mcp.allowed-origins:}") String allowedOrigins) {
        this.mcpServiceDAO = mcpServiceDAO;
        this.snapshotDAO = snapshotDAO;
        this.protocolHandler = protocolHandler;
        this.allowedOrigins = Arrays.stream(allowedOrigins.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toSet());
    }

    /** Endpoint result: HTTP status plus optional JSON-RPC response body. */
    public record McpEndpointResult(HttpStatus status, Object body) {
    }

    public McpEndpointResult handle(String code, String body, String origin, String protocolVersion) {
        // 1. Service status semantics: unknown code / DRAFT are indistinguishable (404),
        //    DISABLED is explicit (503 + JSON-RPC error)
        McpServicePO service = mcpServiceDAO.findByCode(code).orElse(null);
        if (service == null || service.getStatus() == McpServiceStatus.DRAFT) {
            return new McpEndpointResult(HttpStatus.NOT_FOUND, null);
        }
        if (service.getStatus() == McpServiceStatus.DISABLED) {
            return new McpEndpointResult(HttpStatus.SERVICE_UNAVAILABLE,
                errorEnvelope(body, McpSchema.ErrorCodes.INTERNAL_ERROR, "Service is disabled"));
        }
        // 2. Transport validation: Origin DNS-rebinding protection (loopback or
        //    whitelisted origins only) and MCP-Protocol-Version (initialize exempt).
        //    Accept is intentionally not validated: we always respond with JSON.
        if (origin != null && !origin.isBlank() && !isTrustedOrigin(origin)) {
            return new McpEndpointResult(HttpStatus.FORBIDDEN, null);
        }
        String method = parseMethod(body);
        if (!McpSchema.METHOD_INITIALIZE.equals(method) && !McpProtocolHandler.PROTOCOL_VERSION.equals(protocolVersion)) {
            return new McpEndpointResult(HttpStatus.BAD_REQUEST, null);
        }
        // 3. Active snapshot only (version isolation: drafts are never exposed)
        McpServiceSnapshot.SnapshotContent content = loadActiveSnapshot(service);
        if (content == null) {
            return new McpEndpointResult(HttpStatus.INTERNAL_SERVER_ERROR, null);
        }
        // 4. JSON-RPC dispatch
        try {
            McpSchema.JSONRPCMessage message = McpSchema.deserializeJsonRpcMessage(jsonMapper, body);
            if (message instanceof McpSchema.JSONRPCRequest req) {
                return new McpEndpointResult(HttpStatus.OK, protocolHandler.handle(service, content, req));
            }
            // notifications: accept without response (2025-11-25 allows; we have none to handle)
            return new McpEndpointResult(HttpStatus.ACCEPTED, null);
        } catch (Exception e) {
            log.warn("Invalid MCP message from {}: {}", code, e.getMessage());
            return new McpEndpointResult(HttpStatus.BAD_REQUEST,
                errorEnvelope(body, McpSchema.ErrorCodes.PARSE_ERROR, "Parse error: " + e.getMessage()));
        }
    }

    /**
     * DNS rebinding protection (MCP 2025-11-25 security best practices):
     * loopback origins are always trusted (browser-based MCP clients run locally);
     * additional origins must be whitelisted via {@code dati.mcp.allowed-origins}
     * (comma-separated full origins, e.g. https://dati.example.com) for remote
     * deployments. Requests without an Origin header are not checked.
     */
    private boolean isTrustedOrigin(String origin) {
        String normalized = origin.endsWith("/") ? origin.substring(0, origin.length() - 1) : origin;
        if (allowedOrigins.contains(normalized)) {
            return true;
        }
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

    /**
     * JSON-RPC error envelope, echoing the request id when parseable. The SDK's
     * {@link McpSchema.JSONRPCResponse} rejects null ids, so unparseable bodies
     * (e.g. malformed JSON) fall back to the minimal JSON-RPC 2.0 envelope.
     */
    private Object errorEnvelope(String body, int code, String message) {
        Object id = parseId(body);
        McpSchema.JSONRPCResponse.JSONRPCError error =
            new McpSchema.JSONRPCResponse.JSONRPCError(code, message);
        return id != null
            ? McpSchema.JSONRPCResponse.error(id, error)
            : Map.of("jsonrpc", McpSchema.JSONRPC_VERSION, "error", error);
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
