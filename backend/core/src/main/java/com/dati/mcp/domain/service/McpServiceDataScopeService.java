package com.dati.mcp.domain.service;

import com.dati.auth.authentication.User;
import com.dati.base.RequestContext;
import com.dati.base.exception.DatiException;
import com.dati.base.exception.ErrorCode;
import com.dati.datasource.repository.dao.DataSourceDAO;
import com.dati.mcp.domain.model.McpDataScopeAccessMode;
import com.dati.mcp.domain.model.McpDataScopeType;
import com.dati.mcp.domain.model.McpServiceDataScope;
import com.dati.mcp.repository.dao.McpServiceDAO;
import com.dati.mcp.repository.dao.McpServiceDataScopeDAO;
import com.dati.mcp.repository.mapper.McpServiceDataScopeMapper;
import com.dati.mcp.repository.po.McpServiceDataScopePO;
import com.dati.mcp.repository.po.McpServicePO;
import com.dati.permission.domain.model.Permission;
import com.dati.permission.domain.model.ResourceType;
import com.dati.permission.domain.service.PermissionService;
import com.dati.semantic.domain.model.Subject;
import com.dati.semantic.domain.service.SubjectService;
import com.dati.semantic.repository.dao.SubjectDAO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class McpServiceDataScopeService {

    private final McpServiceDataScopeDAO dataScopeDAO;
    private final SubjectService subjectService;
    private final McpServiceDAO mcpServiceDAO;
    private final DataSourceDAO dataSourceDAO;
    private final SubjectDAO subjectDAO;
    private final PermissionService permissionService;

    public McpServiceDataScopeService(McpServiceDataScopeDAO dataScopeDAO,
                                       SubjectService subjectService,
                                       McpServiceDAO mcpServiceDAO,
                                       DataSourceDAO dataSourceDAO,
                                       SubjectDAO subjectDAO,
                                       PermissionService permissionService) {
        this.dataScopeDAO = dataScopeDAO;
        this.subjectService = subjectService;
        this.mcpServiceDAO = mcpServiceDAO;
        this.dataSourceDAO = dataSourceDAO;
        this.subjectDAO = subjectDAO;
        this.permissionService = permissionService;
    }

    @Transactional
    public void saveDataScope(String serviceId, List<McpServiceDataScope> scopes) {
        McpServicePO servicePO = mcpServiceDAO.findById(serviceId)
                .orElseThrow(() -> new DatiException(ErrorCode.MS_SERVICE_NOT_FOUND, serviceId));
        permissionService.requireCurrentUser(ResourceType.MCP_SERVICE, serviceId, Permission.EDIT, servicePO.getCreatedBy());
        validateScopePermission(scopes);
        normalizeAndValidateScopes(scopes);
        dataScopeDAO.deleteAllByServiceId(serviceId);
        if (scopes != null && !scopes.isEmpty()) {
            List<McpServiceDataScopePO> pos = scopes.stream()
                    .map(McpServiceDataScopeMapper::toPO)
                    .toList();
            dataScopeDAO.saveAll(pos);
        }
    }

    public List<McpServiceDataScope> getDataScope(String serviceId) {
        McpServicePO servicePO = mcpServiceDAO.findById(serviceId)
                .orElseThrow(() -> new DatiException(ErrorCode.MS_SERVICE_NOT_FOUND, serviceId));
        permissionService.requireCurrentUser(ResourceType.MCP_SERVICE, serviceId, Permission.VIEW, servicePO.getCreatedBy());
        return dataScopeDAO.findAllByServiceId(serviceId).stream()
                .map(McpServiceDataScopeMapper::toModel)
                .toList();
    }

    /**
     * 传播校验：当前用户对被绑定的每个数据源/主题至少 VIEW。
     * 创建/更新/发布 MCP 服务时调用，防止把无权访问的数据源打包进服务。
     */
    public void validateScopePermission(List<McpServiceDataScope> scopes) {
        if (scopes == null) {
            return;
        }
        User user = RequestContext.getUser();
        for (McpServiceDataScope scope : scopes) {
            if (scope.getScopeType() == McpDataScopeType.DATA_SOURCE) {
                String ownerId = dataSourceDAO.findById(scope.getReferenceId())
                        .orElseThrow(() -> new DatiException(ErrorCode.DS_NOT_FOUND, scope.getReferenceId()))
                        .getCreatedBy();
                permissionService.require(user.getId(), user.getName(), ResourceType.DATA_SOURCE,
                        scope.getReferenceId(), Permission.VIEW, ownerId);
            } else if (scope.getScopeType() == McpDataScopeType.SUBJECT) {
                String ownerId = subjectDAO.findById(scope.getReferenceId())
                        .orElseThrow(() -> new DatiException(ErrorCode.SM_SUBJECT_NOT_FOUND, scope.getReferenceId()))
                        .getCreatedBy();
                permissionService.require(user.getId(), user.getName(), ResourceType.SUBJECT,
                        scope.getReferenceId(), Permission.VIEW, ownerId);
            }
        }
    }

    /**
     * Normalizes access configuration and validates it before persistence.
     * SUBJECT scopes are always IMPORTED_TABLES; IMPORTED_TABLES clears allowed schemas;
     * SCHEMA_SCOPE requires at least one non-blank schema.
     */
    public void normalizeAndValidateScopes(List<McpServiceDataScope> scopes) {
        if (scopes == null) {
            return;
        }
        for (McpServiceDataScope scope : scopes) {
            if (scope.getScopeType() == null) {
                throw new DatiException(ErrorCode.INVALID_PARAMETER, "scope_type is required");
            }
            if (scope.getScopeType() == McpDataScopeType.SUBJECT) {
                scope.setAccessMode(McpDataScopeAccessMode.IMPORTED_TABLES);
                scope.setAllowedSchemas(new ArrayList<>());
                continue;
            }

            McpDataScopeAccessMode accessMode = scope.getAccessMode() != null
                    ? scope.getAccessMode() : McpDataScopeAccessMode.IMPORTED_TABLES;
            scope.setAccessMode(accessMode);

            if (accessMode == McpDataScopeAccessMode.IMPORTED_TABLES) {
                scope.setAllowedSchemas(new ArrayList<>());
                continue;
            }

            List<String> schemas = normalizeSchemas(scope.getAllowedSchemas());
            if (schemas.isEmpty()) {
                throw new DatiException(ErrorCode.INVALID_PARAMETER,
                        "SCHEMA_SCOPE requires at least one allowed schema");
            }
            scope.setAllowedSchemas(schemas);
        }
    }

    private List<String> normalizeSchemas(List<String> schemas) {
        if (schemas == null || schemas.isEmpty()) {
            return new ArrayList<>();
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String schema : schemas) {
            if (schema != null && !schema.isBlank()) {
                normalized.add(schema.trim());
            }
        }
        return new ArrayList<>(normalized);
    }

    public Set<String> getResolvedDataSourceIds(String serviceId) {
        List<McpServiceDataScope> scopes = dataScopeDAO.findAllByServiceId(serviceId).stream()
                .map(McpServiceDataScopeMapper::toModel)
                .toList();

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
