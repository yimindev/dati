package com.dati.mcp.server.pojo;

import com.dati.mcp.domain.model.McpToolType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Create-only request for custom tools. Unlike {@link UpdateToolRequest} (partial updates),
 * creation requires tool_type and a description: PRD US-03 mandates description on creation
 * ("必填，帮助 LLM 理解用途"), while partial updates (e.g. the enabled toggle) must be allowed
 * without them.
 */
@Data
public class CreateCustomToolRequest {
    @NotNull
    private McpToolType toolType;
    private String name;
    private String title;
    @NotBlank(message = "description is required")
    private String description;
    private Boolean enabled;
    private String config;   // JSON of ToolConfig
}
