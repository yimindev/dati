package com.dati.mcp.server.controller;

import com.dati.base.pojo.BasePO;
import com.dati.base.pojo.IdResponse;
import com.dati.base.pojo.PageReq;
import com.dati.base.pojo.PageResponse;
import com.dati.mcp.domain.model.McpService;
import com.dati.mcp.domain.model.McpServiceStatus;
import com.dati.mcp.domain.service.McpServiceService;
import com.dati.mcp.server.assembler.McpServiceAssembler;
import com.dati.mcp.server.pojo.McpServiceVO;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/v1/mcp-services")
public class McpServiceController {

    private final McpServiceService mcpServiceService;
    private final McpServiceAssembler mcpServiceAssembler;

    public McpServiceController(McpServiceService mcpServiceService, McpServiceAssembler mcpServiceAssembler) {
        this.mcpServiceService = mcpServiceService;
        this.mcpServiceAssembler = mcpServiceAssembler;
    }

    @PostMapping
    public IdResponse createMcpService(@Valid @RequestBody McpService service) {
        mcpServiceAssembler.fillUsersFromRequest(service);
        return new IdResponse(mcpServiceService.createMcpService(service));
    }

    @PutMapping("/{id}")
    public IdResponse updateMcpService(@PathVariable String id, @Valid @RequestBody McpService service) {
        mcpServiceAssembler.fillUpdateUserFromRequest(service);
        mcpServiceService.updateMcpService(id, service);
        return new IdResponse(id);
    }

    @GetMapping("/{id}")
    public McpServiceVO getMcpService(@PathVariable String id) {
        McpService service = mcpServiceService.getMcpService(id);
        return mcpServiceAssembler.toMcpServiceVO(service);
    }

    @GetMapping
    public PageResponse<McpServiceVO> listMcpServices(
            PageReq pageReq,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "status", required = false) McpServiceStatus status) {
        Sort sortBy = Sort.by(Sort.Direction.DESC, BasePO.Fields.createdAt);
        Page<McpServiceVO> voPage = mcpServiceService.listMcpServices(keyword, status, pageReq.toPageRequest().withSort(sortBy))
                .map(mcpServiceAssembler::toMcpServiceVO);
        return PageResponse.of(voPage);
    }

}
