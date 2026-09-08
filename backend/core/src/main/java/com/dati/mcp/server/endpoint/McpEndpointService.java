package com.dati.mcp.server.endpoint;

import com.dati.auth.authentication.User;
import com.dati.base.RequestContext;
import com.dati.common.JsonUtils;
import com.dati.mcp.domain.model.McpServiceSnapshot;
import com.dati.mcp.domain.model.McpServiceStatus;
import com.dati.mcp.repository.dao.McpServiceDAO;
import com.dati.mcp.repository.dao.McpServiceSnapshotDAO;
import com.dati.mcp.repository.mapper.McpServiceSnapshotMapper;
import com.dati.mcp.repository.po.McpServicePO;
import com.dati.mcp.repository.po.McpServiceSnapshotPO;
import com.dati.permission.domain.model.Permission;
import com.dati.permission.domain.model.ResourceType;
import com.dati.permission.domain.service.PermissionService;
import com.dati.mcp.domain.service.McpUsageCollector;
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
    private final PermissionService permissionService;
    private final McpUsageCollector usageCollector;
    private final Set<String> allowedOrigins;
    private final McpJsonMapper jsonMapper = McpJsonDefaults.getMapper();

    public McpEndpointService(McpServiceDAO mcpServiceDAO,
                              McpServiceSnapshotDAO snapshotDAO,
                              McpProtocolHandler protocolHandler,
                              PermissionService permissionService,
                              McpUsageCollector usageCollector,
                              @Value("${dati.mcp.allowed-origins:}") String allowedOrigins) {
        this.mcpServiceDAO = mcpServiceDAO;
        this.snapshotDAO = snapshotDAO;
        this.protocolHandler = protocolHandler;
        this.permissionService = permissionService;
        this.usageCollector = usageCollector;
        this.allowedOrigins = Arrays.stream(allowedOrigins.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toSet());
    }

    /** Endpoint result: HTTP status plus optional JSON-RPC response body. */
    public record McpEndpointResult(HttpStatus status, Object body) {
    }

    public McpEndpointResult handle(String code, String body, String origin, String protocolVersion, String clientIp) {
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
        User user = RequestContext.getUser();
        if (user != null && !permissionService.can(user.getId(), user.getName(), ResourceType.MCP_SERVICE,
                service.getId(), Permission.VIEW, service.getCreatedBy())) {
            return new McpEndpointResult(HttpStatus.FORBIDDEN,
                errorEnvelope(body, McpSchema.ErrorCodes.INTERNAL_ERROR, "Permission denied"));
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
                long startTime = System.currentTimeMillis();
                McpSchema.JSONRPCResponse resp = protocolHandler.handle(service, content, req);
                long duration = System.currentTimeMillis() - startTime;
                recordUsage(service.getId(), req, resp, duration, user, clientIp);
                return new McpEndpointResult(HttpStatus.OK, resp);
            }
            // notifications: accept without response (2025-11-25 allows; we have none to handle)
            return new McpEndpointResult(HttpStatus.ACCEPTED, null);
        } catch (Exception e) {
            log.warn("Invalid MCP message from {}: {}", code, e.getMessage());
            return new McpEndpointResult(HttpStatus.BAD_REQUEST,
                errorEnvelope(body, McpSchema.ErrorCodes.PARSE_ERROR, "Parse error: " + e.getMessage()));
        }
    }

    private void recordUsage(String serviceId, McpSchema.JSONRPCRequest req, McpSchema.JSONRPCResponse resp,
                             long durationMs, User user, String clientIp) {
        if (usageCollector == null || serviceId == null || req == null) {
            return;
        }
        String method = req.method();
        // Only execution-type requests count as usage: protocol handshakes and metadata queries
        // (initialize, tools/list, resources/list, ping, ...) would pollute the statistics.
        if (!McpSchema.METHOD_TOOLS_CALL.equals(method) && !McpSchema.METHOD_PROMPT_GET.equals(method)) {
            return;
        }
        String toolName = extractToolName(req);
        boolean success = true;
        String errorMessage = null;

        if (resp != null && resp.error() != null) {
            success = false;
            errorMessage = resp.error().message();
        } else if (resp != null && resp.result() instanceof McpSchema.CallToolResult callResult) {
            if (Boolean.TRUE.equals(callResult.isError())) {
                success = false;
                if (callResult.content() != null && !callResult.content().isEmpty()) {
                    Object first = callResult.content().getFirst();
                    if (first instanceof McpSchema.TextContent textContent) {
                        errorMessage = textContent.text();
                    } else {
                        errorMessage = String.valueOf(first);
                    }
                } else {
                    errorMessage = "Tool error";
                }
            }
        }

        String callerId = user != null ? user.getId() : null;
        String callerName = user != null ? user.getName() : null;

        usageCollector.record(serviceId, method, toolName, callerId, callerName, clientIp, success, durationMs, errorMessage);
    }

    private String extractToolName(McpSchema.JSONRPCRequest req) {
        try {
            if (McpSchema.METHOD_TOOLS_CALL.equals(req.method())) {
                var callReq = jsonMapper.convertValue(req.params(), McpSchema.CallToolRequest.class);
                return callReq != null ? callReq.name() : null;
            } else if (McpSchema.METHOD_PROMPT_GET.equals(req.method())) {
                var promptReq = jsonMapper.convertValue(req.params(), McpSchema.GetPromptRequest.class);
                return promptReq != null ? promptReq.name() : null;
            }
        } catch (Exception ignored) {
        }
        return null;
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
