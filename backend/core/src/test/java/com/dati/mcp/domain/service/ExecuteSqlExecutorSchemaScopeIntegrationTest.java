package com.dati.mcp.domain.service;

import com.dati.datasource.domain.model.DataSource;
import com.dati.datasource.domain.service.DataSourceService;
import com.dati.datasource.domain.service.JdbcMetaService;
import com.dati.datasource.repository.dao.ColumnInfoDAO;
import com.dati.datasource.repository.dao.DataSourceDAO;
import com.dati.datasource.repository.dao.TableInfoDAO;
import com.dati.datasource.repository.po.ColumnInfoPO;
import com.dati.datasource.repository.po.DataSourcePO;
import com.dati.datasource.repository.po.TableInfoPO;
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
import com.dati.semantic.domain.service.SemanticIndexService;
import com.dati.semantic.repository.dao.SubjectDAO;
import com.dati.semantic.repository.dao.SubjectTableDAO;
import com.dati.semantic.repository.po.SemanticSearchDocument;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExecuteSqlExecutor schema scope integration tests")
class ExecuteSqlExecutorSchemaScopeIntegrationTest {

    private static final String DS_ID = "ds-h2";
    private static final String JDBC_URL =
            "jdbc:h2:mem:schema_scope_ddl_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL";

    @Mock
    private TableInfoDAO tableInfoDAO;

    @Mock
    private ColumnInfoDAO columnInfoDAO;

    @Mock
    private DataSourceDAO dataSourceDAO;

    @Mock
    private SubjectTableDAO subjectTableDAO;

    @Mock
    private SubjectDAO subjectDAO;

    @Mock
    private DataSourceService dataSourceService;

    @Mock
    private SemanticIndexService semanticIndexService;

    private ExecuteSqlExecutor executor;
    private JdbcConnector connector;

    @BeforeEach
    void setUp() throws Exception {
        DataSource dataSource = new DataSource();
        dataSource.setId(DS_ID);
        dataSource.setType(DbType.POSTGRESQL);
        dataSource.setJdbcUrl(JDBC_URL);
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        dataSource.setDefaultSchema("PUBLIC");
        when(dataSourceService.getDataSourceInternal(DS_ID)).thenReturn(Optional.of(dataSource));
        connector = new JdbcConnector(dataSource);

        DataSourcePO dataSourcePO = new DataSourcePO();
        dataSourcePO.setId(DS_ID);
        dataSourcePO.setType(DbType.POSTGRESQL);
        dataSourcePO.setJdbcUrl(JDBC_URL);
        dataSourcePO.setUserName("sa");
        dataSourcePO.setEncryptedPassword("");
        dataSourcePO.setDefaultSchema("PUBLIC");
        lenient().when(dataSourceDAO.findById(DS_ID)).thenReturn(Optional.of(dataSourcePO));

        try (Connection conn = DriverManager.getConnection(JDBC_URL, "sa", "");
             Statement stmt = conn.createStatement()) {
            stmt.execute("DROP ALL OBJECTS");
        }

        JdbcMetaService jdbcMetaService = new JdbcMetaService(dataSourceDAO);
        TableAutoRegistrationService autoRegistrationService = new TableAutoRegistrationService(
                tableInfoDAO, columnInfoDAO, jdbcMetaService, semanticIndexService);
        ScopeValidator scopeValidator = new ScopeValidator(tableInfoDAO, subjectTableDAO, subjectDAO);
        executor = new ExecuteSqlExecutor(scopeValidator, dataSourceService, autoRegistrationService);
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
                new ExecuteSqlArgs(DS_ID, sql), schemaScope("PUBLIC"));
    }

    private List<McpServiceDataScope> schemaScope(String... schemas) {
        McpServiceDataScope scope = new McpServiceDataScope();
        scope.setServiceId("svc-h2");
        scope.setScopeType(McpDataScopeType.DATA_SOURCE);
        scope.setReferenceId(DS_ID);
        scope.setAccessMode(McpDataScopeAccessMode.SCHEMA_SCOPE);
        scope.setAllowedSchemas(List.of(schemas));
        return List.of(scope);
    }

    private void stubAutoRegistration() {
        when(tableInfoDAO.findFirstByDataSourceIdAndSchemaAndNameIgnoreCase(
                anyString(), anyString(), anyString())).thenReturn(Optional.empty());
        when(tableInfoDAO.save(any(TableInfoPO.class))).thenAnswer(invocation -> {
            TableInfoPO po = invocation.getArgument(0);
            po.setId("tbl-auto");
            return po;
        });
        AtomicInteger columnIds = new AtomicInteger();
        when(columnInfoDAO.save(any(ColumnInfoPO.class))).thenAnswer(invocation -> {
            ColumnInfoPO po = invocation.getArgument(0);
            po.setId("col-" + columnIds.incrementAndGet());
            return po;
        });
    }

    @Test
    @DisplayName("CREATE + INSERT in one multi-statement call works in schema scope")
    void createAndInsertInSameMultiStatement_shouldSucceed() throws Exception {
        stubAutoRegistration();

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
    @DisplayName("auto-registers table created in schema scope")
    void autoRegistersTableCreatedInSchemaScope() {
        stubAutoRegistration();

        executor.execute(ctx("CREATE TABLE auto_registered (id INT, name VARCHAR(20))", true));

        ArgumentCaptor<TableInfoPO> tableCaptor = ArgumentCaptor.forClass(TableInfoPO.class);
        verify(tableInfoDAO).save(tableCaptor.capture());
        assertThat(tableCaptor.getValue().getDataSourceId()).isEqualTo(DS_ID);
        assertThat(tableCaptor.getValue().getSchema()).isEqualTo("PUBLIC");
        assertThat(tableCaptor.getValue().getName()).isEqualTo("AUTO_REGISTERED");

        ArgumentCaptor<ColumnInfoPO> columnCaptor = ArgumentCaptor.forClass(ColumnInfoPO.class);
        verify(columnInfoDAO, times(2)).save(columnCaptor.capture());
        assertThat(columnCaptor.getAllValues())
                .extracting(ColumnInfoPO::getName)
                .containsExactlyInAnyOrder("ID", "NAME");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<SemanticSearchDocument>> docsCaptor = ArgumentCaptor.forClass(List.class);
        verify(semanticIndexService).saveBatch(docsCaptor.capture());
        assertThat(docsCaptor.getValue()).hasSize(3);
    }

    @Test
    @DisplayName("SELECT from unimported table in schema scope is allowed")
    void selectUnimportedTableInSchemaScope_shouldSucceed() throws Exception {
        try (Connection conn = DriverManager.getConnection(JDBC_URL, "sa", "");
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE existing_unimported (id INT)");
            stmt.execute("INSERT INTO existing_unimported VALUES (7)");
        }

        // No table_info rows: access is granted by the schema scope.
        var result = executor.execute(ctx("SELECT * FROM existing_unimported", false));

        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("SELECT from table outside schema scope is rejected")
    void selectTableOutsideSchemaScope_shouldReject() throws Exception {
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
