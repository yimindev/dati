package com.dati.mcp.server.pojo;

import com.dati.mcp.domain.model.ToolParameter;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class DetectAnnotationsRequest {
    @NotBlank
    private String template;
    private List<ToolParameter> parameters;
}
