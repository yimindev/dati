package com.dati.mcp.server.pojo;

import com.dati.mcp.domain.model.TemplateRenderMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.Map;

@Data
public class TemplatePreviewRequest {
    @NotNull
    private TemplateRenderMode mode;

    @NotBlank
    private String template;

    @NotNull
    private Map<String, Object> values;
}
