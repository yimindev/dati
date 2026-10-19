package com.dati.mcp.domain.service;

import com.dati.datasource.domain.model.DataSourceDef;
import com.dati.datasource.domain.model.TableDef;
import com.dati.datasource.domain.service.DataSourceService;
import com.dati.datasource.domain.service.JdbcMetaService;
import com.dati.datasource.repository.dao.TableInfoDAO;
import com.dati.datasource.repository.po.TableInfoPO;
import com.dati.db.DbType;
import com.dati.db.Table;
import com.dati.mcp.domain.model.McpDataScopeAccessMode;
import com.dati.mcp.domain.model.McpDataScopeType;
import com.dati.mcp.domain.model.McpServiceDataScope;
import com.dati.mcp.domain.model.McpToolType;
import com.dati.mcp.domain.model.ToolConfig;
import com.dati.mcp.domain.model.param.ListTablesArgs;
import com.dati.mcp.server.pojo.TableListData;
import com.dati.semantic.domain.service.TermService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListTablesExecutor tests")
class ListTablesExecutorTest {

    @Mock
    private McpServiceDataScopeService dataScopeService;

    @Mock
    private TableInfoDAO tableInfoDAO;

    @Mock
    private DataSourceService dataSourceService;

    @Mock
    private TermService termService;

    @Mock
    private JdbcMetaService jdbcMetaService;

    private ListTablesExecutor executor;

    @BeforeEach
    void setUp() {
        executor = new ListTablesExecutor(dataScopeService, tableInfoDAO, dataSourceService,
            termService, jdbcMetaService, 1000);
    }

    private static McpServiceDataScope schemaScope(String... schemas) {
        McpServiceDataScope scope = new McpServiceDataScope();
        scope.setScopeType(McpDataScopeType.DATA_SOURCE);
        scope.setReferenceId("ds-1");
        scope.setAccessMode(McpDataScopeAccessMode.SCHEMA_SCOPE);
        scope.setAllowedSchemas(List.of(schemas));
        return scope;
    }

    private static TableInfoPO importedTable(String schema, String name, String description,
                                             List<String> aliases) {
        TableInfoPO table = new TableInfoPO();
        table.setDataSourceId("ds-1");
        table.setSchema(schema);
        table.setName(name);
        table.setDescription(description);
        table.setAliases(aliases);
        return table;
    }

    private ToolExecutionContext ctx(List<McpServiceDataScope> scopes) {
        return new ToolExecutionContext("svc-1", McpToolType.LIST_TABLES,
            new ToolConfig.ListTablesConfig(), new ListTablesArgs(), scopes);
    }

    @Test
    @DisplayName("empty scope returns empty result without DB queries")
    void emptyScopeReturnsEmpty() {
        TableListData data = (TableListData) executor.execute(ctx(List.of()));

        assertThat(data.dataSources()).isEmpty();
        assertThat(data.terms()).isEmpty();
        verifyNoInteractions(tableInfoDAO, dataSourceService, termService);
    }

    @Test
    @DisplayName("lists table-level metadata grouped by data source, no columns")
    void listsTablesGroupedByDataSource() {
        McpServiceDataScope scope = new McpServiceDataScope();
        scope.setScopeType(McpDataScopeType.DATA_SOURCE);
        scope.setReferenceId("ds-1");
        when(dataScopeService.getResolvedDataSourceIds("svc-1")).thenReturn(Set.of("ds-1"));

        TableInfoPO genre = new TableInfoPO();
        genre.setId("t-1");
        genre.setDataSourceId("ds-1");
        genre.setSchema("public");
        genre.setName("genre");
        genre.setDescription("音乐流派");
        genre.setAliases(List.of("流派"));

        TableInfoPO artist = new TableInfoPO();
        artist.setId("t-2");
        artist.setDataSourceId("ds-1");
        artist.setSchema("public");
        artist.setName("artist");
        artist.setAliases(null);

        when(tableInfoDAO.findByDataSourceId("ds-1")).thenReturn(List.of(genre, artist));
        when(dataSourceService.getDataSourceBriefs(Set.of("ds-1")))
            .thenReturn(Map.of("ds-1", new DataSourceService.DsBrief("音乐库", DbType.POSTGRESQL, "public", "音乐数据")));

        TableListData data = (TableListData) executor.execute(ctx(List.of(scope)));

        assertThat(data.dataSources()).hasSize(1);
        DataSourceDef ds = data.dataSources().getFirst();
        assertThat(ds.id()).isEqualTo("ds-1");
        assertThat(ds.name()).isEqualTo("音乐库");
        assertThat(ds.dbType()).isEqualTo(DbType.POSTGRESQL.name());
        assertThat(ds.defaultSchema()).isEqualTo("public");
        assertThat(ds.description()).isEqualTo("音乐数据");

        assertThat(ds.tables()).hasSize(2);
        TableDef first = ds.tables().getFirst();
        assertThat(first.table()).isEqualTo("genre");
        assertThat(first.schema()).isEqualTo("public");
        assertThat(first.description()).isEqualTo("音乐流派");
        assertThat(first.aliases()).containsExactly("流派");
        assertThat(first.columns()).isNull();

        // Schema-less table still listed with null alias list
        TableDef second = ds.tables().get(1);
        assertThat(second.table()).isEqualTo("artist");
        assertThat(second.aliases()).isNull();
        assertThat(second.columns()).isNull();

        assertThat(data.terms()).isEmpty();
        verifyNoInteractions(jdbcMetaService);
    }

    @Test
    @DisplayName("schema scope adds physical tables that are not imported")
    void schemaScopeAddsPhysicalTables() throws SQLException {
        when(dataScopeService.getResolvedDataSourceIds("svc-1")).thenReturn(Set.of("ds-1"));
        when(tableInfoDAO.findByDataSourceId("ds-1")).thenReturn(List.of());
        when(dataSourceService.getDataSourceBriefs(Set.of("ds-1")))
            .thenReturn(Map.of("ds-1", new DataSourceService.DsBrief("音乐库", DbType.POSTGRESQL, "sandbox", "音乐数据")));
        when(jdbcMetaService.getTables("ds-1", "sandbox"))
            .thenReturn(List.of(new Table("orders", "orders table")));

        TableListData data = (TableListData) executor.execute(
            ctx(List.of(schemaScope("sandbox"))));

        assertThat(data.dataSources()).hasSize(1);
        assertThat(data.dataSources().getFirst().tables()).hasSize(1);
        TableDef table = data.dataSources().getFirst().tables().getFirst();
        assertThat(table.table()).isEqualTo("orders");
        assertThat(table.schema()).isEqualTo("sandbox");
        assertThat(table.description()).isEqualTo("orders table");
        assertThat(table.aliases()).isEmpty();
        assertThat(table.columns()).isNull();
    }

    @Test
    @DisplayName("imported metadata wins and physical duplicates are removed")
    void importedMetadataWinsOverPhysicalDuplicate() throws SQLException {
        when(dataScopeService.getResolvedDataSourceIds("svc-1")).thenReturn(Set.of("ds-1"));
        when(tableInfoDAO.findByDataSourceId("ds-1"))
            .thenReturn(List.of(importedTable("sandbox", "orders", "imported description", List.of("订单"))));
        when(dataSourceService.getDataSourceBriefs(Set.of("ds-1")))
            .thenReturn(Map.of("ds-1", new DataSourceService.DsBrief("音乐库", DbType.POSTGRESQL, "sandbox", null)));
        when(jdbcMetaService.getTables("ds-1", "sandbox"))
            .thenReturn(List.of(new Table("orders", "physical description")));

        TableListData data = (TableListData) executor.execute(
            ctx(List.of(schemaScope("sandbox"))));

        assertThat(data.dataSources().getFirst().tables()).hasSize(1);
        TableDef table = data.dataSources().getFirst().tables().getFirst();
        assertThat(table.description()).isEqualTo("imported description");
        assertThat(table.aliases()).containsExactly("订单");
    }

    @Test
    @DisplayName("physical tables from multiple schemas are merged")
    void multipleSchemaScopesAreMerged() throws SQLException {
        when(dataScopeService.getResolvedDataSourceIds("svc-1")).thenReturn(Set.of("ds-1"));
        when(tableInfoDAO.findByDataSourceId("ds-1")).thenReturn(List.of());
        when(dataSourceService.getDataSourceBriefs(Set.of("ds-1")))
            .thenReturn(Map.of("ds-1", new DataSourceService.DsBrief("音乐库", DbType.POSTGRESQL, "sandbox", null)));
        when(jdbcMetaService.getTables("ds-1", "sandbox"))
            .thenReturn(List.of(new Table("orders", null)));
        when(jdbcMetaService.getTables("ds-1", "audit"))
            .thenReturn(List.of(new Table("events", null)));

        TableListData data = (TableListData) executor.execute(
            ctx(List.of(schemaScope("sandbox", "audit"))));

        assertThat(data.dataSources().getFirst().tables())
            .extracting(TableDef::schema, TableDef::table)
            .containsExactly(
                org.assertj.core.groups.Tuple.tuple("sandbox", "orders"),
                org.assertj.core.groups.Tuple.tuple("audit", "events"));
    }

    @Test
    @DisplayName("caps physical tables per data source while keeping imported tables")
    void capsPhysicalTablesPerDataSource() throws SQLException {
        when(dataScopeService.getResolvedDataSourceIds("svc-1")).thenReturn(Set.of("ds-1"));
        when(tableInfoDAO.findByDataSourceId("ds-1"))
            .thenReturn(List.of(importedTable("sandbox", "imported", "imported table", List.of())));
        when(dataSourceService.getDataSourceBriefs(Set.of("ds-1")))
            .thenReturn(Map.of("ds-1", new DataSourceService.DsBrief("音乐库", DbType.POSTGRESQL, "sandbox", null)));
        when(jdbcMetaService.getTables("ds-1", "sandbox"))
            .thenReturn(List.of(
                new Table("physical1", null),
                new Table("physical2", null),
                new Table("physical3", null)));

        ListTablesExecutor capped = new ListTablesExecutor(dataScopeService, tableInfoDAO,
            dataSourceService, termService, jdbcMetaService, 2);
        TableListData data = (TableListData) capped.execute(ctx(List.of(schemaScope("sandbox"))));

        assertThat(data.dataSources().getFirst().tables())
            .extracting(TableDef::table)
            .containsExactly("imported", "physical1", "physical2");
    }

    @Test
    @DisplayName("zero physical limit keeps imported tables only")
    void zeroPhysicalLimitKeepsImportedTablesOnly() throws SQLException {
        when(dataScopeService.getResolvedDataSourceIds("svc-1")).thenReturn(Set.of("ds-1"));
        when(tableInfoDAO.findByDataSourceId("ds-1"))
            .thenReturn(List.of(importedTable("sandbox", "imported", null, null)));
        when(dataSourceService.getDataSourceBriefs(Set.of("ds-1")))
            .thenReturn(Map.of("ds-1", new DataSourceService.DsBrief("音乐库", DbType.POSTGRESQL, "sandbox", null)));
        when(jdbcMetaService.getTables("ds-1", "sandbox"))
            .thenReturn(List.of(new Table("physical1", null)));

        ListTablesExecutor capped = new ListTablesExecutor(dataScopeService, tableInfoDAO,
            dataSourceService, termService, jdbcMetaService, 0);
        TableListData data = (TableListData) capped.execute(ctx(List.of(schemaScope("sandbox"))));

        assertThat(data.dataSources().getFirst().tables())
            .extracting(TableDef::table)
            .containsExactly("imported");
    }

    @Test
    @DisplayName("physical metadata failure keeps imported tables and does not fail")
    void physicalMetadataFailureKeepsImportedTables() throws SQLException {
        when(dataScopeService.getResolvedDataSourceIds("svc-1")).thenReturn(Set.of("ds-1"));
        when(tableInfoDAO.findByDataSourceId("ds-1"))
            .thenReturn(List.of(importedTable("public", "users", "users table", List.of())));
        when(dataSourceService.getDataSourceBriefs(Set.of("ds-1")))
            .thenReturn(Map.of("ds-1", new DataSourceService.DsBrief("音乐库", DbType.POSTGRESQL, "public", null)));
        when(jdbcMetaService.getTables("ds-1", "sandbox"))
            .thenThrow(new SQLException("metadata unavailable"));

        TableListData data = (TableListData) executor.execute(
            ctx(List.of(schemaScope("sandbox"))));

        assertThat(data.dataSources()).hasSize(1);
        assertThat(data.dataSources().getFirst().tables())
            .extracting(TableDef::table)
            .containsExactly("users");
    }

    @Test
    @DisplayName("imported-table mode does not read physical tables")
    void importedTablesModeDoesNotReadPhysicalTables() {
        McpServiceDataScope scope = new McpServiceDataScope();
        scope.setScopeType(McpDataScopeType.DATA_SOURCE);
        scope.setReferenceId("ds-1");
        when(dataScopeService.getResolvedDataSourceIds("svc-1")).thenReturn(Set.of("ds-1"));
        when(tableInfoDAO.findByDataSourceId("ds-1"))
            .thenReturn(List.of(importedTable("public", "users", null, null)));
        when(dataSourceService.getDataSourceBriefs(Set.of("ds-1")))
            .thenReturn(Map.of("ds-1", new DataSourceService.DsBrief("音乐库", DbType.POSTGRESQL, "public", null)));

        executor.execute(ctx(List.of(scope)));

        verifyNoInteractions(jdbcMetaService);
    }

    @Test
    @DisplayName("lists business terms when subject scopes are present")
    void listsTermsWhenSubjectScopesPresent() {
        McpServiceDataScope subjectScope = new McpServiceDataScope();
        subjectScope.setScopeType(McpDataScopeType.SUBJECT);
        subjectScope.setReferenceId("sub-1");

        when(termService.getTermsBySubjectIds(List.of("sub-1")))
            .thenReturn(List.of(new com.dati.semantic.domain.service.TermService.TermInfo("GMV", "成交总额", "电商主题")));
        when(dataScopeService.getResolvedDataSourceIds("svc-1")).thenReturn(Set.of());

        TableListData data = (TableListData) executor.execute(ctx(List.of(subjectScope)));

        assertThat(data.dataSources()).isEmpty();
        assertThat(data.terms()).hasSize(1);
        assertThat(data.terms().getFirst().name()).isEqualTo("GMV");
        assertThat(data.terms().getFirst().description()).isEqualTo("成交总额");
        assertThat(data.terms().getFirst().subjectName()).isEqualTo("电商主题");
    }
}
