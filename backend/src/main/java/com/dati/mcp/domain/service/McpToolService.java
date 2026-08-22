package com.dati.mcp.domain.service;

import com.dati.base.exception.DatiException;
import com.dati.base.exception.ErrorCode;
import com.dati.common.JsonUtils;
import com.dati.common.template.CompiledTemplate;
import com.dati.common.template.PreparedSql;
import com.dati.common.template.SqlRenderer;
import com.dati.common.template.TemplateParseException;
import com.dati.common.template.TemplateParser;
import com.dati.db.analysis.SqlAnalysisResult;
import com.dati.db.analysis.SqlAnalyzer;
import com.dati.db.analysis.SqlOperationType;
import com.dati.mcp.domain.model.DetectedAnnotations;
import com.dati.mcp.domain.model.McpCustomTool;
import com.dati.mcp.domain.model.McpPrebuiltToolConfig;
import com.dati.mcp.domain.model.McpToolType;
import com.dati.mcp.domain.model.ToolConfig;
import com.dati.mcp.domain.model.ToolParameter;
import com.dati.mcp.repository.dao.McpCustomToolDAO;
import com.dati.mcp.repository.dao.McpPrebuiltToolConfigDAO;
import com.dati.mcp.repository.mapper.McpCustomToolMapper;
import com.dati.mcp.repository.mapper.McpPrebuiltToolConfigMapper;
import com.dati.mcp.repository.po.McpCustomToolPO;
import com.dati.mcp.repository.po.McpPrebuiltToolConfigPO;
import com.dati.permission.domain.model.Permission;
import com.dati.permission.domain.service.PermissionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashMap;
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
    private final TemplateParser templateParser;
    private final SqlRenderer sqlRenderer;
    private final PermissionService permissionService;

    public McpToolService(McpPrebuiltToolConfigDAO prebuiltDAO,
                          McpCustomToolDAO customToolDAO,
                          TemplateParser templateParser,
                          SqlRenderer sqlRenderer,
                          PermissionService permissionService) {
        this.prebuiltDAO = prebuiltDAO;
        this.customToolDAO = customToolDAO;
        this.templateParser = templateParser;
        this.sqlRenderer = sqlRenderer;
        this.permissionService = permissionService;
    }

    // ── 列表 ──

    /** 返回分组数据 */
    @Transactional(readOnly = true)
    public ToolsResult listTools(String serviceId) {
        permissionService.requireMcpService(serviceId, Permission.VIEW);
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
                cfg.setEnabled(type.isDefaultEnabled());
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
        permissionService.requireMcpService(serviceId, Permission.EDIT);
        McpPrebuiltToolConfigPO po = prebuiltDAO.findByServiceIdAndToolType(serviceId, toolType)
            .orElseGet(() -> {
                McpPrebuiltToolConfigPO newPO = new McpPrebuiltToolConfigPO();
                newPO.setServiceId(serviceId);
                newPO.setToolType(toolType);
                newPO.setEnabled(toolType.isDefaultEnabled());
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
        permissionService.requireMcpService(serviceId, Permission.EDIT);
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
        permissionService.requireMcpService(tool.getServiceId(), Permission.EDIT);
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
        permissionService.requireMcpService(serviceId, Permission.EDIT);
        McpCustomToolPO po = customToolDAO.findByServiceIdAndId(serviceId, toolId)
            .orElseThrow(() -> new DatiException(ErrorCode.MS_TOOL_NOT_FOUND, toolId));
        customToolDAO.delete(po);
    }

    // ── 全量替换（回滚恢复草稿用）──

    /** 删除该服务全部自定义工具并以给定列表全量替换（内容来自已发布快照，跳过名称/模板校验） */
    @Transactional
    public void replaceCustomTools(String serviceId, List<McpCustomTool> tools) {
        customToolDAO.deleteAllByServiceId(serviceId);
        // Flush the bulk delete before queuing inserts: Hibernate executes queued
        // inserts before queued deletes within one flush, which would violate the
        // (service_id, name) unique constraint when restored names collide with existing rows.
        customToolDAO.flush();
        if (tools != null && !tools.isEmpty()) {
            List<McpCustomToolPO> pos = tools.stream()
                    .map(McpCustomToolMapper::toPO)
                    .toList();
            customToolDAO.saveAll(pos);
        }
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
        Set<String> templateVars = compiled.getVariables().stream()
                .filter(v -> !SystemVariableResolver.isSystemVariable(v))
                .collect(Collectors.toSet());
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

    public DetectedAnnotations detectAnnotations(String template, List<ToolParameter> parameters) {
        if (template == null || template.isBlank()) {
            throw new DatiException(ErrorCode.INVALID_PARAMETER, "template cannot be blank");
        }

        CompiledTemplate compiled;
        try {
            compiled = templateParser.parse(template);
        } catch (TemplateParseException e) {
            throw new DatiException(ErrorCode.INVALID_PARAMETER, e.getMessage());
        }

        Map<String, Object> mockValues = generateMockValues(compiled, parameters);
        SqlAnalysisResult analysis;
        try {
            PreparedSql ps = sqlRenderer.render(compiled, mockValues);
            analysis = SqlAnalyzer.analyze(ps.sql());
        } catch (Exception e) {
            analysis = SqlAnalyzer.analyze(template);
        }

        SqlOperationType op = analysis.type();
        if (op == SqlOperationType.MULTI && analysis.statementTypes() != null && !analysis.statementTypes().isEmpty()) {
            if (analysis.statementTypes().stream().anyMatch(t -> t == SqlOperationType.DELETE || t == SqlOperationType.DDL)) {
                op = SqlOperationType.DELETE;
            } else if (analysis.statementTypes().stream().anyMatch(t -> t == SqlOperationType.INSERT || t == SqlOperationType.UPDATE || t == SqlOperationType.MERGE)) {
                op = SqlOperationType.UPDATE;
            } else if (analysis.statementTypes().stream().allMatch(t -> t == SqlOperationType.SELECT || t == SqlOperationType.METADATA)) {
                op = SqlOperationType.SELECT;
            }
        }

        Boolean readOnly = null;
        Boolean idempotent = null;
        Boolean destructive = null;
        String detectedOperation = op != null ? op.name() : "UNKNOWN";

        if (op != null) {
            switch (op) {
                case SELECT, METADATA -> {
                    readOnly = true;
                    idempotent = true;
                    destructive = false;
                }
                case UPDATE, MERGE -> {
                    readOnly = false;
                    idempotent = true;
                    destructive = false;
                }
                case INSERT -> {
                    readOnly = false;
                    idempotent = false;
                    destructive = false;
                }
                case DELETE, DDL -> {
                    readOnly = false;
                    idempotent = false;
                    destructive = true;
                }
                default -> {
                    String trimmed = template.trim().toUpperCase();
                    if (trimmed.startsWith("SELECT") || trimmed.startsWith("WITH")) {
                        readOnly = true;
                        idempotent = true;
                        destructive = false;
                        detectedOperation = "SELECT";
                    } else if (trimmed.startsWith("DELETE") || trimmed.startsWith("DROP") || trimmed.startsWith("TRUNCATE")) {
                        readOnly = false;
                        idempotent = false;
                        destructive = true;
                        detectedOperation = "DELETE";
                    } else if (trimmed.startsWith("UPDATE")) {
                        readOnly = false;
                        idempotent = true;
                        destructive = false;
                        detectedOperation = "UPDATE";
                    } else if (trimmed.startsWith("INSERT")) {
                        readOnly = false;
                        idempotent = false;
                        destructive = false;
                        detectedOperation = "INSERT";
                    }
                }
            }
        }

        return new DetectedAnnotations(readOnly, idempotent, destructive, detectedOperation);
    }

    private Map<String, Object> generateMockValues(CompiledTemplate compiled, List<ToolParameter> parameters) {
        Map<String, Object> values = new HashMap<>();
        Map<String, String> typeMap = new HashMap<>();
        if (parameters != null) {
            for (ToolParameter p : parameters) {
                if (p.getName() != null) {
                    typeMap.put(p.getName(), p.getType());
                }
            }
        }
        for (String var : compiled.getVariables()) {
            String type = typeMap.getOrDefault(var, "String");
            values.put(var, createMockValue(type));
        }
        return values;
    }

    private Object createMockValue(String type) {
        if (type == null) return "mock";
        return switch (type.toLowerCase()) {
            case "number", "integer", "int", "double", "float" -> 1;
            case "boolean", "bool" -> true;
            case "datetime", "date", "time", "timestamp" -> "2026-01-01 00:00:00";
            case "array", "list" -> List.of("mock_item");
            default -> "mock";
        };
    }
}
