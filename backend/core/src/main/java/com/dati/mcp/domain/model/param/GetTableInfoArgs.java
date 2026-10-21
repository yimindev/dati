package com.dati.mcp.domain.model.param;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/** GET_TABLE_INFO tool parameters (decision 12: data_source_id inside each tables[] item). */
public record GetTableInfoArgs(
    @NotNull(message = "is required") @Size(min = 1, max = 20, message = "must contain between 1 and 20 items")
    @JsonPropertyDescription("Tables to describe (1-20)")
    List<TableRef> tables
) {
    public record TableRef(
        @JsonProperty("data_source_id") @NotNull(message = "is required") @NotBlank(message = "must not be blank")
        @JsonPropertyDescription("Data source ID")
        String dataSourceId,

        @JsonPropertyDescription("Table schema (optional)")
        String schema,

        @JsonProperty("table") @NotNull(message = "is required") @NotBlank(message = "must not be blank")
        @JsonPropertyDescription("Table name")
        String table,

        @Size(max = 100, message = "must contain at most 100 columns")
        @JsonPropertyDescription("Optional column name filter")
        List<@Size(max = 100, message = "must be at most 100 characters") String> fields
    ) {}
}
