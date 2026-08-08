package com.dati.mcp.server.converter;

import com.dati.common.JsonUtils;
import com.dati.mcp.domain.service.ToolExecuteException;
import com.dati.mcp.server.pojo.ToolTestData;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Converts ToolTestData (DatI internal result) into MCP CallToolResult JSON.
 * Protocol errors are JSON-RPC errors (handled by McpProtocolHandler);
 * tool execution errors become isError=true results so LLMs can self-correct.
 */
@Component
public class ToolResultConverter {

    public Map<String, Object> toResult(ToolTestData data) {
        Map<String, Object> result = new HashMap<>();
        result.put("content", List.of(Map.of("type", "text", "text", JsonUtils.toJson(data))));
        result.put("structuredContent", JsonUtils.toMap(JsonUtils.toJson(data)));
        result.put("isError", false);
        return result;
    }

    public Map<String, Object> toError(ToolExecuteException e) {
        Map<String, Object> result = new HashMap<>();
        result.put("content", List.of(Map.of("type", "text", "text", e.getMessage())));
        result.put("isError", true);
        return result;
    }
}
