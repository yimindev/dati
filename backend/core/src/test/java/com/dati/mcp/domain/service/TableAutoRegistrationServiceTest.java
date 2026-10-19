package com.dati.mcp.domain.service;

import com.dati.auth.authentication.User;
import com.dati.base.RequestContext;
import com.dati.datasource.domain.service.JdbcMetaService;
import com.dati.datasource.repository.dao.ColumnInfoDAO;
import com.dati.datasource.repository.dao.TableInfoDAO;
import com.dati.datasource.repository.po.ColumnInfoPO;
import com.dati.datasource.repository.po.TableInfoPO;
import com.dati.db.Column;
import com.dati.db.Table;
import com.dati.db.analysis.TableRef;
import com.dati.semantic.domain.SemanticEntityType;
import com.dati.semantic.domain.service.SemanticIndexService;
import com.dati.semantic.repository.po.SemanticSearchDocument;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TableAutoRegistrationService tests")
class TableAutoRegistrationServiceTest {

    private static final String DS_ID = "ds-1";
    private static final String SERVICE_ID = "svc-1";
    private static final String SCHEMA = "public";
    private static final String ACTOR = "u-1";

    @Mock
    private TableInfoDAO tableInfoDAO;

    @Mock
    private ColumnInfoDAO columnInfoDAO;

    @Mock
    private JdbcMetaService jdbcMetaService;

    @Mock
    private SemanticIndexService semanticIndexService;

    @InjectMocks
    private TableAutoRegistrationService service;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setId(ACTOR);
        RequestContext.setUser(user);
    }

    @AfterEach
    void tearDown() {
        RequestContext.clear();
    }

    private static Table table(String name, String comment) {
        return new Table(name, comment);
    }

    private static Column column(String name, String type, String comment) {
        return new Column(name, type, comment);
    }

    private void stubPhysicalTable(String name, String comment) throws SQLException {
        when(jdbcMetaService.getTables(DS_ID, SCHEMA)).thenReturn(List.of(table(name, comment)));
    }

    private void stubColumns(String name, Column... columns) throws SQLException {
        when(jdbcMetaService.getColumns(DS_ID, SCHEMA, name)).thenReturn(List.of(columns));
    }

    private void stubNewTable(String physicalName) {
        when(tableInfoDAO.findFirstByDataSourceIdAndSchemaAndNameIgnoreCase(DS_ID, SCHEMA, physicalName))
                .thenReturn(Optional.empty());
    }

    private void stubSavedTable(String tableId) {
        when(tableInfoDAO.save(any(TableInfoPO.class))).thenAnswer(invocation -> {
            TableInfoPO po = invocation.getArgument(0);
            po.setId(tableId);
            return po;
        });
    }

    private void stubSavedColumns(String... columnIds) {
        AtomicInteger index = new AtomicInteger();
        when(columnInfoDAO.save(any(ColumnInfoPO.class))).thenAnswer(invocation -> {
            ColumnInfoPO po = invocation.getArgument(0);
            po.setId(columnIds[index.getAndIncrement()]);
            return po;
        });
    }

    @Test
    @DisplayName("registers new table and columns")
    void registersNewTableAndColumns() throws Exception {
        stubPhysicalTable("orders", "orders table");
        stubColumns("orders", column("id", "INT", "pk"), column("name", "VARCHAR", "name"));
        stubNewTable("orders");
        stubSavedTable("tbl-1");
        stubSavedColumns("col-1", "col-2");

        List<String> ids = service.registerCreatedTables(
                DS_ID, Set.of(new TableRef(SCHEMA, "orders")), SCHEMA, SERVICE_ID);

        assertThat(ids).containsExactly("tbl-1");

        ArgumentCaptor<TableInfoPO> tableCaptor = ArgumentCaptor.forClass(TableInfoPO.class);
        verify(tableInfoDAO).save(tableCaptor.capture());
        TableInfoPO table = tableCaptor.getValue();
        assertThat(table.getDataSourceId()).isEqualTo(DS_ID);
        assertThat(table.getSchema()).isEqualTo(SCHEMA);
        assertThat(table.getName()).isEqualTo("orders");
        assertThat(table.getDescription()).isEqualTo("orders table");
        assertThat(table.getAliases()).isEmpty();
        assertThat(table.getCreatedBy()).isEqualTo(ACTOR);
        assertThat(table.getUpdatedBy()).isEqualTo(ACTOR);

        ArgumentCaptor<ColumnInfoPO> columnCaptor = ArgumentCaptor.forClass(ColumnInfoPO.class);
        verify(columnInfoDAO, times(2)).save(columnCaptor.capture());
        assertThat(columnCaptor.getAllValues())
                .extracting(ColumnInfoPO::getName).containsExactly("id", "name");
        assertThat(columnCaptor.getAllValues())
                .extracting(ColumnInfoPO::getColumnType).containsExactly("INT", "VARCHAR");
        assertThat(columnCaptor.getAllValues())
                .extracting(ColumnInfoPO::getDescription).containsExactly("pk", "name");
        assertThat(columnCaptor.getAllValues()).allSatisfy(column -> {
            assertThat(column.getTableId()).isEqualTo("tbl-1");
            assertThat(column.getCreatedBy()).isEqualTo(ACTOR);
        });
    }

    @Test
    @DisplayName("indexes auto-registered table and columns")
    @SuppressWarnings("unchecked")
    void indexesTableAndColumns() throws Exception {
        stubPhysicalTable("orders", "orders table");
        stubColumns("orders", column("id", "INT", "pk"), column("name", "VARCHAR", "name"));
        stubNewTable("orders");
        stubSavedTable("tbl-1");
        stubSavedColumns("col-1", "col-2");

        service.registerCreatedTables(
                DS_ID, Set.of(new TableRef(SCHEMA, "orders")), SCHEMA, SERVICE_ID);

        ArgumentCaptor<List<SemanticSearchDocument>> captor = ArgumentCaptor.forClass(List.class);
        verify(semanticIndexService).saveBatch(captor.capture());
        List<SemanticSearchDocument> docs = captor.getValue();
        assertThat(docs).hasSize(3);
        assertThat(docs).extracting(SemanticSearchDocument::getId)
                .containsExactly("table:tbl-1", "field:col-1", "field:col-2");
        assertThat(docs).extracting(SemanticSearchDocument::getType)
                .containsExactly(SemanticEntityType.TABLE, SemanticEntityType.FIELD, SemanticEntityType.FIELD);

        SemanticSearchDocument tableDoc = docs.get(0);
        assertThat(tableDoc.getKeywords()).containsExactly("orders");
        assertThat(tableDoc.getEntity().getDatasourceId()).isEqualTo(DS_ID);
        assertThat(tableDoc.getEntity().getTableId()).isEqualTo("tbl-1");
        assertThat(tableDoc.getEntity().getTableName()).isEqualTo("orders");

        SemanticSearchDocument fieldDoc = docs.get(1);
        assertThat(fieldDoc.getKeywords()).containsExactly("id");
        assertThat(fieldDoc.getEntity().getField()).isEqualTo("id");
    }

    @Test
    @DisplayName("resolves unqualified table with the data source default schema")
    void resolvesUnqualifiedTableWithDefaultSchema() throws Exception {
        stubPhysicalTable("orders", "orders table");
        stubColumns("orders", column("id", "INT", null));
        stubNewTable("orders");
        stubSavedTable("tbl-1");
        stubSavedColumns("col-1");

        List<String> ids = service.registerCreatedTables(
                DS_ID, Set.of(TableRef.of("orders")), SCHEMA, SERVICE_ID);

        assertThat(ids).containsExactly("tbl-1");
        ArgumentCaptor<TableInfoPO> captor = ArgumentCaptor.forClass(TableInfoPO.class);
        verify(tableInfoDAO).save(captor.capture());
        assertThat(captor.getValue().getSchema()).isEqualTo(SCHEMA);
    }

    @Test
    @DisplayName("skips table when schema cannot be resolved")
    void skipsTableWhenSchemaCannotBeResolved() {
        List<String> ids = service.registerCreatedTables(
                DS_ID, Set.of(TableRef.of("orders")), null, SERVICE_ID);

        assertThat(ids).isEmpty();
        verifyNoInteractions(jdbcMetaService, tableInfoDAO, columnInfoDAO, semanticIndexService);
    }

    @Test
    @DisplayName("uses the physical table name returned by JDBC metadata")
    void usesPhysicalTableName() throws Exception {
        stubPhysicalTable("ORDERS", "orders table");
        stubColumns("ORDERS", column("ID", "INT", null));
        stubNewTable("ORDERS");
        stubSavedTable("tbl-1");
        stubSavedColumns("col-1");

        service.registerCreatedTables(
                DS_ID, Set.of(new TableRef(SCHEMA, "orders")), SCHEMA, SERVICE_ID);

        ArgumentCaptor<TableInfoPO> captor = ArgumentCaptor.forClass(TableInfoPO.class);
        verify(tableInfoDAO).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("ORDERS");
        verify(tableInfoDAO).findFirstByDataSourceIdAndSchemaAndNameIgnoreCase(DS_ID, SCHEMA, "ORDERS");
    }

    @Test
    @DisplayName("skips an already registered table")
    void skipsAlreadyRegisteredTable() throws Exception {
        stubPhysicalTable("orders", "orders table");
        when(tableInfoDAO.findFirstByDataSourceIdAndSchemaAndNameIgnoreCase(DS_ID, SCHEMA, "orders"))
                .thenReturn(Optional.of(new TableInfoPO()));

        List<String> ids = service.registerCreatedTables(
                DS_ID, Set.of(new TableRef(SCHEMA, "orders")), SCHEMA, SERVICE_ID);

        assertThat(ids).isEmpty();
        verify(jdbcMetaService, never()).getColumns(DS_ID, SCHEMA, "orders");
        verify(tableInfoDAO, never()).save(any(TableInfoPO.class));
        verify(columnInfoDAO, never()).save(any(ColumnInfoPO.class));
        verifyNoInteractions(semanticIndexService);
    }

    @Test
    @DisplayName("skips a table missing from physical metadata")
    void skipsMissingPhysicalTable() throws Exception {
        when(jdbcMetaService.getTables(DS_ID, SCHEMA)).thenReturn(List.of());

        List<String> ids = service.registerCreatedTables(
                DS_ID, Set.of(new TableRef(SCHEMA, "ghost")), SCHEMA, SERVICE_ID);

        assertThat(ids).isEmpty();
        verifyNoInteractions(tableInfoDAO, columnInfoDAO, semanticIndexService);
    }

    @Test
    @DisplayName("skips a table when column metadata is empty")
    void skipsTableWithoutColumns() throws Exception {
        stubPhysicalTable("orders", "orders table");
        stubNewTable("orders");
        when(jdbcMetaService.getColumns(DS_ID, SCHEMA, "orders")).thenReturn(List.of());

        List<String> ids = service.registerCreatedTables(
                DS_ID, Set.of(new TableRef(SCHEMA, "orders")), SCHEMA, SERVICE_ID);

        assertThat(ids).isEmpty();
        verify(tableInfoDAO, never()).save(any(TableInfoPO.class));
        verify(columnInfoDAO, never()).save(any(ColumnInfoPO.class));
        verifyNoInteractions(semanticIndexService);
    }

    @Test
    @DisplayName("JDBC metadata failure does not propagate")
    void jdbcFailureDoesNotPropagate() throws Exception {
        when(jdbcMetaService.getTables(DS_ID, SCHEMA)).thenThrow(new SQLException("down"));

        assertThatCode(() -> service.registerCreatedTables(
                DS_ID, Set.of(new TableRef(SCHEMA, "orders")), SCHEMA, SERVICE_ID))
                .doesNotThrowAnyException();

        verifyNoInteractions(tableInfoDAO, columnInfoDAO, semanticIndexService);
    }

    @Test
    @DisplayName("semantic index failure does not fail registration")
    void semanticIndexFailureDoesNotFailRegistration() throws Exception {
        stubPhysicalTable("orders", "orders table");
        stubColumns("orders", column("id", "INT", null));
        stubNewTable("orders");
        stubSavedTable("tbl-1");
        stubSavedColumns("col-1");
        doThrow(new RuntimeException("es down")).when(semanticIndexService).saveBatch(anyList());

        assertThatCode(() -> service.registerCreatedTables(
                DS_ID, Set.of(new TableRef(SCHEMA, "orders")), SCHEMA, SERVICE_ID))
                .doesNotThrowAnyException();

        verify(tableInfoDAO).save(any(TableInfoPO.class));
        verify(columnInfoDAO).save(any(ColumnInfoPO.class));
    }

    @Test
    @DisplayName("uses the MCP service as actor when no request user is present")
    void usesServiceActorWithoutRequestUser() throws Exception {
        RequestContext.clear();
        stubPhysicalTable("orders", "orders table");
        stubColumns("orders", column("id", "INT", null));
        stubNewTable("orders");
        stubSavedTable("tbl-1");
        stubSavedColumns("col-1");

        service.registerCreatedTables(
                DS_ID, Set.of(new TableRef(SCHEMA, "orders")), SCHEMA, SERVICE_ID);

        ArgumentCaptor<TableInfoPO> captor = ArgumentCaptor.forClass(TableInfoPO.class);
        verify(tableInfoDAO).save(captor.capture());
        assertThat(captor.getValue().getCreatedBy()).isEqualTo("mcp:" + SERVICE_ID);
    }

    @Test
    @DisplayName("does nothing when no tables were created")
    void noCreatedTablesDoesNothing() {
        List<String> ids = service.registerCreatedTables(DS_ID, Set.of(), SCHEMA, SERVICE_ID);

        assertThat(ids).isEmpty();
        verifyNoInteractions(jdbcMetaService, tableInfoDAO, columnInfoDAO, semanticIndexService);
    }

    @Test
    @DisplayName("registers multiple created tables")
    void registersMultipleCreatedTables() throws Exception {
        when(jdbcMetaService.getTables(DS_ID, SCHEMA))
                .thenReturn(List.of(table("orders", "orders"), table("items", "items")));
        when(tableInfoDAO.findFirstByDataSourceIdAndSchemaAndNameIgnoreCase(DS_ID, SCHEMA, "orders"))
                .thenReturn(Optional.empty());
        when(tableInfoDAO.findFirstByDataSourceIdAndSchemaAndNameIgnoreCase(DS_ID, SCHEMA, "items"))
                .thenReturn(Optional.empty());
        when(jdbcMetaService.getColumns(DS_ID, SCHEMA, "orders"))
                .thenReturn(List.of(column("id", "INT", null)));
        when(jdbcMetaService.getColumns(DS_ID, SCHEMA, "items"))
                .thenReturn(List.of(column("id", "INT", null)));

        AtomicInteger tableIds = new AtomicInteger();
        when(tableInfoDAO.save(any(TableInfoPO.class))).thenAnswer(invocation -> {
            TableInfoPO po = invocation.getArgument(0);
            po.setId("tbl-" + tableIds.incrementAndGet());
            return po;
        });
        AtomicInteger columnIds = new AtomicInteger();
        when(columnInfoDAO.save(any(ColumnInfoPO.class))).thenAnswer(invocation -> {
            ColumnInfoPO po = invocation.getArgument(0);
            po.setId("col-" + columnIds.incrementAndGet());
            return po;
        });

        List<String> ids = service.registerCreatedTables(DS_ID,
                Set.of(new TableRef(SCHEMA, "orders"), new TableRef(SCHEMA, "items")),
                SCHEMA, SERVICE_ID);

        assertThat(ids).containsExactlyInAnyOrder("tbl-1", "tbl-2");
        verify(tableInfoDAO, times(2)).save(any(TableInfoPO.class));
        verify(columnInfoDAO, times(2)).save(any(ColumnInfoPO.class));
        verify(semanticIndexService).saveBatch(anyList());
    }
}
