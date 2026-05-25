package com.dati.mcp.repository.mapper;

import com.dati.base.MapperUtils;
import com.dati.common.JsonUtils;
import com.dati.mcp.domain.model.McpPrompt;
import com.dati.mcp.domain.model.PromptParameter;
import com.dati.mcp.repository.po.McpPromptPO;

import java.util.List;

public class McpPromptMapper {

    public static McpPromptPO toPO(McpPrompt model) {
        McpPromptPO po = new McpPromptPO();
        copyProperties(model, po);
        return po;
    }

    public static void copyProperties(McpPrompt source, McpPromptPO target) {
        MapperUtils.copyBaseInfo(source, target);
        target.setName(source.getName());
        target.setDescription(source.getDescription());
        target.setServiceId(source.getServiceId());
        target.setEnabled(source.isEnabled());
        target.setContent(source.getContent());
        target.setParameters(JsonUtils.toJson(source.getParameters()));
    }

    public static McpPrompt toModel(McpPromptPO po) {
        McpPrompt model = new McpPrompt();
        MapperUtils.copyBaseInfo(po, model);
        model.setName(po.getName());
        model.setDescription(po.getDescription());
        model.setServiceId(po.getServiceId());
        model.setEnabled(po.getEnabled());
        model.setContent(po.getContent());
        model.setParameters(parseParameters(po.getParameters()));
        return model;
    }

    private static List<PromptParameter> parseParameters(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return JsonUtils.toList(json, PromptParameter.class);
        } catch (Exception e) {
            return List.of();
        }
    }
}
