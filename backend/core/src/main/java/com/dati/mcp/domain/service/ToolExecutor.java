package com.dati.mcp.domain.service;

import com.dati.mcp.domain.model.McpToolType;
import com.dati.mcp.server.pojo.ToolTestData;

public interface ToolExecutor {

    McpToolType getToolType();

    ToolTestData execute(ToolExecutionContext ctx);
}
