package com.dati.mcp.server.pojo;

import com.dati.mcp.domain.model.McpToolType;
import lombok.Data;

/**
 * Update request for tools (both prebuilt and custom). Every field is optional: omitted
 * fields keep their stored value, so partial updates such as {"enabled": false} work.
 * {@link McpToolType} is only needed to disambiguate the request body from the path id.
 */
@Data
public class UpdateToolRequest {
    private McpToolType toolType;
    private String name;
    private String title;
    private String description;
    private Boolean enabled;
    private String config;   // JSON of ToolConfig
}
