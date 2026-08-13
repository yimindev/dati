package com.dati.mcp.domain.model.param;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/** UPSERT_TERM tool parameters. */
public record UpsertTermArgs(
    @NotNull @Size(min = 1, max = 20) @Valid
    @JsonPropertyDescription("Terms to upsert (1-20)")
    List<UpsertTermItem> terms
) {
    public record UpsertTermItem(
        @JsonProperty("subject_name") @NotNull @NotBlank @Size(max = 200)
        @JsonPropertyDescription("Subject name the term belongs to (from SEARCH_METADATA results)")
        String subjectName,

        @JsonProperty("name") @NotNull @NotBlank @Size(max = 200)
        @JsonPropertyDescription("Term name, e.g. the business word used by the user")
        String name,

        @Size(max = 500)
        @JsonPropertyDescription("Term description; replaces the existing one")
        String description,

        @Size(max = 20)
        @JsonPropertyDescription("Complete alias list; REPLACES all existing aliases")
        List<@Size(max = 100) String> aliases
    ) {}
}
