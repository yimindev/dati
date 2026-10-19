package com.dati.mcp.domain.service;

import com.dati.db.analysis.TableRef;
import com.dati.mcp.domain.model.McpDataScopeAccessMode;
import com.dati.mcp.domain.model.McpDataScopeType;
import com.dati.mcp.domain.model.McpServiceDataScope;
import jakarta.annotation.Nullable;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Resolves the schema-level portion of an MCP service data scope. */
final class McpDataScopeSchemas {

    private McpDataScopeSchemas() {
    }

    /** Union of all SCHEMA_SCOPE data source entries for the given data source. */
    static Set<String> allowedSchemas(List<McpServiceDataScope> scopeItems, String dsId) {
        Set<String> schemas = new LinkedHashSet<>();
        if (scopeItems == null) {
            return schemas;
        }

        for (McpServiceDataScope scope : scopeItems) {
            if (scope.getScopeType() != McpDataScopeType.DATA_SOURCE
                    || !Objects.equals(scope.getReferenceId(), dsId)
                    || scope.getAccessMode() != McpDataScopeAccessMode.SCHEMA_SCOPE
                    || scope.getAllowedSchemas() == null) {
                continue;
            }
            for (String schema : scope.getAllowedSchemas()) {
                String normalized = normalize(schema);
                if (normalized != null) {
                    schemas.add(normalized);
                }
            }
        }
        return schemas;
    }

    @Nullable
    static String effectiveSchema(TableRef ref, @Nullable String defaultSchema) {
        return effectiveSchema(ref.schema(), defaultSchema);
    }

    @Nullable
    static String effectiveSchema(@Nullable String schema, @Nullable String defaultSchema) {
        String normalized = normalize(schema);
        return normalized != null ? normalized : normalize(defaultSchema);
    }

    @Nullable
    private static String normalize(@Nullable String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
