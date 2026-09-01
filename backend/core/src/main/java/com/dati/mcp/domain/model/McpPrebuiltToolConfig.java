package com.dati.mcp.domain.model;

import com.dati.base.pojo.BaseResource;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class McpPrebuiltToolConfig extends BaseResource {
    private String serviceId;
    private McpToolType toolType;
    private boolean enabled = true;
    private ToolConfig config;
}
