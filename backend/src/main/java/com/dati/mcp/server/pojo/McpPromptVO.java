package com.dati.mcp.server.pojo;

import com.dati.mcp.domain.model.PromptParameter;
import lombok.Data;

import java.util.List;

@Data
public class McpPromptVO {
    private String id;
    private String serviceId;
    private String name;
    private String description;
    private boolean enabled;
    private String content;
    private List<PromptParameter> parameters;
}
