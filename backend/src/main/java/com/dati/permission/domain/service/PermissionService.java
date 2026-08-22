package com.dati.permission.domain.service;

import com.dati.auth.authentication.User;
import com.dati.base.RequestContext;
import com.dati.base.exception.DatiException;
import com.dati.base.exception.ErrorCode;
import com.dati.datasource.repository.dao.DataSourceDAO;
import com.dati.datasource.repository.po.DataSourcePO;
import com.dati.mcp.repository.dao.McpServiceDAO;
import com.dati.mcp.repository.po.McpServicePO;
import com.dati.permission.domain.model.Permission;
import com.dati.permission.domain.model.ResourceType;
import com.dati.semantic.repository.dao.SubjectDAO;
import com.dati.semantic.repository.po.SubjectPO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Service
public class PermissionService {

    private final PermissionChecker checker;
    private final DataSourceDAO dataSourceDAO;
    private final SubjectDAO subjectDAO;
    private final McpServiceDAO mcpServiceDAO;
    private final Set<String> adminUsers;

    public PermissionService(PermissionChecker checker,
                             DataSourceDAO dataSourceDAO,
                             SubjectDAO subjectDAO,
                             McpServiceDAO mcpServiceDAO,
                             @Value("${auth.admin-users:}") String adminUsers) {
        this.checker = checker;
        this.dataSourceDAO = dataSourceDAO;
        this.subjectDAO = subjectDAO;
        this.mcpServiceDAO = mcpServiceDAO;
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
        return checker.can(userId, type, resourceId, permission);
    }

    public void require(String userId, String principalName, ResourceType type, String resourceId,
                        Permission permission, String ownerId) {
        if (!can(userId, principalName, type, resourceId, permission, ownerId)) {
            throw new DatiException(ErrorCode.PERMISSION_DENIED);
        }
    }

    /** Evaluate permission with current user context: userId for owner/ACL, userName for admin. */
    public void requireCurrentUser(ResourceType type, String resourceId,
                                   Permission permission, String ownerId) {
        User user = RequestContext.getUser();
        require(user.getId(), user.getName(), type, resourceId, permission, ownerId);
    }

    /** Require current user to have permission on a data source PO. */
    public void requireDataSource(DataSourcePO po, Permission permission) {
        if (po == null) {
            throw new DatiException(ErrorCode.DS_NOT_FOUND);
        }
        requireCurrentUser(ResourceType.DATA_SOURCE, po.getId(), permission, po.getCreatedBy());
    }

    /** Require current user to have permission on a data source by ID. */
    public void requireDataSource(String datasourceId, Permission permission) {
        DataSourcePO po = dataSourceDAO.findById(datasourceId)
                .orElseThrow(() -> new DatiException(ErrorCode.DS_NOT_FOUND, datasourceId));
        requireDataSource(po, permission);
    }

    /** Require current user to have permission on a subject PO. */
    public void requireSubject(SubjectPO po, Permission permission) {
        if (po == null) {
            throw new DatiException(ErrorCode.SM_SUBJECT_NOT_FOUND);
        }
        requireCurrentUser(ResourceType.SUBJECT, po.getId(), permission, po.getCreatedBy());
    }

    /** Require current user to have permission on a subject by ID. */
    public void requireSubject(String subjectId, Permission permission) {
        SubjectPO po = subjectDAO.findById(subjectId)
                .orElseThrow(() -> new DatiException(ErrorCode.SM_SUBJECT_NOT_FOUND, subjectId));
        requireSubject(po, permission);
    }

    /** Require current user to have permission on an MCP service PO. */
    public void requireMcpService(McpServicePO po, Permission permission) {
        if (po == null) {
            throw new DatiException(ErrorCode.MS_SERVICE_NOT_FOUND);
        }
        requireCurrentUser(ResourceType.MCP_SERVICE, po.getId(), permission, po.getCreatedBy());
    }

    /** Require current user to have permission on an MCP service by ID. */
    public void requireMcpService(String serviceId, Permission permission) {
        McpServicePO po = mcpServiceDAO.findById(serviceId)
                .orElseThrow(() -> new DatiException(ErrorCode.MS_SERVICE_NOT_FOUND, serviceId));
        requireMcpService(po, permission);
    }
}
