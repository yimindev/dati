package com.dati.mcp.repository.mapper;

import com.dati.base.MapperUtils;
import com.dati.mcp.domain.model.McpServiceDataScope;
import com.dati.mcp.repository.po.McpServiceDataScopePO;

public class McpServiceDataScopeMapper {

    public static McpServiceDataScopePO toPO(McpServiceDataScope scope) {
        McpServiceDataScopePO po = new McpServiceDataScopePO();
        copyProperties(scope, po);
        return po;
    }

    public static void copyProperties(McpServiceDataScope source, McpServiceDataScopePO target) {
        MapperUtils.copyBaseInfo(source, target);
        target.setServiceId(source.getServiceId());
        target.setScopeType(source.getScopeType());
        target.setReferenceId(source.getReferenceId());
        target.setReferenceName(source.getReferenceName());
    }

    public static McpServiceDataScope toModel(McpServiceDataScopePO po) {
        McpServiceDataScope scope = new McpServiceDataScope();
        MapperUtils.copyBaseInfo(po, scope);
        scope.setServiceId(po.getServiceId());
        scope.setScopeType(po.getScopeType());
        scope.setReferenceId(po.getReferenceId());
        scope.setReferenceName(po.getReferenceName());
        return scope;
    }

}
