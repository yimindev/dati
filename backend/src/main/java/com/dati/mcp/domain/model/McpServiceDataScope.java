package com.dati.mcp.domain.model;

import com.dati.base.pojo.BaseResource;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class McpServiceDataScope extends BaseResource {

    private String serviceId;
    private McpDataScopeType scopeType;
    private String referenceId;
    private String referenceName;

}
