package com.dati.mcp.domain.model.param;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/** UPDATE_COLUMN_INFO tool parameters. */
public record UpdateColumnInfoArgs(
    @NotNull @Size(min = 1, max = 20) @Valid
    @JsonPropertyDescription("Columns to update (1-20)")
    List<UpdateColumnItem> columns
) {
    public record UpdateColumnItem(
        @JsonProperty("data_source_id") @NotNull @NotBlank
        @JsonPropertyDescription("Data source ID")
        String dataSourceId,

        @JsonPropertyDescription("Table schema (optional)")
        String schema,

        @JsonProperty("table") @NotNull @NotBlank
        @JsonPropertyDescription("Table name")
        String table,

        @JsonProperty("column") @NotNull @NotBlank
        @JsonPropertyDescription("Column name")
        String column,

        @Size(max = 500)
        @JsonPropertyDescription("New description (omit to keep current)")
        String description,

        @Size(max = 20)
        @JsonPropertyDescription("Complete alias list, replaces existing (omit to keep current)")
        List<@Size(max = 100) String> aliases
    ) {}
}
