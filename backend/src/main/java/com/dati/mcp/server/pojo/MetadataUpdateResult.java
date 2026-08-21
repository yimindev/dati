package com.dati.mcp.server.pojo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * Per-item result of a metadata update tool (UPDATE_TABLE_INFO /
 * UPDATE_COLUMN_INFO / UPSERT_TERM). `old`/`new` hold {description, aliases}
 * maps; `new` is expressed via @JsonProperty because `new` is a Java keyword.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record MetadataUpdateResult(
    String entityType,
    String entity,
    boolean success,
    String changeType,
    Map<String, Object> old,
    @JsonProperty("new") Map<String, Object> newValue,
    MetadataUpdateError error
) {
    /** Failure detail for an item; null when the item succeeded. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record MetadataUpdateError(String errorCategory, String message) {}
}
