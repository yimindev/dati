package com.dati.mcp.server.pojo;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ToolTestResponse(
    boolean success, long executionTimeMs, ToolTestData data, ToolTestError error
) {}
