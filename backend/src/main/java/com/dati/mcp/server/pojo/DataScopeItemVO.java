package com.dati.mcp.server.pojo;

import com.dati.mcp.domain.model.McpDataScopeType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DataScopeItemVO {

    private String id;
    @NotNull
    private McpDataScopeType scopeType;
    @NotNull
    private String referenceId;
    private String referenceName;

}
