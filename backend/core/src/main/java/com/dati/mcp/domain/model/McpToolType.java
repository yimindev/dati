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
        "search_tables_and_terms",
        "Search Tables and Terms",
        "Search relevant database tables, column names, sample values, and business terms by keywords. Always use this tool first to discover the right tables and terms instead of guessing tables or running exploratory SQL.",
        SearchMetadataArgs.class,
        "{\"readOnlyHint\":true}",
        true
    ),
    GET_TABLE_INFO(
        "get_table_schema",
        "Get Table Schema",
        "Get detailed column schemas (names, types, comments, and sample values) for specified tables (up to 20 tables).",
        GetTableInfoArgs.class,
        "{\"readOnlyHint\":true}",
        true
    ),
    LIST_TABLES(
        "list_tables_and_terms",
        "List Tables and Terms",
        "List all available tables (with schema, name, and description; overview only, no columns) and business terms under the service scope. Use this tool when exploring the catalog or when keyword search returns no matches.",
        ListTablesArgs.class,
        "{\"readOnlyHint\":true}",
        true
    ),
    EXECUTE_SQL(
        "execute_sql",
        "Execute SQL",
        "Execute an SQL query or statement against a data source and return result rows. Use this tool only when no specialized or custom analysis tool is available for your task.",
        ExecuteSqlArgs.class,
        null,
        true
    ),
    UPDATE_TABLE_INFO(
        "update_table_metadata",
        "Update Table Metadata",
        "Enrich table description or aliases after analysis to improve future queries and search accuracy.",
        UpdateTableInfoArgs.class,
        "{\"readOnlyHint\":false,\"destructiveHint\":false,\"idempotentHint\":true,\"openWorldHint\":true}",
        false
    ),
    UPDATE_COLUMN_INFO(
        "update_column_metadata",
        "Update Column Metadata",
        "Enrich column descriptions (e.g. business meanings, enum mappings) or aliases after analysis to improve future queries.",
        UpdateColumnInfoArgs.class,
        "{\"readOnlyHint\":false,\"destructiveHint\":false,\"idempotentHint\":true,\"openWorldHint\":true}",
        false
    ),
    UPSERT_TERM(
        "upsert_business_term",
        "Upsert Business Term",
        "Create or update a business glossary term (definition, calculation rules, or aliases) under a subject after analysis.",
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
