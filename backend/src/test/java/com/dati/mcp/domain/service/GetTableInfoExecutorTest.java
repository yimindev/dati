package com.dati.mcp.domain.service;

import com.dati.datasource.domain.model.TableDef;
import com.dati.datasource.domain.service.TableMetadataService;
import com.dati.datasource.domain.service.TableMetadataService.TableMeta;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetTableInfoExecutor tests")
class GetTableInfoExecutorTest {

    @Mock
    private ScopeValidator scopeValidator;

    @Mock
    private TableMetadataService tableMetadataService;

    private GetTableInfoExecutor executor;

    @BeforeEach
    void setUp() {
        executor = new GetTableInfoExecutor(scopeValidator, tableMetadataService);
    }

    private ToolExecutionContext ctx(GetTableInfoArgs args) {
        return new ToolExecutionContext("svc-1", McpToolType.GET_TABLE_INFO,
            new ToolConfig.GetTableInfoConfig(), args, List.of());
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
        verify(scopeValidator).validate(anyList(), eq("ds-1"), anySet(), isNull());
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
            .when(scopeValidator).validate(anyList(), eq("ds-1"), anySet(), any());
        GetTableInfoArgs args = new GetTableInfoArgs(List.of(
            new GetTableInfoArgs.TableRef("ds-1", null, "orders", null)));

        assertThatThrownBy(() -> executor.execute(ctx(args)))
            .isInstanceOf(ToolExecuteException.class)
            .hasMessageContaining("not in scope");
    }
}
