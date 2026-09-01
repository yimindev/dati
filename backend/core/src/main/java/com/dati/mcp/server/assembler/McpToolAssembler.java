package com.dati.mcp.server.assembler;

import com.dati.common.JsonUtils;
import com.dati.mcp.domain.model.McpCustomTool;
import com.dati.mcp.domain.model.McpPrebuiltToolConfig;
import com.dati.mcp.domain.model.McpToolType;
import com.dati.mcp.domain.model.ToolConfig;
import com.dati.mcp.server.pojo.CustomToolRequest;
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

    /** 预置工具：从 Controller request 构造 Domain Model */
    public McpPrebuiltToolConfig toModel(McpToolType type, String configJson, boolean enabled) {
        McpPrebuiltToolConfig cfg = new McpPrebuiltToolConfig();
        cfg.setToolType(type);
        cfg.setEnabled(enabled);
        if (configJson != null && !configJson.isBlank()) {
            cfg.setConfig(parsePrebuiltConfig(configJson, type));
        } else {
            cfg.setConfig(type.getDefaultConfig());
        }
        return cfg;
    }

    /** 自定义工具：从 request 构造 Domain Model */
    public McpCustomTool toModel(CustomToolRequest request) {
        McpCustomTool tool = new McpCustomTool();
        if (request.getToolType() != null) {
            tool.setToolType(request.getToolType());
        }
        tool.setName(request.getName());
        tool.setTitle(request.getTitle());
        tool.setDescription(request.getDescription());
        tool.setEnabled(request.getEnabled() == null || request.getEnabled());
        if (request.getConfig() != null && !request.getConfig().isBlank()) {
            tool.setConfig(JsonUtils.fromJson(request.getConfig(), ToolConfig.ParamSqlConfig.class));
        }
        return tool;
    }

    private ToolConfig parsePrebuiltConfig(String json, McpToolType type) {
        return switch (type) {
            case SEARCH_METADATA -> JsonUtils.fromJson(json, ToolConfig.SearchMetadataConfig.class);
            case GET_TABLE_INFO  -> JsonUtils.fromJson(json, ToolConfig.GetTableInfoConfig.class);
            case EXECUTE_SQL     -> JsonUtils.fromJson(json, ToolConfig.ExecuteSqlConfig.class);
            default -> type.getDefaultConfig();
        };
    }

}
