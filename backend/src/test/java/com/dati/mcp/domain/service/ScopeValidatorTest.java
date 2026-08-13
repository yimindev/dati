package com.dati.mcp.domain.service;

import com.dati.datasource.repository.dao.TableInfoDAO;
import com.dati.datasource.repository.po.TableInfoPO;
import com.dati.db.analysis.TableRef;
import com.dati.mcp.domain.model.McpDataScopeType;
import com.dati.mcp.domain.model.McpServiceDataScope;
import com.dati.mcp.domain.model.ToolError;
import com.dati.semantic.repository.dao.SubjectDAO;
import com.dati.semantic.repository.dao.SubjectTableDAO;
import com.dati.semantic.repository.po.SubjectPO;
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
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
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

    @Mock
    private SubjectDAO subjectDAO;

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

    private static SubjectPO subject() {
        SubjectPO po = new SubjectPO();
        po.setId("sub-1");
        po.setName("销售");
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

    @Test
    @DisplayName("validateDataSource passes when ds covered by DATA_SOURCE scope")
    void validateDataSourcePasses() {
        assertThatCode(() -> validator.validateDataSource(List.of(dataSourceScope("ds-1")), "ds-1"))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("validateDataSource passes when ds covered via SUBJECT scope tables")
    void validateDataSourcePassesViaSubjectTables() {
        when(subjectTableDAO.findTablesBySubjectId("sub-1", Pageable.unpaged()))
            .thenReturn(new PageImpl<>(List.of(table())));

        assertThatCode(() -> validator.validateDataSource(List.of(subjectScope()), DS_ID))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("validateDataSource rejects ds outside scope")
    void validateDataSourceRejects() {
        assertThatThrownBy(() -> validator.validateDataSource(List.of(dataSourceScope("ds-other")), DS_ID))
            .isInstanceOf(ToolExecuteException.class)
            .satisfies(e -> assertThat(((ToolExecuteException) e).getToolError())
                .isEqualTo(ToolError.SCOPE_VIOLATION));
    }

    @Test
    @DisplayName("validateDataSource rejects empty scope")
    void validateDataSourceRejectsEmptyScope() {
        assertThatThrownBy(() -> validator.validateDataSource(List.of(), DS_ID))
            .isInstanceOf(ToolExecuteException.class)
            .satisfies(e -> assertThat(((ToolExecuteException) e).getToolError())
                .isEqualTo(ToolError.SCOPE_VIOLATION));
    }

    @Test
    @DisplayName("resolveSubjectInScope returns subjectId matching name")
    void resolveSubjectInScopeMatches() {
        when(subjectDAO.findById("sub-1")).thenReturn(Optional.of(subject()));

        String subjectId = validator.resolveSubjectInScope(List.of(subjectScope()), "销售");

        assertThat(subjectId).isEqualTo("sub-1");
    }

    @Test
    @DisplayName("resolveSubjectInScope takes the first match in scope order")
    void resolveSubjectInScopeFirstMatchWins() {
        McpServiceDataScope first = subjectScope();
        McpServiceDataScope second = new McpServiceDataScope();
        second.setScopeType(McpDataScopeType.SUBJECT);
        second.setReferenceId("sub-2");
        when(subjectDAO.findById("sub-1")).thenReturn(Optional.of(subject()));

        String subjectId = validator.resolveSubjectInScope(List.of(first, second), "销售");

        assertThat(subjectId).isEqualTo("sub-1");
    }

    @Test
    @DisplayName("resolveSubjectInScope rejects subject not in scope")
    void resolveSubjectInScopeRejects() {
        when(subjectDAO.findById("sub-1")).thenReturn(Optional.of(subject()));

        assertThatThrownBy(() -> validator.resolveSubjectInScope(List.of(subjectScope()), "财务"))
            .isInstanceOf(ToolExecuteException.class)
            .satisfies(e -> assertThat(((ToolExecuteException) e).getToolError())
                .isEqualTo(ToolError.SCOPE_VIOLATION));
    }
}
