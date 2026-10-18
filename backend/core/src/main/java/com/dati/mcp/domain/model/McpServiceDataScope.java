package com.dati.mcp.domain.model;

import com.dati.base.pojo.BaseResource;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class McpServiceDataScope extends BaseResource {

    private String serviceId;
    private McpDataScopeType scopeType;
    private String referenceId;
    private McpDataScopeAccessMode accessMode = McpDataScopeAccessMode.IMPORTED_TABLES;
    private List<String> allowedSchemas = new ArrayList<>();

}
