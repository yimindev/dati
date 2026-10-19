package com.dati.mcp.domain.service;

import com.dati.db.analysis.TableRef;
import com.dati.mcp.domain.model.McpDataScopeAccessMode;
import com.dati.mcp.domain.model.McpDataScopeType;
import com.dati.mcp.domain.model.McpServiceDataScope;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("McpDataScopeSchemas tests")
class McpDataScopeSchemasTest {

    private static final String DS_ID = "ds-1";

    private static McpServiceDataScope scope(McpDataScopeType type, String dsId,
                                             McpDataScopeAccessMode mode, List<String> schemas) {
        McpServiceDataScope scope = new McpServiceDataScope();
        scope.setScopeType(type);
        scope.setReferenceId(dsId);
        scope.setAccessMode(mode);
        scope.setAllowedSchemas(schemas);
        return scope;
    }

    @Test
    @DisplayName("returns empty when scope items are null")
    void nullScopeItemsReturnsEmpty() {
        assertThat(McpDataScopeSchemas.allowedSchemas(null, DS_ID)).isEmpty();
    }

    @Test
    @DisplayName("returns empty when no schema scoped data source matches")
    void noMatchingSchemaScopeReturnsEmpty() {
        List<McpServiceDataScope> scopes = List.of(
            scope(McpDataScopeType.DATA_SOURCE, DS_ID, McpDataScopeAccessMode.IMPORTED_TABLES, List.of("public")),
            scope(McpDataScopeType.DATA_SOURCE, "ds-other", McpDataScopeAccessMode.SCHEMA_SCOPE, List.of("sandbox")),
            scope(McpDataScopeType.SUBJECT, DS_ID, McpDataScopeAccessMode.SCHEMA_SCOPE, List.of("public"))
        );

        assertThat(McpDataScopeSchemas.allowedSchemas(scopes, DS_ID)).isEmpty();
    }

    @Test
    @DisplayName("unions schema scopes, trims blanks and preserves first-seen order")
    void unionsAndNormalizesSchemas() {
        List<McpServiceDataScope> scopes = List.of(
            scope(McpDataScopeType.DATA_SOURCE, DS_ID, McpDataScopeAccessMode.SCHEMA_SCOPE,
                List.of(" sandbox ", "public", "sandbox", " ")),
            scope(McpDataScopeType.DATA_SOURCE, DS_ID, McpDataScopeAccessMode.SCHEMA_SCOPE,
                Arrays.asList("audit", null, "public"))
        );

        assertThat(McpDataScopeSchemas.allowedSchemas(scopes, DS_ID))
            .containsExactly("sandbox", "public", "audit");
    }

    @Test
    @DisplayName("null allowed schema list is ignored")
    void nullAllowedSchemasIgnored() {
        List<McpServiceDataScope> scopes = List.of(
            scope(McpDataScopeType.DATA_SOURCE, DS_ID, McpDataScopeAccessMode.SCHEMA_SCOPE, null)
        );

        assertThat(McpDataScopeSchemas.allowedSchemas(scopes, DS_ID)).isEmpty();
    }

    @Test
    @DisplayName("effective schema prefers explicit schema over default schema")
    void effectiveSchemaPrefersExplicit() {
        assertThat(McpDataScopeSchemas.effectiveSchema(new TableRef("audit", "orders"), "public"))
            .isEqualTo("audit");
    }

    @Test
    @DisplayName("effective schema falls back to default schema")
    void effectiveSchemaFallsBackToDefault() {
        assertThat(McpDataScopeSchemas.effectiveSchema(new TableRef(null, "orders"), "public"))
            .isEqualTo("public");
    }

    @Test
    @DisplayName("effective schema ignores blank values")
    void effectiveSchemaIgnoresBlankValues() {
        assertThat(McpDataScopeSchemas.effectiveSchema(new TableRef(" ", "orders"), "public"))
            .isEqualTo("public");
        assertThat(McpDataScopeSchemas.effectiveSchema(new TableRef(null, "orders"), " "))
            .isNull();
        assertThat(McpDataScopeSchemas.effectiveSchema((String) null, null)).isNull();
    }
}
