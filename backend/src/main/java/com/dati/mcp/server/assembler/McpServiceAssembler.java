package com.dati.mcp.server.assembler;

import com.dati.base.BaseAssembler;
import com.dati.mcp.domain.model.McpService;
import com.dati.mcp.domain.service.McpToolService;
import com.dati.mcp.server.pojo.McpServiceVO;
import org.springframework.stereotype.Component;

@Component
public class McpServiceAssembler extends BaseAssembler {

    private final McpToolService mcpToolService;

    public McpServiceAssembler(McpToolService mcpToolService) {
        this.mcpToolService = mcpToolService;
    }

    public McpServiceVO toMcpServiceVO(McpService service) {
        McpServiceVO vo = new McpServiceVO();
        super.copyBaseInfo(service, vo);
        vo.setStatus(service.getStatus() != null ? service.getStatus().name() : null);
        vo.setEndpointPath("/" + service.getId() + "/mcp");
        vo.setToolCount((int) mcpToolService.countToolsByServiceId(service.getId()));
        return vo;
    }

}
