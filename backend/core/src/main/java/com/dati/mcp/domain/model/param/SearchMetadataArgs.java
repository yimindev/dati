package com.dati.mcp.domain.model.param;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/** SEARCH_METADATA tool parameters (single source of truth for schema + validation). */
public record SearchMetadataArgs(
    @NotNull @Size(min = 1)
    @JsonPropertyDescription("List of search keywords or business terms. Matched using OR logic (feel free to provide multiple relevant terms or synonyms).")
    List<@NotBlank String> keywords
) {}
