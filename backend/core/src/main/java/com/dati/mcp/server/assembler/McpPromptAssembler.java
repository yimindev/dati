package com.dati.mcp.server.assembler;

import com.dati.mcp.domain.model.McpPrompt;
import com.dati.mcp.server.pojo.McpPromptRequest;
import com.dati.mcp.server.pojo.McpPromptVO;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class McpPromptAssembler {

    public McpPromptVO toVO(McpPrompt model) {
        McpPromptVO vo = new McpPromptVO();
        vo.setId(model.getId());
        vo.setServiceId(model.getServiceId());
        vo.setName(model.getName());
        vo.setDescription(model.getDescription());
        vo.setEnabled(model.isEnabled());
        vo.setContent(model.getContent());
        vo.setParameters(model.getParameters());
        return vo;
    }

    public List<McpPromptVO> toVOList(List<McpPrompt> models) {
        return models.stream().map(this::toVO).toList();
    }

    public McpPrompt toModel(McpPromptRequest request) {
        McpPrompt prompt = new McpPrompt();
        prompt.setName(request.getName());
        prompt.setDescription(request.getDescription());
        prompt.setEnabled(request.getEnabled() == null || request.getEnabled());
        prompt.setContent(request.getContent());
        prompt.setParameters(request.getParameters());
        return prompt;
    }
}
