package com.dati.mcp.domain.service;

import com.dati.datasource.domain.model.DataSource;
import com.dati.datasource.domain.model.DataSourceDef;
import com.dati.datasource.domain.model.TableDef;
import com.dati.datasource.domain.service.DataSourceService;
import com.dati.datasource.domain.service.JdbcMetaService;
import com.dati.datasource.domain.service.TableMetadataService;
import com.dati.datasource.repository.dao.ColumnInfoDAO;
import com.dati.datasource.repository.dao.DataSourceDAO;
import com.dati.datasource.repository.dao.TableInfoDAO;
import com.dati.datasource.repository.po.DataSourcePO;
import com.dati.db.DbType;
import com.dati.db.HikariPoolManager;
import com.dati.db.JdbcConnector;
import com.dati.mcp.domain.model.McpDataScopeAccessMode;
import com.dati.mcp.domain.model.McpDataScopeType;
import com.dati.mcp.domain.model.McpServiceDataScope;
import com.dati.mcp.domain.model.McpToolType;
import com.dati.mcp.domain.model.ToolConfig;
import com.dati.mcp.domain.model.param.GetTableInfoArgs;
import com.dati.mcp.domain.model.param.ListTablesArgs;
import com.dati.mcp.server.pojo.TableListData;
import com.dati.mcp.server.pojo.TableMetadata;
import com.dati.semantic.domain.service.SemanticIndexService;
import com.dati.semantic.domain.service.TermService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Schema-scope metadata integration tests")
class SchemaScopeMetadataIntegrationTest {

    private static final String DS_ID = "ds-h2";
    private static final String SERVICE_ID = "svc-h2";
    private static final String JDBC_URL =
            "jdbc:h2:mem:schema_scope_metadata;DB_CLOSE_DELAY=-1;MODE=PostgreSQL";

    @Mock
    private DataSourceDAO dataSourceDAO;

    @Mock
    private TableInfoDAO tableInfoDAO;

    @Mock
    private ColumnInfoDAO columnInfoDAO;

    @Mock
    private SemanticIndexService semanticIndexService;

    @Mock
    private McpServiceDataScopeService dataScopeService;

    @Mock
    private DataSourceService dataSourceService;

    @Mock
    private TermService termService;

    @Mock
    private ScopeValidator scopeValidator;

    private JdbcConnector connector;
    private JdbcMetaService jdbcMetaService;
    private ListTablesExecutor listTablesExecutor;
    private GetTableInfoExecutor getTableInfoExecutor;

    @BeforeEach
    void setUp() throws Exception {
        try (Connection conn = DriverManager.getConnection(JDBC_URL, "sa", "");
             Statement stmt = conn.createStatement()) {
            stmt.execute("DROP ALL OBJECTS");
            stmt.execute("CREATE SCHEMA SANDBOX");
            stmt.execute("CREATE TABLE SANDBOX.ORDERS (id INT PRIMARY KEY, name VARCHAR(50))");
            stmt.execute("COMMENT ON TABLE SANDBOX.ORDERS IS 'orders table'");
            stmt.execute("COMMENT ON COLUMN SANDBOX.ORDERS.name IS 'order name'");
        }

        DataSourcePO dataSourcePO = new DataSourcePO();
        dataSourcePO.setId(DS_ID);
        dataSourcePO.setType(DbType.POSTGRESQL);
        dataSourcePO.setJdbcUrl(JDBC_URL);
        dataSourcePO.setUserName("sa");
        dataSourcePO.setEncryptedPassword("");
        dataSourcePO.setDefaultSchema("SANDBOX");
        when(dataSourceDAO.findById(DS_ID)).thenReturn(Optional.of(dataSourcePO));

        DataSource dataSource = new DataSource();
        dataSource.setId(DS_ID);
        dataSource.setType(DbType.POSTGRESQL);
        dataSource.setJdbcUrl(JDBC_URL);
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        dataSource.setDefaultSchema("SANDBOX");
        connector = new JdbcConnector(dataSource);

        jdbcMetaService = new JdbcMetaService(dataSourceDAO);
        TableMetadataService tableMetadataService = new TableMetadataService(
                tableInfoDAO, columnInfoDAO, semanticIndexService, jdbcMetaService);
        listTablesExecutor = new ListTablesExecutor(
                dataScopeService, tableInfoDAO, dataSourceService, termService, jdbcMetaService, 1000);
        getTableInfoExecutor = new GetTableInfoExecutor(
                scopeValidator, tableMetadataService, dataSourceService);

        lenient().when(dataScopeService.getResolvedDataSourceIds(SERVICE_ID)).thenReturn(Set.of(DS_ID));
        when(tableInfoDAO.findByDataSourceId(DS_ID)).thenReturn(List.of());
        lenient().when(dataSourceService.getDataSourceBriefs(Set.of(DS_ID))).thenReturn(Map.of(
                DS_ID, new DataSourceService.DsBrief("H2 Sandbox", DbType.POSTGRESQL,
                        "SANDBOX", "integration test")));
        lenient().when(dataSourceService.getDataSourceInternal(DS_ID)).thenReturn(Optional.of(dataSource));
    }

    @AfterEach
    void tearDown() {
        HikariPoolManager.close(connector);
    }

    private static McpServiceDataScope schemaScope() {
        McpServiceDataScope scope = new McpServiceDataScope();
        scope.setServiceId(SERVICE_ID);
        scope.setScopeType(McpDataScopeType.DATA_SOURCE);
        scope.setReferenceId(DS_ID);
        scope.setAccessMode(McpDataScopeAccessMode.SCHEMA_SCOPE);
        scope.setAllowedSchemas(List.of("SANDBOX"));
        return scope;
    }

    private static ToolExecutionContext listCtx() {
        return new ToolExecutionContext(SERVICE_ID, McpToolType.LIST_TABLES,
                new ToolConfig.ListTablesConfig(), new ListTablesArgs(), List.of(schemaScope()));
    }

    private static ToolExecutionContext getCtx(String schema) {
        GetTableInfoArgs args = new GetTableInfoArgs(List.of(
                new GetTableInfoArgs.TableRef(DS_ID, schema, "orders", null)));
        return new ToolExecutionContext(SERVICE_ID, McpToolType.GET_TABLE_INFO,
                new ToolConfig.GetTableInfoConfig(), args, List.of(schemaScope()));
    }

    @Test
    @DisplayName("list_tables discovers unimported physical table in schema scope")
    void listTablesDiscoversPhysicalTable() {
        TableListData data = (TableListData) listTablesExecutor.execute(listCtx());

        assertThat(data.dataSources()).hasSize(1);
        DataSourceDef group = data.dataSources().getFirst();
        assertThat(group.id()).isEqualTo(DS_ID);
        assertThat(group.tables()).anySatisfy(table -> {
            assertThat(table.table()).isEqualToIgnoringCase("orders");
            assertThat(table.schema()).isEqualTo("SANDBOX");
            assertThat(table.description()).isEqualTo("orders table");
            assertThat(table.aliases()).isEmpty();
            assertThat(table.columns()).isNull();
        });
    }

    @Test
    @DisplayName("get_table_schema falls back to physical columns in schema scope")
    void getTableSchemaFallsBackToPhysicalColumns() {
        TableMetadata data = (TableMetadata) getTableInfoExecutor.execute(getCtx("SANDBOX"));

        assertThat(data.tables()).hasSize(1);
        TableDef table = data.tables().getFirst();
        assertThat(table.table()).isEqualToIgnoringCase("orders");
        assertThat(table.schema()).isEqualTo("SANDBOX");
        assertThat(table.description()).isEqualTo("orders table");
        assertThat(table.columns()).extracting(column -> column.name().toLowerCase())
                .containsExactly("id", "name");
        assertThat(table.columns()).anySatisfy(column -> {
            assertThat(column.name()).isEqualToIgnoringCase("name");
            assertThat(column.comment()).isEqualTo("order name");
        });
    }

    @Test
    @DisplayName("get_table_schema resolves default schema for unqualified table")
    void getTableSchemaResolvesDefaultSchema() {
        TableMetadata data = (TableMetadata) getTableInfoExecutor.execute(getCtx(null));

        assertThat(data.tables()).hasSize(1);
        assertThat(data.tables().getFirst().schema()).isEqualTo("SANDBOX");
        assertThat(data.tables().getFirst().columns()).hasSize(2);
    }
}
