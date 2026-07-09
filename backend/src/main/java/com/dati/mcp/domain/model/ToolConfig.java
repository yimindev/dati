package com.dati.mcp.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public interface ToolConfig {

    @Data
    class SearchMetadataConfig implements ToolConfig {
        private int timeout = 30;
    }

    @Data
    class GetTableInfoConfig implements ToolConfig {
        private int timeout = 30;
    }

    @Data
    class ExecuteSqlConfig implements ToolConfig {
        private SqlPolicy sqlPolicy = new SqlPolicy();
        private int timeout = 30;
        private int maxRows = 1000;
    }

    @Data
    class ParamSqlConfig implements ToolConfig {
        private String dataSourceId;
        private String sqlTemplate;
        private List<ToolParameter> parameters = new ArrayList<>();
        private SqlPolicy sqlPolicy = new SqlPolicy();
        private int timeout = 30;
        private int maxRows = 1000;
    }
}
