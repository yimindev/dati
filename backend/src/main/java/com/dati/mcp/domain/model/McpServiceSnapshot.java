package com.dati.mcp.domain.model;

import com.dati.base.pojo.BaseResource;
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

    @Data
    public static class SnapshotContent {
        private ServiceInfo serviceInfo;
        private List<McpServiceDataScope> dataScopes;
        private List<McpPrebuiltToolConfig> prebuiltTools;
        private List<McpCustomTool> customTools;
        private List<McpPrompt> prompts;
    }

    @Data
    public static class ServiceInfo {
        private String id;
        private String name;
        private String description;
        private String code;
    }
}
