package com.dati.permission.domain.service;

import com.dati.auth.repository.dao.UserRepository;
import com.dati.base.RequestContext;
import com.dati.base.exception.DatiException;
import com.dati.base.exception.ErrorCode;
import com.dati.datasource.repository.dao.DataSourceDAO;
import com.dati.mcp.repository.dao.McpServiceDAO;
import com.dati.permission.domain.model.Permission;
import com.dati.permission.domain.model.PrincipalType;
import com.dati.permission.domain.model.ResourceAcl;
import com.dati.permission.domain.model.ResourceType;
import com.dati.permission.repository.dao.ResourceAclDAO;
import com.dati.permission.repository.mapper.ResourceAclMapper;
import com.dati.permission.repository.po.ResourceAclPO;
import com.dati.semantic.repository.dao.SubjectDAO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AclService {

    private final ResourceAclDAO aclDAO;
    private final UserRepository userRepository;
    private final DataSourceDAO dataSourceDAO;
    private final SubjectDAO subjectDAO;
    private final McpServiceDAO mcpServiceDAO;
    private final PermissionService permissionService;

    public AclService(ResourceAclDAO aclDAO,
                      UserRepository userRepository,
                      DataSourceDAO dataSourceDAO,
                      SubjectDAO subjectDAO,
                      McpServiceDAO mcpServiceDAO,
                      PermissionService permissionService) {
        this.aclDAO = aclDAO;
        this.userRepository = userRepository;
        this.dataSourceDAO = dataSourceDAO;
        this.subjectDAO = subjectDAO;
        this.mcpServiceDAO = mcpServiceDAO;
        this.permissionService = permissionService;
    }

    @Transactional
    public String grant(ResourceType type, String resourceId, PrincipalType principalType,
                        String principalId, Permission permission) {
        String ownerId = resolveOwnerId(type, resourceId);
        permissionService.requireCurrentUser(type, resourceId, Permission.EDIT, ownerId);
        if (principalType == PrincipalType.GROUP) {
            // V1 仅支持全公开主体：只读、对所有人可见
            if (!PrincipalType.ALL_USERS.equals(principalId)) {
                throw new DatiException(ErrorCode.INVALID_PARAMETER, "principal_id");
            }
            if (permission != Permission.VIEW) {
                throw new DatiException(ErrorCode.INVALID_PARAMETER, "permission");
            }
        } else if (!userRepository.existsById(principalId)) {
            throw new DatiException(ErrorCode.INVALID_PARAMETER, "principal_id");
        }
        ResourceAclPO po = aclDAO.findByResourceTypeAndResourceIdAndPrincipalTypeAndPrincipalId(
                        type.name(), resourceId, principalType.name(), principalId)
                .orElseGet(ResourceAclPO::new);
        po.setResourceType(type.name());
        po.setResourceId(resourceId);
        po.setPrincipalType(principalType.name());
        po.setPrincipalId(principalId);
        po.setPermission(permission);
        po.setCreatedBy(RequestContext.getUser().getId());
        return aclDAO.save(po).getId();
    }

    @Transactional
    public void revoke(ResourceType type, String resourceId, PrincipalType principalType, String principalId) {
        String ownerId = resolveOwnerId(type, resourceId);
        permissionService.requireCurrentUser(type, resourceId, Permission.EDIT, ownerId);
        aclDAO.deleteByResourceTypeAndResourceIdAndPrincipalTypeAndPrincipalId(
                type.name(), resourceId, principalType.name(), principalId);
    }

    public List<ResourceAcl> list(ResourceType type, String resourceId) {
        String ownerId = resolveOwnerId(type, resourceId);
        permissionService.requireCurrentUser(type, resourceId, Permission.EDIT, ownerId);
        List<ResourceAcl> acls = aclDAO.findByResourceTypeAndResourceId(type.name(), resourceId).stream()
                .map(ResourceAclMapper::toModel)
                .toList();
        fillPrincipalNames(acls);
        return acls;
    }

    /** 批量填充主体用户名（displayName 优先，用户已删除则回退为 null）。 */
    private void fillPrincipalNames(List<ResourceAcl> acls) {
        if (acls.isEmpty()) {
            return;
        }
        var userMap = userRepository.findAllById(
                        acls.stream().map(ResourceAcl::getPrincipalId).distinct().toList())
                .stream()
                .collect(java.util.stream.Collectors.toMap(
                        com.dati.auth.repository.po.UserPO::getId, u -> u));
        acls.forEach(acl -> {
            var user = userMap.get(acl.getPrincipalId());
            if (user != null) {
                acl.setPrincipalName(user.getDisplayName() != null
                        ? user.getDisplayName() : user.getName());
            }
        });
    }

    /** 按资源类型查所属 DAO，返回资源创建者；资源不存在抛对应 NOT_FOUND。 */
    private String resolveOwnerId(ResourceType type, String resourceId) {
        return switch (type) {
            case DATA_SOURCE -> dataSourceDAO.findById(resourceId)
                    .orElseThrow(() -> new DatiException(ErrorCode.DS_NOT_FOUND, resourceId))
                    .getCreatedBy();
            case SUBJECT -> subjectDAO.findById(resourceId)
                    .orElseThrow(() -> new DatiException(ErrorCode.SM_SUBJECT_NOT_FOUND, resourceId))
                    .getCreatedBy();
            case MCP_SERVICE -> mcpServiceDAO.findById(resourceId)
                    .orElseThrow(() -> new DatiException(ErrorCode.MS_SERVICE_NOT_FOUND, resourceId))
                    .getCreatedBy();
        };
    }
}
