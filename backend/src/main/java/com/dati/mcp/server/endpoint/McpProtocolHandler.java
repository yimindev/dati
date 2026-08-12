package com.dati.mcp.server.endpoint;

import com.dati.mcp.domain.model.McpServiceSnapshot;
import com.dati.mcp.domain.model.McpToolType;
import com.dati.mcp.domain.service.ToolExecuteException;
import com.dati.mcp.domain.service.ToolExecutionContext;
import com.dati.mcp.domain.service.ToolExecutor;
import com.dati.mcp.domain.service.ToolParameterBinder;
import com.dati.mcp.repository.po.McpServicePO;
import com.dati.mcp.server.converter.ToolDefinitionConverter;
import com.dati.mcp.server.converter.PromptDefinitionConverter;
import com.dati.mcp.server.converter.ToolResultConverter;
import com.dati.mcp.server.resolver.SnapshotToolResolver;
import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

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
    private final ToolParameterBinder parameterBinder;
    private final Map<McpToolType, ToolExecutor> executorMap;
    private final McpJsonMapper jsonMapper = McpJsonDefaults.getMapper();

    public McpProtocolHandler(ToolDefinitionConverter toolDefinitionConverter,
                              SnapshotToolResolver snapshotToolResolver,
                              ToolResultConverter toolResultConverter,
                              PromptDefinitionConverter promptDefinitionConverter,
                              List<ToolExecutor> toolExecutorList,
                              ToolParameterBinder parameterBinder) {
        this.toolDefinitionConverter = toolDefinitionConverter;
        this.snapshotToolResolver = snapshotToolResolver;
        this.toolResultConverter = toolResultConverter;
        this.promptDefinitionConverter = promptDefinitionConverter;
        this.parameterBinder = parameterBinder;
        this.executorMap = toolExecutorList.stream()
            .collect(Collectors.toMap(ToolExecutor::getToolType, Function.identity()));
    }

    public McpSchema.JSONRPCResponse handle(McpServicePO service,
                                            McpServiceSnapshot.SnapshotContent content,
                                            McpSchema.JSONRPCRequest request) {
        try {
            return switch (request.method()) {
                case McpSchema.METHOD_INITIALIZE -> handleInitialize(service, content, request);
                case McpSchema.METHOD_PING -> McpSchema.JSONRPCResponse.result(request.id(), Map.of());
                case McpSchema.METHOD_TOOLS_LIST -> handleToolsList(content, request);
                case McpSchema.METHOD_TOOLS_CALL -> handleToolsCall(service.getId(), content, request);
                case McpSchema.METHOD_PROMPT_LIST -> handlePromptsList(content, request);
                case McpSchema.METHOD_PROMPT_GET -> handlePromptsGet(content, request);
                default -> McpSchema.JSONRPCResponse.error(request.id(), methodNotImplemented(request.method()));
            };
        } catch (ToolExecuteException e) {
            return McpSchema.JSONRPCResponse.result(request.id(), toolResultConverter.toError(e));
        } catch (Exception e) {
            log.error("MCP request failed: {}", e.getMessage(), e);
            return McpSchema.JSONRPCResponse.error(request.id(),
                new McpSchema.JSONRPCResponse.JSONRPCError(McpSchema.ErrorCodes.INTERNAL_ERROR, "Internal error: " + e.getMessage(), null));
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
        var capabilities = McpSchema.ServerCapabilities.builder();
        if (hasEnabledTools(content)) {
            capabilities.tools(false);
        }
        if (hasEnabledPrompts(content)) {
            capabilities.prompts(false);
        }
        var serverInfo = McpSchema.Implementation.builder(service.getName(),
            "v" + (service.getActiveVersionNumber() == null ? 0 : service.getActiveVersionNumber())).build();
        McpSchema.InitializeResult result = McpSchema.InitializeResult.builder(
            PROTOCOL_VERSION, capabilities.build(), serverInfo).build();
        return McpSchema.JSONRPCResponse.result(request.id(), result);
    }

    private McpSchema.JSONRPCResponse handleToolsList(McpServiceSnapshot.SnapshotContent content,
                                                      McpSchema.JSONRPCRequest request) {
        return McpSchema.JSONRPCResponse.result(request.id(),
            McpSchema.ListToolsResult.builder(toolDefinitionConverter.convert(content)).build());
    }

    private McpSchema.JSONRPCResponse handleToolsCall(String serviceId,
                                                      McpServiceSnapshot.SnapshotContent content,
                                                      McpSchema.JSONRPCRequest request) {
        McpSchema.CallToolRequest callReq = parseParams(request, McpSchema.CallToolRequest.class);
        String name = callReq == null ? null : callReq.name();
        if (name == null || name.isBlank()) {
            return McpSchema.JSONRPCResponse.error(request.id(),
                new McpSchema.JSONRPCResponse.JSONRPCError(McpSchema.ErrorCodes.INVALID_PARAMS, "Missing required parameter: name", null));
        }
        SnapshotToolResolver.ResolvedTool tool = snapshotToolResolver.resolve(content, name).orElse(null);
        if (tool == null) {
            return McpSchema.JSONRPCResponse.error(request.id(),
                new McpSchema.JSONRPCResponse.JSONRPCError(McpSchema.ErrorCodes.INVALID_PARAMS, "Unknown tool: " + name, null));
        }
        ToolExecutor executor = executorMap.get(tool.toolType());
        if (executor == null) {
            return McpSchema.JSONRPCResponse.error(request.id(),
                new McpSchema.JSONRPCResponse.JSONRPCError(McpSchema.ErrorCodes.INTERNAL_ERROR, "No executor for tool type: " + tool.toolType(), null));
        }
        Map<String, Object> arguments = callReq.arguments() == null ? Map.of() : callReq.arguments();
        ToolExecutionContext ctx = new ToolExecutionContext(
            serviceId, tool.toolType(), tool.config(),
            parameterBinder.bind(tool.toolType().getParameterType(), arguments),
            snapshotToolResolver.buildScopeItems(serviceId, content));
        var data = executor.execute(ctx);
        return McpSchema.JSONRPCResponse.result(request.id(), toolResultConverter.toResult(data));
    }

    private McpSchema.JSONRPCResponse handlePromptsList(McpServiceSnapshot.SnapshotContent content,
                                                        McpSchema.JSONRPCRequest request) {
        return McpSchema.JSONRPCResponse.result(request.id(),
            McpSchema.ListPromptsResult.builder(promptDefinitionConverter.list(content)).build());
    }

    private McpSchema.JSONRPCResponse handlePromptsGet(McpServiceSnapshot.SnapshotContent content,
                                                       McpSchema.JSONRPCRequest request) {
        McpSchema.GetPromptRequest promptReq = parseParams(request, McpSchema.GetPromptRequest.class);
        String name = promptReq == null ? null : promptReq.name();
        if (name == null || name.isBlank()) {
            return McpSchema.JSONRPCResponse.error(request.id(),
                new McpSchema.JSONRPCResponse.JSONRPCError(McpSchema.ErrorCodes.INVALID_PARAMS, "Missing required parameter: name", null));
        }
        try {
            Map<String, Object> arguments = promptReq.arguments() == null ? Map.of() : promptReq.arguments();
            McpSchema.GetPromptResult result = promptDefinitionConverter.get(content, name, arguments);
            return McpSchema.JSONRPCResponse.result(request.id(), result);
        } catch (IllegalArgumentException e) {
            return McpSchema.JSONRPCResponse.error(request.id(),
                new McpSchema.JSONRPCResponse.JSONRPCError(McpSchema.ErrorCodes.INVALID_PARAMS, e.getMessage(), null));
        }
    }

    /** Converts the generic JSON-RPC params into the typed MCP request record. */
    private <T> T parseParams(McpSchema.JSONRPCRequest request, Class<T> type) {
        return jsonMapper.convertValue(request.params(), type);
    }

    private McpSchema.JSONRPCResponse.JSONRPCError methodNotImplemented(String method) {
        return new McpSchema.JSONRPCResponse.JSONRPCError(McpSchema.ErrorCodes.METHOD_NOT_FOUND, "Method not found: " + method, null);
    }
}
