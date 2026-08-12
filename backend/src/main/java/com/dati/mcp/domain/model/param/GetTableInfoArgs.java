package com.dati.mcp.domain.model.param;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/** GET_TABLE_INFO tool parameters (decision 12: data_source_id inside each tables[] item). */
public record GetTableInfoArgs(
    @NotNull @Size(min = 1, max = 20)
    @JsonPropertyDescription("Tables to describe (1-20)")
    List<TableRef> tables
) {
    public record TableRef(
        @JsonProperty("data_source_id") @NotNull @NotBlank
        @JsonPropertyDescription("Data source ID")
        String dataSourceId,

        @JsonPropertyDescription("Table schema; defaults to the data source default schema")
        String schema,

        @JsonProperty("table") @NotNull @NotBlank
        @JsonPropertyDescription("Table name")
        String table,

        @Size(max = 100)
        @JsonPropertyDescription("Optional list of column names to retrieve")
        List<@Size(max = 100) String> fields
    ) {}
}
