package com.dati.mcp.server.pojo;

import com.dati.mcp.domain.model.PromptParameter;
import lombok.Data;

import java.util.List;

@Data
public class McpPromptRequest {
    private String name;
    private String description;
    private Boolean enabled;
    private String content;
    private List<PromptParameter> parameters;
}
