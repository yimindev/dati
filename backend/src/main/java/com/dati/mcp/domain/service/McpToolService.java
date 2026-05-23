package com.dati.mcp.domain.service;

import com.dati.base.exception.DatiException;
import com.dati.base.exception.ErrorCode;
import com.dati.common.JsonUtils;
import com.dati.mcp.domain.model.McpCustomTool;
import com.dati.mcp.domain.model.McpPrebuiltToolConfig;
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
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class McpToolService {

    private static final Pattern TOOL_NAME_PATTERN = Pattern.compile("^[A-Za-z0-9_.\\-]{1,128}$");

    private final McpPrebuiltToolConfigDAO prebuiltDAO;
    private final McpCustomToolDAO customToolDAO;
    private final McpServiceDAO mcpServiceDAO;

    public McpToolService(McpPrebuiltToolConfigDAO prebuiltDAO,
                          McpCustomToolDAO customToolDAO,
                          McpServiceDAO mcpServiceDAO) {
        this.prebuiltDAO = prebuiltDAO;
        this.customToolDAO = customToolDAO;
        this.mcpServiceDAO = mcpServiceDAO;
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
        McpCustomToolPO po = McpCustomToolMapper.toPO(tool);
        po = customToolDAO.save(po);
        return po.getId();
    }

    @Transactional
    public void updateCustomTool(String serviceId, String toolId, McpCustomTool tool) {
        McpCustomToolPO po = customToolDAO.findByServiceIdAndId(serviceId, toolId)
            .orElseThrow(() -> new DatiException(ErrorCode.MS_TOOL_NOT_FOUND, toolId));
        if (tool.getName() != null) {
            validateToolName(tool.getName());
            if (!po.getName().equals(tool.getName())
                && customToolDAO.existsByServiceIdAndNameAndIdNot(serviceId, tool.getName(), toolId)) {
                throw new DatiException(ErrorCode.MS_TOOL_NAME_EXISTS, tool.getName());
            }
        }
        McpCustomToolMapper.copyProperties(tool, po);
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
}
