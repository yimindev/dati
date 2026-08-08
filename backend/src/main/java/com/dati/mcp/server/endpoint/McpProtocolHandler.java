package com.dati.mcp.server.endpoint;

import com.dati.mcp.domain.model.McpServiceSnapshot;
import com.dati.mcp.domain.model.McpToolType;
import com.dati.mcp.domain.service.ToolExecuteException;
import com.dati.mcp.domain.service.ToolExecutionContext;
import com.dati.mcp.domain.service.ToolExecutor;
import com.dati.mcp.repository.po.McpServicePO;
import com.dati.mcp.server.converter.ToolDefinitionConverter;
import com.dati.mcp.server.converter.PromptDefinitionConverter;
import com.dati.mcp.server.converter.ToolResultConverter;
import com.dati.mcp.server.resolver.SnapshotToolResolver;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * JSON-RPC method dispatch for the MCP endpoint. Stateless: every request is
 * processed independently against the active snapshot content.
 */
@Slf4j
@Component
public class McpProtocolHandler {

    static final String PROTOCOL_VERSION = "2025-11-25";

    private final ToolDefinitionConverter toolDefinitionConverter;
    private final SnapshotToolResolver snapshotToolResolver;
    private final ToolResultConverter toolResultConverter;
    private final PromptDefinitionConverter promptDefinitionConverter;
    private final Map<McpToolType, ToolExecutor> executorMap;

    public McpProtocolHandler(ToolDefinitionConverter toolDefinitionConverter,
                              SnapshotToolResolver snapshotToolResolver,
                              ToolResultConverter toolResultConverter,
                              PromptDefinitionConverter promptDefinitionConverter,
                              List<ToolExecutor> toolExecutorList) {
        this.toolDefinitionConverter = toolDefinitionConverter;
        this.snapshotToolResolver = snapshotToolResolver;
        this.toolResultConverter = toolResultConverter;
        this.promptDefinitionConverter = promptDefinitionConverter;
        this.executorMap = toolExecutorList.stream()
            .collect(Collectors.toMap(ToolExecutor::getToolType, Function.identity()));
    }

    public McpSchema.JSONRPCResponse handle(McpServicePO service,
                                            McpServiceSnapshot.SnapshotContent content,
                                            McpSchema.JSONRPCRequest request) {
        try {
            return switch (request.method()) {
                case "initialize" -> handleInitialize(service, content, request);
                case "ping" -> McpSchema.JSONRPCResponse.result(request.id(), Map.of());
                case "tools/list" -> handleToolsList(content, request);
                case "tools/call" -> handleToolsCall(service.getId(), content, request);
                case "prompts/list" -> handlePromptsList(content, request);
                case "prompts/get" -> handlePromptsGet(content, request);
                default -> McpSchema.JSONRPCResponse.error(request.id(), methodNotImplemented(request.method()));
            };
        } catch (ToolExecuteException e) {
            return McpSchema.JSONRPCResponse.result(request.id(), toolResultConverter.toError(e));
        } catch (Exception e) {
            log.error("MCP request failed: {}", e.getMessage(), e);
            return McpSchema.JSONRPCResponse.error(request.id(),
                new McpSchema.JSONRPCResponse.JSONRPCError(-32603, "Internal error: " + e.getMessage(), null));
        }
    }

    public boolean hasEnabledTools(McpServiceSnapshot.SnapshotContent content) {
        return !toolDefinitionConverter.convert(content).isEmpty();
    }

    public boolean hasEnabledPrompts(McpServiceSnapshot.SnapshotContent content) {
        return content.getPrompts() != null
            && content.getPrompts().stream().anyMatch(McpServiceSnapshot.PromptDraft::enabled);
    }

    private McpSchema.JSONRPCResponse handleInitialize(McpServicePO service,
                                                       McpServiceSnapshot.SnapshotContent content,
                                                       McpSchema.JSONRPCRequest request) {
        Map<String, Object> capabilities = new HashMap<>();
        if (hasEnabledTools(content)) {
            capabilities.put("tools", Map.of("listChanged", false));
        }
        if (hasEnabledPrompts(content)) {
            capabilities.put("prompts", Map.of("listChanged", false));
        }
        Map<String, Object> result = new HashMap<>();
        result.put("protocolVersion", PROTOCOL_VERSION);
        result.put("capabilities", capabilities);
        result.put("serverInfo", Map.of(
            "name", service.getName(),
            "version", "v" + (service.getActiveVersionNumber() == null ? 0 : service.getActiveVersionNumber())));
        return McpSchema.JSONRPCResponse.result(request.id(), result);
    }

    private McpSchema.JSONRPCResponse handleToolsList(McpServiceSnapshot.SnapshotContent content,
                                                      McpSchema.JSONRPCRequest request) {
        Map<String, Object> result = new HashMap<>();
        result.put("tools", toolDefinitionConverter.convert(content));
        return McpSchema.JSONRPCResponse.result(request.id(), result);
    }

    private McpSchema.JSONRPCResponse handleToolsCall(String serviceId,
                                                      McpServiceSnapshot.SnapshotContent content,
                                                      McpSchema.JSONRPCRequest request) {
        Map<?, ?> params = asParams(request);
        String name = params == null ? null : (String) params.get("name");
        if (name == null || name.isBlank()) {
            return McpSchema.JSONRPCResponse.error(request.id(),
                new McpSchema.JSONRPCResponse.JSONRPCError(-32602, "Missing required parameter: name", null));
        }
        SnapshotToolResolver.ResolvedTool tool = snapshotToolResolver.resolve(content, name).orElse(null);
        if (tool == null) {
            return McpSchema.JSONRPCResponse.error(request.id(),
                new McpSchema.JSONRPCResponse.JSONRPCError(-32602, "Unknown tool: " + name, null));
        }
        ToolExecutor executor = executorMap.get(tool.toolType());
        if (executor == null) {
            return McpSchema.JSONRPCResponse.error(request.id(),
                new McpSchema.JSONRPCResponse.JSONRPCError(-32603, "No executor for tool type: " + tool.toolType(), null));
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> arguments = params.get("arguments") == null
            ? Map.of() : (Map<String, Object>) params.get("arguments");
        ToolExecutionContext ctx = new ToolExecutionContext(
            serviceId, tool.toolType(), tool.config(), arguments,
            snapshotToolResolver.buildScopeItems(serviceId, content));
        var data = executor.execute(ctx);
        return McpSchema.JSONRPCResponse.result(request.id(), toolResultConverter.toResult(data));
    }

    private McpSchema.JSONRPCResponse handlePromptsList(McpServiceSnapshot.SnapshotContent content,
                                                        McpSchema.JSONRPCRequest request) {
        Map<String, Object> result = new HashMap<>();
        result.put("prompts", promptDefinitionConverter.list(content));
        return McpSchema.JSONRPCResponse.result(request.id(), result);
    }

    private McpSchema.JSONRPCResponse handlePromptsGet(McpServiceSnapshot.SnapshotContent content,
                                                       McpSchema.JSONRPCRequest request) {
        Map<?, ?> params = asParams(request);
        String name = params == null ? null : (String) params.get("name");
        if (name == null || name.isBlank()) {
            return McpSchema.JSONRPCResponse.error(request.id(),
                new McpSchema.JSONRPCResponse.JSONRPCError(-32602, "Missing required parameter: name", null));
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> arguments = params.get("arguments") == null
                ? Map.of() : (Map<String, Object>) params.get("arguments");
            Map<String, Object> result = promptDefinitionConverter.get(content, name, arguments);
            return McpSchema.JSONRPCResponse.result(request.id(), result);
        } catch (IllegalArgumentException e) {
            return McpSchema.JSONRPCResponse.error(request.id(),
                new McpSchema.JSONRPCResponse.JSONRPCError(-32602, e.getMessage(), null));
        }
    }

    private Map<?, ?> asParams(McpSchema.JSONRPCRequest request) {
        return request.params() instanceof Map<?, ?> m ? m : null;
    }

    private McpSchema.JSONRPCResponse.JSONRPCError methodNotImplemented(String method) {
        return new McpSchema.JSONRPCResponse.JSONRPCError(-32601, "Method not found: " + method, null);
    }
}
