package com.dati.mcp.domain.service;

import com.dati.common.template.HandlebarsStyleParser;
import com.dati.common.template.SqlRenderer;
import com.dati.datasource.domain.model.DataSource;
import com.dati.datasource.domain.service.DataSourceService;
import com.dati.db.HikariPoolManager;
import com.dati.db.JdbcConnector;
import com.dati.db.analysis.TableRef;
import com.dati.mcp.domain.model.McpToolType;
import com.dati.mcp.domain.model.ToolConfig;
import com.dati.mcp.domain.model.ToolError;
import com.dati.mcp.server.pojo.SqlExecution;
import com.dati.mcp.server.pojo.ToolTestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Executor behavior tests for PARAMETERIZED_SQL.
 * <p>
 * Core regression anchor: the tool config carries NO runtime SQL policy
 * (sqlPolicy removed) — a template rendering to DELETE/INSERT etc. must
 * execute without permission checks. Scope validation and error mapping
 * are still enforced.
 */
@ExtendWith(MockitoExtension.class)
class ParameterizedSqlExecutorTest {

    @Mock
    private ScopeValidator scopeValidator;

    @Mock
    private DataSourceService dataSourceService;

    private ParameterizedSqlExecutor executor;

    @BeforeEach
    void setUp() {
        // mocks are injected by MockitoExtension before this runs
        executor = new ParameterizedSqlExecutor(
            scopeValidator, dataSourceService, new HandlebarsStyleParser(), new SqlRenderer());
    }

    private ToolConfig.ParamSqlConfig paramSqlConfig(String sqlTemplate) {
        ToolConfig.ParamSqlConfig cfg = new ToolConfig.ParamSqlConfig();
        cfg.setDataSourceId("ds-1");
        cfg.setSqlTemplate(sqlTemplate);
        return cfg;
    }

    private DataSource dataSource() {
        DataSource ds = new DataSource();
        ds.setId("ds-1");
        ds.setDefaultSchema("public");
        return ds;
    }

    private PreparedStatement mockStatementNoResults() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        when(stmt.getResultSet()).thenReturn(null);
        when(stmt.getUpdateCount()).thenReturn(-1);
        when(stmt.getMoreResults()).thenReturn(false);
        return stmt;
    }

    @Test
    @DisplayName("execute - DELETE template runs without policy check (no SQL_POLICY_VIOLATION)")
    void execute_deleteTemplate_noPolicyBlock() throws SQLException {
        when(dataSourceService.getDataSource("ds-1")).thenReturn(Optional.of(dataSource()));
        PreparedStatement stmt = mockStatementNoResults();
        Connection conn = mock(Connection.class);
        when(conn.prepareStatement(anyString())).thenReturn(stmt);

        ToolExecutionContext ctx = new ToolExecutionContext(
            "svc-1", McpToolType.PARAMETERIZED_SQL,
            paramSqlConfig("DELETE FROM tasks WHERE id = {{id}}"),
            Map.of("id", 1), List.of());

        try (MockedStatic<HikariPoolManager> hpm = mockStatic(HikariPoolManager.class)) {
            hpm.when(() -> HikariPoolManager.getConnection(any(JdbcConnector.class))).thenReturn(conn);

            ToolTestData data = executor.execute(ctx);

            assertThat(data).isInstanceOf(SqlExecution.class);
            SqlExecution exec = (SqlExecution) data;
            assertThat(exec.executedSql()).isEqualTo("DELETE FROM tasks WHERE id = ?");
            assertThat(exec.results()).isEmpty();
        }

        // table-level scope check still runs against the rendered SQL
        verify(scopeValidator).validate(eq(List.of()), eq("ds-1"),
            argThat(tables -> tables.contains(new TableRef(null, "tasks"))), eq("public"));
    }

    @Test
    @DisplayName("execute - scope violation still enforced")
    void execute_scopeViolation_stillEnforced() {
        when(dataSourceService.getDataSource("ds-1")).thenReturn(Optional.of(dataSource()));
        doThrow(new ToolExecuteException(ToolError.SCOPE_VIOLATION, "table 'tasks' not in scope"))
            .when(scopeValidator).validate(any(), any(), any(), any());

        ToolExecutionContext ctx = new ToolExecutionContext(
            "svc-1", McpToolType.PARAMETERIZED_SQL,
            paramSqlConfig("SELECT * FROM tasks"),
            Map.of(), List.of());

        assertThatThrownBy(() -> executor.execute(ctx))
            .isInstanceOf(ToolExecuteException.class)
            .satisfies(e -> assertThat(((ToolExecuteException) e).getToolError())
                .isEqualTo(ToolError.SCOPE_VIOLATION));
    }

    @Test
    @DisplayName("execute - SQL exception mapped to SQL_EXECUTION_ERROR")
    void execute_sqlException_mappedToSqlExecutionError() throws SQLException {
        when(dataSourceService.getDataSource("ds-1")).thenReturn(Optional.of(dataSource()));
        PreparedStatement stmt = mock(PreparedStatement.class);
        when(stmt.execute()).thenThrow(new SQLException("boom"));
        Connection conn = mock(Connection.class);
        when(conn.prepareStatement(anyString())).thenReturn(stmt);

        ToolExecutionContext ctx = new ToolExecutionContext(
            "svc-1", McpToolType.PARAMETERIZED_SQL,
            paramSqlConfig("SELECT * FROM tasks"),
            Map.of(), List.of());

        try (MockedStatic<HikariPoolManager> hpm = mockStatic(HikariPoolManager.class)) {
            hpm.when(() -> HikariPoolManager.getConnection(any(JdbcConnector.class))).thenReturn(conn);

            assertThatThrownBy(() -> executor.execute(ctx))
                .isInstanceOf(ToolExecuteException.class)
                .satisfies(e -> assertThat(((ToolExecuteException) e).getToolError())
                    .isEqualTo(ToolError.SQL_EXECUTION_ERROR));
        }
    }
}
