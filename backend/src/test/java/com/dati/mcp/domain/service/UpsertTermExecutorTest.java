package com.dati.mcp.domain.service;

import com.dati.mcp.domain.model.McpToolType;
import com.dati.mcp.domain.model.ToolConfig;
import com.dati.mcp.domain.model.ToolError;
import com.dati.mcp.domain.model.param.UpsertTermArgs;
import com.dati.mcp.repository.dao.McpMetadataAuditLogDAO;
import com.dati.mcp.repository.po.McpMetadataAuditLogPO;
import com.dati.mcp.server.pojo.MetadataUpdateData;
import com.dati.mcp.server.pojo.MetadataUpdateResult;
import com.dati.semantic.domain.model.Term;
import com.dati.semantic.domain.service.TermService;
import com.dati.semantic.repository.dao.TermDAO;
import com.dati.semantic.repository.po.TermPO;
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
@DisplayName("UpsertTermExecutor tests")
class UpsertTermExecutorTest {

    @Mock
    private ScopeValidator scopeValidator;

    @Mock
    private TermDAO termDAO;

    @Mock
    private TermService termService;

    @Mock
    private McpMetadataAuditLogDAO auditLogDAO;

    private UpsertTermExecutor executor;

    @BeforeEach
    void setUp() {
        executor = new UpsertTermExecutor(scopeValidator, termDAO, termService, auditLogDAO);
    }

    private ToolExecutionContext ctx(UpsertTermArgs args) {
        return new ToolExecutionContext("svc-1", McpToolType.UPSERT_TERM,
            new ToolConfig.UpdateMetadataConfig(), args, List.of());
    }

    private static UpsertTermArgs.UpsertTermItem item(String subject, String name,
                                                      String description, List<String> aliases) {
        return new UpsertTermArgs.UpsertTermItem(subject, name, description, aliases);
    }

    @Test
    @DisplayName("missing term is created (CREATE) with audit row")
    void createsMissingTerm() {
        when(scopeValidator.resolveSubjectInScope(any(), eq("销售"))).thenReturn("sub-1");
        when(termDAO.findBySubjectIdAndName("sub-1", "退货单")).thenReturn(Optional.empty());
        when(termService.createTerm(any())).thenAnswer(inv -> {
            Term t = inv.getArgument(0);
            t.setId("term-1");
            return t;
        });

        MetadataUpdateData data = (MetadataUpdateData) executor.execute(ctx(
            new UpsertTermArgs(List.of(item("销售", "退货单", "return order", List.of("返单"))))));

        ArgumentCaptor<Term> createCaptor = ArgumentCaptor.forClass(Term.class);
        verify(termService).createTerm(createCaptor.capture());
        assertThat(createCaptor.getValue().getSubjectId()).isEqualTo("sub-1");
        assertThat(createCaptor.getValue().getName()).isEqualTo("退货单");
        assertThat(createCaptor.getValue().getDescription()).isEqualTo("return order");

        ArgumentCaptor<McpMetadataAuditLogPO> auditCaptor = ArgumentCaptor.forClass(McpMetadataAuditLogPO.class);
        verify(auditLogDAO).save(auditCaptor.capture());
        McpMetadataAuditLogPO po = auditCaptor.getValue();
        assertThat(po.getToolType()).isEqualTo("UPSERT_TERM");
        assertThat(po.getEntityType()).isEqualTo("TERM");
        assertThat(po.getEntityId()).isEqualTo("term-1");
        assertThat(po.getEntityName()).isEqualTo("退货单");
        assertThat(po.getChangeType()).isEqualTo("CREATE");
        assertThat(po.getOldValue()).isNull();

        MetadataUpdateResult result = data.results().getFirst();
        assertThat(result.success()).isTrue();
        assertThat(result.changeType()).isEqualTo("CREATE");
        assertThat(result.old()).isNull();
        assertThat(result.newValue()).containsEntry("description", "return order");
    }

    @Test
    @DisplayName("existing term is updated (UPDATE) with old→new audit")
    void updatesExistingTerm() {
        when(scopeValidator.resolveSubjectInScope(any(), eq("销售"))).thenReturn("sub-1");
        TermPO existing = new TermPO();
        existing.setId("term-1");
        existing.setSubjectId("sub-1");
        existing.setName("退货单");
        existing.setDescription("old desc");
        existing.setAliases(List.of("返单"));
        when(termDAO.findBySubjectIdAndName("sub-1", "退货单")).thenReturn(Optional.of(existing));

        MetadataUpdateData data = (MetadataUpdateData) executor.execute(ctx(
            new UpsertTermArgs(List.of(item("销售", "退货单", "new desc", List.of("返单", "r"))))));

        ArgumentCaptor<Term> updateCaptor = ArgumentCaptor.forClass(Term.class);
        verify(termService).updateTerm(eq("term-1"), updateCaptor.capture());
        assertThat(updateCaptor.getValue().getDescription()).isEqualTo("new desc");
        assertThat(updateCaptor.getValue().getAliases()).containsExactly("返单", "r");

        ArgumentCaptor<McpMetadataAuditLogPO> auditCaptor = ArgumentCaptor.forClass(McpMetadataAuditLogPO.class);
        verify(auditLogDAO).save(auditCaptor.capture());
        assertThat(auditCaptor.getValue().getChangeType()).isEqualTo("UPDATE");
        assertThat(auditCaptor.getValue().getOldValue()).contains("old desc");

        MetadataUpdateResult result = data.results().getFirst();
        assertThat(result.changeType()).isEqualTo("UPDATE");
        assertThat(result.old()).containsEntry("description", "old desc");
        assertThat(result.newValue()).containsEntry("description", "new desc");
    }

    @Test
    @DisplayName("subject outside scope fails that item only (SCOPE_VIOLATION)")
    void subjectOutsideScopeFailsPerItem() {
        doThrow(new ToolExecuteException(ToolError.SCOPE_VIOLATION, "Subject 财务 not in service scope"))
            .when(scopeValidator).resolveSubjectInScope(any(), eq("财务"));

        MetadataUpdateData data = (MetadataUpdateData) executor.execute(ctx(
            new UpsertTermArgs(List.of(item("财务", "应收", "x", null)))));

        MetadataUpdateResult result = data.results().getFirst();
        assertThat(result.success()).isFalse();
        assertThat(result.error().errorCategory()).isEqualTo("SCOPE_ERROR");
        verify(termDAO, never()).findBySubjectIdAndName(any(), any());
        verify(auditLogDAO, never()).save(any());
    }

    @Test
    @DisplayName("dedupes identical subject+name items within one call")
    void dedupesItems() {
        when(scopeValidator.resolveSubjectInScope(any(), eq("销售"))).thenReturn("sub-1");
        when(termDAO.findBySubjectIdAndName("sub-1", "退货单")).thenReturn(Optional.empty());
        when(termService.createTerm(any())).thenAnswer(inv -> {
            Term t = inv.getArgument(0);
            t.setId("term-1");
            return t;
        });

        MetadataUpdateData data = (MetadataUpdateData) executor.execute(ctx(
            new UpsertTermArgs(List.of(
                item("销售", "退货单", "d1", null),
                item("销售", "退货单", "d2", null)))));

        verify(termService, org.mockito.Mockito.times(1)).createTerm(any());
        assertThat(data.results()).hasSize(1);
    }
}
