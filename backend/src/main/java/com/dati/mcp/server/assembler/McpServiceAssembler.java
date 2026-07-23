package com.dati.mcp.server.assembler;

import com.dati.base.BaseAssembler;
import com.dati.base.pojo.PageResponse;
import com.dati.mcp.domain.model.McpService;
import com.dati.mcp.domain.service.McpToolService;
import com.dati.mcp.server.pojo.McpServiceVO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class McpServiceAssembler extends BaseAssembler {

    private final McpToolService mcpToolService;

    public McpServiceAssembler(McpToolService mcpToolService) {
        this.mcpToolService = mcpToolService;
    }

    public McpServiceVO toMcpServiceVO(McpService service) {
        McpServiceVO vo = mapFields(service);
        super.fillUserInfo(List.of(vo));
        return vo;
    }

    public PageResponse<McpServiceVO> toPageResponse(Page<McpService> page) {
        List<McpServiceVO> vos = page.getContent().stream()
                .map(this::mapFields)
                .collect(Collectors.toList());
        super.fillUserInfo(vos);
        return PageResponse.of(new PageImpl<>(vos, page.getPageable(), page.getTotalElements()));
    }

    private McpServiceVO mapFields(McpService service) {
        McpServiceVO vo = new McpServiceVO();
        super.copyBaseInfo(service, vo);
        vo.setCode(service.getCode());
        vo.setStatus(service.getStatus() != null ? service.getStatus().name() : null);
        vo.setEndpointPath("/" + service.getCode() + "/mcp");
        vo.setToolCount((int) mcpToolService.countToolsByServiceId(service.getId()));
        return vo;
    }

}
