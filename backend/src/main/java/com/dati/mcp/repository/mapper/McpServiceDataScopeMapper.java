package com.dati.mcp.repository.mapper;

import com.dati.base.MapperUtils;
import com.dati.mcp.domain.model.McpServiceDataScope;
import com.dati.mcp.repository.po.McpServiceDataScopePO;

public class McpServiceDataScopeMapper {

    public static McpServiceDataScopePO toPO(McpServiceDataScope scope) {
        McpServiceDataScopePO po = new McpServiceDataScopePO();
        MapperUtils.copyBaseInfo(scope, po);
        po.setServiceId(scope.getServiceId());
        po.setScopeType(scope.getScopeType());
        po.setReferenceId(scope.getReferenceId());

        return po;
    }

    public static McpServiceDataScope toModel(McpServiceDataScopePO po) {
        McpServiceDataScope scope = new McpServiceDataScope();
        MapperUtils.copyBaseInfo(po, scope);
        scope.setServiceId(po.getServiceId());
        scope.setScopeType(po.getScopeType());
        scope.setReferenceId(po.getReferenceId());

        return scope;
    }

}
