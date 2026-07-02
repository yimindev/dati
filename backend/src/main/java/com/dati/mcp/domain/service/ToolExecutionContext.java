package com.dati.mcp.domain.service;

import com.dati.mcp.domain.model.McpServiceDataScope;
import com.dati.mcp.domain.model.McpToolType;
import com.dati.mcp.domain.model.ToolConfig;

import java.util.List;
import java.util.Map;

public record ToolExecutionContext(String serviceId, McpToolType toolType,
                                   ToolConfig config, Map<String, Object> arguments,
                                   List<McpServiceDataScope> scopeItems) {}
