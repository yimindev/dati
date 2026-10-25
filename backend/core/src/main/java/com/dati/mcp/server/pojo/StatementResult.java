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

    /**
     * Builds a successful SELECT result. Truncation is derived from the row limit the
     * executor applied, which is discovered by probing one row past that limit.
     */
    static SelectResult select(List<String> columns, List<List<Object>> rows, boolean truncated) {
        return new SelectResult(true, columns, rows, rows.size(), truncated,
            truncated ? truncationNotice(rows.size()) : null, null);
    }

    private static String truncationNotice(int rowLimit) {
        return "Truncated to the first " + rowLimit
            + " rows (row limit). More rows match this query: add LIMIT/OFFSET, filters,"
            + " or aggregate in SQL (COUNT/SUM) for exact results.";
    }

    static SelectResult selectFailure(String errorMessage) {
        return new SelectResult(false, null, null, 0, false, null, errorMessage);
    }

    static WriteResult write(int affectedRows) {
        return new WriteResult(true, affectedRows, null);
    }

    static WriteResult writeFailure(String errorMessage) {
        return new WriteResult(false, 0, errorMessage);
    }

    /**
     * @param returnedRows number of rows actually returned (equals the row limit when truncated)
     * @param truncated    true when the query matched more rows than the limit allowed to return
     * @param notice       LLM-facing explanation, present only when truncated
     */
    record SelectResult(boolean success, List<String> columns, List<List<Object>> rows,
                        int returnedRows, boolean truncated, String notice,
                        String errorMessage) implements StatementResult {
        public String type() { return "SELECT"; }
    }

    record WriteResult(boolean success, int affectedRows,
                       String errorMessage) implements StatementResult {
        public String type() { return "WRITE"; }
    }
}
