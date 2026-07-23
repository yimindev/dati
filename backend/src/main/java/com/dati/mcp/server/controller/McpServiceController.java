package com.dati.mcp.server.controller;

import com.dati.base.pojo.BasePO;
import com.dati.base.pojo.IdResponse;
import com.dati.base.pojo.PageReq;
import com.dati.base.pojo.PageResponse;
import com.dati.mcp.domain.model.McpDataScopeType;
import com.dati.mcp.domain.model.McpService;
import com.dati.mcp.domain.model.McpServiceDataScope;
import com.dati.mcp.domain.model.McpServiceStatus;
import com.dati.mcp.domain.service.McpServiceDataScopeService;
import com.dati.mcp.domain.service.McpServiceService;
import com.dati.mcp.server.assembler.McpDataScopeAssembler;
import com.dati.mcp.server.assembler.McpServiceAssembler;
import com.dati.mcp.server.pojo.DataScopeRequest;
import com.dati.mcp.server.pojo.DataScopeResponse;
import com.dati.mcp.server.pojo.McpServiceVO;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import java.util.List;
import java.util.Set;

import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/v1/mcp-services")
public class McpServiceController {

    private final McpServiceService mcpServiceService;
    private final McpServiceAssembler mcpServiceAssembler;
    private final McpServiceDataScopeService dataScopeService;
    private final McpDataScopeAssembler dataScopeAssembler;

    public McpServiceController(McpServiceService mcpServiceService,
                                McpServiceAssembler mcpServiceAssembler,
                                McpServiceDataScopeService dataScopeService,
                                McpDataScopeAssembler dataScopeAssembler) {
        this.mcpServiceService = mcpServiceService;
        this.mcpServiceAssembler = mcpServiceAssembler;
        this.dataScopeService = dataScopeService;
        this.dataScopeAssembler = dataScopeAssembler;
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
        return mcpServiceAssembler.toPageResponse(
                mcpServiceService.listMcpServices(keyword, status, pageReq.toPageRequest().withSort(sortBy)));
    }

    @GetMapping("/{id}/data-scope")
    public DataScopeResponse getDataScope(@PathVariable String id) {
        List<McpServiceDataScope> scopes = dataScopeService.getDataScope(id);
        Set<String> resolvedIds = dataScopeService.getResolvedDataSourceIds(id);
        return dataScopeAssembler.toDataScopeResponse(scopes, resolvedIds);
    }

    @PutMapping("/{id}/data-scope")
    public IdResponse saveDataScope(@PathVariable String id,
                                     @RequestBody DataScopeRequest request) {
        List<McpServiceDataScope> scopes = request.getItems().stream().map(item -> {
            McpServiceDataScope scope = new McpServiceDataScope();
            scope.setServiceId(id);
            scope.setScopeType(McpDataScopeType.valueOf(item.getScopeType()));
            scope.setReferenceId(item.getReferenceId());
            return scope;
        }).toList();
        dataScopeService.saveDataScope(id, scopes);
        return new IdResponse(id);
    }

}
