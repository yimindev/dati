package com.dati.mcp.server.pojo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class McpServiceDiffVO {

    @JsonProperty("has_changes")
    private boolean hasChanges;

    @JsonProperty("active_version_number")
    private Integer activeVersionNumber;

    @JsonProperty("modified_components")
    private List<String> modifiedComponents = new ArrayList<>();

    @JsonProperty("basic_info_changed")
    private boolean basicInfoChanged;

    @JsonProperty("data_scope_changed")
    private boolean dataScopeChanged;

    @JsonProperty("tools_changed")
    private boolean toolsChanged;

    @JsonProperty("prompts_changed")
    private boolean promptsChanged;

    @JsonProperty("added_tools")
    private List<String> addedTools = new ArrayList<>();

    @JsonProperty("modified_tools")
    private List<String> modifiedTools = new ArrayList<>();

    @JsonProperty("deleted_tools")
    private List<String> deletedTools = new ArrayList<>();

    @JsonProperty("added_prompts")
    private List<String> addedPrompts = new ArrayList<>();

    @JsonProperty("modified_prompts")
    private List<String> modifiedPrompts = new ArrayList<>();

    @JsonProperty("deleted_prompts")
    private List<String> deletedPrompts = new ArrayList<>();

}
