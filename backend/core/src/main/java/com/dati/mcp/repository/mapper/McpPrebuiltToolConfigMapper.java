package com.dati.mcp.repository.mapper;

import com.dati.base.MapperUtils;
import com.dati.common.JsonUtils;
import com.dati.mcp.domain.model.McpPrebuiltToolConfig;
import com.dati.mcp.domain.model.McpToolType;
import com.dati.mcp.domain.model.ToolConfig;
import com.dati.mcp.repository.po.McpPrebuiltToolConfigPO;

public class McpPrebuiltToolConfigMapper {

    public static McpPrebuiltToolConfigPO toPO(McpPrebuiltToolConfig model) {
        McpPrebuiltToolConfigPO po = new McpPrebuiltToolConfigPO();
        MapperUtils.copyBaseInfo(model, po);
        po.setServiceId(model.getServiceId());
        po.setToolType(model.getToolType());
        po.setEnabled(model.isEnabled());
        if (model.getConfig() != null) {
            po.setConfig(JsonUtils.toJson(model.getConfig()));
        }
        return po;
    }

    public static McpPrebuiltToolConfig toModel(McpPrebuiltToolConfigPO po) {
        McpPrebuiltToolConfig model = new McpPrebuiltToolConfig();
        MapperUtils.copyBaseInfo(po, model);
        model.setServiceId(po.getServiceId());
        model.setToolType(po.getToolType());
        model.setEnabled(po.getEnabled());
        model.setConfig(parseConfig(po.getConfig(), po.getToolType()));
        return model;
    }

    private static ToolConfig parseConfig(String json, McpToolType type) {
        if (json == null || json.isBlank()) {
            return type.getDefaultConfig();
        }
        return switch (type) {
            case SEARCH_METADATA -> JsonUtils.fromJson(json, ToolConfig.SearchMetadataConfig.class);
            case GET_TABLE_INFO  -> JsonUtils.fromJson(json, ToolConfig.GetTableInfoConfig.class);
            case EXECUTE_SQL     -> JsonUtils.fromJson(json, ToolConfig.ExecuteSqlConfig.class);
            default -> type.getDefaultConfig();
        };
    }
}
