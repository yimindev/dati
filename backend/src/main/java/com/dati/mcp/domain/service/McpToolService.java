package com.dati.mcp.domain.service;

import com.dati.base.exception.DatiException;
import com.dati.base.exception.ErrorCode;
import com.dati.common.JsonUtils;
import com.dati.common.template.CompiledTemplate;
import com.dati.common.template.TemplateParseException;
import com.dati.common.template.TemplateParser;
import com.dati.mcp.domain.model.McpCustomTool;
import com.dati.mcp.domain.model.McpPrebuiltToolConfig;
import com.dati.mcp.domain.model.ToolConfig;
import com.dati.mcp.domain.model.ToolParameter;
import com.dati.mcp.domain.model.McpToolType;
import com.dati.mcp.repository.dao.McpCustomToolDAO;
import com.dati.mcp.repository.dao.McpPrebuiltToolConfigDAO;
import com.dati.mcp.repository.dao.McpServiceDAO;
import com.dati.mcp.repository.mapper.McpCustomToolMapper;
import com.dati.mcp.repository.mapper.McpPrebuiltToolConfigMapper;
import com.dati.mcp.repository.po.McpCustomToolPO;
import com.dati.mcp.repository.po.McpPrebuiltToolConfigPO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class McpToolService {

    private static final Pattern TOOL_NAME_PATTERN = Pattern.compile("^[A-Za-z0-9_.\\-]{1,128}$");

    private final McpPrebuiltToolConfigDAO prebuiltDAO;
    private final McpCustomToolDAO customToolDAO;
    private final McpServiceDAO mcpServiceDAO;
    private final TemplateParser templateParser;

    public McpToolService(McpPrebuiltToolConfigDAO prebuiltDAO,
                          McpCustomToolDAO customToolDAO,
                          McpServiceDAO mcpServiceDAO,
                          TemplateParser templateParser) {
        this.prebuiltDAO = prebuiltDAO;
        this.customToolDAO = customToolDAO;
        this.mcpServiceDAO = mcpServiceDAO;
        this.templateParser = templateParser;
    }

    // ── 列表 ──

    /** 返回分组数据 */
    @Transactional(readOnly = true)
    public ToolsResult listTools(String serviceId) {
        return new ToolsResult(buildPrebuiltList(serviceId), buildCustomList(serviceId));
    }

    private List<McpPrebuiltToolConfig> buildPrebuiltList(String serviceId) {
        Map<McpToolType, McpPrebuiltToolConfigPO> dbMap = prebuiltDAO.findAllByServiceId(serviceId).stream()
            .collect(Collectors.toMap(McpPrebuiltToolConfigPO::getToolType, Function.identity()));

        return Arrays.stream(McpToolType.values())
            .filter(McpToolType::isPrebuilt)
            .map(type -> {
                McpPrebuiltToolConfigPO po = dbMap.get(type);
                if (po != null) {
                    return McpPrebuiltToolConfigMapper.toModel(po);
                }
                McpPrebuiltToolConfig cfg = new McpPrebuiltToolConfig();
                cfg.setServiceId(serviceId);
                cfg.setToolType(type);
                cfg.setEnabled(true);
                cfg.setConfig(type.getDefaultConfig());
                return cfg;
            })
            .toList();
    }

    private List<McpCustomTool> buildCustomList(String serviceId) {
        return customToolDAO.findAllByServiceIdOrderByCreatedAtDesc(serviceId)
            .stream()
            .map(McpCustomToolMapper::toModel)
            .toList();
    }

    // ── 预置工具 ──

    @Transactional
    public void updatePrebuiltTool(String serviceId, McpToolType toolType, McpPrebuiltToolConfig input) {
        McpPrebuiltToolConfigPO po = prebuiltDAO.findByServiceIdAndToolType(serviceId, toolType)
            .orElseGet(() -> {
                McpPrebuiltToolConfigPO newPO = new McpPrebuiltToolConfigPO();
                newPO.setServiceId(serviceId);
                newPO.setToolType(toolType);
                newPO.setEnabled(true);
                return newPO;
            });
        po.setEnabled(input.isEnabled());
        if (input.getConfig() != null) {
            po.setConfig(JsonUtils.toJson(input.getConfig()));
        }
        prebuiltDAO.save(po);
    }

    // ── 自定义工具 ──

    @Transactional
    public String createCustomTool(String serviceId, McpCustomTool tool) {
        validateServiceExists(serviceId);
        validateToolName(tool.getName());
        if (customToolDAO.existsByServiceIdAndName(serviceId, tool.getName())) {
            throw new DatiException(ErrorCode.MS_TOOL_NAME_EXISTS, tool.getName());
        }
        tool.setServiceId(serviceId);
        if (tool.getToolType() == null) {
            tool.setToolType(McpToolType.PARAMETERIZED_SQL);
        }
        validateSqlTemplate(tool);
        McpCustomToolPO po = McpCustomToolMapper.toPO(tool);
        po = customToolDAO.save(po);
        return po.getId();
    }

    @Transactional
    public void updateCustomTool(McpCustomTool tool) {
        McpCustomToolPO po = customToolDAO.findByServiceIdAndId(tool.getServiceId(), tool.getId())
            .orElseThrow(() -> new DatiException(ErrorCode.MS_TOOL_NOT_FOUND, tool.getId()));
        if (tool.getName() != null) {
            validateToolName(tool.getName());
            if (!po.getName().equals(tool.getName())
                && customToolDAO.existsByServiceIdAndNameAndIdNot(tool.getServiceId(), tool.getName(), tool.getId())) {
                throw new DatiException(ErrorCode.MS_TOOL_NAME_EXISTS, tool.getName());
            }
            po.setName(tool.getName());
        }
        if (tool.getDescription() != null) {
            po.setDescription(tool.getDescription());
        }
        if (tool.getTitle() != null) {
            po.setTitle(tool.getTitle());
        }
        if (tool.getToolType() != null) {
            po.setToolType(tool.getToolType());
        }
        po.setEnabled(tool.isEnabled());
        if (tool.getConfig() != null) {
            validateSqlTemplate(tool);
            po.setConfig(JsonUtils.toJson(tool.getConfig()));
        }
        customToolDAO.save(po);
    }

    @Transactional
    public void deleteCustomTool(String serviceId, String toolId) {
        McpCustomToolPO po = customToolDAO.findByServiceIdAndId(serviceId, toolId)
            .orElseThrow(() -> new DatiException(ErrorCode.MS_TOOL_NOT_FOUND, toolId));
        customToolDAO.delete(po);
    }

    // ── 计数 ──

    @Transactional(readOnly = true)
    public long countToolsByServiceId(String serviceId) {
        long prebuiltCount = Arrays.stream(McpToolType.values())
            .filter(McpToolType::isPrebuilt)
            .count();
        return prebuiltCount + customToolDAO.countByServiceId(serviceId);
    }

    // ── helpers ──

    private void validateServiceExists(String serviceId) {
        if (!mcpServiceDAO.existsById(serviceId)) {
            throw new DatiException(ErrorCode.MS_SERVICE_NOT_FOUND, serviceId);
        }
    }

    private void validateToolName(String name) {
        if (name == null || !TOOL_NAME_PATTERN.matcher(name).matches()) {
            throw new DatiException(ErrorCode.MS_TOOL_NAME_INVALID);
        }
    }

    private void validateSqlTemplate(McpCustomTool tool) {
        if (!(tool.getConfig() instanceof ToolConfig.ParamSqlConfig cfg)) {
            throw new DatiException(ErrorCode.INVALID_PARAMETER, "config is required for PARAMETERIZED_SQL tool");
        }
        String sqlTemplate = cfg.getSqlTemplate();
        if (sqlTemplate == null || sqlTemplate.isBlank()) {
            throw new DatiException(ErrorCode.INVALID_PARAMETER, "sql_template is required in config");
        }

        // 1. Template syntax validation
        CompiledTemplate compiled;
        try {
            compiled = templateParser.parse(sqlTemplate);
        } catch (TemplateParseException e) {
            throw new DatiException(ErrorCode.MS_TEMPLATE_SYNTAX_ERROR, "sql_template", e.getMessage());
        }

        // 2. Variable ↔ parameter consistency
        Set<String> templateVars = new HashSet<>(compiled.getVariables());
        Set<String> paramNames = cfg.getParameters().stream()
            .map(ToolParameter::getName)
            .filter(name -> name != null && !name.isBlank())
            .collect(Collectors.toSet());

        Set<String> undefinedParams = new HashSet<>(templateVars);
        undefinedParams.removeAll(paramNames);
        if (!undefinedParams.isEmpty()) {
            throw new DatiException(ErrorCode.MS_TOOL_ARG_MISMATCH,
                "Template references undefined parameter(s): " + String.join(", ", undefinedParams));
        }
    }
}
