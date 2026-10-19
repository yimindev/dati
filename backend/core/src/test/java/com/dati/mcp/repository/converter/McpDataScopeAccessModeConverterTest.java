package com.dati.mcp.repository.converter;

import com.dati.mcp.domain.model.McpDataScopeAccessMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("McpDataScopeAccessModeConverter tests")
class McpDataScopeAccessModeConverterTest {

    private final McpDataScopeAccessModeConverter converter = new McpDataScopeAccessModeConverter();

    @Test
    @DisplayName("converts enum to string and back")
    void roundTrip() {
        assertThat(converter.convertToDatabaseColumn(McpDataScopeAccessMode.SCHEMA_SCOPE))
                .isEqualTo("SCHEMA_SCOPE");
        assertThat(converter.convertToEntityAttribute("SCHEMA_SCOPE"))
                .isEqualTo(McpDataScopeAccessMode.SCHEMA_SCOPE);
        assertThat(converter.convertToDatabaseColumn(McpDataScopeAccessMode.IMPORTED_TABLES))
                .isEqualTo("IMPORTED_TABLES");
        assertThat(converter.convertToEntityAttribute("IMPORTED_TABLES"))
                .isEqualTo(McpDataScopeAccessMode.IMPORTED_TABLES);
    }

    @Test
    @DisplayName("keeps null as null")
    void keepsNull() {
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }
}
