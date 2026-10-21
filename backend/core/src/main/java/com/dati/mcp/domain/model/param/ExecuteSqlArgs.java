package com.dati.mcp.domain.model.param;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** EXECUTE_SQL tool parameters (single source of truth for schema + validation). */
public record ExecuteSqlArgs(
    @JsonProperty("data_source_id") @NotNull(message = "is required") @NotBlank(message = "must not be blank")
    @JsonPropertyDescription("Data source ID")
    String dataSourceId,

    @JsonProperty("sql") @NotNull(message = "is required") @NotBlank(message = "must not be blank")
    @JsonPropertyDescription("SQL statement to execute")
    String sql
) {}
