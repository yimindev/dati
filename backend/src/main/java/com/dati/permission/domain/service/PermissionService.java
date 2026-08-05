package com.dati.permission.domain.service;

import com.dati.auth.authentication.User;
import com.dati.base.RequestContext;
import com.dati.base.exception.DatiException;
import com.dati.base.exception.ErrorCode;
import com.dati.permission.domain.model.Permission;
import com.dati.permission.domain.model.ResourceType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Service
public class PermissionService {

    private final PermissionChecker checker;
    private final Set<String> adminUsers;

    public PermissionService(PermissionChecker checker,
                             @Value("${auth.admin-users:}") String adminUsers) {
        this.checker = checker;
        this.adminUsers = new HashSet<>(Arrays.asList(adminUsers.split(",")));
    }

    /** 全局管理员按用户名（登录名）配置，如 auth.admin-users: admin。 */
    public boolean isAdmin(String userName) {
        return adminUsers.contains(userName);
    }

    /**
     * 判定顺序：全局管理员（用户名）→ 资源创建者 owner（用户 UUID，与 createdBy 对齐）→ PermissionChecker（ACL 主体为用户 UUID）。
     * ownerId 为 null 时跳过 owner 判定（调用方未加载资源）。
     */
    public boolean can(String userId, String principalName, ResourceType type, String resourceId,
                       Permission permission, String ownerId) {
        if (isAdmin(principalName)) {
            return true;
        }
        if (ownerId != null && ownerId.equals(userId)) {
            return true;
        }
        return checker.can("USER", userId, type, resourceId, permission);
    }

    public void require(String userId, String principalName, ResourceType type, String resourceId,
                        Permission permission, String ownerId) {
        if (!can(userId, principalName, type, resourceId, permission, ownerId)) {
            throw new DatiException(ErrorCode.PERMISSION_DENIED);
        }
    }

    /** 以当前登录用户执行判定：userId（UUID）用于 owner 与 ACL 判定，userName 用于管理员判定。 */
    public void requireCurrentUser(ResourceType type, String resourceId,
                                   Permission permission, String ownerId) {
        User user = RequestContext.getUser();
        require(user.getId(), user.getName(), type, resourceId, permission, ownerId);
    }
}
