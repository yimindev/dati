package com.dati.mcp.repository.mapper;

import com.dati.base.MapperUtils;
import com.dati.mcp.domain.model.McpService;
import com.dati.mcp.repository.po.McpServicePO;

public class McpServiceMapper {

    public static McpServicePO toPO(McpService service) {
        McpServicePO po = new McpServicePO();
        MapperUtils.copyBaseResourceInfo(service, po);
        if (service.getCode() != null) {
            po.setCode(service.getCode());
        }
        po.setStatus(service.getStatus());
        po.setActiveVersionId(service.getActiveVersionId());
        po.setActiveVersionNumber(service.getActiveVersionNumber());
        return po;
    }

    public static McpService toModel(McpServicePO po) {
        McpService service = new McpService();
        MapperUtils.copyBaseResourceInfo(po, service);
        service.setCode(po.getCode());
        service.setStatus(po.getStatus());
        service.setActiveVersionId(po.getActiveVersionId());
        service.setActiveVersionNumber(po.getActiveVersionNumber());
        return service;
    }

}
