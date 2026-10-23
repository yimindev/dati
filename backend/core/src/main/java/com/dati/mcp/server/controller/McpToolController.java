package com.dati.mcp.server.controller;

import com.dati.base.exception.DatiException;
import com.dati.base.exception.ErrorCode;
import com.dati.base.pojo.IdResponse;
import com.dati.mcp.domain.model.DetectedAnnotations;
import com.dati.mcp.domain.model.McpCustomTool;
import com.dati.mcp.domain.model.McpToolType;
import com.dati.mcp.domain.model.ToolConfig;
import com.dati.mcp.domain.service.McpToolService;
import com.dati.mcp.domain.service.McpToolTestService;
import com.dati.mcp.domain.service.ToolsResult;
import com.dati.mcp.server.assembler.McpToolAssembler;
import com.dati.mcp.server.pojo.CreateCustomToolRequest;
import com.dati.mcp.server.pojo.UpdateToolRequest;
import com.dati.mcp.server.pojo.DetectAnnotationsRequest;
import com.dati.mcp.server.pojo.DetectAnnotationsResponse;
import com.dati.mcp.server.pojo.ToolTestRequest;
import com.dati.mcp.server.pojo.ToolTestResponse;
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
    private final McpToolTestService mcpToolTestService;

    public McpToolController(McpToolService mcpToolService, McpToolAssembler mcpToolAssembler,
                              McpToolTestService mcpToolTestService) {
        this.mcpToolService = mcpToolService;
        this.mcpToolAssembler = mcpToolAssembler;
        this.mcpToolTestService = mcpToolTestService;
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
                                  @RequestBody @Valid UpdateToolRequest request) {
        McpToolType prebuiltType = resolvePrebuiltType(toolId, request.getToolType());
        if (prebuiltType != null) {
            ToolConfig config = mcpToolAssembler.parsePrebuiltConfig(prebuiltType, request.getConfig());
            mcpToolService.updatePrebuiltTool(serviceId, prebuiltType, config, request.getEnabled());
        } else {
            McpCustomTool tool = mcpToolAssembler.toUpdateModel(request);
            tool.setId(toolId);
            tool.setServiceId(serviceId);
            mcpToolService.updateCustomTool(tool, request.getEnabled());
        }
        return new IdResponse(toolId);
    }

    /**
     * The path id identifies the target: prebuilt tools use their tool type name as id, custom
     * tools use a UUID. When tool_type is also supplied it must agree with the path id.
     */
    private McpToolType resolvePrebuiltType(String toolId, McpToolType requested) {
        McpToolType pathType = parseToolType(toolId);
        boolean pathIsPrebuilt = pathType != null && pathType.isPrebuilt();
        if (requested != null && (requested.isPrebuilt() != pathIsPrebuilt
                || (pathIsPrebuilt && requested != pathType))) {
            throw new DatiException(ErrorCode.INVALID_PARAMETER,
                "tool_type '" + requested + "' does not match tool id '" + toolId + "'");
        }
        return pathIsPrebuilt ? pathType : null;
    }

    private McpToolType parseToolType(String toolId) {
        try {
            return McpToolType.valueOf(toolId);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @PostMapping
    public IdResponse createTool(@PathVariable String serviceId,
                                  @RequestBody @Valid CreateCustomToolRequest request) {
        McpCustomTool tool = mcpToolAssembler.toModel(request);
        return new IdResponse(mcpToolService.createCustomTool(serviceId, tool));
    }

    @DeleteMapping("/{toolId}")
    public IdResponse deleteTool(@PathVariable String serviceId,
                                  @PathVariable String toolId) {
        mcpToolService.deleteCustomTool(serviceId, toolId);
        return new IdResponse(toolId);
    }

    @PostMapping("/{toolId}/test")
    public ToolTestResponse testTool(@PathVariable String serviceId,
                                      @PathVariable String toolId,
                                      @RequestBody ToolTestRequest request) {
        return mcpToolTestService.test(serviceId, toolId, request);
    }

    @PostMapping("/detect-annotations")
    public DetectAnnotationsResponse detectAnnotations(@PathVariable String serviceId,
                                                        @Valid @RequestBody DetectAnnotationsRequest request) {
        DetectedAnnotations detected = mcpToolService.detectAnnotations(request.getTemplate(), request.getParameters());
        return new DetectAnnotationsResponse(
                detected.readOnly(),
                detected.idempotent(),
                detected.destructive(),
                detected.detectedOperation()
        );
    }
}
