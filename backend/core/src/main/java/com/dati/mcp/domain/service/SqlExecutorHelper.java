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
 */
final class SqlExecutorHelper {

    private SqlExecutorHelper() {}

    static List<StatementResult> collect(Statement stmt) throws SQLException {
        List<StatementResult> results = new ArrayList<>();
        boolean isResultSet = stmt.getResultSet() != null;

        while (true) {
            try {
                if (isResultSet) {
                    ResultSet rs = stmt.getResultSet();
                    results.add(collectSelect(rs));
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

    private static StatementResult.SelectResult collectSelect(ResultSet rs) throws SQLException {
        try (rs) {
            ResultSetMetaData meta = rs.getMetaData();
            int colCount = meta.getColumnCount();
            List<String> columns = new ArrayList<>(colCount);
            for (int i = 1; i <= colCount; i++) {
                columns.add(meta.getColumnLabel(i));
            }
            List<List<Object>> rows = new ArrayList<>();
            while (rs.next()) {
                List<Object> row = new ArrayList<>(colCount);
                for (int i = 1; i <= colCount; i++) {
                    row.add(rs.getObject(i));
                }
                rows.add(row);
            }
            return StatementResult.select(columns, rows, rows.size());
        }
    }
}
