package com.dati.mcp.domain.model;

import lombok.Getter;

@Getter
public enum McpToolType {
    SEARCH_METADATA(
        "search_metadata",
        "Search metadata across data sources based on keywords. Returns matching tables, columns, and terms grouped by data source.",
        """
        {
          "type": "object",
          "properties": {
            "keywords": {"type": "array", "items": {"type": "string"}, "description": "Search keywords"}
          },
          "required": ["keywords"],
          "additionalProperties": false
        }
        """
    ),
    GET_TABLE_INFO(
        "get_table_info",
        "Get detailed information about a specific table, including column names, types, descriptions, and related terms. Data is based on synced metadata. For latest structure, consider querying information_schema.",
        """
        {
          "type": "object",
          "properties": {
            "data_source_id": {"type": "string", "description": "Data source ID"},
            "table": {"type": "string", "description": "Table name"},
            "fields": {
              "type": "array",
              "items": {"type": "string"},
              "description": "Optional list of column names to retrieve"
            }
          },
          "required": ["data_source_id", "table"],
          "additionalProperties": false
        }
        """
    ),
    EXECUTE_SQL(
        "execute_sql",
        "Execute an arbitrary SQL statement against a data source within the service's data scope. Permissions (SELECT/INSERT/UPDATE/DELETE/DDL) are configurable per service.",
        """
        {
          "type": "object",
          "properties": {
            "data_source_id": {"type": "string", "description": "Data source ID"},
            "sql": {"type": "string", "description": "SQL statement to execute"}
          },
          "required": ["data_source_id", "sql"],
          "additionalProperties": false
        }
        """
    ),
    PARAMETERIZED_SQL(
        null,   // no predefined name — user defines it
        null,   // user-defined description
        null   // inputSchema generated from parameters
    );

    private final String toolName;
    private final String description;
    private final String inputSchema;

    McpToolType(String name, String description, String inputSchema) {
        this.toolName = name;
        this.description = description;
        this.inputSchema = inputSchema;
    }

    public ToolConfig getDefaultConfig() {
        return switch (this) {
            case SEARCH_METADATA -> new ToolConfig.SearchMetadataConfig();
            case GET_TABLE_INFO -> new ToolConfig.GetTableInfoConfig();
            case EXECUTE_SQL -> new ToolConfig.ExecuteSqlConfig();
            case PARAMETERIZED_SQL -> new ToolConfig.ParamSqlConfig();
        };
    }

    /** 是否为预置工具 */
    public boolean isPrebuilt() {
        return this != PARAMETERIZED_SQL;
    }
}
