package com.dati.mcp.domain.service;

import com.dati.mcp.domain.model.McpDataScopeType;
import com.dati.mcp.domain.model.McpServiceDataScope;
import com.dati.mcp.repository.dao.McpServiceDataScopeDAO;
import com.dati.mcp.repository.mapper.McpServiceDataScopeMapper;
import com.dati.mcp.repository.po.McpServiceDataScopePO;
import com.dati.semantic.domain.model.Subject;
import com.dati.semantic.domain.service.SubjectService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class McpServiceDataScopeService {

    private final McpServiceDataScopeDAO dataScopeDAO;
    private final SubjectService subjectService;

    public McpServiceDataScopeService(McpServiceDataScopeDAO dataScopeDAO,
                                       SubjectService subjectService) {
        this.dataScopeDAO = dataScopeDAO;
        this.subjectService = subjectService;
    }

    @Transactional
    public void saveDataScope(String serviceId, List<McpServiceDataScope> scopes) {
        dataScopeDAO.deleteAllByServiceId(serviceId);
        if (scopes != null && !scopes.isEmpty()) {
            List<McpServiceDataScopePO> pos = scopes.stream()
                    .map(McpServiceDataScopeMapper::toPO)
                    .toList();
            dataScopeDAO.saveAll(pos);
        }
    }

    public List<McpServiceDataScope> getDataScope(String serviceId) {
        return dataScopeDAO.findAllByServiceId(serviceId).stream()
                .map(McpServiceDataScopeMapper::toModel)
                .toList();
    }

    public Set<String> getResolvedDataSourceIds(String serviceId) {
        List<McpServiceDataScope> scopes = getDataScope(serviceId);

        Set<String> dsIds = new LinkedHashSet<>();

        for (McpServiceDataScope scope : scopes) {
            if (scope.getScopeType() == McpDataScopeType.DATA_SOURCE) {
                dsIds.add(scope.getReferenceId());
            }
        }

        List<String> subjectIds = scopes.stream()
                .filter(s -> s.getScopeType() == McpDataScopeType.SUBJECT)
                .map(McpServiceDataScope::getReferenceId)
                .distinct()
                .toList();

        if (!subjectIds.isEmpty()) {
            List<Subject> subjects = subjectService.getSubjectsByIds(subjectIds);
            Set<String> subjectDsIds = subjects.stream()
                    .map(Subject::getDatasourceId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            dsIds.addAll(subjectDsIds);
        }

        return dsIds;
    }

}
