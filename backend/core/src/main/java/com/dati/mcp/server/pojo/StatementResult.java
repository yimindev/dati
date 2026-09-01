package com.dati.mcp.server.pojo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public interface StatementResult {
    @JsonProperty("type")
    String type();
    boolean success();
    String errorMessage();

    static SelectResult select(List<String> columns, List<List<Object>> rows, int totalRows) {
        return new SelectResult(true, columns, rows, totalRows, null);
    }

    static SelectResult selectFailure(String errorMessage) {
        return new SelectResult(false, null, null, 0, errorMessage);
    }

    static WriteResult write(int affectedRows) {
        return new WriteResult(true, affectedRows, null);
    }

    static WriteResult writeFailure(String errorMessage) {
        return new WriteResult(false, 0, errorMessage);
    }

    record SelectResult(boolean success, List<String> columns, List<List<Object>> rows,
                        int totalRows, String errorMessage) implements StatementResult {
        public String type() { return "SELECT"; }
    }

    record WriteResult(boolean success, int affectedRows,
                       String errorMessage) implements StatementResult {
        public String type() { return "WRITE"; }
    }
}
