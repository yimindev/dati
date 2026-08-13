package com.dati.mcp.domain.service;

import com.dati.datasource.domain.model.TableInfo;
import com.dati.datasource.domain.service.TableService;
import com.dati.mcp.domain.model.McpToolType;
import com.dati.mcp.domain.model.ToolConfig;
import com.dati.mcp.domain.model.ToolError;
import com.dati.mcp.domain.model.param.UpdateTableInfoArgs;
import com.dati.mcp.repository.dao.McpMetadataAuditLogDAO;
import com.dati.mcp.repository.po.McpMetadataAuditLogPO;
import com.dati.mcp.server.pojo.MetadataUpdateData;
import com.dati.mcp.server.pojo.MetadataUpdateResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UpdateTableInfoExecutor tests")
class UpdateTableInfoExecutorTest {

    @Mock
    private ScopeValidator scopeValidator;

    @Mock
    private MetadataEntityResolver resolver;

    @Mock
    private TableService tableService;

    @Mock
    private McpMetadataAuditLogDAO auditLogDAO;

    private UpdateTableInfoExecutor executor;

    @BeforeEach
    void setUp() {
        executor = new UpdateTableInfoExecutor(scopeValidator, resolver, tableService, auditLogDAO);
    }

    private ToolExecutionContext ctx(UpdateTableInfoArgs args) {
        return new ToolExecutionContext("svc-1", McpToolType.UPDATE_TABLE_INFO,
            new ToolConfig.UpdateMetadataConfig(), args, List.of());
    }

    private static UpdateTableInfoArgs.UpdateTableItem item(String dsId, String table,
                                                            String description, List<String> aliases) {
        return new UpdateTableInfoArgs.UpdateTableItem(dsId, null, table, description, aliases);
    }

    @Test
    @DisplayName("updates table, writes audit row, returns old→new result")
    void updatesTableAndAudits() {
        when(resolver.resolveTable("ds-1", null, "orders"))
            .thenReturn(Optional.of(new MetadataEntityResolver.TableTarget(
                "t1", "old desc", List.of("old"))));

        MetadataUpdateData data = (MetadataUpdateData) executor.execute(ctx(
            new UpdateTableInfoArgs(List.of(item("ds-1", "orders", "new desc", List.of("a", "b"))))));

        ArgumentCaptor<TableInfo> tableCaptor = ArgumentCaptor.forClass(TableInfo.class);
        verify(tableService).updateTable(eq("t1"), tableCaptor.capture());
        assertThat(tableCaptor.getValue().getDescription()).isEqualTo("new desc");
        assertThat(tableCaptor.getValue().getAliases()).containsExactly("a", "b");

        ArgumentCaptor<McpMetadataAuditLogPO> auditCaptor = ArgumentCaptor.forClass(McpMetadataAuditLogPO.class);
        verify(auditLogDAO).save(auditCaptor.capture());
        McpMetadataAuditLogPO po = auditCaptor.getValue();
        assertThat(po.getServiceId()).isEqualTo("svc-1");
        assertThat(po.getToolType()).isEqualTo("UPDATE_TABLE_INFO");
        assertThat(po.getEntityType()).isEqualTo("TABLE");
        assertThat(po.getEntityId()).isEqualTo("t1");
        assertThat(po.getEntityName()).isEqualTo("orders");
        assertThat(po.getChangeType()).isEqualTo("UPDATE");
        assertThat(po.getOldValue()).contains("\"description\":\"old desc\"");
        assertThat(po.getNewValue()).contains("\"description\":\"new desc\"");

        MetadataUpdateResult result = data.results().getFirst();
        assertThat(result.success()).isTrue();
        assertThat(result.entityType()).isEqualTo("TABLE");
        assertThat(result.entity()).isEqualTo("orders");
        assertThat(result.changeType()).isEqualTo("UPDATE");
        assertThat(result.old()).containsEntry("description", "old desc");
        assertThat(result.newValue()).containsEntry("description", "new desc");
    }

    @Test
    @DisplayName("omitted description/aliases keep current values")
    void omittedFieldsKeepExisting() {
        when(resolver.resolveTable("ds-1", null, "orders"))
            .thenReturn(Optional.of(new MetadataEntityResolver.TableTarget(
                "t1", "old desc", List.of("keep"))));

        executor.execute(ctx(new UpdateTableInfoArgs(List.of(
            item("ds-1", "orders", null, null)))));

        ArgumentCaptor<TableInfo> tableCaptor = ArgumentCaptor.forClass(TableInfo.class);
        verify(tableService).updateTable(eq("t1"), tableCaptor.capture());
        assertThat(tableCaptor.getValue().getDescription()).isEqualTo("old desc");
        assertThat(tableCaptor.getValue().getAliases()).containsExactly("keep");
    }

    @Test
    @DisplayName("missing table fails that item only (ENTITY_NOT_FOUND → PARAM_ERROR)")
    void missingTableFailsPerItem() {
        when(resolver.resolveTable("ds-1", null, "ghost")).thenReturn(Optional.empty());

        MetadataUpdateData data = (MetadataUpdateData) executor.execute(ctx(
            new UpdateTableInfoArgs(List.of(item("ds-1", "ghost", "x", null)))));

        MetadataUpdateResult result = data.results().getFirst();
        assertThat(result.success()).isFalse();
        assertThat(result.error().errorCategory()).isEqualTo("PARAM_ERROR");
        assertThat(result.error().message()).contains("ghost");
        verify(tableService, never()).updateTable(any(), any());
        verify(auditLogDAO, never()).save(any());
    }

    @Test
    @DisplayName("scope violation fails that item only")
    void scopeViolationFailsPerItem() {
        doThrow(new ToolExecuteException(ToolError.SCOPE_VIOLATION, "ds-9 not in scope"))
            .when(scopeValidator).validateDataSource(any(), eq("ds-9"));

        MetadataUpdateData data = (MetadataUpdateData) executor.execute(ctx(
            new UpdateTableInfoArgs(List.of(item("ds-9", "orders", "x", null)))));

        MetadataUpdateResult result = data.results().getFirst();
        assertThat(result.success()).isFalse();
        assertThat(result.error().errorCategory()).isEqualTo("SCOPE_ERROR");
    }

    @Test
    @DisplayName("dedupes identical items within one call")
    void dedupesItems() {
        when(resolver.resolveTable("ds-1", null, "orders"))
            .thenReturn(Optional.of(new MetadataEntityResolver.TableTarget(
                "t1", "old", List.of())));

        MetadataUpdateData data = (MetadataUpdateData) executor.execute(ctx(
            new UpdateTableInfoArgs(List.of(
                item("ds-1", "orders", "d1", null),
                item("ds-1", "orders", "d2", null)))));

        verify(tableService, org.mockito.Mockito.times(1)).updateTable(any(), any());
        assertThat(data.results()).hasSize(1);
    }

    @Test
    @DisplayName("partial failure: one bad table does not block the other")
    void partialFailureContinues() {
        when(resolver.resolveTable("ds-1", null, "ok"))
            .thenReturn(Optional.of(new MetadataEntityResolver.TableTarget("t1", "old", List.of())));
        when(resolver.resolveTable("ds-1", null, "bad")).thenReturn(Optional.empty());

        MetadataUpdateData data = (MetadataUpdateData) executor.execute(ctx(
            new UpdateTableInfoArgs(List.of(
                item("ds-1", "ok", "d1", null),
                item("ds-1", "bad", "d2", null)))));

        assertThat(data.results()).hasSize(2);
        assertThat(data.results().get(0).success()).isTrue();
        assertThat(data.results().get(1).success()).isFalse();
        verify(tableService, org.mockito.Mockito.times(1)).updateTable(any(), any());
    }
}
