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
     * 手动反序列化快照内容，根据 {@code tool_type} 显式确定 {@link ToolConfig} 具体类。
     * <p>不依赖 {@code @JsonTypeInfo}，避免污染其他反序列化路径（如 Mapper/Assembler）。</p>
     */
    private static McpServiceSnapshot.SnapshotContent parseContent(String json) {
        ObjectNode root = (ObjectNode) JsonUtils.parseJson(json);
        McpServiceSnapshot.SnapshotContent content = new McpServiceSnapshot.SnapshotContent();

        // service_info — simple POJO
        if (root.has("service_info")) {
            content.setServiceInfo(JsonUtils.fromJson(
                root.get("service_info").toString(), McpServiceSnapshot.ServiceInfo.class));
        }

        // data_scopes
        if (root.has("data_scopes")) {
            List<McpServiceDataScope> scopes = new ArrayList<>();
            for (JsonNode n : root.get("data_scopes")) {
                scopes.add(JsonUtils.fromJson(n.toString(), McpServiceDataScope.class));
            }
            content.setDataScopes(scopes);
        }

        // prebuilt_tools — config 根据 tool_type 显式反序列化
        if (root.has("prebuilt_tools")) {
            List<McpPrebuiltToolConfig> tools = new ArrayList<>();
            for (JsonNode n : root.get("prebuilt_tools")) {
                tools.add(buildPrebuiltTool((ObjectNode) n));
            }
            content.setPrebuiltTools(tools);
        }

        // custom_tools — 同上
        if (root.has("custom_tools")) {
            List<McpCustomTool> tools = new ArrayList<>();
            for (JsonNode n : root.get("custom_tools")) {
                tools.add(buildCustomTool((ObjectNode) n));
            }
            content.setCustomTools(tools);
        }

        // prompts — simple POJO
        if (root.has("prompts")) {
            List<McpPrompt> prompts = new ArrayList<>();
            for (JsonNode n : root.get("prompts")) {
                prompts.add(JsonUtils.fromJson(n.toString(), McpPrompt.class));
            }
            content.setPrompts(prompts);
        }

        return content;
    }

    private static McpPrebuiltToolConfig buildPrebuiltTool(ObjectNode node) {
        McpPrebuiltToolConfig tool = new McpPrebuiltToolConfig();
        if (node.has("id") && !node.get("id").isNull())
            tool.setId(node.get("id").asText());
        if (node.has("service_id") && !node.get("service_id").isNull())
            tool.setServiceId(node.get("service_id").asText());
        if (node.has("tool_type") && !node.get("tool_type").isNull())
            tool.setToolType(McpToolType.valueOf(node.get("tool_type").asText()));
        if (node.has("enabled") && !node.get("enabled").isNull())
            tool.setEnabled(node.get("enabled").asBoolean());
        // 根据 tool_type 显式选择具体类反序列化 config
        if (node.has("config") && !node.get("config").isNull() && tool.getToolType() != null) {
            tool.setConfig(deserializeConfig(node.get("config").toString(), tool.getToolType()));
        }
        return tool;
    }

    private static McpCustomTool buildCustomTool(ObjectNode node) {
        McpCustomTool tool = new McpCustomTool();
        if (node.has("id") && !node.get("id").isNull())
            tool.setId(node.get("id").asText());
        if (node.has("name") && !node.get("name").isNull())
            tool.setName(node.get("name").asText());
        if (node.has("description") && !node.get("description").isNull())
            tool.setDescription(node.get("description").asText());
        if (node.has("service_id") && !node.get("service_id").isNull())
            tool.setServiceId(node.get("service_id").asText());
        if (node.has("tool_type") && !node.get("tool_type").isNull())
            tool.setToolType(McpToolType.valueOf(node.get("tool_type").asText()));
        if (node.has("title") && !node.get("title").isNull())
            tool.setTitle(node.get("title").asText());
        if (node.has("enabled") && !node.get("enabled").isNull())
            tool.setEnabled(node.get("enabled").asBoolean());
        // 根据 tool_type 显式选择具体类反序列化 config
        if (node.has("config") && !node.get("config").isNull() && tool.getToolType() != null) {
            tool.setConfig(deserializeConfig(node.get("config").toString(), tool.getToolType()));
        }
        return tool;
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

}
