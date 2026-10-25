package com.dati.mcp.domain.service;

import com.dati.TestFixtures;
import com.dati.datasource.domain.service.DataSourceService;
import com.dati.db.HikariPoolManager;
import com.dati.db.JdbcConnector;
import com.dati.db.analysis.TableRef;
import com.dati.mcp.domain.model.McpToolType;
import com.dati.mcp.domain.model.ToolConfig.ExecuteSqlConfig;
import com.dati.mcp.domain.model.ToolError;
import com.dati.mcp.domain.model.param.ExecuteSqlArgs;
import com.dati.mcp.server.pojo.SqlExecution;
import com.dati.mcp.server.pojo.StatementResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExecuteSqlExecutor tests")
class ExecuteSqlExecutorTest {

    @Mock
    private ScopeValidator scopeValidator;

    @Mock
    private DataSourceService dataSourceService;

    @Mock
    private TableAutoRegistrationService tableAutoRegistrationService;

    private ExecuteSqlExecutor executor;

    @BeforeEach
    void setUp() {
        executor = new ExecuteSqlExecutor(scopeValidator, dataSourceService, tableAutoRegistrationService);
    }

    private Statement mockSuccessfulStatement() throws SQLException {
        Statement stmt = mock(Statement.class);
        when(stmt.getUpdateCount()).thenReturn(-1);
        when(stmt.getMoreResults()).thenReturn(false);
        return stmt;
    }

    private ToolExecutionContext ddlCtx(String sql) {
        ExecuteSqlConfig config = new ExecuteSqlConfig();
        config.getSqlPolicy().setAllowDdl(true);
        return new ToolExecutionContext("svc-1", McpToolType.EXECUTE_SQL, config,
            new ExecuteSqlArgs("ds-1", sql), List.of());
    }

    private ToolExecutionContext ctx(ExecuteSqlArgs args) {
        return new ToolExecutionContext("svc-1", McpToolType.EXECUTE_SQL,
            new ExecuteSqlConfig(), args, List.of());
    }

    @Test
    @DisplayName("SQL policy still enforced before execution")
    void policyStillEnforced() {
        ExecuteSqlArgs args = new ExecuteSqlArgs("ds-1", "DELETE FROM orders");

        assertThatThrownBy(() -> executor.execute(ctx(args)))
            .isInstanceOf(ToolExecuteException.class)
            .satisfies(e -> assertThat(((ToolExecuteException) e).getErrorCategory())
                .isEqualTo("PERMISSION_DENIED"));
    }

    @Test
    @DisplayName("missing data source is reported as DATA_SOURCE_NOT_FOUND")
    void missingDataSource_shouldReportDataSourceNotFound() {
        when(dataSourceService.getDataSourceInternal("ghost")).thenReturn(Optional.empty());
        ExecuteSqlArgs args = new ExecuteSqlArgs("ghost", "SELECT * FROM orders");

        assertThatThrownBy(() -> executor.execute(ctx(args)))
            .isInstanceOf(ToolExecuteException.class)
            .hasMessage("Data source not found: ghost")
            .satisfies(e -> assertThat(((ToolExecuteException) e).getErrorCategory())
                .isEqualTo("PARAM_ERROR"));

        verifyNoInteractions(scopeValidator);
    }

    @Test
    @DisplayName("scope violation still enforced")
    void scopeStillEnforced() {
        when(dataSourceService.getDataSourceInternal("ds-1"))
            .thenReturn(Optional.of(TestFixtures.createTestDataSource()));
        doThrow(new ToolExecuteException(ToolError.SCOPE_VIOLATION, "orders not in service scope"))
            .when(scopeValidator).validate(anyList(), eq("ds-1"), anySet(), anySet(), eq("public"));
        ExecuteSqlArgs args = new ExecuteSqlArgs("ds-1", "SELECT * FROM orders");

        assertThatThrownBy(() -> executor.execute(ctx(args)))
            .isInstanceOf(ToolExecuteException.class)
            .hasMessageContaining("not in service scope");

        verify(scopeValidator, times(1)).validate(anyList(), eq("ds-1"), anySet(), anySet(), eq("public"));
    }

    @Test
    @DisplayName("passes created tables to scope validation")
    void passesCreatedTablesToScopeValidation() {
        when(dataSourceService.getDataSourceInternal("ds-1"))
            .thenReturn(Optional.of(TestFixtures.createTestDataSource()));
        doThrow(new ToolExecuteException(ToolError.SCOPE_VIOLATION, "stop"))
            .when(scopeValidator).validate(anyList(), eq("ds-1"), anySet(), anySet(), eq("public"));

        ExecuteSqlConfig config = new ExecuteSqlConfig();
        config.getSqlPolicy().setAllowDdl(true);
        ToolExecutionContext ctx = new ToolExecutionContext("svc-1", McpToolType.EXECUTE_SQL,
            config, new ExecuteSqlArgs("ds-1", "CREATE TABLE tmp (id INT)"), List.of());

        assertThatThrownBy(() -> executor.execute(ctx)).hasMessageContaining("stop");

        verify(scopeValidator, times(1)).validate(anyList(), eq("ds-1"), eq(Set.of()),
            argThat(created -> created.contains(TableRef.of("tmp"))), eq("public"));
    }

    @Test
    @DisplayName("DDL is rejected by SQL policy when allowDdl is false")
    void ddlRejectedByPolicyWhenAllowDdlFalse() {
        ToolExecutionContext ctx = new ToolExecutionContext("svc-1", McpToolType.EXECUTE_SQL,
            new ExecuteSqlConfig(), new ExecuteSqlArgs("ds-1", "CREATE TABLE tmp (id INT)"), List.of());

        assertThatThrownBy(() -> executor.execute(ctx))
            .isInstanceOf(ToolExecuteException.class)
            .satisfies(e -> assertThat(((ToolExecuteException) e).getToolError())
                .isEqualTo(ToolError.SQL_POLICY_VIOLATION));
    }

    @Test
    @DisplayName("unparseable SQL is rejected by SQL policy")
    void unparseableSqlRejectedByPolicy() {
        ToolExecutionContext ctx = new ToolExecutionContext("svc-1", McpToolType.EXECUTE_SQL,
            new ExecuteSqlConfig(), new ExecuteSqlArgs("ds-1", "NOT A SQL STATEMENT !!!"), List.of());

        assertThatThrownBy(() -> executor.execute(ctx))
            .isInstanceOf(ToolExecuteException.class)
            .satisfies(e -> assertThat(((ToolExecuteException) e).getToolError())
                .isEqualTo(ToolError.SQL_POLICY_VIOLATION));
    }

    @Test
    @DisplayName("registers created table after successful DDL")
    void registersCreatedTableAfterSuccessfulDdl() throws Exception {
        when(dataSourceService.getDataSourceInternal("ds-1"))
            .thenReturn(Optional.of(TestFixtures.createTestDataSource()));
        Statement stmt = mockSuccessfulStatement();
        Connection conn = mock(Connection.class);
        when(conn.createStatement()).thenReturn(stmt);

        try (MockedStatic<HikariPoolManager> hpm = mockStatic(HikariPoolManager.class)) {
            hpm.when(() -> HikariPoolManager.getConnection(any(JdbcConnector.class))).thenReturn(conn);

            executor.execute(ddlCtx("CREATE TABLE tmp (id INT)"));
        }

        verify(tableAutoRegistrationService).registerCreatedTables(
            eq("ds-1"), eq(Set.of(TableRef.of("tmp"))), eq("public"), eq("svc-1"));
    }

    @Test
    @DisplayName("does not register for non-DDL")
    void doesNotRegisterForNonDdl() throws Exception {
        when(dataSourceService.getDataSourceInternal("ds-1"))
            .thenReturn(Optional.of(TestFixtures.createTestDataSource()));
        Statement stmt = mockSuccessfulStatement();
        Connection conn = mock(Connection.class);
        when(conn.createStatement()).thenReturn(stmt);

        try (MockedStatic<HikariPoolManager> hpm = mockStatic(HikariPoolManager.class)) {
            hpm.when(() -> HikariPoolManager.getConnection(any(JdbcConnector.class))).thenReturn(conn);

            executor.execute(ctx(new ExecuteSqlArgs("ds-1", "SELECT 1")));
        }

        verifyNoInteractions(tableAutoRegistrationService);
    }

    @Test
    @DisplayName("does not register when SQL execution fails")
    void doesNotRegisterWhenExecutionFails() throws Exception {
        when(dataSourceService.getDataSourceInternal("ds-1"))
            .thenReturn(Optional.of(TestFixtures.createTestDataSource()));
        Statement stmt = mock(Statement.class);
        doThrow(new SQLException("boom")).when(stmt).execute(anyString());
        Connection conn = mock(Connection.class);
        when(conn.createStatement()).thenReturn(stmt);

        try (MockedStatic<HikariPoolManager> hpm = mockStatic(HikariPoolManager.class)) {
            hpm.when(() -> HikariPoolManager.getConnection(any(JdbcConnector.class))).thenReturn(conn);

            assertThatThrownBy(() -> executor.execute(ddlCtx("CREATE TABLE tmp (id INT)")))
                .isInstanceOf(ToolExecuteException.class)
                .satisfies(e -> assertThat(((ToolExecuteException) e).getToolError())
                    .isEqualTo(ToolError.SQL_EXECUTION_ERROR));
        }

        verifyNoInteractions(tableAutoRegistrationService);
    }

    @Test
    @DisplayName("registration failure does not fail SQL execution")
    void registrationFailureDoesNotFailSqlExecution() throws Exception {
        when(dataSourceService.getDataSourceInternal("ds-1"))
            .thenReturn(Optional.of(TestFixtures.createTestDataSource()));
        Statement stmt = mockSuccessfulStatement();
        Connection conn = mock(Connection.class);
        when(conn.createStatement()).thenReturn(stmt);
        doThrow(new RuntimeException("registration down"))
            .when(tableAutoRegistrationService)
            .registerCreatedTables(anyString(), anySet(), anyString(), anyString());

        try (MockedStatic<HikariPoolManager> hpm = mockStatic(HikariPoolManager.class)) {
            hpm.when(() -> HikariPoolManager.getConnection(any(JdbcConnector.class))).thenReturn(conn);

            SqlExecution execution = (SqlExecution) executor.execute(ddlCtx("CREATE TABLE tmp (id INT)"));

            assertThat(execution.executedSql()).isEqualTo("CREATE TABLE tmp (id INT)");
        }
    }

    @Test
    @DisplayName("SELECT executes and returns results")
    void executesSelect() throws Exception {
        when(dataSourceService.getDataSourceInternal("ds-1"))
            .thenReturn(Optional.of(TestFixtures.createTestDataSource()));
        Statement stmt = mockSuccessfulStatement();
        Connection conn = mock(Connection.class);
        when(conn.createStatement()).thenReturn(stmt);

        ExecuteSqlArgs args = new ExecuteSqlArgs("ds-1", "SELECT 1");
        try (MockedStatic<HikariPoolManager> hpm = mockStatic(HikariPoolManager.class)) {
            hpm.when(() -> HikariPoolManager.getConnection(any(JdbcConnector.class))).thenReturn(conn);

            SqlExecution exec = (SqlExecution) executor.execute(ctx(args));

            assertThat(exec.executedSql()).isEqualTo("SELECT 1");
            assertThat(exec.results()).isEmpty();
        }
    }

    @Test
    @DisplayName("SELECT honours max rows and reports truncation through the probe row")
    void executesSelectAndReportsTruncation() throws Exception {
        when(dataSourceService.getDataSourceInternal("ds-1"))
            .thenReturn(Optional.of(TestFixtures.createTestDataSource()));

        ResultSetMetaData meta = mock(ResultSetMetaData.class);
        when(meta.getColumnCount()).thenReturn(1);
        when(meta.getColumnLabel(1)).thenReturn("id");
        ResultSet rs = mock(ResultSet.class);
        when(rs.getMetaData()).thenReturn(meta);
        // Two rows are kept, the third one is the probe that proves truncation.
        when(rs.next()).thenReturn(true, true, true, false);
        when(rs.getObject(1)).thenReturn(42);

        Statement stmt = mock(Statement.class);
        when(stmt.getResultSet()).thenReturn(rs);
        when(stmt.getUpdateCount()).thenReturn(-1);
        when(stmt.getMoreResults()).thenReturn(false);

        Connection conn = mock(Connection.class);
        when(conn.createStatement()).thenReturn(stmt);

        ExecuteSqlConfig config = new ExecuteSqlConfig();
        config.setMaxRows(2);
        ToolExecutionContext ctx = new ToolExecutionContext("svc-1", McpToolType.EXECUTE_SQL, config,
            new ExecuteSqlArgs("ds-1", "SELECT id FROM orders"), List.of());

        try (MockedStatic<HikariPoolManager> hpm = mockStatic(HikariPoolManager.class)) {
            hpm.when(() -> HikariPoolManager.getConnection(any(JdbcConnector.class))).thenReturn(conn);

            SqlExecution exec = (SqlExecution) executor.execute(ctx);

            // One row beyond the limit is requested from the driver to detect truncation.
            verify(stmt).setMaxRows(3);
            StatementResult.SelectResult select = (StatementResult.SelectResult) exec.results().getFirst();
            assertThat(select.rows()).hasSize(2);
            assertThat(select.returnedRows()).isEqualTo(2);
            assertThat(select.truncated()).isTrue();
            assertThat(select.notice()).isNotBlank();
        }
    }

    @Test
    @DisplayName("unlimited max rows passes zero to the driver")
    void unlimitedMaxRowsPassesZero() throws Exception {
        when(dataSourceService.getDataSourceInternal("ds-1"))
            .thenReturn(Optional.of(TestFixtures.createTestDataSource()));
        Statement stmt = mockSuccessfulStatement();
        Connection conn = mock(Connection.class);
        when(conn.createStatement()).thenReturn(stmt);

        ExecuteSqlConfig config = new ExecuteSqlConfig();
        config.setMaxRows(0);
        ToolExecutionContext ctx = new ToolExecutionContext("svc-1", McpToolType.EXECUTE_SQL, config,
            new ExecuteSqlArgs("ds-1", "SELECT 1"), List.of());

        try (MockedStatic<HikariPoolManager> hpm = mockStatic(HikariPoolManager.class)) {
            hpm.when(() -> HikariPoolManager.getConnection(any(JdbcConnector.class))).thenReturn(conn);

            executor.execute(ctx);

            verify(stmt).setMaxRows(0);
        }
    }
}
