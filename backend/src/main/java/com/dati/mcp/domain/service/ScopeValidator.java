package com.dati.mcp.domain.service;

import com.dati.datasource.repository.dao.TableInfoDAO;
import com.dati.datasource.repository.po.TableInfoPO;
import com.dati.db.analysis.TableRef;
import com.dati.mcp.domain.model.McpDataScopeType;
import com.dati.mcp.domain.model.McpServiceDataScope;
import com.dati.mcp.domain.model.ToolError;
import com.dati.semantic.repository.dao.SubjectTableDAO;
import jakarta.annotation.Nullable;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ScopeValidator {

    private final TableInfoDAO tableInfoDAO;
    private final SubjectTableDAO subjectTableDAO;

    public ScopeValidator(TableInfoDAO tableInfoDAO, SubjectTableDAO subjectTableDAO) {
        this.tableInfoDAO = tableInfoDAO;
        this.subjectTableDAO = subjectTableDAO;
    }

    public void validate(List<McpServiceDataScope> scopeItems, String dsId,
                         Set<TableRef> tableRefs, @Nullable String defaultSchema) {
        if (scopeItems == null || scopeItems.isEmpty()) {
            throw new ToolExecuteException(ToolError.SCOPE_VIOLATION, "No data scope configured");
        }

        Set<String> coveredDsIds = new HashSet<>();
        for (McpServiceDataScope scope : scopeItems) {
            if (scope.getScopeType() == McpDataScopeType.DATA_SOURCE) {
                coveredDsIds.add(scope.getReferenceId());
            } else if (scope.getScopeType() == McpDataScopeType.SUBJECT) {
                List<TableInfoPO> tables = subjectTableDAO.findTablesBySubjectId(
                    scope.getReferenceId(), Pageable.unpaged()).getContent();
                for (TableInfoPO ti : tables) {
                    coveredDsIds.add(ti.getDataSourceId());
                }
            }
        }
        if (!coveredDsIds.contains(dsId)) {
            throw new ToolExecuteException(ToolError.SCOPE_VIOLATION,
                "Data source " + dsId + " not in service scope");
        }

        if (tableRefs == null || tableRefs.isEmpty()) return;

        Set<TableRef> allowed = buildAllowedTableRefs(scopeItems, dsId);
        for (TableRef ref : tableRefs) {
            if (allowed.contains(ref)) continue;
            if (tryResolve(ref, defaultSchema, allowed)) continue;
            throw new ToolExecuteException(ToolError.SCOPE_VIOLATION,
                "Table " + ref.qualifiedName() + " not in scope");
        }
    }

    private Set<TableRef> buildAllowedTableRefs(List<McpServiceDataScope> scopeItems, String dsId) {
        Set<TableRef> allowed = new HashSet<>();
        for (McpServiceDataScope scope : scopeItems) {
            if (scope.getScopeType() == McpDataScopeType.DATA_SOURCE
                    && scope.getReferenceId().equals(dsId)) {
                tableInfoDAO.findByDataSourceId(dsId)
                    .forEach(ti -> allowed.add(new TableRef(ti.getSchema(), ti.getName())));
            } else if (scope.getScopeType() == McpDataScopeType.SUBJECT) {
                subjectTableDAO.findTablesBySubjectId(
                    scope.getReferenceId(), Pageable.unpaged()).getContent().stream()
                    .filter(ti -> ti.getDataSourceId().equals(dsId))
                    .forEach(ti -> allowed.add(new TableRef(ti.getSchema(), ti.getName())));
            }
        }
        return allowed;
    }

    /** Try to match tableRef against allowed via schema resolution or stripping. */
    private boolean tryResolve(TableRef ref, @Nullable String defaultSchema, Set<TableRef> allowed) {
        if (ref.schema() == null && defaultSchema != null
                && allowed.contains(new TableRef(defaultSchema, ref.name()))) {
            return true;
        }
        return ref.schema() != null && allowed.contains(new TableRef(null, ref.name()));
    }
}
