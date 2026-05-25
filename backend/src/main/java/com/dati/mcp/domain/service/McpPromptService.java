package com.dati.mcp.domain.service;

import com.dati.base.exception.DatiException;
import com.dati.base.exception.ErrorCode;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class McpPromptService {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{(\\w+)}}");

    private final McpPromptDAO promptDAO;
    private final McpServiceDAO mcpServiceDAO;

    public McpPromptService(McpPromptDAO promptDAO, McpServiceDAO mcpServiceDAO) {
        this.promptDAO = promptDAO;
        this.mcpServiceDAO = mcpServiceDAO;
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

    /** content 中所有 {{xxx}} 必须在 parameters 中已定义 */
    private void validateContentParams(McpPrompt prompt) {
        Set<String> contentParams = extractPlaceholders(prompt.getContent());
        Set<String> definedParams = extractDefinedParams(prompt.getParameters());

        Set<String> undefined = new HashSet<>(contentParams);
        undefined.removeAll(definedParams);
        if (!undefined.isEmpty()) {
            throw new DatiException(ErrorCode.MS_PROMPT_ARG_MISMATCH,
                "Unknown parameter(s) in content: " + String.join(", ", undefined));
        }
    }

    private Set<String> extractPlaceholders(String content) {
        Set<String> params = new HashSet<>();
        if (content == null) {
            return params;
        }
        Matcher m = PLACEHOLDER_PATTERN.matcher(content);
        while (m.find()) {
            params.add(m.group(1));
        }
        return params;
    }

    private Set<String> extractDefinedParams(List<PromptParameter> parameters) {
        Set<String> names = new HashSet<>();
        for (PromptParameter p : parameters) {
            if (p.getName() != null && !p.getName().isBlank()) {
                names.add(p.getName());
            }
        }
        return names;
    }
}
