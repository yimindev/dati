package com.dati.mcp.repository.mapper;

import com.dati.base.MapperUtils;
import com.dati.mcp.domain.model.McpService;
import com.dati.mcp.repository.po.McpServicePO;

public class McpServiceMapper {

    public static McpServicePO toPO(McpService service) {
        McpServicePO po = new McpServicePO();
        copyProperties(service, po);
        return po;
    }

    public static void copyProperties(McpService source, McpServicePO target) {
        MapperUtils.copyBaseResourceInfo(source, target);
        target.setStatus(source.getStatus());
    }

    public static McpService toModel(McpServicePO po) {
        McpService service = new McpService();
        MapperUtils.copyBaseResourceInfo(po, service);
        service.setStatus(po.getStatus());
        return service;
    }

}
