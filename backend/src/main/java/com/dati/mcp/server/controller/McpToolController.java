package com.dati.mcp.server.controller;

import com.dati.base.pojo.IdResponse;
import com.dati.mcp.domain.model.McpCustomTool;
import com.dati.mcp.domain.model.McpPrebuiltToolConfig;
import com.dati.mcp.domain.service.McpToolService;
import com.dati.mcp.domain.service.ToolsResult;
import com.dati.mcp.server.assembler.McpToolAssembler;
import com.dati.mcp.server.pojo.CustomToolRequest;
import com.dati.mcp.server.pojo.ToolsResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/v1/mcp-services/{serviceId}/tools")
public class McpToolController {

    private final McpToolService mcpToolService;
    private final McpToolAssembler mcpToolAssembler;

    public McpToolController(McpToolService mcpToolService, McpToolAssembler mcpToolAssembler) {
        this.mcpToolService = mcpToolService;
        this.mcpToolAssembler = mcpToolAssembler;
    }

    @GetMapping
    public ToolsResponse listTools(@PathVariable String serviceId) {
        ToolsResult result = mcpToolService.listTools(serviceId);
        ToolsResponse resp = new ToolsResponse();
        resp.setPrebuilt(mcpToolAssembler.toPrebuiltVOList(result.prebuilt()));
        resp.setCustom(mcpToolAssembler.toCustomVOList(result.custom()));
        return resp;
    }

    @PutMapping("/{toolId}")
    public IdResponse updateTool(@PathVariable String serviceId,
                                  @PathVariable String toolId,
                                  @RequestBody @Valid CustomToolRequest request) {
        if (request.getToolType().isPrebuilt()) {
            McpPrebuiltToolConfig input = mcpToolAssembler.toModel(request.getToolType(), request.getConfig(),
                request.getEnabled() != null ? request.getEnabled() : true);
            mcpToolService.updatePrebuiltTool(serviceId, request.getToolType(), input);
        } else {
            McpCustomTool tool = mcpToolAssembler.toModel(request);
            mcpToolService.updateCustomTool(serviceId, toolId, tool);
        }
        return new IdResponse(toolId);
    }

    @PostMapping
    public IdResponse createTool(@PathVariable String serviceId,
                                  @RequestBody @Valid CustomToolRequest request) {
        McpCustomTool tool = mcpToolAssembler.toModel(request);
        return new IdResponse(mcpToolService.createCustomTool(serviceId, tool));
    }

    @DeleteMapping("/{toolId}")
    public IdResponse deleteTool(@PathVariable String serviceId,
                                  @PathVariable String toolId) {
        mcpToolService.deleteCustomTool(serviceId, toolId);
        return new IdResponse(toolId);
    }
}
