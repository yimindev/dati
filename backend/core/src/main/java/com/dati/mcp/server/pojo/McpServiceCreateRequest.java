package com.dati.mcp.server.pojo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class McpServiceCreateRequest {

    @NotBlank
    @Size(max = 64)
    private String code;

    @NotBlank
    @Size(max = 64)
    private String name;

    @Size(max = 1024)
    private String description;

    @NotEmpty
    @Valid
    private List<DataScopeItemVO> dataScopes;

}
