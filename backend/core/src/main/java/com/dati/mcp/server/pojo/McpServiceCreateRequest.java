package com.dati.mcp.server.pojo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class McpServiceCreateRequest {

    @NotBlank
    private String code;

    @NotBlank
    private String name;

    private String description;

    @NotEmpty
    @Valid
    private List<DataScopeItemVO> dataScopes;

}
