package com.dati.mcp.server.assembler;

import com.dati.base.BaseAssembler;
import com.dati.datasource.domain.service.DataSourceService;
import com.dati.mcp.domain.model.McpDataScopeType;
import com.dati.mcp.domain.model.McpServiceDataScope;
import com.dati.mcp.server.pojo.DataScopeItemVO;
import com.dati.mcp.server.pojo.DataScopeResponse;
import com.dati.mcp.server.pojo.DataSourceRefVO;
import com.dati.semantic.domain.model.Subject;
import com.dati.semantic.domain.service.SubjectService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class McpDataScopeAssembler extends BaseAssembler {

    private final SubjectService subjectService;
    private final DataSourceService dataSourceService;

    public McpDataScopeAssembler(SubjectService subjectService,
                                  DataSourceService dataSourceService) {
        this.subjectService = subjectService;
        this.dataSourceService = dataSourceService;
    }

    public DataScopeResponse toDataScopeResponse(List<McpServiceDataScope> scopes,
                                                  Set<String> resolvedDataSourceIds) {
        List<String> dsRefIds = scopes.stream()
                .filter(s -> s.getScopeType() == McpDataScopeType.DATA_SOURCE)
                .map(McpServiceDataScope::getReferenceId)
                .distinct()
                .toList();

        List<String> subjectRefIds = scopes.stream()
                .filter(s -> s.getScopeType() == McpDataScopeType.SUBJECT)
                .map(McpServiceDataScope::getReferenceId)
                .distinct()
                .toList();

        Map<String, String> dsNameMap = dataSourceService.getDataSourceNameMap(dsRefIds);
        Map<String, String> subjectNameMap = subjectService.getSubjectsByIds(subjectRefIds).stream()
                .collect(Collectors.toMap(Subject::getId, Subject::getName));

        List<DataScopeItemVO> items = scopes.stream().map(scope -> {
            DataScopeItemVO vo = new DataScopeItemVO();
            vo.setId(scope.getId());
            vo.setScopeType(scope.getScopeType());
            vo.setReferenceId(scope.getReferenceId());
            if (scope.getScopeType() == McpDataScopeType.DATA_SOURCE) {
                vo.setReferenceName(dsNameMap.getOrDefault(scope.getReferenceId(), scope.getReferenceId()));
            } else {
                vo.setReferenceName(subjectNameMap.getOrDefault(scope.getReferenceId(), scope.getReferenceId()));
            }
            return vo;
        }).toList();

        Map<String, String> resolvedNameMap = dataSourceService.getDataSourceNameMap(resolvedDataSourceIds);
        List<DataSourceRefVO> resolvedDataSources = resolvedDataSourceIds.stream()
                .map(id -> new DataSourceRefVO(id, resolvedNameMap.getOrDefault(id, id)))
                .toList();

        DataScopeResponse response = new DataScopeResponse();
        response.setItems(items);
        response.setResolvedDataSources(resolvedDataSources);
        return response;
    }

}
