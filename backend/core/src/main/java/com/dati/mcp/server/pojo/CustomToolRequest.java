package com.dati.mcp.server.pojo;

import com.dati.mcp.domain.model.McpToolType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CustomToolRequest {
    @NotNull
    private McpToolType toolType;
    private String name;
    private String title;
    private String description;
    private Boolean enabled;
    private String config;   // JSON of ToolConfig
}
