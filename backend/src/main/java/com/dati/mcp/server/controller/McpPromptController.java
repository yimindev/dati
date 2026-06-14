package com.dati.mcp.server.controller;

import com.dati.base.pojo.IdResponse;
import com.dati.mcp.domain.model.McpPrompt;
import com.dati.mcp.domain.service.McpPromptService;
import com.dati.mcp.server.assembler.McpPromptAssembler;
import com.dati.mcp.server.pojo.McpPromptRequest;
import com.dati.mcp.server.pojo.McpPromptVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/v1/mcp-services/{serviceId}/prompts")
public class McpPromptController {

    private final McpPromptService promptService;
    private final McpPromptAssembler promptAssembler;

    public McpPromptController(McpPromptService promptService, McpPromptAssembler promptAssembler) {
        this.promptService = promptService;
        this.promptAssembler = promptAssembler;
    }

    @GetMapping
    public List<McpPromptVO> listPrompts(@PathVariable String serviceId) {
        return promptAssembler.toVOList(promptService.listPrompts(serviceId));
    }

    @PostMapping
    public IdResponse createPrompt(@PathVariable String serviceId, @RequestBody McpPromptRequest request) {
        McpPrompt prompt = promptAssembler.toModel(request);
        return new IdResponse(promptService.createPrompt(serviceId, prompt));
    }

    @PutMapping("/{promptId}")
    public IdResponse updatePrompt(@PathVariable String serviceId, @PathVariable String promptId,
                                    @RequestBody McpPromptRequest request) {
        McpPrompt prompt = promptAssembler.toModel(request);
        prompt.setId(promptId);
        prompt.setServiceId(serviceId);
        promptService.updatePrompt(prompt);
        return new IdResponse(promptId);
    }

    @DeleteMapping("/{promptId}")
    public IdResponse deletePrompt(@PathVariable String serviceId, @PathVariable String promptId) {
        promptService.deletePrompt(serviceId, promptId);
        return new IdResponse(promptId);
    }
}
