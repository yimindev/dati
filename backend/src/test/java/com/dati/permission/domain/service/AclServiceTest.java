package com.dati.permission.domain.service;

import com.dati.auth.authentication.User;
import com.dati.auth.domain.service.UserService;
import com.dati.base.RequestContext;
import com.dati.base.exception.DatiException;
import com.dati.base.exception.ErrorCode;
import com.dati.datasource.repository.dao.DataSourceDAO;
import com.dati.datasource.repository.po.DataSourcePO;
import com.dati.mcp.repository.dao.McpServiceDAO;
import com.dati.permission.domain.model.Permission;
import com.dati.permission.domain.model.PrincipalType;
import com.dati.permission.domain.model.ResourceAcl;
import com.dati.permission.domain.model.ResourceType;
import com.dati.permission.repository.dao.ResourceAclDAO;
import com.dati.permission.repository.po.ResourceAclPO;
import com.dati.semantic.repository.dao.SubjectDAO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AclService unit tests")
class AclServiceTest {

    @Mock private ResourceAclDAO aclDAO;
    @Mock private UserService userService;
    @Mock private DataSourceDAO dataSourceDAO;
    @Mock private SubjectDAO subjectDAO;
    @Mock private McpServiceDAO mcpServiceDAO;
    @Mock private PermissionChecker checker;

    private AclService service;

    @BeforeEach
    void setUp() {
        service = new AclService(aclDAO, userService, dataSourceDAO, subjectDAO,
                mcpServiceDAO, new PermissionService(checker, "admin"));
        User user = new User();
        user.setId("u-1");
        user.setName("u1");
        RequestContext.setUser(user);
    }

    @AfterEach
    void tearDown() {
        RequestContext.getContext().clear();
    }

    /** 当前用户 u1 不是资源 owner（owner=u2），ACL 无记录 → 拒绝。 */
    private void denyAcl() {
        when(checker.can(anyString(), any(), anyString(), any())).thenReturn(false);
    }

    /** 当前用户 u1 被 ACL 授权（VIEW/EDIT 均可）。 */
    private void allowAcl() {
        when(checker.can(anyString(), any(), anyString(), any())).thenReturn(true);
    }

    @Test
    void grantRequiresEditorPermission() {
        when(dataSourceDAO.findById("ds-1")).thenReturn(Optional.of(dsPo()));
        denyAcl();
        DatiException ex = assertThrows(DatiException.class,
                () -> service.grant(ResourceType.DATA_SOURCE, "ds-1", PrincipalType.USER, "u3", Permission.VIEW));
        assertThat(ex.getCode()).isEqualTo(ErrorCode.PERMISSION_DENIED);
    }

    @Test
    void grantRejectsUnknownPrincipal() {
        when(dataSourceDAO.findById("ds-1")).thenReturn(Optional.of(dsPo()));
        allowAcl();
        when(userService.getUserMap(List.of("u3"))).thenReturn(Map.of());
        DatiException ex = assertThrows(DatiException.class,
                () -> service.grant(ResourceType.DATA_SOURCE, "ds-1", PrincipalType.USER, "u3", Permission.VIEW));
        assertThat(ex.getCode()).isEqualTo(ErrorCode.INVALID_PARAMETER);
    }

    @Test
    void grantInsertsNewRow() {
        when(dataSourceDAO.findById("ds-1")).thenReturn(Optional.of(dsPo()));
        allowAcl();
        when(userService.getUserMap(List.of("u3"))).thenReturn(Map.of("u3", principalUser()));
        when(aclDAO.findByResourceTypeAndResourceIdAndPrincipalTypeAndPrincipalId(
                ResourceType.DATA_SOURCE, "ds-1", PrincipalType.USER, "u3")).thenReturn(Optional.empty());
        ResourceAclPO saved = new ResourceAclPO();
        saved.setId("acl-1");
        when(aclDAO.save(any(ResourceAclPO.class))).thenReturn(saved);

        String id = service.grant(ResourceType.DATA_SOURCE, "ds-1", PrincipalType.USER, "u3", Permission.VIEW);
        assertThat(id).isEqualTo("acl-1");
    }

    @Test
    void grantUpgradesExistingRow() {
        when(dataSourceDAO.findById("ds-1")).thenReturn(Optional.of(dsPo()));
        allowAcl();
        when(userService.getUserMap(List.of("u3"))).thenReturn(Map.of("u3", principalUser()));
        ResourceAclPO existing = new ResourceAclPO();
        existing.setId("acl-1");
        existing.setPermission(Permission.VIEW);
        when(aclDAO.findByResourceTypeAndResourceIdAndPrincipalTypeAndPrincipalId(
                ResourceType.DATA_SOURCE, "ds-1", PrincipalType.USER, "u3")).thenReturn(Optional.of(existing));
        when(aclDAO.save(any(ResourceAclPO.class))).thenAnswer(inv -> inv.getArgument(0));

        service.grant(ResourceType.DATA_SOURCE, "ds-1", PrincipalType.USER, "u3", Permission.EDIT);
        assertThat(existing.getPermission()).isEqualTo(Permission.EDIT);
    }

    @Test
    void revokeDeletesRow() {
        when(dataSourceDAO.findById("ds-1")).thenReturn(Optional.of(dsPo()));
        allowAcl();
        service.revoke(ResourceType.DATA_SOURCE, "ds-1", PrincipalType.USER, "u3");
        verify(aclDAO).deleteByResourceTypeAndResourceIdAndPrincipalTypeAndPrincipalId(
                ResourceType.DATA_SOURCE, "ds-1", PrincipalType.USER, "u3");
    }

    @Test
    void revokeRequiresEditorPermission() {
        when(dataSourceDAO.findById("ds-1")).thenReturn(Optional.of(dsPo()));
        denyAcl();
        DatiException ex = assertThrows(DatiException.class,
                () -> service.revoke(ResourceType.DATA_SOURCE, "ds-1", PrincipalType.USER, "u3"));
        assertThat(ex.getCode()).isEqualTo(ErrorCode.PERMISSION_DENIED);
    }

    @Test
    void listReturnsAclModelsWithPrincipalNames() {
        when(dataSourceDAO.findById("ds-1")).thenReturn(Optional.of(dsPo()));
        allowAcl();
        when(aclDAO.findByResourceTypeAndResourceId(ResourceType.DATA_SOURCE, "ds-1"))
                .thenReturn(List.of(aclPo()));
        when(userService.getUserMap(List.of("u3"))).thenReturn(Map.of("u3", principalUser()));

        List<ResourceAcl> acls = service.list(ResourceType.DATA_SOURCE, "ds-1");
        assertThat(acls).hasSize(1);
        assertThat(acls.getFirst().getPrincipalName()).isEqualTo("Bob");
    }

    @Test
    void unknownResourceThrowsNotFound() {
        when(dataSourceDAO.findById("ds-1")).thenReturn(Optional.empty());
        DatiException ex = assertThrows(DatiException.class,
                () -> service.list(ResourceType.DATA_SOURCE, "ds-1"));
        assertThat(ex.getCode()).isEqualTo(ErrorCode.DS_NOT_FOUND);
    }

    private DataSourcePO dsPo() {
        DataSourcePO po = new DataSourcePO();
        po.setId("ds-1");
        po.setCreatedBy("u2");
        return po;
    }

    private User principalUser() {
        User u = new User();
        u.setId("u3");
        u.setName("bob");
        u.setDisplayName("Bob");
        return u;
    }

    private ResourceAclPO aclPo() {
        ResourceAclPO po = new ResourceAclPO();
        po.setResourceType(ResourceType.DATA_SOURCE);
        po.setPrincipalType(PrincipalType.USER);
        po.setPrincipalId("u3");
        po.setPermission(Permission.VIEW);
        return po;
    }

    @Test
    void grantPublicAllUsersView() {
        when(dataSourceDAO.findById("ds-1")).thenReturn(Optional.of(dsPo()));
        allowAcl();
        when(aclDAO.findByResourceTypeAndResourceIdAndPrincipalTypeAndPrincipalId(
                ResourceType.DATA_SOURCE, "ds-1", PrincipalType.GROUP, PrincipalType.ALL_USERS)).thenReturn(Optional.empty());
        ResourceAclPO saved = new ResourceAclPO();
        saved.setId("acl-pub");
        when(aclDAO.save(any(ResourceAclPO.class))).thenReturn(saved);

        String id = service.grant(ResourceType.DATA_SOURCE, "ds-1",
                PrincipalType.GROUP, PrincipalType.ALL_USERS, Permission.VIEW);
        assertThat(id).isEqualTo("acl-pub");
    }

    @Test
    void grantPublicRejectsNonAllUsersId() {
        when(dataSourceDAO.findById("ds-1")).thenReturn(Optional.of(dsPo()));
        allowAcl();
        DatiException ex = assertThrows(DatiException.class,
                () -> service.grant(ResourceType.DATA_SOURCE, "ds-1",
                        PrincipalType.GROUP, "team-x", Permission.VIEW));
        assertThat(ex.getCode()).isEqualTo(ErrorCode.INVALID_PARAMETER);
    }

    @Test
    void grantPublicRejectsEditPermission() {
        when(dataSourceDAO.findById("ds-1")).thenReturn(Optional.of(dsPo()));
        allowAcl();
        DatiException ex = assertThrows(DatiException.class,
                () -> service.grant(ResourceType.DATA_SOURCE, "ds-1",
                        PrincipalType.GROUP, PrincipalType.ALL_USERS, Permission.EDIT));
        assertThat(ex.getCode()).isEqualTo(ErrorCode.INVALID_PARAMETER);
    }
}

