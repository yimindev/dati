package com.dati.mcp.domain.service;

import com.dati.base.exception.DatiException;
import com.dati.base.exception.ErrorCode;
import com.dati.common.JsonUtils;
import com.dati.mcp.domain.model.McpCustomTool;
import com.dati.mcp.domain.model.McpPrebuiltToolConfig;
import com.dati.mcp.domain.model.McpPrompt;
import com.dati.mcp.domain.model.McpServiceDataScope;
import com.dati.mcp.domain.model.McpServiceSnapshot;
import com.dati.mcp.domain.model.McpServiceStatus;
import com.dati.mcp.repository.dao.McpServiceDAO;
import com.dati.mcp.repository.dao.McpServiceSnapshotDAO;
import com.dati.mcp.repository.mapper.McpServiceSnapshotMapper;
import com.dati.mcp.repository.po.McpServicePO;
import com.dati.mcp.repository.po.McpServiceSnapshotPO;
import com.dati.mcp.server.pojo.McpServiceDiffVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class McpServicePublishService {

    private final McpServiceDAO mcpServiceDAO;
    private final McpServiceSnapshotDAO snapshotDAO;
    private final McpServiceDataScopeService dataScopeService;
    private final McpToolService toolService;
    private final McpPromptService promptService;

    public McpServicePublishService(McpServiceDAO mcpServiceDAO,
                                     McpServiceSnapshotDAO snapshotDAO,
                                     McpServiceDataScopeService dataScopeService,
                                     McpToolService toolService,
                                     McpPromptService promptService) {
        this.mcpServiceDAO = mcpServiceDAO;
        this.snapshotDAO = snapshotDAO;
        this.dataScopeService = dataScopeService;
        this.toolService = toolService;
        this.promptService = promptService;
    }

    @Transactional
    public McpServiceSnapshot publish(String serviceId, String releaseNote) {
        McpServicePO servicePO = mcpServiceDAO.findById(serviceId)
                .orElseThrow(() -> new DatiException(ErrorCode.MS_SERVICE_NOT_FOUND, serviceId));

        McpServiceSnapshot.SnapshotContent content = buildCurrentSnapshotContent(servicePO);

        Integer maxVersion = snapshotDAO.findMaxVersionNumberByServiceId(serviceId);
        int nextVersion = (maxVersion == null ? 0 : maxVersion) + 1;

        McpServiceSnapshot snapshot = new McpServiceSnapshot();
        snapshot.setServiceId(serviceId);
        snapshot.setVersionNumber(nextVersion);
        snapshot.setReleaseNote(releaseNote != null ? releaseNote.trim() : null);
        snapshot.setContent(content);

        McpServiceSnapshotPO snapshotPO = McpServiceSnapshotMapper.toPO(snapshot);
        snapshotPO = snapshotDAO.save(snapshotPO);

        // 仅首次发布（DRAFT）时置为 PUBLISHED；DISABLED 状态下发布保持停用（发布 ≠ 上线）
        if (servicePO.getStatus() == McpServiceStatus.DRAFT) {
            servicePO.setStatus(McpServiceStatus.PUBLISHED);
        }
        servicePO.setActiveVersionId(snapshotPO.getId());
        servicePO.setActiveVersionNumber(nextVersion);
        mcpServiceDAO.save(servicePO);

        return McpServiceSnapshotMapper.toModel(snapshotPO);
    }

    @Transactional
    public void disable(String serviceId) {
        McpServicePO servicePO = mcpServiceDAO.findById(serviceId)
                .orElseThrow(() -> new DatiException(ErrorCode.MS_SERVICE_NOT_FOUND, serviceId));
        if (servicePO.getStatus() != McpServiceStatus.PUBLISHED) {
            throw new DatiException(ErrorCode.MS_SERVICE_STATUS_INVALID,
                    "Only PUBLISHED service can be disabled, current: " + servicePO.getStatus());
        }
        servicePO.setStatus(McpServiceStatus.DISABLED);
        mcpServiceDAO.save(servicePO);
    }

    @Transactional
    public void enable(String serviceId) {
        McpServicePO servicePO = mcpServiceDAO.findById(serviceId)
                .orElseThrow(() -> new DatiException(ErrorCode.MS_SERVICE_NOT_FOUND, serviceId));
        if (servicePO.getStatus() != McpServiceStatus.DISABLED) {
            throw new DatiException(ErrorCode.MS_SERVICE_STATUS_INVALID,
                    "Only DISABLED service can be enabled, current: " + servicePO.getStatus());
        }
        servicePO.setStatus(McpServiceStatus.PUBLISHED);
        mcpServiceDAO.save(servicePO);
    }

    public List<McpServiceSnapshot> getSnapshots(String serviceId) {
        return snapshotDAO.findAllByServiceIdOrderByVersionNumberDesc(serviceId).stream()
                .map(McpServiceSnapshotMapper::toModel)
                .collect(Collectors.toList());
    }

    public McpServiceDiffVO getDiff(String serviceId) {
        McpServicePO servicePO = mcpServiceDAO.findById(serviceId)
                .orElseThrow(() -> new DatiException(ErrorCode.MS_SERVICE_NOT_FOUND, serviceId));

        McpServiceDiffVO diff = new McpServiceDiffVO();
        diff.setActiveVersionNumber(servicePO.getActiveVersionNumber());

        if (servicePO.getActiveVersionId() == null) {
            diff.setHasChanges(true);
            diff.getModifiedComponents().add("INITIAL_RELEASE");
            return diff;
        }

        McpServiceSnapshotPO activeSnapshotPO = snapshotDAO.findById(servicePO.getActiveVersionId())
                .orElse(null);

        if (activeSnapshotPO == null) {
            diff.setHasChanges(true);
            return diff;
        }

        McpServiceSnapshot activeSnapshot = McpServiceSnapshotMapper.toModel(activeSnapshotPO);
        McpServiceSnapshot.SnapshotContent activeContent = activeSnapshot.getContent();
        McpServiceSnapshot.SnapshotContent currentContent = buildCurrentSnapshotContent(servicePO);

        if (activeContent == null) {
            diff.setHasChanges(true);
            return diff;
        }

        // 1. Basic Info Diff
        if (!Objects.equals(activeContent.getServiceInfo().getName(), currentContent.getServiceInfo().getName())
                || !Objects.equals(activeContent.getServiceInfo().getDescription(), currentContent.getServiceInfo().getDescription())) {
            diff.setBasicInfoChanged(true);
            diff.getModifiedComponents().add("BASIC_INFO");
        }

        // 2. Data Scope Diff（仅比较业务字段，忽略 id/审计字段）
        if (!Objects.equals(dataScopeKeys(activeContent.getDataScopes()), dataScopeKeys(currentContent.getDataScopes()))) {
            diff.setDataScopeChanged(true);
            diff.getModifiedComponents().add("DATA_SCOPE");
        }

        // 3. Custom Tools Diff
        Map<String, McpCustomTool> activeCustomTools = activeContent.getCustomTools() != null
                ? activeContent.getCustomTools().stream().collect(Collectors.toMap(McpCustomTool::getName, Function.identity(), (a, b) -> a))
                : Map.of();
        Map<String, McpCustomTool> currentCustomTools = currentContent.getCustomTools() != null
                ? currentContent.getCustomTools().stream().collect(Collectors.toMap(McpCustomTool::getName, Function.identity(), (a, b) -> a))
                : Map.of();

        List<String> addedTools = new ArrayList<>();
        List<String> modifiedTools = new ArrayList<>();
        List<String> deletedTools = new ArrayList<>();

        for (Map.Entry<String, McpCustomTool> entry : currentCustomTools.entrySet()) {
            if (!activeCustomTools.containsKey(entry.getKey())) {
                addedTools.add(entry.getKey());
            } else {
                if (!Objects.equals(customToolKey(activeCustomTools.get(entry.getKey())), customToolKey(entry.getValue()))) {
                    modifiedTools.add(entry.getKey());
                }
            }
        }
        for (String activeName : activeCustomTools.keySet()) {
            if (!currentCustomTools.containsKey(activeName)) {
                deletedTools.add(activeName);
            }
        }

        // Prebuilt tools check（仅比较业务字段，忽略 id/审计字段）
        List<McpPrebuiltToolConfig> activePrebuilt = activeContent.getPrebuiltTools();
        List<McpPrebuiltToolConfig> currentPrebuilt = currentContent.getPrebuiltTools();
        boolean prebuiltChanged = !Objects.equals(prebuiltKeys(activePrebuilt), prebuiltKeys(currentPrebuilt));
        if (prebuiltChanged) {
            // prebuilt 变更没有单独的增删改列表，将具体工具名并入 modified_tools
            modifiedTools.addAll(changedPrebuiltTypes(activePrebuilt, currentPrebuilt));
        }

        if (!addedTools.isEmpty() || !modifiedTools.isEmpty() || !deletedTools.isEmpty() || prebuiltChanged) {
            diff.setToolsChanged(true);
            diff.setAddedTools(addedTools);
            diff.setModifiedTools(modifiedTools);
            diff.setDeletedTools(deletedTools);
            diff.getModifiedComponents().add("TOOLS");
        }

        // 4. Prompts Diff
        Map<String, McpPrompt> activePrompts = activeContent.getPrompts() != null
                ? activeContent.getPrompts().stream().collect(Collectors.toMap(McpPrompt::getName, Function.identity(), (a, b) -> a))
                : Map.of();
        Map<String, McpPrompt> currentPrompts = currentContent.getPrompts() != null
                ? currentContent.getPrompts().stream().collect(Collectors.toMap(McpPrompt::getName, Function.identity(), (a, b) -> a))
                : Map.of();

        List<String> addedPrompts = new ArrayList<>();
        List<String> modifiedPrompts = new ArrayList<>();
        List<String> deletedPrompts = new ArrayList<>();

        for (Map.Entry<String, McpPrompt> entry : currentPrompts.entrySet()) {
            if (!activePrompts.containsKey(entry.getKey())) {
                addedPrompts.add(entry.getKey());
            } else {
                if (!Objects.equals(promptKey(activePrompts.get(entry.getKey())), promptKey(entry.getValue()))) {
                    modifiedPrompts.add(entry.getKey());
                }
            }
        }
        for (String activeName : activePrompts.keySet()) {
            if (!currentPrompts.containsKey(activeName)) {
                deletedPrompts.add(activeName);
            }
        }

        if (!addedPrompts.isEmpty() || !modifiedPrompts.isEmpty() || !deletedPrompts.isEmpty()) {
            diff.setPromptsChanged(true);
            diff.setAddedPrompts(addedPrompts);
            diff.setModifiedPrompts(modifiedPrompts);
            diff.setDeletedPrompts(deletedPrompts);
            diff.getModifiedComponents().add("PROMPTS");
        }

        boolean hasChanges = diff.isBasicInfoChanged() || diff.isDataScopeChanged() || diff.isToolsChanged() || diff.isPromptsChanged();
        diff.setHasChanges(hasChanges);

        return diff;
    }

    @Transactional
    public McpServiceSnapshot rollback(String serviceId, Integer targetVersionNumber, String releaseNote) {
        McpServicePO servicePO = mcpServiceDAO.findById(serviceId)
                .orElseThrow(() -> new DatiException(ErrorCode.MS_SERVICE_NOT_FOUND, serviceId));

        McpServiceSnapshotPO targetSnapshotPO = snapshotDAO.findByServiceIdAndVersionNumber(serviceId, targetVersionNumber)
                .orElseThrow(() -> new DatiException(ErrorCode.MS_SERVICE_VERSION_NOT_FOUND,
                        "v" + targetVersionNumber + " of service " + serviceId));

        McpServiceSnapshot targetSnapshot = McpServiceSnapshotMapper.toModel(targetSnapshotPO);
        McpServiceSnapshot.SnapshotContent content = targetSnapshot.getContent();
        if (content == null) {
            throw new DatiException(ErrorCode.MS_SERVICE_VERSION_NOT_FOUND,
                    "v" + targetVersionNumber + " of service " + serviceId + " has no content");
        }

        // 1. 基础信息写回草稿
        McpServiceSnapshot.ServiceInfo info = content.getServiceInfo();
        if (info != null) {
            servicePO.setName(info.getName());
            servicePO.setDescription(info.getDescription());
            servicePO.setCode(info.getCode());
            mcpServiceDAO.save(servicePO);
        }

        // 2. 数据范围写回草稿
        dataScopeService.saveDataScope(serviceId, content.getDataScopes());

        // 3. Tools 写回草稿
        toolService.replaceCustomTools(serviceId, content.getCustomTools());
        if (content.getPrebuiltTools() != null) {
            content.getPrebuiltTools().forEach(cfg -> toolService.updatePrebuiltTool(serviceId, cfg.getToolType(), cfg));
        }

        // 4. Prompts 写回草稿
        promptService.replacePrompts(serviceId, content.getPrompts());

        // 5. 重新发布生成新快照 vN+1（保留回滚审计痕迹）
        String note = "Rollback to v" + targetVersionNumber
                + (releaseNote != null && !releaseNote.isBlank() ? ": " + releaseNote : "");
        return publish(serviceId, note);
    }

    /**
     * 业务字段比较 key：diff 只关注配置内容差异，忽略 id / 审计字段
     * （快照反序列化不保留审计字段，且回滚恢复会刷新时间戳，全字段比较必然误报）。
     */
    private List<String> dataScopeKeys(List<McpServiceDataScope> scopes) {
        return scopes == null ? List.of() : scopes.stream()
                .map(s -> s.getScopeType() + "|" + s.getReferenceId())
                .toList();
    }

    private List<String> prebuiltKeys(List<McpPrebuiltToolConfig> tools) {
        return tools == null ? List.of() : tools.stream()
                .map(t -> t.getToolType() + "|" + t.isEnabled() + "|" + JsonUtils.toJson(t.getConfig()))
                .toList();
    }

    /** 返回配置发生变化的 prebuilt 工具名（toolType），用于 diff 明细展示 */
    private List<String> changedPrebuiltTypes(List<McpPrebuiltToolConfig> active, List<McpPrebuiltToolConfig> current) {
        List<String> changed = new ArrayList<>();
        List<String> activeKeys = prebuiltKeys(active);
        List<String> currentKeys = prebuiltKeys(current);
        int max = Math.max(activeKeys.size(), currentKeys.size());
        for (int i = 0; i < max; i++) {
            String a = i < activeKeys.size() ? activeKeys.get(i) : null;
            String c = i < currentKeys.size() ? currentKeys.get(i) : null;
            if (!Objects.equals(a, c)) {
                McpPrebuiltToolConfig cfg = i < current.size() ? current.get(i) : (i < active.size() ? active.get(i) : null);
                if (cfg != null && cfg.getToolType() != null) {
                    changed.add(cfg.getToolType().name());
                }
            }
        }
        return changed;
    }

    private String customToolKey(McpCustomTool t) {
        return t.getToolType() + "|" + t.getName() + "|" + t.getTitle() + "|" + t.getDescription()
                + "|" + t.isEnabled() + "|" + JsonUtils.toJson(t.getConfig());
    }

    private String promptKey(McpPrompt p) {
        return p.getName() + "|" + p.getDescription() + "|" + p.getContent()
                + "|" + JsonUtils.toJson(p.getParameters()) + "|" + p.isEnabled();
    }

    private McpServiceSnapshot.SnapshotContent buildCurrentSnapshotContent(McpServicePO servicePO) {
        String serviceId = servicePO.getId();

        McpServiceSnapshot.ServiceInfo serviceInfo = new McpServiceSnapshot.ServiceInfo();
        serviceInfo.setId(servicePO.getId());
        serviceInfo.setName(servicePO.getName());
        serviceInfo.setDescription(servicePO.getDescription());
        serviceInfo.setCode(servicePO.getCode());

        List<McpServiceDataScope> dataScopes = dataScopeService.getDataScope(serviceId);

        ToolsResult toolsResult = toolService.listTools(serviceId);
        List<McpPrebuiltToolConfig> prebuiltTools = toolsResult.prebuilt() != null
                ? toolsResult.prebuilt()
                : List.of();
        List<McpCustomTool> customTools = toolsResult.custom() != null
                ? toolsResult.custom()
                : List.of();

        List<McpPrompt> prompts = promptService.listPrompts(serviceId);

        McpServiceSnapshot.SnapshotContent content = new McpServiceSnapshot.SnapshotContent();
        content.setServiceInfo(serviceInfo);
        content.setDataScopes(dataScopes);
        content.setPrebuiltTools(prebuiltTools);
        content.setCustomTools(customTools);
        content.setPrompts(prompts);
        return content;
    }

}
