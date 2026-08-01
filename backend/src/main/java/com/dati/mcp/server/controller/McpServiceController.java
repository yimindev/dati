package com.dati.mcp.server.controller;

import com.dati.base.pojo.BasePO;
import com.dati.base.pojo.IdResponse;
import com.dati.base.pojo.PageReq;
import com.dati.base.pojo.PageResponse;
import com.dati.mcp.domain.model.McpService;
import com.dati.mcp.domain.model.McpServiceDataScope;
import com.dati.mcp.domain.model.McpServiceSnapshot;
import com.dati.mcp.domain.model.McpServiceStatus;
import com.dati.mcp.domain.service.McpServiceDataScopeService;
import com.dati.mcp.domain.service.McpServicePublishService;
import com.dati.mcp.domain.service.McpServiceService;
import com.dati.mcp.server.assembler.McpDataScopeAssembler;
import com.dati.mcp.server.assembler.McpServiceAssembler;
import com.dati.mcp.server.pojo.DataScopeRequest;
import com.dati.mcp.server.pojo.DataScopeResponse;
import com.dati.mcp.server.pojo.McpServiceCreateRequest;
import com.dati.mcp.server.pojo.McpServiceDiffVO;
import com.dati.mcp.server.pojo.McpServiceSnapshotVO;
import com.dati.mcp.server.pojo.McpServiceVO;
import com.dati.mcp.server.pojo.PublishRequest;
import com.dati.mcp.server.pojo.RollbackRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

@Slf4j
@RestController
@RequestMapping("/v1/mcp-services")
public class McpServiceController {

    private final McpServiceService mcpServiceService;
    private final McpServiceAssembler mcpServiceAssembler;
    private final McpServiceDataScopeService dataScopeService;
    private final McpDataScopeAssembler dataScopeAssembler;
    private final McpServicePublishService publishService;

    public McpServiceController(McpServiceService mcpServiceService,
                                McpServiceAssembler mcpServiceAssembler,
                                McpServiceDataScopeService dataScopeService,
                                McpDataScopeAssembler dataScopeAssembler,
                                McpServicePublishService publishService) {
        this.mcpServiceService = mcpServiceService;
        this.mcpServiceAssembler = mcpServiceAssembler;
        this.dataScopeService = dataScopeService;
        this.dataScopeAssembler = dataScopeAssembler;
        this.publishService = publishService;
    }

    @PostMapping
    public IdResponse createMcpService(@Valid @RequestBody McpServiceCreateRequest request) {
        McpService service = new McpService();
        service.setCode(request.getCode());
        service.setName(request.getName());
        service.setDescription(request.getDescription());
        List<McpServiceDataScope> scopes = request.getDataScopes().stream().map(item -> {
            McpServiceDataScope scope = new McpServiceDataScope();
            scope.setScopeType(item.getScopeType());
            scope.setReferenceId(item.getReferenceId());
            return scope;
        }).toList();
        mcpServiceAssembler.fillUsersFromRequest(service);
        return new IdResponse(mcpServiceService.createMcpService(service, scopes));
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
            @Valid PageReq pageReq,
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
                                     @Valid @RequestBody DataScopeRequest request) {
        List<McpServiceDataScope> scopes = request.getItems().stream().map(item -> {
            McpServiceDataScope scope = new McpServiceDataScope();
            scope.setServiceId(id);
            scope.setScopeType(item.getScopeType());
            scope.setReferenceId(item.getReferenceId());
            return scope;
        }).toList();
        dataScopeService.saveDataScope(id, scopes);
        return new IdResponse(id);
    }

    @PostMapping("/{id}/publish")
    public IdResponse publishService(@PathVariable String id,
                                      @RequestBody(required = false) PublishRequest request) {
        String note = request != null ? request.getReleaseNote() : null;
        McpServiceSnapshot snapshot = publishService.publish(id, note);
        return new IdResponse(snapshot.getId());
    }

    @PostMapping("/{id}/disable")
    public IdResponse disableService(@PathVariable String id) {
        publishService.disable(id);
        return new IdResponse(id);
    }

    @PostMapping("/{id}/enable")
    public IdResponse enableService(@PathVariable String id) {
        publishService.enable(id);
        return new IdResponse(id);
    }

    @GetMapping("/{id}/diff")
    public McpServiceDiffVO getServiceDiff(@PathVariable String id) {
        return publishService.getDiff(id);
    }

    @GetMapping("/{id}/snapshots")
    public List<McpServiceSnapshotVO> getSnapshots(@PathVariable String id) {
        return publishService.getSnapshots(id).stream()
                .map(mcpServiceAssembler::toSnapshotVO)
                .toList();
    }

    @PostMapping("/{id}/rollback")
    public IdResponse rollbackService(@PathVariable String id,
                                       @Valid @RequestBody RollbackRequest request) {
        McpServiceSnapshot snapshot = publishService.rollback(id, request.getTargetVersionNumber(), request.getReleaseNote());
        return new IdResponse(snapshot.getId());
    }

}
