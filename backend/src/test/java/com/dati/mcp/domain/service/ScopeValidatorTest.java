package com.dati.mcp.domain.service;

import com.dati.datasource.repository.dao.TableInfoDAO;
import com.dati.datasource.repository.po.TableInfoPO;
import com.dati.db.analysis.TableRef;
import com.dati.mcp.domain.model.McpDataScopeType;
import com.dati.mcp.domain.model.McpServiceDataScope;
import com.dati.mcp.domain.model.ToolError;
import com.dati.semantic.repository.dao.SubjectTableDAO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ScopeValidator unit tests")
class ScopeValidatorTest {

    @Mock
    private TableInfoDAO tableInfoDAO;

    @Mock
    private SubjectTableDAO subjectTableDAO;

    @InjectMocks
    private ScopeValidator validator;

    private static final String DS_ID = "ds-1";

    private static McpServiceDataScope dataSourceScope(String dsId) {
        McpServiceDataScope s = new McpServiceDataScope();
        s.setScopeType(McpDataScopeType.DATA_SOURCE);
        s.setReferenceId(dsId);
        return s;
    }

    private static McpServiceDataScope subjectScope() {
        McpServiceDataScope s = new McpServiceDataScope();
        s.setScopeType(McpDataScopeType.SUBJECT);
        s.setReferenceId("sub-1");
        return s;
    }

    private static TableInfoPO table() {
        TableInfoPO po = new TableInfoPO();
        po.setDataSourceId(ScopeValidatorTest.DS_ID);
        po.setSchema("public");
        po.setName("users");
        return po;
    }

    @Test
    @DisplayName("Empty scopeItems → throws TOOL_SCOPE_VIOLATION")
    void shouldRejectEmptyScope() {
        assertThatThrownBy(() -> validator.validate(List.of(), DS_ID, Set.of(), null))
            .isInstanceOf(ToolExecuteException.class)
            .extracting(e -> ((ToolExecuteException) e).getToolError())
            .isEqualTo(ToolError.SCOPE_VIOLATION);
    }

    @Test
    @DisplayName("DATA_SOURCE covers dsId → passes (no table-level check)")
    void shouldPassWhenDataSourceScopeCovers() {
        assertThatCode(() ->
            validator.validate(List.of(dataSourceScope(DS_ID)), DS_ID, Set.of(), null))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("SUBJECT covers dsId → passes (no table-level check)")
    void shouldPassWhenSubjectScopeCovers() {
        Page<TableInfoPO> page = new PageImpl<>(List.of(table()));
        when(subjectTableDAO.findTablesBySubjectId(anyString(), any(Pageable.class))).thenReturn(page);

        assertThatCode(() ->
            validator.validate(List.of(subjectScope()), DS_ID, Set.of(), null))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("dsId not covered by any scope → throws ToolExecuteException")
    void shouldRejectWhenDsNotInScope() {
        assertThatThrownBy(() ->
            validator.validate(List.of(dataSourceScope("ds-other")), DS_ID, Set.of(), null))
            .isInstanceOf(ToolExecuteException.class)
            .extracting(e -> ((ToolExecuteException) e).getToolError())
            .isEqualTo(ToolError.SCOPE_VIOLATION);
    }

    @Test
    @DisplayName("SQL without schema + defaultSchema set → resolved and matched against allowed tables")
    void shouldResolveSchemaUsingDefaultSchema() {
        when(tableInfoDAO.findByDataSourceId(DS_ID))
            .thenReturn(List.of(table()));

        assertThatCode(() ->
            validator.validate(
                List.of(dataSourceScope(DS_ID)), DS_ID,
                Set.of(new TableRef(null, "users")), "public"))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("SQL without schema + null defaultSchema → unresolvable, throws SCOPE_VIOLATION")
    void shouldRejectUnqualifiedTableWhenDefaultSchemaNull() {
        when(tableInfoDAO.findByDataSourceId(DS_ID))
            .thenReturn(List.of(table()));

        assertThatThrownBy(() ->
            validator.validate(
                List.of(dataSourceScope(DS_ID)), DS_ID,
                Set.of(new TableRef(null, "users")), null))
            .isInstanceOf(ToolExecuteException.class)
            .extracting(e -> ((ToolExecuteException) e).getToolError())
            .isEqualTo(ToolError.SCOPE_VIOLATION);
    }
}
