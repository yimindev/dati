package com.dati.permission.domain.service;

import com.dati.permission.domain.model.Permission;
import com.dati.permission.domain.model.PrincipalType;
import com.dati.permission.domain.model.ResourceType;
import com.dati.permission.repository.dao.ResourceAclDAO;
import org.springframework.stereotype.Component;

@Component
public class AclPermissionChecker implements PermissionChecker {

    private final ResourceAclDAO aclDAO;

    public AclPermissionChecker(ResourceAclDAO aclDAO) {
        this.aclDAO = aclDAO;
    }

    @Override
    public boolean can(String principalType, String principalId,
                       ResourceType resourceType, String resourceId, Permission permission) {
        // 1. 具体主体授权
        if (aclDAO.findByResourceTypeAndResourceIdAndPrincipalTypeAndPrincipalId(
                        resourceType.name(), resourceId, principalType, principalId)
                .map(po -> po.getPermission().covers(permission))
                .orElse(false)) {
            return true;
        }
        // 2. 全公开（GROUP/ALL_USERS）：仅覆盖只读级别
        if (PrincipalType.USER.name().equals(principalType)) {
            return aclDAO.findByResourceTypeAndResourceIdAndPrincipalTypeAndPrincipalId(
                            resourceType.name(), resourceId, PrincipalType.GROUP.name(), PrincipalType.ALL_USERS)
                    .map(po -> po.getPermission().covers(permission))
                    .orElse(false);
        }
        return false;
    }
}
