package com.dati.mcp.domain.service;

import com.dati.mcp.server.pojo.StatementResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("SqlExecutorHelper tests")
class SqlExecutorHelperTest {

    private Statement stmtWithRows(ResultSet rs) throws SQLException {
        Statement stmt = mock(Statement.class);
        when(stmt.getResultSet()).thenReturn(rs);
        when(stmt.getUpdateCount()).thenReturn(-1);
        when(stmt.getMoreResults()).thenReturn(false);
        return stmt;
    }

    /** Builds a single-column (id) result set with the given number of rows. */
    private ResultSet rsWithRows(int totalRows) throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        ResultSetMetaData meta = mock(ResultSetMetaData.class);
        when(rs.getMetaData()).thenReturn(meta);
        when(meta.getColumnCount()).thenReturn(1);
        when(meta.getColumnLabel(1)).thenReturn("id");

        Boolean[] sequence = new Boolean[totalRows + 1];
        for (int i = 0; i < totalRows; i++) {
            sequence[i] = true;
        }
        sequence[totalRows] = false;
        when(rs.next()).thenReturn(sequence[0], java.util.Arrays.copyOfRange(sequence, 1, sequence.length));
        when(rs.getObject(1)).thenReturn(1);
        return rs;
    }

    @Test
    @DisplayName("rows beyond the limit are dropped and the result is marked truncated")
    void truncatesWhenMoreRowsExistThanLimit() throws Exception {
        ResultSet rs = rsWithRows(3);

        List<StatementResult> results = SqlExecutorHelper.collect(stmtWithRows(rs), 2);

        StatementResult.SelectResult select = (StatementResult.SelectResult) results.getFirst();
        assertThat(select.rows()).hasSize(2);
        assertThat(select.returnedRows()).isEqualTo(2);
        assertThat(select.truncated()).isTrue();
        assertThat(select.notice()).contains("Truncated to the first 2 rows");
    }

    @Test
    @DisplayName("result sets exactly at the limit are not marked truncated")
    void doesNotMarkTruncatedWhenRowCountEqualsLimit() throws Exception {
        ResultSet rs = rsWithRows(2);

        List<StatementResult> results = SqlExecutorHelper.collect(stmtWithRows(rs), 2);

        StatementResult.SelectResult select = (StatementResult.SelectResult) results.getFirst();
        assertThat(select.rows()).hasSize(2);
        assertThat(select.returnedRows()).isEqualTo(2);
        assertThat(select.truncated()).isFalse();
        assertThat(select.notice()).isNull();
    }

    @Test
    @DisplayName("non-positive limit returns every row and is never truncated")
    void unlimitedWhenLimitIsNonPositive() throws Exception {
        ResultSet rs = rsWithRows(5);

        List<StatementResult> results = SqlExecutorHelper.collect(stmtWithRows(rs), 0);

        StatementResult.SelectResult select = (StatementResult.SelectResult) results.getFirst();
        assertThat(select.rows()).hasSize(5);
        assertThat(select.returnedRows()).isEqualTo(5);
        assertThat(select.truncated()).isFalse();
    }

    @Test
    @DisplayName("probe limit fetches one extra row, zero stays unlimited")
    void probeLimitFetchesOneExtraRow() {
        assertThat(SqlExecutorHelper.probeRowLimit(1000)).isEqualTo(1001);
        assertThat(SqlExecutorHelper.probeRowLimit(1)).isEqualTo(2);
        assertThat(SqlExecutorHelper.probeRowLimit(0)).isZero();
        assertThat(SqlExecutorHelper.probeRowLimit(-1)).isZero();
        assertThat(SqlExecutorHelper.probeRowLimit(Integer.MAX_VALUE)).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    @DisplayName("write results are reported with affected rows")
    void reportsWriteResults() throws Exception {
        Statement stmt = mock(Statement.class);
        when(stmt.getResultSet()).thenReturn(null);
        // First call reports the affected-row count, every later call reports "no more results".
        when(stmt.getUpdateCount()).thenReturn(4, -1, -1);
        when(stmt.getMoreResults()).thenReturn(false, false);

        List<StatementResult> results = SqlExecutorHelper.collect(stmt, 1000);

        assertThat(results).hasSize(1);
        StatementResult.WriteResult write = (StatementResult.WriteResult) results.getFirst();
        assertThat(write.affectedRows()).isEqualTo(4);
        assertThat(write.success()).isTrue();
    }
}
