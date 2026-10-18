package com.dati.mcp.repository.mapper;

import com.dati.base.MapperUtils;
import com.dati.mcp.domain.model.McpDataScopeAccessMode;
import com.dati.mcp.domain.model.McpServiceDataScope;
import com.dati.mcp.repository.po.McpServiceDataScopePO;

import java.util.ArrayList;

public class McpServiceDataScopeMapper {

    public static McpServiceDataScopePO toPO(McpServiceDataScope scope) {
        McpServiceDataScopePO po = new McpServiceDataScopePO();
        MapperUtils.copyBaseInfo(scope, po);
        po.setServiceId(scope.getServiceId());
        po.setScopeType(scope.getScopeType());
        po.setReferenceId(scope.getReferenceId());
        po.setAccessMode(scope.getAccessMode() != null
                ? scope.getAccessMode() : McpDataScopeAccessMode.IMPORTED_TABLES);
        po.setAllowedSchemas(scope.getAllowedSchemas() != null
                ? new ArrayList<>(scope.getAllowedSchemas()) : new ArrayList<>());

        return po;
    }

    public static McpServiceDataScope toModel(McpServiceDataScopePO po) {
        McpServiceDataScope scope = new McpServiceDataScope();
        MapperUtils.copyBaseInfo(po, scope);
        scope.setServiceId(po.getServiceId());
        scope.setScopeType(po.getScopeType());
        scope.setReferenceId(po.getReferenceId());
        scope.setAccessMode(po.getAccessMode() != null
                ? po.getAccessMode() : McpDataScopeAccessMode.IMPORTED_TABLES);
        scope.setAllowedSchemas(po.getAllowedSchemas() != null
                ? new ArrayList<>(po.getAllowedSchemas()) : new ArrayList<>());

        return scope;
    }

}
