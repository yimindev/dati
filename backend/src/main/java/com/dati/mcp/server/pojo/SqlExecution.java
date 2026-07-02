package com.dati.mcp.server.pojo;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.annotation.Nullable;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SqlExecution(String executedSql, List<StatementResult> results,
                           @Nullable List<Object> bindings) implements ToolTestData {}
