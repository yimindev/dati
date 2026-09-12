package com.dati.mcp.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("McpToolType tests")
class McpToolTypeTest {

    @Test
    @DisplayName("SEARCH_METADATA has updated toolName, title, and guidance description")
    void searchMetadataDefinitions() {
        McpToolType type = McpToolType.SEARCH_METADATA;
        assertThat(type.getToolName()).isEqualTo("search_tables_and_terms");
        assertThat(type.getTitle()).isEqualTo("Search Tables and Terms");
        assertThat(type.getDescription()).isEqualTo(
            "Search relevant database tables, column names, sample values, and business terms by keywords. Always use this tool first to discover the right tables and terms instead of guessing tables or running exploratory SQL."
        );
        assertThat(type.isPrebuilt()).isTrue();
        assertThat(type.isDefaultEnabled()).isTrue();
    }

    @Test
    @DisplayName("GET_TABLE_INFO has updated toolName, title, and description")
    void getTableInfoDefinitions() {
        McpToolType type = McpToolType.GET_TABLE_INFO;
        assertThat(type.getToolName()).isEqualTo("get_table_schema");
        assertThat(type.getTitle()).isEqualTo("Get Table Schema");
        assertThat(type.getDescription()).isEqualTo(
            "Get detailed column schemas (names, types, comments, and sample values) for specified tables (up to 20 tables)."
        );
        assertThat(type.isPrebuilt()).isTrue();
        assertThat(type.isDefaultEnabled()).isTrue();
    }

    @Test
    @DisplayName("LIST_TABLES has updated toolName, title, and guidance description")
    void listTablesDefinitions() {
        McpToolType type = McpToolType.LIST_TABLES;
        assertThat(type.getToolName()).isEqualTo("list_tables_and_terms");
        assertThat(type.getTitle()).isEqualTo("List Tables and Terms");
        assertThat(type.getDescription()).isEqualTo(
            "List all available tables (with schema, name, and description; overview only, no columns) and business terms under the service scope. Use this tool when exploring the catalog or when keyword search returns no matches."
        );
        assertThat(type.isPrebuilt()).isTrue();
        assertThat(type.isDefaultEnabled()).isTrue();
    }

    @Test
    @DisplayName("EXECUTE_SQL retains toolName and title with fallback guidance description")
    void executeSqlDefinitions() {
        McpToolType type = McpToolType.EXECUTE_SQL;
        assertThat(type.getToolName()).isEqualTo("execute_sql");
        assertThat(type.getTitle()).isEqualTo("Execute SQL");
        assertThat(type.getDescription()).isEqualTo(
            "Execute an SQL query or statement against a data source and return result rows. Use this tool only when no specialized or custom analysis tool is available for your task."
        );
        assertThat(type.isPrebuilt()).isTrue();
        assertThat(type.isDefaultEnabled()).isTrue();
    }

    @Test
    @DisplayName("UPDATE_TABLE_INFO has updated toolName, title, and description")
    void updateTableInfoDefinitions() {
        McpToolType type = McpToolType.UPDATE_TABLE_INFO;
        assertThat(type.getToolName()).isEqualTo("update_table_metadata");
        assertThat(type.getTitle()).isEqualTo("Update Table Metadata");
        assertThat(type.getDescription()).isEqualTo(
            "Enrich table description or aliases after analysis to improve future queries and search accuracy."
        );
        assertThat(type.isPrebuilt()).isTrue();
        assertThat(type.isDefaultEnabled()).isFalse();
    }

    @Test
    @DisplayName("UPDATE_COLUMN_INFO has updated toolName, title, and description")
    void updateColumnInfoDefinitions() {
        McpToolType type = McpToolType.UPDATE_COLUMN_INFO;
        assertThat(type.getToolName()).isEqualTo("update_column_metadata");
        assertThat(type.getTitle()).isEqualTo("Update Column Metadata");
        assertThat(type.getDescription()).isEqualTo(
            "Enrich column descriptions (e.g. business meanings, enum mappings) or aliases after analysis to improve future queries."
        );
        assertThat(type.isPrebuilt()).isTrue();
        assertThat(type.isDefaultEnabled()).isFalse();
    }

    @Test
    @DisplayName("UPSERT_TERM has updated toolName, title, and description")
    void upsertTermDefinitions() {
        McpToolType type = McpToolType.UPSERT_TERM;
        assertThat(type.getToolName()).isEqualTo("upsert_business_term");
        assertThat(type.getTitle()).isEqualTo("Upsert Business Term");
        assertThat(type.getDescription()).isEqualTo(
            "Create or update a business glossary term (definition, calculation rules, or aliases) under a subject after analysis."
        );
        assertThat(type.isPrebuilt()).isTrue();
        assertThat(type.isDefaultEnabled()).isFalse();
    }
}
