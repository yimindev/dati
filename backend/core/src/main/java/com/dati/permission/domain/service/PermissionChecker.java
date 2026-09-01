package com.dati.permission.domain.service;

import com.dati.permission.domain.model.Permission;
import com.dati.permission.domain.model.ResourceType;

/**
 * 授权判定 SPI：给定用户是否能以指定权限访问资源。
 * V1 由 AclPermissionChecker（本地 ACL 表）实现，主体匹配（用户个体/所在组）是其内部逻辑；
 * 未来对接外部权限中心时提供新实现，业务代码零改动。
 */
public interface PermissionChecker {

    boolean can(String userId, ResourceType resourceType, String resourceId, Permission permission);
}
