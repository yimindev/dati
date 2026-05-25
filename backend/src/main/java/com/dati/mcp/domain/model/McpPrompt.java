package com.dati.mcp.domain.model;

import com.dati.base.pojo.BaseResource;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class McpPrompt extends BaseResource {
    private String serviceId;
    private boolean enabled = true;
    private String content;
    private List<PromptParameter> parameters = new ArrayList<>();
}
