package com.dati.permission.domain.service;

import com.dati.auth.domain.service.UserGroupService;
import com.dati.permission.domain.model.Permission;
import com.dati.permission.domain.model.PrincipalType;
import com.dati.permission.domain.model.ResourceType;
import com.dati.permission.repository.dao.ResourceAclDAO;
import com.dati.permission.repository.po.ResourceAclPO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AclPermissionChecker unit tests")
class AclPermissionCheckerTest {

    @Mock
    private ResourceAclDAO aclDAO;

    @Mock
    private UserGroupService userGroupService;

    @InjectMocks
    private AclPermissionChecker checker;

    private ResourceAclPO po(Permission permission) {
        ResourceAclPO po = new ResourceAclPO();
        po.setPermission(permission);
        return po;
    }

    @Test
    void noAclRowMeansDenied() {
        when(userGroupService.groupIdsOf("u1")).thenReturn(Set.of(PrincipalType.ALL_USERS));
        when(aclDAO.findByResourceTypeAndResourceIdAndPrincipalTypeAndPrincipalId(
                ResourceType.DATA_SOURCE, "ds-1", PrincipalType.USER, "u1")).thenReturn(Optional.empty());
        when(aclDAO.findByResourceTypeAndResourceIdAndPrincipalTypeAndPrincipalIdIn(
                ResourceType.DATA_SOURCE, "ds-1", PrincipalType.GROUP, Set.of(PrincipalType.ALL_USERS))).thenReturn(List.of());
        assertThat(checker.can("u1", ResourceType.DATA_SOURCE, "ds-1", Permission.VIEW)).isFalse();
    }

    @Test
    void viewPermissionCoversView() {
        when(aclDAO.findByResourceTypeAndResourceIdAndPrincipalTypeAndPrincipalId(
                ResourceType.DATA_SOURCE, "ds-1", PrincipalType.USER, "u1")).thenReturn(Optional.of(po(Permission.VIEW)));
        assertThat(checker.can("u1", ResourceType.DATA_SOURCE, "ds-1", Permission.VIEW)).isTrue();
    }

    @Test
    void viewPermissionDoesNotCoverEdit() {
        when(aclDAO.findByResourceTypeAndResourceIdAndPrincipalTypeAndPrincipalId(
                ResourceType.DATA_SOURCE, "ds-1", PrincipalType.USER, "u1")).thenReturn(Optional.of(po(Permission.VIEW)));
        assertThat(checker.can("u1", ResourceType.DATA_SOURCE, "ds-1", Permission.EDIT)).isFalse();
    }

    @Test
    void editPermissionCoversBoth() {
        when(aclDAO.findByResourceTypeAndResourceIdAndPrincipalTypeAndPrincipalId(
                ResourceType.MCP_SERVICE, "svc-1", PrincipalType.USER, "u1")).thenReturn(Optional.of(po(Permission.EDIT)));
        assertThat(checker.can("u1", ResourceType.MCP_SERVICE, "svc-1", Permission.VIEW)).isTrue();
        assertThat(checker.can("u1", ResourceType.MCP_SERVICE, "svc-1", Permission.EDIT)).isTrue();
    }

    @Test
    void publicRowGrantsViewToAnyone() {
        when(userGroupService.groupIdsOf("u1")).thenReturn(Set.of(PrincipalType.ALL_USERS));
        when(aclDAO.findByResourceTypeAndResourceIdAndPrincipalTypeAndPrincipalId(
                ResourceType.DATA_SOURCE, "ds-1", PrincipalType.USER, "u1")).thenReturn(Optional.empty());
        when(aclDAO.findByResourceTypeAndResourceIdAndPrincipalTypeAndPrincipalIdIn(
                ResourceType.DATA_SOURCE, "ds-1", PrincipalType.GROUP, Set.of(PrincipalType.ALL_USERS)))
                .thenReturn(List.of(po(Permission.VIEW)));
        assertThat(checker.can("u1", ResourceType.DATA_SOURCE, "ds-1", Permission.VIEW)).isTrue();
    }

    @Test
    void publicRowDoesNotGrantEdit() {
        when(userGroupService.groupIdsOf("u1")).thenReturn(Set.of(PrincipalType.ALL_USERS));
        when(aclDAO.findByResourceTypeAndResourceIdAndPrincipalTypeAndPrincipalId(
                ResourceType.DATA_SOURCE, "ds-1", PrincipalType.USER, "u1")).thenReturn(Optional.empty());
        when(aclDAO.findByResourceTypeAndResourceIdAndPrincipalTypeAndPrincipalIdIn(
                ResourceType.DATA_SOURCE, "ds-1", PrincipalType.GROUP, Set.of(PrincipalType.ALL_USERS)))
                .thenReturn(List.of(po(Permission.VIEW)));
        assertThat(checker.can("u1", ResourceType.DATA_SOURCE, "ds-1", Permission.EDIT)).isFalse();
    }

    @Test
    void groupRowGrantsViewThroughMembership() {
        // 未来真实团队：用户属于 team-x，组授权生效
        when(userGroupService.groupIdsOf("u1")).thenReturn(Set.of("team-x"));
        when(aclDAO.findByResourceTypeAndResourceIdAndPrincipalTypeAndPrincipalId(
                ResourceType.DATA_SOURCE, "ds-1", PrincipalType.USER, "u1")).thenReturn(Optional.empty());
        when(aclDAO.findByResourceTypeAndResourceIdAndPrincipalTypeAndPrincipalIdIn(
                ResourceType.DATA_SOURCE, "ds-1", PrincipalType.GROUP, Set.of("team-x"))).thenReturn(List.of(po(Permission.VIEW)));
        assertThat(checker.can("u1", ResourceType.DATA_SOURCE, "ds-1", Permission.VIEW)).isTrue();
    }
}
