package com.dati.mcp.domain.service;

import com.dati.datasource.domain.model.ColumnInfo;
import com.dati.datasource.domain.service.ColumnService;
import com.dati.mcp.domain.model.McpToolType;
import com.dati.mcp.domain.model.ToolConfig;
import com.dati.mcp.domain.model.ToolError;
import com.dati.mcp.domain.model.param.UpdateColumnInfoArgs;
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
@DisplayName("UpdateColumnInfoExecutor tests")
class UpdateColumnInfoExecutorTest {

    @Mock
    private ScopeValidator scopeValidator;

    @Mock
    private MetadataEntityResolver resolver;

    @Mock
    private ColumnService columnService;

    @Mock
    private McpMetadataAuditLogDAO auditLogDAO;

    private UpdateColumnInfoExecutor executor;

    @BeforeEach
    void setUp() {
        executor = new UpdateColumnInfoExecutor(scopeValidator, resolver, columnService, auditLogDAO);
    }

    private ToolExecutionContext ctx(UpdateColumnInfoArgs args) {
        return new ToolExecutionContext("svc-1", McpToolType.UPDATE_COLUMN_INFO,
            new ToolConfig.UpdateMetadataConfig(), args, List.of());
    }

    @Test
    @DisplayName("updates column, writes audit row, returns old→new result")
    void updatesColumnAndAudits() {
        when(resolver.resolveColumn("ds-1", "public", "orders", "status"))
            .thenReturn(Optional.of(new MetadataEntityResolver.ColumnTarget(
                "c1", "t1", "old col desc", List.of("st"))));

        UpdateColumnInfoArgs args = new UpdateColumnInfoArgs(List.of(
            new UpdateColumnInfoArgs.UpdateColumnItem("ds-1", "public", "orders", "status",
                "order status", List.of("stat"))));
        MetadataUpdateData data = (MetadataUpdateData) executor.execute(ctx(args));

        ArgumentCaptor<ColumnInfo> colCaptor = ArgumentCaptor.forClass(ColumnInfo.class);
        verify(columnService).updateColumn(eq("c1"), colCaptor.capture());
        assertThat(colCaptor.getValue().getDescription()).isEqualTo("order status");
        assertThat(colCaptor.getValue().getAliases()).containsExactly("stat");

        ArgumentCaptor<McpMetadataAuditLogPO> auditCaptor = ArgumentCaptor.forClass(McpMetadataAuditLogPO.class);
        verify(auditLogDAO).save(auditCaptor.capture());
        McpMetadataAuditLogPO po = auditCaptor.getValue();
        assertThat(po.getToolType()).isEqualTo("UPDATE_COLUMN_INFO");
        assertThat(po.getEntityType()).isEqualTo("COLUMN");
        assertThat(po.getEntityId()).isEqualTo("c1");
        assertThat(po.getEntityName()).isEqualTo("status");
        assertThat(po.getChangeType()).isEqualTo("UPDATE");

        MetadataUpdateResult result = data.results().getFirst();
        assertThat(result.success()).isTrue();
        assertThat(result.entityType()).isEqualTo("COLUMN");
        assertThat(result.old()).containsEntry("description", "old col desc");
        assertThat(result.newValue()).containsEntry("description", "order status");
    }

    @Test
    @DisplayName("missing column fails that item with ENTITY_NOT_FOUND")
    void missingColumnFailsPerItem() {
        when(resolver.resolveColumn("ds-1", "public", "orders", "ghost"))
            .thenReturn(Optional.empty());

        UpdateColumnInfoArgs args = new UpdateColumnInfoArgs(List.of(
            new UpdateColumnInfoArgs.UpdateColumnItem("ds-1", "public", "orders", "ghost",
                "x", null)));
        MetadataUpdateData data = (MetadataUpdateData) executor.execute(ctx(args));

        MetadataUpdateResult result = data.results().getFirst();
        assertThat(result.success()).isFalse();
        assertThat(result.error().errorCategory()).isEqualTo("PARAM_ERROR");
        assertThat(result.error().message()).contains("ghost");
        verify(columnService, never()).updateColumn(any(), any());
        verify(auditLogDAO, never()).save(any());
    }

    @Test
    @DisplayName("scope violation fails that item only")
    void scopeViolationFailsPerItem() {
        doThrow(new ToolExecuteException(ToolError.SCOPE_VIOLATION, "ds-9 not in scope"))
            .when(scopeValidator).validateDataSource(any(), eq("ds-9"));

        UpdateColumnInfoArgs args = new UpdateColumnInfoArgs(List.of(
            new UpdateColumnInfoArgs.UpdateColumnItem("ds-9", null, "orders", "status",
                "x", null)));
        MetadataUpdateData data = (MetadataUpdateData) executor.execute(ctx(args));

        assertThat(data.results().getFirst().success()).isFalse();
        assertThat(data.results().getFirst().error().errorCategory()).isEqualTo("SCOPE_ERROR");
    }

    @Test
    @DisplayName("dedupes identical column items within one call")
    void dedupesItems() {
        when(resolver.resolveColumn("ds-1", "public", "orders", "status"))
            .thenReturn(Optional.of(new MetadataEntityResolver.ColumnTarget(
                "c1", "t1", "old", List.of())));

        UpdateColumnInfoArgs args = new UpdateColumnInfoArgs(List.of(
            new UpdateColumnInfoArgs.UpdateColumnItem("ds-1", "public", "orders", "status", "d1", null),
            new UpdateColumnInfoArgs.UpdateColumnItem("ds-1", "public", "orders", "status", "d2", null)));
        MetadataUpdateData data = (MetadataUpdateData) executor.execute(ctx(args));

        verify(columnService, org.mockito.Mockito.times(1)).updateColumn(any(), any());
        assertThat(data.results()).hasSize(1);
    }
}
