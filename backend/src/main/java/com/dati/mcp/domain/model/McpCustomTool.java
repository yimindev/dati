package com.dati.mcp.domain.model;

import com.dati.base.pojo.BaseResource;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class McpCustomTool extends BaseResource {
    private String serviceId;
    private McpToolType toolType = McpToolType.PARAMETERIZED_SQL;
    private String title;
    private boolean enabled = true;
    private ToolConfig config;
}
