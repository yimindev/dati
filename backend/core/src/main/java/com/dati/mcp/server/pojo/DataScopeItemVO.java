package com.dati.mcp.server.pojo;

import com.dati.mcp.domain.model.McpDataScopeAccessMode;
import com.dati.mcp.domain.model.McpDataScopeType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class DataScopeItemVO {

    private String id;
    @NotNull
    private McpDataScopeType scopeType;
    @NotNull
    private String referenceId;
    private String referenceName;
    private McpDataScopeAccessMode accessMode;
    private List<String> allowedSchemas;

}
