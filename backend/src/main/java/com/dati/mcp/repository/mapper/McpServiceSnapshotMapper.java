package com.dati.mcp.repository.mapper;

import com.dati.base.MapperUtils;
import com.dati.common.JsonUtils;
import com.dati.mcp.domain.model.McpCustomTool;
import com.dati.mcp.domain.model.McpPrebuiltToolConfig;
import com.dati.mcp.domain.model.McpPrompt;
import com.dati.mcp.domain.model.McpServiceDataScope;
import com.dati.mcp.domain.model.McpServiceSnapshot;
import com.dati.mcp.domain.model.McpToolType;
import com.dati.mcp.domain.model.ToolConfig;
import com.dati.mcp.repository.po.McpServiceSnapshotPO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;

public class McpServiceSnapshotMapper {

    public static McpServiceSnapshotPO toPO(McpServiceSnapshot snapshot) {
        McpServiceSnapshotPO po = new McpServiceSnapshotPO();
        MapperUtils.copyBaseInfo(snapshot, po);
        po.setServiceId(snapshot.getServiceId());
        po.setVersionNumber(snapshot.getVersionNumber());
        po.setReleaseNote(snapshot.getReleaseNote());
        if (snapshot.getContent() != null) {
            po.setSnapshotContent(JsonUtils.toJson(snapshot.getContent()));
        }
        return po;
    }

    public static McpServiceSnapshot toModel(McpServiceSnapshotPO po) {
        McpServiceSnapshot snapshot = new McpServiceSnapshot();
        MapperUtils.copyBaseInfo(po, snapshot);
        snapshot.setServiceId(po.getServiceId());
        snapshot.setVersionNumber(po.getVersionNumber());
        snapshot.setReleaseNote(po.getReleaseNote());
        if (po.getSnapshotContent() != null) {
            snapshot.setContent(parseContent(po.getSnapshotContent()));
        }
        return snapshot;
    }

    /**
     * 手动反序列化快照内容为 Draft（纯业务字段）类型。
     * <p>config 根据 {@code tool_type} 显式路由到具体 {@link ToolConfig} 实现类
     * （不依赖 {@code @JsonTypeInfo}，避免污染其他反序列化路径）。
     * Draft 上的 {@code @JsonIgnoreProperties(ignoreUnknown = true)} 兼容历史快照
     * （早期格式含 id / 审计字段，反序列化时静默忽略）。</p>
     */
    private static McpServiceSnapshot.SnapshotContent parseContent(String json) {
        ObjectNode root = (ObjectNode) JsonUtils.parseJson(json);
        McpServiceSnapshot.SnapshotContent content = new McpServiceSnapshot.SnapshotContent();

        // service_info — simple POJO
        if (root.has("service_info")) {
            content.setServiceInfo(JsonUtils.fromJson(
                root.get("service_info").toString(), McpServiceSnapshot.ServiceInfo.class));
        }

        // data_scopes — Draft record（ignoreUnknown 忽略旧格式的 id/审计字段）
        if (root.has("data_scopes")) {
            List<McpServiceSnapshot.DataScopeDraft> scopes = new ArrayList<>();
            for (JsonNode n : root.get("data_scopes")) {
                scopes.add(JsonUtils.fromJson(n.toString(), McpServiceSnapshot.DataScopeDraft.class));
            }
            content.setDataScopes(scopes);
        }

        // prebuilt_tools — config 根据 tool_type 显式反序列化
        if (root.has("prebuilt_tools")) {
            List<McpServiceSnapshot.PrebuiltToolDraft> tools = new ArrayList<>();
            for (JsonNode n : root.get("prebuilt_tools")) {
                tools.add(buildPrebuiltTool((ObjectNode) n));
            }
            content.setPrebuiltTools(tools);
        }

        // custom_tools — 同上
        if (root.has("custom_tools")) {
            List<McpServiceSnapshot.CustomToolDraft> tools = new ArrayList<>();
            for (JsonNode n : root.get("custom_tools")) {
                tools.add(buildCustomTool((ObjectNode) n));
            }
            content.setCustomTools(tools);
        }

        // prompts — Draft record
        if (root.has("prompts")) {
            List<McpServiceSnapshot.PromptDraft> prompts = new ArrayList<>();
            for (JsonNode n : root.get("prompts")) {
                prompts.add(JsonUtils.fromJson(n.toString(), McpServiceSnapshot.PromptDraft.class));
            }
            content.setPrompts(prompts);
        }

        return content;
    }

    private static McpServiceSnapshot.PrebuiltToolDraft buildPrebuiltTool(ObjectNode node) {
        McpToolType toolType = node.has("tool_type") && !node.get("tool_type").isNull()
                ? McpToolType.valueOf(node.get("tool_type").asText()) : null;
        boolean enabled = node.has("enabled") && !node.get("enabled").isNull()
                && node.get("enabled").asBoolean();
        ToolConfig config = null;
        if (node.has("config") && !node.get("config").isNull() && toolType != null) {
            config = deserializeConfig(node.get("config").toString(), toolType);
        }
        return new McpServiceSnapshot.PrebuiltToolDraft(
                textOrNull(node, "service_id"), toolType, enabled, config);
    }

    private static McpServiceSnapshot.CustomToolDraft buildCustomTool(ObjectNode node) {
        McpToolType toolType = node.has("tool_type") && !node.get("tool_type").isNull()
                ? McpToolType.valueOf(node.get("tool_type").asText()) : null;
        ToolConfig config = null;
        if (node.has("config") && !node.get("config").isNull() && toolType != null) {
            config = deserializeConfig(node.get("config").toString(), toolType);
        }
        return new McpServiceSnapshot.CustomToolDraft(
                textOrNull(node, "service_id"),
                textOrNull(node, "name"),
                toolType,
                textOrNull(node, "title"),
                textOrNull(node, "description"),
                node.has("enabled") && !node.get("enabled").isNull() && node.get("enabled").asBoolean(),
                config);
    }

    private static String textOrNull(ObjectNode node, String field) {
        return node.has(field) && !node.get(field).isNull() ? node.get(field).asText() : null;
    }

    /** 根据已知的 {@link McpToolType} 选择正确的 {@link ToolConfig} 实现类反序列化。 */
    private static ToolConfig deserializeConfig(String configJson, McpToolType type) {
        return switch (type) {
            case SEARCH_METADATA -> JsonUtils.fromJson(configJson, ToolConfig.SearchMetadataConfig.class);
            case GET_TABLE_INFO  -> JsonUtils.fromJson(configJson, ToolConfig.GetTableInfoConfig.class);
            case EXECUTE_SQL     -> JsonUtils.fromJson(configJson, ToolConfig.ExecuteSqlConfig.class);
            case PARAMETERIZED_SQL -> JsonUtils.fromJson(configJson, ToolConfig.ParamSqlConfig.class);
        };
    }

    // ── Draft → Model（回滚恢复草稿用；Draft 无 id，重建即 INSERT，天然避免 stale-update）──

    public static McpServiceDataScope toDataScope(McpServiceSnapshot.DataScopeDraft draft) {
        McpServiceDataScope scope = new McpServiceDataScope();
        scope.setServiceId(draft.serviceId());
        scope.setScopeType(draft.scopeType());
        scope.setReferenceId(draft.referenceId());
        return scope;
    }

    public static McpPrebuiltToolConfig toPrebuiltTool(McpServiceSnapshot.PrebuiltToolDraft draft) {
        McpPrebuiltToolConfig tool = new McpPrebuiltToolConfig();
        tool.setServiceId(draft.serviceId());
        tool.setToolType(draft.toolType());
        tool.setEnabled(draft.enabled());
        tool.setConfig(draft.config());
        return tool;
    }

    public static McpCustomTool toCustomTool(McpServiceSnapshot.CustomToolDraft draft) {
        McpCustomTool tool = new McpCustomTool();
        tool.setServiceId(draft.serviceId());
        tool.setName(draft.name());
        tool.setToolType(draft.toolType());
        tool.setTitle(draft.title());
        tool.setDescription(draft.description());
        tool.setEnabled(draft.enabled());
        tool.setConfig(draft.config());
        return tool;
    }

    public static McpPrompt toPrompt(McpServiceSnapshot.PromptDraft draft) {
        McpPrompt prompt = new McpPrompt();
        prompt.setServiceId(draft.serviceId());
        prompt.setName(draft.name());
        prompt.setDescription(draft.description());
        prompt.setEnabled(draft.enabled());
        prompt.setContent(draft.content());
        prompt.setParameters(draft.parameters());
        return prompt;
    }

    // ── Model → Draft（发布打包用；仅业务字段入快照）──

    public static McpServiceSnapshot.DataScopeDraft toDataScopeDraft(McpServiceDataScope scope) {
        return new McpServiceSnapshot.DataScopeDraft(scope.getServiceId(), scope.getScopeType(), scope.getReferenceId());
    }

    public static McpServiceSnapshot.PrebuiltToolDraft toPrebuiltToolDraft(McpPrebuiltToolConfig tool) {
        return new McpServiceSnapshot.PrebuiltToolDraft(
                tool.getServiceId(), tool.getToolType(), tool.isEnabled(), tool.getConfig());
    }

    public static McpServiceSnapshot.CustomToolDraft toCustomToolDraft(McpCustomTool tool) {
        return new McpServiceSnapshot.CustomToolDraft(
                tool.getServiceId(), tool.getName(), tool.getToolType(), tool.getTitle(),
                tool.getDescription(), tool.isEnabled(), tool.getConfig());
    }

    public static McpServiceSnapshot.PromptDraft toPromptDraft(McpPrompt prompt) {
        return new McpServiceSnapshot.PromptDraft(
                prompt.getServiceId(), prompt.getName(), prompt.getDescription(), prompt.isEnabled(),
                prompt.getContent(), prompt.getParameters());
    }

}
