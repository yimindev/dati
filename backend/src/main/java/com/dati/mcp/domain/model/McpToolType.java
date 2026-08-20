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
        "Search metadata across data sources based on keywords. Returns matching tables, columns, and terms grouped by data source.",
        SearchMetadataArgs.class,
        "{\"readOnlyHint\":true}",
        true
    ),
    GET_TABLE_INFO(
        "get_table_info",
        "Get Table Info",
        "Get detailed information about a specific table, including column names, types, descriptions, and related terms. Data is based on synced metadata. For latest structure, consider querying information_schema.",
        GetTableInfoArgs.class,
        "{\"readOnlyHint\":true}",
        true
    ),
    LIST_TABLES(
        "list_tables",
        "List Tables",
        "List all tables in the service's data scope with schema, description and aliases. Table-level overview only — no columns. Use get_table_info to inspect a specific table.",
        ListTablesArgs.class,
        "{\"readOnlyHint\":true}",
        true
    ),
    EXECUTE_SQL(
        "execute_sql",
        "Execute SQL",
        "Execute an arbitrary SQL statement against a data source within the service's data scope. Permissions (SELECT/INSERT/UPDATE/DELETE/DDL) are configurable per service.",
        ExecuteSqlArgs.class,
        null,
        true
    ),
    UPDATE_TABLE_INFO(
        "update_table_info",
        "Update Table Metadata",
        "Update table metadata (description, aliases) in the shared metadata store. `aliases` REPLACES the entire existing list — query current values via get_table_info first. Changes are shared by all services and take effect immediately; only write facts you are confident about.",
        UpdateTableInfoArgs.class,
        "{\"readOnlyHint\":false,\"destructiveHint\":false,\"idempotentHint\":true,\"openWorldHint\":true}",
        false
    ),
    UPDATE_COLUMN_INFO(
        "update_column_info",
        "Update Column Metadata",
        "Update column metadata (description, aliases) in the shared metadata store. `aliases` REPLACES the entire existing list — query current values via get_table_info first. Column value semantics (enum meanings, units, formats) are the most valuable knowledge to add. Changes are shared by all services and take effect immediately.",
        UpdateColumnInfoArgs.class,
        "{\"readOnlyHint\":false,\"destructiveHint\":false,\"idempotentHint\":true,\"openWorldHint\":true}",
        false
    ),
    UPSERT_TERM(
        "upsert_term",
        "Upsert Business Term",
        "Create or update a business term (business word ↔ platform term) in the shared metadata store. Terms belong to a subject; `subject_name` locates the subject by name (first match within service scope). Creates the term when missing, otherwise updates description/aliases. Changes are shared by all services and take effect immediately.",
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
