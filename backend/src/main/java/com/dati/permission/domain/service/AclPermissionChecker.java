package com.dati.permission.domain.service;

import com.dati.auth.domain.service.UserGroupService;
import com.dati.permission.domain.model.Permission;
import com.dati.permission.domain.model.PrincipalType;
import com.dati.permission.domain.model.ResourceType;
import com.dati.permission.repository.dao.ResourceAclDAO;
import org.springframework.stereotype.Component;

@Component
public class AclPermissionChecker implements PermissionChecker {

    private final ResourceAclDAO aclDAO;
    private final UserGroupService userGroupService;

    public AclPermissionChecker(ResourceAclDAO aclDAO, UserGroupService userGroupService) {
        this.aclDAO = aclDAO;
        this.userGroupService = userGroupService;
    }

    @Override
    public boolean can(String userId, ResourceType resourceType, String resourceId, Permission permission) {
        // 1. 用户个体授权
        if (aclDAO.findByResourceTypeAndResourceIdAndPrincipalTypeAndPrincipalId(
                        resourceType, resourceId, PrincipalType.USER, userId)
                .map(po -> po.getPermission().covers(permission))
                .orElse(false)) {
            return true;
        }
        // 2. 用户所在组授权（含隐式 ALL_USERS 全公开组）
        return aclDAO.findByResourceTypeAndResourceIdAndPrincipalTypeAndPrincipalIdIn(
                        resourceType, resourceId, PrincipalType.GROUP,
                        userGroupService.groupIdsOf(userId))
                .stream()
                .anyMatch(po -> po.getPermission().covers(permission));
    }
}
