package com.dati.mcp.domain.service;

import com.dati.datasource.domain.model.ColumnDef;
import com.dati.datasource.domain.model.DataSource;
import com.dati.datasource.domain.model.TableDef;
import com.dati.datasource.domain.service.DataSourceService;
import com.dati.datasource.domain.service.TableMetadataService;
import com.dati.datasource.domain.service.TableMetadataService.TableMeta;
import com.dati.mcp.domain.model.McpDataScopeAccessMode;
import com.dati.mcp.domain.model.McpDataScopeType;
import com.dati.mcp.domain.model.McpServiceDataScope;
import com.dati.mcp.domain.model.McpToolType;
import com.dati.mcp.domain.model.ToolConfig;
import com.dati.mcp.domain.model.ToolError;
import com.dati.mcp.domain.model.param.GetTableInfoArgs;
import com.dati.mcp.server.pojo.TableMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetTableInfoExecutor tests")
class GetTableInfoExecutorTest {

    @Mock
    private ScopeValidator scopeValidator;

    @Mock
    private TableMetadataService tableMetadataService;

    @Mock
    private DataSourceService dataSourceService;

    private GetTableInfoExecutor executor;

    @BeforeEach
    void setUp() {
        executor = new GetTableInfoExecutor(scopeValidator, tableMetadataService, dataSourceService);
    }

    private ToolExecutionContext ctx(GetTableInfoArgs args) {
        return ctx(args, List.of());
    }

    private ToolExecutionContext ctx(GetTableInfoArgs args, List<McpServiceDataScope> scopes) {
        return new ToolExecutionContext("svc-1", McpToolType.GET_TABLE_INFO,
            new ToolConfig.GetTableInfoConfig(), args, scopes);
    }

    private static McpServiceDataScope schemaScope(String... schemas) {
        McpServiceDataScope scope = new McpServiceDataScope();
        scope.setScopeType(McpDataScopeType.DATA_SOURCE);
        scope.setReferenceId("ds-1");
        scope.setAccessMode(McpDataScopeAccessMode.SCHEMA_SCOPE);
        scope.setAllowedSchemas(List.of(schemas));
        return scope;
    }

    private static DataSource dataSource(String defaultSchema) {
        DataSource dataSource = new DataSource();
        dataSource.setDefaultSchema(defaultSchema);
        return dataSource;
    }

    private static TableMeta physicalMeta() {
        return new TableMeta(null, "sandbox", "orders", "orders table", List.of(), "ds-1",
            List.of(new ColumnDef("id", "integer", "primary key", List.of(), List.of())));
    }

    @Test
    @DisplayName("reads data_source_id from each tables[] item and returns metadata")
    void readsPerItemDataSourceId() {
        TableMeta meta = new TableMeta("t1", "public", "orders", "Orders table",
            List.of("ord"), "ds-1", List.of());
        when(tableMetadataService.getTableMeta("ds-1", null, "orders")).thenReturn(Optional.of(meta));

        GetTableInfoArgs args = new GetTableInfoArgs(List.of(
            new GetTableInfoArgs.TableRef("ds-1", null, "orders", null)));
        TableMetadata result = (TableMetadata) executor.execute(ctx(args));

        assertThat(result.tables()).hasSize(1);
        TableDef def = result.tables().getFirst();
        assertThat(def.table()).isEqualTo("orders");
        assertThat(def.schema()).isNull();
        assertThat(def.description()).isEqualTo("Orders table");
        verify(scopeValidator).validateDataSource(anyList(), eq("ds-1"));
    }

    @Test
    @DisplayName("unregistered table is skipped, not failed")
    void missingTableSkipped() {
        when(tableMetadataService.getTableMeta("ds-1", null, "ghost")).thenReturn(Optional.empty());

        GetTableInfoArgs args = new GetTableInfoArgs(List.of(
            new GetTableInfoArgs.TableRef("ds-1", null, "ghost", null)));
        TableMetadata result = (TableMetadata) executor.execute(ctx(args));

        assertThat(result.tables()).isEmpty();
    }

    @Test
    @DisplayName("scope violation still enforced per item")
    void scopeViolationStillEnforced() {
        doThrow(new ToolExecuteException(ToolError.SCOPE_VIOLATION, "ds-1 not in scope"))
            .when(scopeValidator).validateDataSource(anyList(), eq("ds-1"));
        GetTableInfoArgs args = new GetTableInfoArgs(List.of(
            new GetTableInfoArgs.TableRef("ds-1", null, "orders", null)));

        assertThatThrownBy(() -> executor.execute(ctx(args)))
            .isInstanceOf(ToolExecuteException.class)
            .hasMessageContaining("not in scope");
    }

    @Test
    @DisplayName("registered table does not use physical fallback")
    void registeredTableDoesNotFallback() {
        TableMeta meta = new TableMeta("t1", "sandbox", "orders", "Orders table",
            List.of("ord"), "ds-1", List.of());
        when(tableMetadataService.getTableMeta("ds-1", "sandbox", "orders"))
            .thenReturn(Optional.of(meta));

        GetTableInfoArgs args = new GetTableInfoArgs(List.of(
            new GetTableInfoArgs.TableRef("ds-1", "sandbox", "orders", null)));
        TableMetadata result = (TableMetadata) executor.execute(ctx(args, List.of(schemaScope("sandbox"))));

        assertThat(result.tables()).hasSize(1);
        verify(tableMetadataService, never()).getPhysicalTableMeta("ds-1", "sandbox", "orders");
    }

    @Test
    @DisplayName("unregistered table in schema scope falls back to physical metadata")
    void schemaScopeFallbackReturnsPhysicalMetadata() {
        when(tableMetadataService.getTableMeta("ds-1", "sandbox", "orders"))
            .thenReturn(Optional.empty());
        when(tableMetadataService.getPhysicalTableMeta("ds-1", "sandbox", "orders"))
            .thenReturn(Optional.of(physicalMeta()));

        GetTableInfoArgs args = new GetTableInfoArgs(List.of(
            new GetTableInfoArgs.TableRef("ds-1", "sandbox", "orders", null)));
        TableMetadata result = (TableMetadata) executor.execute(ctx(args, List.of(schemaScope("sandbox"))));

        assertThat(result.tables()).hasSize(1);
        TableDef def = result.tables().getFirst();
        assertThat(def.table()).isEqualTo("orders");
        assertThat(def.schema()).isEqualTo("sandbox");
        assertThat(def.description()).isEqualTo("orders table");
        assertThat(def.columns()).hasSize(1);
        assertThat(def.columns().getFirst().name()).isEqualTo("id");
        verify(dataSourceService, never()).getDataSourceInternal("ds-1");
    }

    @Test
    @DisplayName("unqualified table resolves default schema for physical fallback")
    void unqualifiedTableUsesDefaultSchemaForFallback() {
        when(tableMetadataService.getTableMeta("ds-1", null, "orders"))
            .thenReturn(Optional.empty());
        when(dataSourceService.getDataSourceInternal("ds-1"))
            .thenReturn(Optional.of(dataSource("sandbox")));
        when(tableMetadataService.getPhysicalTableMeta("ds-1", "sandbox", "orders"))
            .thenReturn(Optional.of(physicalMeta()));

        GetTableInfoArgs args = new GetTableInfoArgs(List.of(
            new GetTableInfoArgs.TableRef("ds-1", null, "orders", null)));
        TableMetadata result = (TableMetadata) executor.execute(ctx(args, List.of(schemaScope("sandbox"))));

        assertThat(result.tables()).hasSize(1);
        assertThat(result.tables().getFirst().schema()).isEqualTo("sandbox");
    }

    @Test
    @DisplayName("unqualified table outside default schema scope is skipped")
    void unqualifiedTableOutsideScopeIsSkipped() {
        when(tableMetadataService.getTableMeta("ds-1", null, "orders"))
            .thenReturn(Optional.empty());
        when(dataSourceService.getDataSourceInternal("ds-1"))
            .thenReturn(Optional.of(dataSource("public")));

        GetTableInfoArgs args = new GetTableInfoArgs(List.of(
            new GetTableInfoArgs.TableRef("ds-1", null, "orders", null)));
        TableMetadata result = (TableMetadata) executor.execute(ctx(args, List.of(schemaScope("sandbox"))));

        assertThat(result.tables()).isEmpty();
        verify(tableMetadataService, never()).getPhysicalTableMeta("ds-1", "public", "orders");
    }

    @Test
    @DisplayName("explicit schema outside scope does not use physical fallback")
    void explicitSchemaOutsideScopeDoesNotFallback() {
        when(tableMetadataService.getTableMeta("ds-1", "public", "orders"))
            .thenReturn(Optional.empty());

        GetTableInfoArgs args = new GetTableInfoArgs(List.of(
            new GetTableInfoArgs.TableRef("ds-1", "public", "orders", null)));
        TableMetadata result = (TableMetadata) executor.execute(ctx(args, List.of(schemaScope("sandbox"))));

        assertThat(result.tables()).isEmpty();
        verify(tableMetadataService, never()).getPhysicalTableMeta("ds-1", "public", "orders");
        verify(dataSourceService, never()).getDataSourceInternal("ds-1");
    }

    @Test
    @DisplayName("empty physical metadata is skipped")
    void emptyPhysicalMetadataIsSkipped() {
        when(tableMetadataService.getTableMeta("ds-1", "sandbox", "orders"))
            .thenReturn(Optional.empty());
        when(tableMetadataService.getPhysicalTableMeta("ds-1", "sandbox", "orders"))
            .thenReturn(Optional.empty());

        GetTableInfoArgs args = new GetTableInfoArgs(List.of(
            new GetTableInfoArgs.TableRef("ds-1", "sandbox", "orders", null)));
        TableMetadata result = (TableMetadata) executor.execute(ctx(args, List.of(schemaScope("sandbox"))));

        assertThat(result.tables()).isEmpty();
    }
}
