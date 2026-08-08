package com.dati.mcp.server.converter;

import com.dati.common.JsonUtils;
import com.dati.mcp.domain.service.ToolExecuteException;
import com.dati.mcp.server.pojo.ToolTestData;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Converts ToolTestData (DatI internal result) into MCP {@link McpSchema.CallToolResult}.
 * Protocol errors are JSON-RPC errors (handled by McpProtocolHandler);
 * tool execution errors become isError=true results so LLMs can self-correct.
 */
@Component
public class ToolResultConverter {

    public McpSchema.CallToolResult toResult(ToolTestData data) {
        String json = JsonUtils.toJson(data);
        return McpSchema.CallToolResult.builder(List.of(McpSchema.TextContent.builder(json).build()))
            .structuredContent(JsonUtils.toMap(json))
            .isError(false)
            .build();
    }

    public McpSchema.CallToolResult toError(ToolExecuteException e) {
        return McpSchema.CallToolResult.builder(List.of(McpSchema.TextContent.builder(e.getMessage()).build()))
            .isError(true)
            .build();
    }
}
