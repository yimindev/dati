package com.dati.mcp.domain.service;

import com.dati.auth.authentication.User;
import com.dati.auth.domain.service.UserGroupService;
import com.dati.base.RequestContext;
import com.dati.base.exception.DatiException;
import com.dati.base.exception.ErrorCode;
import com.dati.common.StringUtils;
import com.dati.mcp.domain.model.McpService;
import com.dati.mcp.domain.model.McpServiceDataScope;
import com.dati.mcp.domain.model.McpServiceStatus;
import com.dati.mcp.repository.dao.McpCustomToolDAO;
import com.dati.mcp.repository.dao.McpPrebuiltToolConfigDAO;
import com.dati.mcp.repository.dao.McpPromptDAO;
import com.dati.mcp.repository.dao.McpServiceDAO;
import com.dati.mcp.repository.dao.McpServiceDataScopeDAO;
import com.dati.mcp.repository.dao.McpServiceSnapshotDAO;
import com.dati.mcp.repository.mapper.McpServiceMapper;
import com.dati.mcp.repository.mapper.McpServiceDataScopeMapper;
import com.dati.mcp.repository.po.McpServicePO;
import com.dati.mcp.repository.po.McpServiceDataScopePO;
import com.dati.permission.domain.service.PermissionService;
import com.dati.permission.domain.model.Permission;
import com.dati.permission.domain.model.ResourceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.regex.Pattern;

@Service
public class McpServiceService {

    private static final Pattern CODE_PATTERN = Pattern.compile("^[a-z0-9]([a-z0-9_-]{0,62}[a-z0-9])?$");

    private final McpServiceDAO mcpServiceDAO;
    private final McpServiceDataScopeDAO dataScopeDAO;
    private final McpServiceSnapshotDAO snapshotDAO;
    private final McpPrebuiltToolConfigDAO prebuiltToolConfigDAO;
    private final McpCustomToolDAO customToolDAO;
    private final McpPromptDAO promptDAO;
    private final PermissionService permissionService;
    private final McpServiceDataScopeService dataScopeService;
    private final UserGroupService userGroupService;

    public McpServiceService(McpServiceDAO mcpServiceDAO,
                             McpServiceDataScopeDAO dataScopeDAO,
                             McpServiceSnapshotDAO snapshotDAO,
                             McpPrebuiltToolConfigDAO prebuiltToolConfigDAO,
                             McpCustomToolDAO customToolDAO,
                             McpPromptDAO promptDAO,
                             PermissionService permissionService,
                             McpServiceDataScopeService dataScopeService,
                             UserGroupService userGroupService) {
        this.mcpServiceDAO = mcpServiceDAO;
        this.dataScopeDAO = dataScopeDAO;
        this.snapshotDAO = snapshotDAO;
        this.prebuiltToolConfigDAO = prebuiltToolConfigDAO;
        this.customToolDAO = customToolDAO;
        this.promptDAO = promptDAO;
        this.permissionService = permissionService;
        this.dataScopeService = dataScopeService;
        this.userGroupService = userGroupService;
    }

    @Transactional
    public String createMcpService(McpService service, List<McpServiceDataScope> scopes) {
        String code = service.getCode();
        if (code == null || code.isBlank()) {
            throw new DatiException(ErrorCode.MS_SERVICE_CODE_REQUIRED);
        }
        if (!CODE_PATTERN.matcher(code).matches()) {
            throw new DatiException(ErrorCode.MS_SERVICE_CODE_INVALID);
        }
        if (mcpServiceDAO.existsByCode(code)) {
            throw new DatiException(ErrorCode.MS_SERVICE_CODE_EXISTS, code);
        }
        if (scopes == null || scopes.isEmpty()) {
            throw new DatiException(ErrorCode.MS_SERVICE_DATA_SCOPE_REQUIRED);
        }
        dataScopeService.validateScopePermission(scopes);
        service.setStatus(McpServiceStatus.DRAFT);
        McpServicePO po = McpServiceMapper.toPO(service);
        po = mcpServiceDAO.save(po);
        final String serviceId = po.getId();
        List<McpServiceDataScopePO> scopePos = scopes.stream().map(scope -> {
            scope.setServiceId(serviceId);
            return McpServiceDataScopeMapper.toPO(scope);
        }).toList();
        dataScopeDAO.saveAll(scopePos);
        return po.getId();
    }

    public void updateMcpService(String id, McpService service) {
        McpServicePO po = mcpServiceDAO.findById(id)
                .orElseThrow(() -> new DatiException(ErrorCode.MS_SERVICE_NOT_FOUND, id));
        permissionService.requireCurrentUser(ResourceType.MCP_SERVICE, id, Permission.EDIT, po.getCreatedBy());
        if (service.getName() != null) {
            po.setName(service.getName());
        }
        if (service.getDescription() != null) {
            po.setDescription(service.getDescription());
        }
        if (service.getUpdatedBy() != null) {
            po.setUpdatedBy(service.getUpdatedBy());
        }
        mcpServiceDAO.save(po);
    }

    public McpService getMcpService(String id) {
        McpServicePO po = mcpServiceDAO.findById(id)
                .orElseThrow(() -> new DatiException(ErrorCode.MS_SERVICE_NOT_FOUND, id));
        permissionService.requireCurrentUser(ResourceType.MCP_SERVICE, id, Permission.VIEW, po.getCreatedBy());
        return McpServiceMapper.toModel(po);
    }

    public Page<McpService> listMcpServices(String keyword, McpServiceStatus status, Pageable pageable) {
        User user = RequestContext.getUser();
        if (permissionService.isAdmin(user.getName())) {
            return listMcpServicesUnfiltered(keyword, status, pageable);
        }
        var groupIds = userGroupService.groupIdsOf(user.getId());
        if (StringUtils.isEmpty(keyword) && status == null) {
            return mcpServiceDAO.findAllAccessible(user.getId(), groupIds, pageable).map(McpServiceMapper::toModel);
        }
        if (StringUtils.isEmpty(keyword)) {
            return mcpServiceDAO.findAllByStatusAndAccessible(status, user.getId(), groupIds, pageable)
                    .map(McpServiceMapper::toModel);
        }
        if (status == null) {
            return mcpServiceDAO.findAllByNameContainingOrIdAndAccessible(keyword, user.getId(), groupIds, pageable)
                    .map(McpServiceMapper::toModel);
        }
        return mcpServiceDAO.searchByKeywordAndStatusAndAccessible(keyword, status, user.getId(), groupIds, pageable)
                .map(McpServiceMapper::toModel);
    }

    private Page<McpService> listMcpServicesUnfiltered(String keyword, McpServiceStatus status, Pageable pageable) {
        if (StringUtils.isEmpty(keyword) && status == null) {
            return mcpServiceDAO.findAll(pageable).map(McpServiceMapper::toModel);
        }
        if (StringUtils.isEmpty(keyword)) {
            return mcpServiceDAO.findAllByStatus(status, pageable).map(McpServiceMapper::toModel);
        }
        if (status == null) {
            return mcpServiceDAO.findAllByNameContainingOrId(keyword, keyword, pageable)
                    .map(McpServiceMapper::toModel);
        }
        return mcpServiceDAO.searchByKeywordAndStatus(keyword, status, pageable)
                .map(McpServiceMapper::toModel);
    }

    /**
     * 级联删除服务及其全部子数据（快照/数据范围/预置工具/自定义工具/Prompt）。
     * 已发布服务同样允许直接删除，无需先停用。
     */
    @Transactional
    public void deleteMcpService(String serviceId) {
        McpServicePO po = mcpServiceDAO.findById(serviceId)
                .orElseThrow(() -> new DatiException(ErrorCode.MS_SERVICE_NOT_FOUND, serviceId));
        permissionService.requireCurrentUser(ResourceType.MCP_SERVICE, serviceId, Permission.EDIT, po.getCreatedBy());
        snapshotDAO.deleteAllByServiceId(serviceId);
        dataScopeDAO.deleteAllByServiceId(serviceId);
        prebuiltToolConfigDAO.deleteAllByServiceId(serviceId);
        customToolDAO.deleteAllByServiceId(serviceId);
        promptDAO.deleteAllByServiceId(serviceId);
        mcpServiceDAO.deleteById(serviceId);
    }

}
