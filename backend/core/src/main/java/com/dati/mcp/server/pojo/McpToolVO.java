package com.dati.mcp.server.pojo;

import com.dati.mcp.domain.model.McpToolType;
import com.dati.mcp.domain.model.ToolConfig;
import lombok.Data;

@Data
public class McpToolVO {
    private String id;
    private McpToolType toolType;
    private String name;
    private String title;
    private String description;
    private boolean enabled;
    private ToolConfig config;
}
