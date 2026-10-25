package com.dati.mcp.domain.service;

import com.dati.mcp.server.pojo.StatementResult;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Collects results from an already-executed {@link Statement}.
 * Callers are responsible for calling {@link Statement#execute(String)}
 * or {@link java.sql.PreparedStatement#execute()} and passing the
 * {@link Statement} here for result iteration, using the standard
 * JDBC {@code getMoreResults / getResultSet / getUpdateCount} loop.
 * <p>
 * Row limits are honoured by probing one row past the limit: the caller sets
 * {@code maxRows} to {@link #probeRowLimit(int)} so that a result set holding
 * exactly {@code rowLimit + 1} rows proves the query matched more rows than
 * returned, which is reported as {@code truncated}.
 */
final class SqlExecutorHelper {

    private SqlExecutorHelper() {}

    static List<StatementResult> collect(Statement stmt, int rowLimit) throws SQLException {
        List<StatementResult> results = new ArrayList<>();
        boolean isResultSet = stmt.getResultSet() != null;

        while (true) {
            try {
                if (isResultSet) {
                    ResultSet rs = stmt.getResultSet();
                    results.add(collectSelect(rs, rowLimit));
                } else {
                    int count = stmt.getUpdateCount();
                    if (count >= 0) {
                        results.add(StatementResult.write(count));
                    }
                }
            } catch (SQLException e) {
                results.add(isResultSet
                    ? StatementResult.selectFailure(e.getMessage())
                    : StatementResult.writeFailure(e.getMessage()));
            }
            if (stmt.getMoreResults() || stmt.getUpdateCount() >= 0) {
                isResultSet = stmt.getResultSet() != null;
            } else {
                break;
            }
        }
        return results;
    }

    /**
     * Returns the JDBC {@code maxRows} used for the SELECT probe: one row beyond the
     * configured limit, so the collector can tell "exactly at the limit" from
     * "more rows match". A non-positive limit means unlimited and is passed through.
     */
    static int probeRowLimit(int configuredMaxRows) {
        if (configuredMaxRows <= 0 || configuredMaxRows == Integer.MAX_VALUE) {
            return Math.max(configuredMaxRows, 0);
        }
        return configuredMaxRows + 1;
    }

    private static StatementResult.SelectResult collectSelect(ResultSet rs, int rowLimit)
            throws SQLException {
        try (rs) {
            ResultSetMetaData meta = rs.getMetaData();
            int colCount = meta.getColumnCount();
            List<String> columns = new ArrayList<>(colCount);
            for (int i = 1; i <= colCount; i++) {
                columns.add(meta.getColumnLabel(i));
            }
            List<List<Object>> rows = new ArrayList<>();
            boolean truncated = false;
            while (rs.next()) {
                if (rowLimit > 0 && rows.size() == rowLimit) {
                    // This row is the probe: it exists, so the query matched more than the limit.
                    truncated = true;
                    break;
                }
                List<Object> row = new ArrayList<>(colCount);
                for (int i = 1; i <= colCount; i++) {
                    row.add(rs.getObject(i));
                }
                rows.add(row);
            }
            return StatementResult.select(columns, rows, truncated);
        }
    }
}
