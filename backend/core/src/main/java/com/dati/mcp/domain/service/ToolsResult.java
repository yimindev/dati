package com.dati.mcp.domain.service;

import com.dati.mcp.domain.model.McpCustomTool;
import com.dati.mcp.domain.model.McpPrebuiltToolConfig;

import java.util.List;

public record ToolsResult(List<McpPrebuiltToolConfig> prebuilt, List<McpCustomTool> custom) {
}
