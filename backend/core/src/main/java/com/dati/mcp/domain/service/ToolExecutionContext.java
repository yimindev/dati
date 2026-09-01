package com.dati.mcp.domain.service;

import com.dati.mcp.domain.model.McpServiceDataScope;
import com.dati.mcp.domain.model.McpToolType;
import com.dati.mcp.domain.model.ToolConfig;

import java.util.List;
import java.util.Map;

public record ToolExecutionContext(String serviceId, McpToolType toolType,
                                   ToolConfig config, Object arguments,
                                   List<McpServiceDataScope> scopeItems) {

    /** Returns the bound parameter record; dynamic tools (PARAMETERIZED_SQL) have none. */
    @SuppressWarnings("unchecked")
    public <T> T args(Class<T> type) {
        if (!type.isInstance(arguments)) {
            throw new IllegalStateException("Tool " + toolType + " expects " + type.getSimpleName()
                + " but got " + (arguments == null ? "null" : arguments.getClass().getSimpleName()));
        }
        return (T) arguments;
    }

    /** Returns the raw argument map; only dynamic tools (PARAMETERIZED_SQL) use this. */
    @SuppressWarnings("unchecked")
    public Map<String, Object> argumentsMap() {
        if (!(arguments instanceof Map<?, ?>)) {
            throw new IllegalStateException("Tool " + toolType + " arguments are not a Map");
        }
        return (Map<String, Object>) arguments;
    }
}
