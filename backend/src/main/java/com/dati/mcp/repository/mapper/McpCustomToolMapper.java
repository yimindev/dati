package com.dati.mcp.repository.mapper;

import com.dati.base.MapperUtils;
import com.dati.common.JsonUtils;
import com.dati.mcp.domain.model.McpCustomTool;
import com.dati.mcp.domain.model.McpToolType;
import com.dati.mcp.domain.model.ToolConfig;
import com.dati.mcp.repository.po.McpCustomToolPO;

public class McpCustomToolMapper {

    public static McpCustomToolPO toPO(McpCustomTool model) {
        McpCustomToolPO po = new McpCustomToolPO();
        MapperUtils.copyBaseInfo(model, po);
        if (model.getServiceId() != null) {
            po.setServiceId(model.getServiceId());
        }
        if (model.getName() != null) {
            po.setName(model.getName());
        }
        if (model.getDescription() != null) {
            po.setDescription(model.getDescription());
        }
        if (model.getToolType() != null) {
            po.setToolType(model.getToolType());
        }
        if (model.getTitle() != null) {
            po.setTitle(model.getTitle());
        }
        po.setEnabled(model.isEnabled());
        if (model.getConfig() != null) {
            po.setConfig(JsonUtils.toJson(model.getConfig()));
        }
        return po;
    }

    public static McpCustomTool toModel(McpCustomToolPO po) {
        McpCustomTool model = new McpCustomTool();
        MapperUtils.copyBaseInfo(po, model);
        model.setName(po.getName());
        model.setDescription(po.getDescription());
        model.setServiceId(po.getServiceId());
        model.setToolType(po.getToolType());
        model.setTitle(po.getTitle());
        model.setEnabled(po.getEnabled());
        model.setConfig(parseConfig(po.getConfig(), po.getToolType()));
        return model;
    }

    private static ToolConfig parseConfig(String json, McpToolType type) {
        if (json == null || json.isBlank()) {
            return type.getDefaultConfig();
        }
        return JsonUtils.fromJson(json, ToolConfig.ParamSqlConfig.class);
    }
}
