package com.dati.mcp.server.pojo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TemplateExtractRequest {
    @NotBlank
    private String template;
}
