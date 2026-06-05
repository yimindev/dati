package com.dati.mcp.domain.service;

import com.dati.base.exception.DatiException;
import com.dati.base.exception.ErrorCode;
import com.dati.common.template.CompiledTemplate;
import com.dati.common.template.TemplateParseException;
import com.dati.common.template.TemplateParser;
import com.dati.mcp.domain.model.McpPrompt;
import com.dati.mcp.domain.model.PromptParameter;
import com.dati.mcp.repository.dao.McpPromptDAO;
import com.dati.mcp.repository.dao.McpServiceDAO;
import com.dati.mcp.repository.mapper.McpPromptMapper;
import com.dati.mcp.repository.po.McpPromptPO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class McpPromptService {

    private final McpPromptDAO promptDAO;
    private final McpServiceDAO mcpServiceDAO;
    private final TemplateParser templateParser;

    public McpPromptService(McpPromptDAO promptDAO,
                            McpServiceDAO mcpServiceDAO,
                            TemplateParser templateParser) {
        this.promptDAO = promptDAO;
        this.mcpServiceDAO = mcpServiceDAO;
        this.templateParser = templateParser;
    }

    @Transactional
    public String createPrompt(String serviceId, McpPrompt prompt) {
        validateServiceExists(serviceId);
        if (promptDAO.existsByServiceIdAndName(serviceId, prompt.getName())) {
            throw new DatiException(ErrorCode.MS_PROMPT_NAME_EXISTS, prompt.getName());
        }
        validateContentParams(prompt);
        prompt.setServiceId(serviceId);
        McpPromptPO po = McpPromptMapper.toPO(prompt);
        return promptDAO.save(po).getId();
    }

    @Transactional
    public void updatePrompt(String serviceId, String promptId, McpPrompt prompt) {
        McpPromptPO po = promptDAO.findByServiceIdAndId(serviceId, promptId)
            .orElseThrow(() -> new DatiException(ErrorCode.MS_PROMPT_NOT_FOUND, promptId));
        if (prompt.getName() != null && !po.getName().equals(prompt.getName())
            && promptDAO.existsByServiceIdAndNameAndIdNot(serviceId, prompt.getName(), promptId)) {
            throw new DatiException(ErrorCode.MS_PROMPT_NAME_EXISTS, prompt.getName());
        }
        validateContentParams(prompt);
        McpPromptMapper.copyProperties(prompt, po);
        promptDAO.save(po);
    }

    @Transactional
    public void deletePrompt(String serviceId, String promptId) {
        McpPromptPO po = promptDAO.findByServiceIdAndId(serviceId, promptId)
            .orElseThrow(() -> new DatiException(ErrorCode.MS_PROMPT_NOT_FOUND, promptId));
        promptDAO.delete(po);
    }

    @Transactional(readOnly = true)
    public List<McpPrompt> listPrompts(String serviceId) {
        return promptDAO.findAllByServiceIdOrderByCreatedAtDesc(serviceId)
            .stream().map(McpPromptMapper::toModel).toList();
    }

    private void validateServiceExists(String serviceId) {
        if (!mcpServiceDAO.existsById(serviceId)) {
            throw new DatiException(ErrorCode.MS_SERVICE_NOT_FOUND, serviceId);
        }
    }

    /** content 模板语法校验 + 参数一致性检查 */
    private void validateContentParams(McpPrompt prompt) {
        String content = prompt.getContent();
        if (content == null) return;

        // 1. Parse template and validate syntax
        CompiledTemplate compiled;
        try {
            compiled = templateParser.parse(content);
        } catch (TemplateParseException e) {
            throw new DatiException(ErrorCode.MS_TEMPLATE_SYNTAX_ERROR, "content", e.getMessage());
        }

        // 2. Extract variable names and check consistency
        Set<String> contentVars = new HashSet<>(compiled.getVariables());
        Set<String> definedParams = extractDefinedParams(prompt.getParameters());

        // Defined but unused in content
        Set<String> unused = new HashSet<>(definedParams);
        unused.removeAll(contentVars);
        if (!unused.isEmpty()) {
            throw new DatiException(ErrorCode.MS_PROMPT_ARG_MISMATCH,
                "Unused parameter(s) not referenced in content: " + String.join(", ", unused));
        }

        // Undefined in parameters
        Set<String> undefined = new HashSet<>(contentVars);
        undefined.removeAll(definedParams);
        if (!undefined.isEmpty()) {
            throw new DatiException(ErrorCode.MS_PROMPT_ARG_MISMATCH,
                "Unknown parameter(s) in content: " + String.join(", ", undefined));
        }
    }

    private Set<String> extractDefinedParams(List<PromptParameter> parameters) {
        Set<String> names = new HashSet<>();
        if (parameters != null) {
            for (PromptParameter p : parameters) {
                if (p.getName() != null && !p.getName().isBlank()) {
                    names.add(p.getName());
                }
            }
        }
        return names;
    }
}
