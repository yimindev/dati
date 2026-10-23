package com.dati.mcp.server.assembler;

import com.dati.common.JsonUtils;
import com.dati.mcp.domain.model.McpCustomTool;
import com.dati.mcp.domain.model.McpPrebuiltToolConfig;
import com.dati.mcp.domain.model.McpToolType;
import com.dati.mcp.domain.model.ToolConfig;
import com.dati.mcp.server.pojo.CreateCustomToolRequest;
import com.dati.mcp.server.pojo.UpdateToolRequest;
import com.dati.mcp.server.pojo.McpToolVO;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class McpToolAssembler {

    public McpToolVO toVO(McpPrebuiltToolConfig cfg) {
        McpToolType type = cfg.getToolType();
        McpToolVO vo = new McpToolVO();
        vo.setId(type.name());
        vo.setToolType(type);
        vo.setName(type.getToolName());
        vo.setTitle(type.getTitle());
        vo.setDescription(type.getDescription());
        vo.setEnabled(cfg.isEnabled());
        vo.setConfig(cfg.getConfig());
        return vo;
    }

    public McpToolVO toVO(McpCustomTool tool) {
        McpToolType type = tool.getToolType();
        McpToolVO vo = new McpToolVO();
        vo.setId(tool.getId());
        vo.setToolType(type != null ? type : McpToolType.PARAMETERIZED_SQL);
        vo.setName(tool.getName());
        vo.setTitle(tool.getTitle());
        vo.setDescription(tool.getDescription());
        vo.setEnabled(tool.isEnabled());
        vo.setConfig(tool.getConfig());
        return vo;
    }

    public List<McpToolVO> toPrebuiltVOList(List<McpPrebuiltToolConfig> configs) {
        return configs.stream().map(this::toVO).toList();
    }

    public List<McpToolVO> toCustomVOList(List<McpCustomTool> tools) {
        return tools.stream().map(this::toVO).toList();
    }

    /** Prebuilt tool config from request: null/blank means "keep the stored config". */
    public ToolConfig parsePrebuiltConfig(McpToolType type, String configJson) {
        if (configJson == null || configJson.isBlank()) {
            return null;
        }
        return switch (type) {
            case SEARCH_METADATA -> JsonUtils.fromJson(configJson, ToolConfig.SearchMetadataConfig.class);
            case GET_TABLE_INFO  -> JsonUtils.fromJson(configJson, ToolConfig.GetTableInfoConfig.class);
            case EXECUTE_SQL     -> JsonUtils.fromJson(configJson, ToolConfig.ExecuteSqlConfig.class);
            default -> type.getDefaultConfig();
        };
    }

    /** Custom tool create: omitted optional fields fall back to defaults. */
    public McpCustomTool toModel(CreateCustomToolRequest request) {
        McpCustomTool tool = buildCustomTool(request.getToolType(), request.getName(), request.getTitle(),
            request.getDescription(), request.getConfig());
        if (tool.getToolType() == null) {
            tool.setToolType(McpToolType.PARAMETERIZED_SQL);
        }
        tool.setEnabled(request.getEnabled() == null || request.getEnabled());
        return tool;
    }

    /**
     * Custom tool partial update: omitted fields stay null so the service keeps the stored
     * values. {@code enabled} is applied from the request by the service, not from the model.
     */
    public McpCustomTool toUpdateModel(UpdateToolRequest request) {
        return buildCustomTool(request.getToolType(), request.getName(), request.getTitle(),
            request.getDescription(), request.getConfig());
    }

    private McpCustomTool buildCustomTool(McpToolType toolType, String name, String title,
                                          String description, String config) {
        McpCustomTool tool = new McpCustomTool();
        tool.setToolType(toolType); // explicit null = keep the stored type
        tool.setName(name);
        tool.setTitle(title);
        tool.setDescription(description);
        if (config != null && !config.isBlank()) {
            tool.setConfig(JsonUtils.fromJson(config, ToolConfig.ParamSqlConfig.class));
        }
        return tool;
    }
}
