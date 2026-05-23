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
        copyProperties(model, po);
        return po;
    }

    public static void copyProperties(McpCustomTool source, McpCustomToolPO target) {
        MapperUtils.copyBaseInfo(source, target);
        target.setName(source.getName());
        target.setDescription(source.getDescription());
        target.setServiceId(source.getServiceId());
        target.setToolType(source.getToolType());
        target.setTitle(source.getTitle());
        target.setEnabled(source.isEnabled());
        if (source.getConfig() != null) {
            target.setConfig(JsonUtils.toJson(source.getConfig()));
        }
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
