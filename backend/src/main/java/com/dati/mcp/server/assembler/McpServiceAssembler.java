package com.dati.mcp.server.assembler;

import com.dati.base.BaseAssembler;
import com.dati.mcp.domain.model.McpService;
import com.dati.mcp.server.pojo.McpServiceVO;
import org.springframework.stereotype.Component;

@Component
public class McpServiceAssembler extends BaseAssembler {

    public McpServiceVO toMcpServiceVO(McpService service) {
        McpServiceVO vo = new McpServiceVO();
        super.copyBaseInfo(service, vo);
        vo.setStatus(service.getStatus() != null ? service.getStatus().name() : null);
        vo.setEndpointPath("/" + service.getId() + "/mcp");
        vo.setToolCount(0); // TODO: update when Tool association is implemented
        return vo;
    }

}
