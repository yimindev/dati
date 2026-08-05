package com.dati.permission.domain.service;

import com.dati.permission.domain.model.Permission;
import com.dati.permission.domain.model.ResourceType;

/**
 * 授权判定 SPI。V1 由 AclPermissionChecker（本地 ACL 表）实现；
 * 未来对接外部权限中心时提供新实现，业务代码零改动。
 */
public interface PermissionChecker {

    boolean can(String principalType, String principalId,
                ResourceType resourceType, String resourceId, Permission permission);
}
