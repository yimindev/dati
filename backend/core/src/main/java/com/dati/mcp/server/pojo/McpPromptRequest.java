package com.dati.mcp.server.pojo;

import com.dati.mcp.domain.model.PromptParameter;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class McpPromptRequest {
    @Size(max = 128)
    @Pattern(regexp = "(?s).*\\S.*", message = "must not be blank")
    private String name;
    private String description;
    private Boolean enabled;
    private String content;
    private List<PromptParameter> parameters;
}
