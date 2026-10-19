package com.dati.mcp.repository.converter;

import com.dati.mcp.domain.model.McpDataScopeAccessMode;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Stores the access mode as a plain string instead of a database enum.
 * This keeps schema evolution (enum value renames) independent of DDL auto-update
 * and avoids database-specific enum constraints.
 */
@Converter
public class McpDataScopeAccessModeConverter
        implements AttributeConverter<McpDataScopeAccessMode, String> {

    @Override
    public String convertToDatabaseColumn(McpDataScopeAccessMode attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public McpDataScopeAccessMode convertToEntityAttribute(String dbData) {
        return dbData == null ? null : McpDataScopeAccessMode.valueOf(dbData);
    }
}
