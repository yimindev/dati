package com.dati.mcp.domain.service;

import com.dati.TestFixtures;
import com.dati.datasource.domain.service.DataSourceService;
import com.dati.db.HikariPoolManager;
import com.dati.db.JdbcConnector;
import com.dati.mcp.domain.model.McpToolType;
import com.dati.mcp.domain.model.ToolConfig.ExecuteSqlConfig;
import com.dati.mcp.domain.model.ToolError;
import com.dati.mcp.domain.model.param.ExecuteSqlArgs;
import com.dati.mcp.server.pojo.SqlExecution;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Connection;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExecuteSqlExecutor tests")
class ExecuteSqlExecutorTest {

    @Mock
    private ScopeValidator scopeValidator;

    @Mock
    private DataSourceService dataSourceService;

    private ExecuteSqlExecutor executor;

    @BeforeEach
    void setUp() {
        executor = new ExecuteSqlExecutor(scopeValidator, dataSourceService);
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
    @DisplayName("missing data source still reported as DATA_SOURCE_NOT_FOUND")
    void missingDataSource() {
        when(dataSourceService.getDataSource("ghost")).thenReturn(Optional.empty());
        ExecuteSqlArgs args = new ExecuteSqlArgs("ghost", "SELECT * FROM orders");

        assertThatThrownBy(() -> executor.execute(ctx(args)))
            .isInstanceOf(ToolExecuteException.class)
            .satisfies(e -> assertThat(((ToolExecuteException) e).getErrorCategory())
                .isEqualTo("PARAM_ERROR"));
    }

    @Test
    @DisplayName("scope violation still enforced")
    void scopeStillEnforced() {
        when(dataSourceService.getDataSource("ds-1"))
            .thenReturn(Optional.of(TestFixtures.createTestDataSource()));
        doThrow(new ToolExecuteException(ToolError.SCOPE_VIOLATION, "orders not in scope"))
            .when(scopeValidator).validate(anyList(), eq("ds-1"), anySet(), eq("public"));
        ExecuteSqlArgs args = new ExecuteSqlArgs("ds-1", "SELECT * FROM orders");

        assertThatThrownBy(() -> executor.execute(ctx(args)))
            .isInstanceOf(ToolExecuteException.class)
            .hasMessageContaining("not in scope");
    }

    @Test
    @DisplayName("SELECT executes and returns results")
    void executesSelect() throws Exception {
        when(dataSourceService.getDataSource("ds-1"))
            .thenReturn(Optional.of(TestFixtures.createTestDataSource()));
        Statement stmt = mock(Statement.class);
        when(stmt.getUpdateCount()).thenReturn(-1);
        when(stmt.getMoreResults()).thenReturn(false);
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
}
