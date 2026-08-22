package com.dati.mcp.domain.model;

import com.dati.mcp.domain.model.param.ExecuteSqlArgs;
import com.dati.mcp.domain.model.param.GetTableInfoArgs;
import com.dati.mcp.domain.model.param.ListTablesArgs;
import com.dati.mcp.domain.model.param.SearchMetadataArgs;
import com.dati.mcp.domain.model.param.UpdateColumnInfoArgs;
import com.dati.mcp.domain.model.param.UpdateTableInfoArgs;
import com.dati.mcp.domain.model.param.UpsertTermArgs;
import lombok.Getter;

@Getter
public enum McpToolType {
    SEARCH_METADATA(
        "search_metadata",
        "Search Metadata",
        "Search tables, columns, sample values, and business terms by keywords across data sources.",
        SearchMetadataArgs.class,
        "{\"readOnlyHint\":true}",
        true
    ),
    GET_TABLE_INFO(
        "get_table_info",
        "Get Table Info",
        "Get full column schemas (names, types, comments, sample values) for up to 20 tables.",
        GetTableInfoArgs.class,
        "{\"readOnlyHint\":true}",
        true
    ),
    LIST_TABLES(
        "list_tables",
        "List Tables",
        "List all available tables with schema, name, and description (table-level only, no columns).",
        ListTablesArgs.class,
        "{\"readOnlyHint\":true}",
        true
    ),
    EXECUTE_SQL(
        "execute_sql",
        "Execute SQL",
        "Execute an SQL query or statement against a data source.",
        ExecuteSqlArgs.class,
        null,
        true
    ),
    UPDATE_TABLE_INFO(
        "update_table_info",
        "Update Table Metadata",
        "Enrich table description or aliases after analysis to improve future queries.",
        UpdateTableInfoArgs.class,
        "{\"readOnlyHint\":false,\"destructiveHint\":false,\"idempotentHint\":true,\"openWorldHint\":true}",
        false
    ),
    UPDATE_COLUMN_INFO(
        "update_column_info",
        "Update Column Metadata",
        "Enrich column description (e.g. enum meanings, value formats) or aliases after analysis to improve future queries.",
        UpdateColumnInfoArgs.class,
        "{\"readOnlyHint\":false,\"destructiveHint\":false,\"idempotentHint\":true,\"openWorldHint\":true}",
        false
    ),
    UPSERT_TERM(
        "upsert_term",
        "Upsert Business Term",
        "Enrich business vocabulary by creating or updating terms under a subject after analysis.",
        UpsertTermArgs.class,
        "{\"readOnlyHint\":false,\"destructiveHint\":false,\"idempotentHint\":true,\"openWorldHint\":true}",
        false
    ),
    PARAMETERIZED_SQL(
        null,   // no predefined name — user defines it
        null,   // no protocol title
        null,   // user-defined description
        null,   // no record contract — schema generated from ToolParameter list
        null,   // no annotations
        true    // custom tools default enabled; irrelevant for the lazy-init path
    );

    private final String toolName;
    private final String title;
    private final String description;
    private final Class<?> parameterType;
    private final String annotationsJson;
    private final boolean defaultEnabled;

    McpToolType(String name, String title, String description, Class<?> parameterType, String annotationsJson,
                boolean defaultEnabled) {
        this.toolName = name;
        this.title = title;
        this.description = description;
        this.parameterType = parameterType;
        this.annotationsJson = annotationsJson;
        this.defaultEnabled = defaultEnabled;
    }

    public ToolConfig getDefaultConfig() {
        return switch (this) {
            case SEARCH_METADATA -> new ToolConfig.SearchMetadataConfig();
            case GET_TABLE_INFO -> new ToolConfig.GetTableInfoConfig();
            case LIST_TABLES -> new ToolConfig.ListTablesConfig();
            case EXECUTE_SQL -> new ToolConfig.ExecuteSqlConfig();
            case UPDATE_TABLE_INFO, UPDATE_COLUMN_INFO, UPSERT_TERM -> new ToolConfig.UpdateMetadataConfig();
            case PARAMETERIZED_SQL -> new ToolConfig.ParamSqlConfig();
        };
    }

    /** 是否为预置工具 */
    public boolean isPrebuilt() {
        return this != PARAMETERIZED_SQL;
    }
}
