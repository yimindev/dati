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
    @NotNull(message = "is required") @Size(min = 1, max = 20, message = "must contain between 1 and 20 items") @Valid
    @JsonPropertyDescription("Terms to upsert (1-20)")
    List<UpsertTermItem> terms
) {
    public record UpsertTermItem(
        @JsonProperty("subject_name") @NotNull(message = "is required") @NotBlank(message = "must not be blank") @Size(max = 200, message = "must be at most 200 characters")
        @JsonPropertyDescription("Subject name the term belongs to")
        String subjectName,

        @JsonProperty("name") @NotNull(message = "is required") @NotBlank(message = "must not be blank") @Size(max = 200, message = "must be at most 200 characters")
        @JsonPropertyDescription("Term name")
        String name,

        @Size(max = 500, message = "must be at most 500 characters")
        @JsonPropertyDescription("Term description (omit to keep current)")
        String description,

        @Size(max = 20, message = "must contain at most 20 items")
        @JsonPropertyDescription("Complete alias list, replaces existing (omit to keep current)")
        List<@Size(max = 100, message = "must be at most 100 characters") String> aliases
    ) {}
}
