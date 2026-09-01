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
        MapperUtils.copyBaseInfo(model, po);
        if (model.getName() != null) {
            po.setName(model.getName());
        }
        if (model.getDescription() != null) {
            po.setDescription(model.getDescription());
        }
        if (model.getServiceId() != null) {
            po.setServiceId(model.getServiceId());
        }
        po.setEnabled(model.isEnabled());
        if (model.getContent() != null) {
            po.setContent(model.getContent());
        }
        if (model.getParameters() != null) {
            po.setParameters(JsonUtils.toJson(model.getParameters()));
        }
        return po;
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
