package com.dati.mcp.domain.model;

import com.dati.base.pojo.BaseResource;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class McpServiceSnapshot extends BaseResource {

    private String serviceId;

    private Integer versionNumber;

    private String releaseNote;

    private SnapshotContent content;

    /**
     * 快照内容 = 纯业务字段（配置快照）。
     * 不含 id / 审计字段（created_by 等）：它们属于运行时数据，对回滚恢复、diff 比较、版本展示均无意义，
     * 且携带 id 会导致「先删后存」恢复时对同事务已删除行执行 UPDATE（历史 BUG-20260801-001）。
     * 子实体以 Draft 类型承载，序列化/反序列化边界由类型系统强制。
     */
    @Data
    public static class SnapshotContent {
        private ServiceInfo serviceInfo;
        private List<DataScopeDraft> dataScopes;
        private List<PrebuiltToolDraft> prebuiltTools;
        private List<CustomToolDraft> customTools;
        private List<PromptDraft> prompts;
    }

    @Data
    public static class ServiceInfo {
        /** 服务 id（= serviceId），快照自包含标识 */
        private String id;
        private String name;
        private String description;
        private String code;
    }

    /**
     * 快照数据范围（仅业务字段）。
     * {@code @JsonIgnoreProperties(ignoreUnknown = true)}：兼容历史快照 JSON
     * （早期格式含 id / 审计字段，反序列化时静默忽略）。
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DataScopeDraft(String serviceId, McpDataScopeType scopeType, String referenceId) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PrebuiltToolDraft(String serviceId, McpToolType toolType, boolean enabled, ToolConfig config) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CustomToolDraft(String serviceId, String name, McpToolType toolType, String title,
                                  String description, boolean enabled, ToolConfig config) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PromptDraft(String serviceId, String name, String description, boolean enabled,
                              String content, List<PromptParameter> parameters) {
    }

}
