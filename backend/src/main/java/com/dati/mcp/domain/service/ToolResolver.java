package com.dati.mcp.domain.service;

import com.dati.mcp.domain.model.McpCustomTool;
import com.dati.mcp.domain.model.McpPrebuiltToolConfig;
import com.dati.mcp.domain.model.McpToolType;
import com.dati.mcp.domain.model.ToolConfig;
import com.dati.mcp.domain.model.ToolError;
import com.dati.mcp.repository.dao.McpCustomToolDAO;
import com.dati.mcp.repository.dao.McpPrebuiltToolConfigDAO;
import com.dati.mcp.repository.mapper.McpCustomToolMapper;
import com.dati.mcp.repository.mapper.McpPrebuiltToolConfigMapper;
import org.springframework.stereotype.Service;

@Service
public class ToolResolver {

    private final McpPrebuiltToolConfigDAO prebuiltDAO;
    private final McpCustomToolDAO customToolDAO;

    public ToolResolver(McpPrebuiltToolConfigDAO prebuiltDAO, McpCustomToolDAO customToolDAO) {
        this.prebuiltDAO = prebuiltDAO;
        this.customToolDAO = customToolDAO;
    }

    public ResolvedTool resolve(String serviceId, String toolId) {
        McpToolType toolType = parseToolType(toolId);
        if (toolType != null && toolType.isPrebuilt()) {
            return resolvePrebuilt(serviceId, toolId, toolType);
        }
        return resolveCustom(serviceId, toolId);
    }

    private McpToolType parseToolType(String toolId) {
        try {
            return McpToolType.valueOf(toolId);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private ResolvedTool resolvePrebuilt(String serviceId, String toolId, McpToolType toolType) {
        McpPrebuiltToolConfig cfg = prebuiltDAO.findByServiceIdAndToolType(serviceId, toolType)
            .map(McpPrebuiltToolConfigMapper::toModel)
            .orElseGet(() -> createDefaultConfig(serviceId, toolType));
        if (!cfg.isEnabled()) {
            throw new ToolExecuteException(ToolError.TOOL_DISABLED, toolId);
        }
        return new ResolvedTool(toolType, true, cfg.getConfig(), true);
    }

    private ResolvedTool resolveCustom(String serviceId, String toolId) {
        McpCustomTool custom = customToolDAO.findByServiceIdAndId(serviceId, toolId)
            .map(McpCustomToolMapper::toModel)
            .orElseThrow(() -> new ToolExecuteException(ToolError.TOOL_NOT_FOUND, toolId));
        if (!custom.isEnabled()) {
            throw new ToolExecuteException(ToolError.TOOL_DISABLED, toolId);
        }
        return new ResolvedTool(custom.getToolType(), true, custom.getConfig(), false);
    }

    private McpPrebuiltToolConfig createDefaultConfig(String serviceId, McpToolType toolType) {
        McpPrebuiltToolConfig cfg = new McpPrebuiltToolConfig();
        cfg.setServiceId(serviceId);
        cfg.setToolType(toolType);
        cfg.setEnabled(toolType.isDefaultEnabled());
        cfg.setConfig(toolType.getDefaultConfig());
        return cfg;
    }

    public record ResolvedTool(McpToolType toolType, boolean enabled,
                                ToolConfig config, boolean isPrebuilt) {}
}
