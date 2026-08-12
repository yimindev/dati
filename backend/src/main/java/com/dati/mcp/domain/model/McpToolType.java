package com.dati.mcp.domain.model;

import com.dati.mcp.domain.model.param.ExecuteSqlArgs;
import com.dati.mcp.domain.model.param.GetTableInfoArgs;
import com.dati.mcp.domain.model.param.SearchMetadataArgs;
import lombok.Getter;

@Getter
public enum McpToolType {
    SEARCH_METADATA(
        "search_metadata",
        "Search metadata across data sources based on keywords. Returns matching tables, columns, and terms grouped by data source.",
        SearchMetadataArgs.class
    ),
    GET_TABLE_INFO(
        "get_table_info",
        "Get detailed information about a specific table, including column names, types, descriptions, and related terms. Data is based on synced metadata. For latest structure, consider querying information_schema.",
        GetTableInfoArgs.class
    ),
    EXECUTE_SQL(
        "execute_sql",
        "Execute an arbitrary SQL statement against a data source within the service's data scope. Permissions (SELECT/INSERT/UPDATE/DELETE/DDL) are configurable per service.",
        ExecuteSqlArgs.class
    ),
    PARAMETERIZED_SQL(
        null,   // no predefined name — user defines it
        null,   // user-defined description
        null    // no record contract — schema generated from ToolParameter list
    );

    private final String toolName;
    private final String description;
    private final Class<?> parameterType;

    McpToolType(String name, String description, Class<?> parameterType) {
        this.toolName = name;
        this.description = description;
        this.parameterType = parameterType;
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
