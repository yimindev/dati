package com.dati.mcp.domain.model.param;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/** UPDATE_TABLE_INFO tool parameters. */
public record UpdateTableInfoArgs(
    @NotNull @Size(min = 1, max = 20) @Valid
    @JsonPropertyDescription("Tables to update (1-20)")
    List<UpdateTableItem> tables
) {
    public record UpdateTableItem(
        @JsonProperty("data_source_id") @NotNull @NotBlank
        @JsonPropertyDescription("Data source ID")
        String dataSourceId,

        @JsonPropertyDescription("Table schema; defaults to the data source default schema")
        String schema,

        @JsonProperty("table") @NotNull @NotBlank
        @JsonPropertyDescription("Table name")
        String table,

        @Size(max = 500)
        @JsonPropertyDescription("New table description; replaces the existing one")
        String description,

        @Size(max = 20)
        @JsonPropertyDescription("Complete alias list; REPLACES all existing aliases")
        List<@Size(max = 100) String> aliases
    ) {}
}
