package com.dati.mcp.server.pojo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DataScopeItemVO {

    private String id;
    @NotNull
    private String scopeType;
    private String referenceId;
    private String referenceName;

}
