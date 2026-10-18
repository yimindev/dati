package com.dati.mcp.domain.service;

import com.dati.datasource.domain.model.DataSource;
import com.dati.datasource.domain.service.DataSourceService;
import com.dati.datasource.repository.dao.TableInfoDAO;
import com.dati.db.DbType;
import com.dati.db.HikariPoolManager;
import com.dati.db.JdbcConnector;
import com.dati.mcp.domain.model.McpDataScopeAccessMode;
import com.dati.mcp.domain.model.McpDataScopeType;
import com.dati.mcp.domain.model.McpServiceDataScope;
import com.dati.mcp.domain.model.McpToolType;
import com.dati.mcp.domain.model.ToolConfig.ExecuteSqlConfig;
import com.dati.mcp.domain.model.ToolError;
import com.dati.mcp.domain.model.param.ExecuteSqlArgs;
import com.dati.semantic.repository.dao.SubjectDAO;
import com.dati.semantic.repository.dao.SubjectTableDAO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExecuteSqlExecutor managed schema integration tests")
class ExecuteSqlExecutorManagedSchemaIntegrationTest {

    private static final String DS_ID = "ds-h2";
    private static final String JDBC_URL =
            "jdbc:h2:mem:managed_ddl_test;DB_CLOSE_DELAY=-1;MODE=MySQL";

    @Mock
    private TableInfoDAO tableInfoDAO;

    @Mock
    private SubjectTableDAO subjectTableDAO;

    @Mock
    private SubjectDAO subjectDAO;

    @Mock
    private DataSourceService dataSourceService;

    private ExecuteSqlExecutor executor;
    private JdbcConnector connector;

    @BeforeEach
    void setUp() throws Exception {
        DataSource dataSource = new DataSource();
        dataSource.setId(DS_ID);
        dataSource.setType(DbType.UNKNOWN);
        dataSource.setJdbcUrl(JDBC_URL);
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        dataSource.setDefaultSchema("PUBLIC");
        when(dataSourceService.getDataSourceInternal(DS_ID)).thenReturn(Optional.of(dataSource));
        connector = new JdbcConnector(dataSource);

        try (Connection conn = DriverManager.getConnection(JDBC_URL, "sa", "");
             Statement stmt = conn.createStatement()) {
            stmt.execute("DROP ALL OBJECTS");
        }

        ScopeValidator scopeValidator = new ScopeValidator(tableInfoDAO, subjectTableDAO, subjectDAO);
        executor = new ExecuteSqlExecutor(scopeValidator, dataSourceService);
    }

    @AfterEach
    void tearDown() {
        HikariPoolManager.close(connector);
    }

    private ToolExecutionContext ctx(String sql, boolean allowDdl) {
        ExecuteSqlConfig config = new ExecuteSqlConfig();
        config.getSqlPolicy().setAllowDdl(allowDdl);
        config.getSqlPolicy().setAllowMulti(true);
        config.getSqlPolicy().setAllowSelect(true);
        config.getSqlPolicy().setAllowInsert(true);
        return new ToolExecutionContext("svc-h2", McpToolType.EXECUTE_SQL, config,
                new ExecuteSqlArgs(DS_ID, sql), managedScope("PUBLIC"));
    }

    private List<McpServiceDataScope> managedScope(String... schemas) {
        McpServiceDataScope scope = new McpServiceDataScope();
        scope.setServiceId("svc-h2");
        scope.setScopeType(McpDataScopeType.DATA_SOURCE);
        scope.setReferenceId(DS_ID);
        scope.setAccessMode(McpDataScopeAccessMode.MANAGED_SCHEMAS);
        scope.setAllowedSchemas(List.of(schemas));
        return List.of(scope);
    }

    @Test
    @DisplayName("CREATE + INSERT in one multi-statement call works in managed schema")
    void createAndInsertInSameMultiStatement_shouldSucceed() throws Exception {
        executor.execute(ctx(
                "CREATE TABLE sandbox_t (id INT, name VARCHAR(20)); "
                        + "INSERT INTO sandbox_t VALUES (1, 'a');", true));

        try (Connection conn = DriverManager.getConnection(JDBC_URL, "sa", "");
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM sandbox_t")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getInt(1)).isEqualTo(1);
        }
    }

    @Test
    @DisplayName("SELECT from unimported table in managed schema is allowed")
    void selectUnimportedTableInManagedSchema_shouldSucceed() throws Exception {
        try (Connection conn = DriverManager.getConnection(JDBC_URL, "sa", "");
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE existing_unimported (id INT)");
            stmt.execute("INSERT INTO existing_unimported VALUES (7)");
        }

        // No table_info rows: access is granted by the managed schema.
        var result = executor.execute(ctx("SELECT * FROM existing_unimported", false));

        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("SELECT from table outside managed schema is rejected")
    void selectTableOutsideManagedSchema_shouldReject() throws Exception {
        try (Connection conn = DriverManager.getConnection(JDBC_URL, "sa", "");
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE SCHEMA other_schema");
            stmt.execute("CREATE TABLE other_schema.orders (id INT)");
        }

        assertThatThrownBy(() -> executor.execute(ctx("SELECT * FROM other_schema.orders", false)))
                .isInstanceOf(ToolExecuteException.class)
                .satisfies(e -> assertThat(((ToolExecuteException) e).getToolError())
                        .isEqualTo(ToolError.SCOPE_VIOLATION));
    }
}
