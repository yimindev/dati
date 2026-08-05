package com.dati.permission.domain.service;

import com.dati.permission.domain.model.Permission;
import com.dati.permission.domain.model.ResourceType;
import com.dati.permission.repository.dao.ResourceAclDAO;
import com.dati.permission.repository.po.ResourceAclPO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AclPermissionChecker unit tests")
class AclPermissionCheckerTest {

    @Mock
    private ResourceAclDAO aclDAO;

    @InjectMocks
    private AclPermissionChecker checker;

    private ResourceAclPO po(Permission permission) {
        ResourceAclPO po = new ResourceAclPO();
        po.setPermission(permission);
        return po;
    }

    @Test
    void noAclRowMeansDenied() {
        when(aclDAO.findByResourceTypeAndResourceIdAndPrincipalTypeAndPrincipalId(
                "DATA_SOURCE", "ds-1", "USER", "u1")).thenReturn(Optional.empty());
        assertThat(checker.can("USER", "u1", ResourceType.DATA_SOURCE, "ds-1", Permission.VIEW)).isFalse();
    }

    @Test
    void viewPermissionCoversView() {
        when(aclDAO.findByResourceTypeAndResourceIdAndPrincipalTypeAndPrincipalId(
                "DATA_SOURCE", "ds-1", "USER", "u1")).thenReturn(Optional.of(po(Permission.VIEW)));
        assertThat(checker.can("USER", "u1", ResourceType.DATA_SOURCE, "ds-1", Permission.VIEW)).isTrue();
    }

    @Test
    void viewPermissionDoesNotCoverEdit() {
        when(aclDAO.findByResourceTypeAndResourceIdAndPrincipalTypeAndPrincipalId(
                "DATA_SOURCE", "ds-1", "USER", "u1")).thenReturn(Optional.of(po(Permission.VIEW)));
        assertThat(checker.can("USER", "u1", ResourceType.DATA_SOURCE, "ds-1", Permission.EDIT)).isFalse();
    }

    @Test
    void editPermissionCoversBoth() {
        when(aclDAO.findByResourceTypeAndResourceIdAndPrincipalTypeAndPrincipalId(
                "MCP_SERVICE", "svc-1", "USER", "u1")).thenReturn(Optional.of(po(Permission.EDIT)));
        assertThat(checker.can("USER", "u1", ResourceType.MCP_SERVICE, "svc-1", Permission.VIEW)).isTrue();
        assertThat(checker.can("USER", "u1", ResourceType.MCP_SERVICE, "svc-1", Permission.EDIT)).isTrue();
    }

    @Test
    void publicRowGrantsViewToAnyone() {
        when(aclDAO.findByResourceTypeAndResourceIdAndPrincipalTypeAndPrincipalId(
                "DATA_SOURCE", "ds-1", "USER", "u1")).thenReturn(Optional.empty());
        when(aclDAO.findByResourceTypeAndResourceIdAndPrincipalTypeAndPrincipalId(
                "DATA_SOURCE", "ds-1", "GROUP", "ALL_USERS")).thenReturn(Optional.of(po(Permission.VIEW)));
        assertThat(checker.can("USER", "u1", ResourceType.DATA_SOURCE, "ds-1", Permission.VIEW)).isTrue();
    }

    @Test
    void publicRowDoesNotGrantEdit() {
        when(aclDAO.findByResourceTypeAndResourceIdAndPrincipalTypeAndPrincipalId(
                "DATA_SOURCE", "ds-1", "USER", "u1")).thenReturn(Optional.empty());
        when(aclDAO.findByResourceTypeAndResourceIdAndPrincipalTypeAndPrincipalId(
                "DATA_SOURCE", "ds-1", "GROUP", "ALL_USERS")).thenReturn(Optional.of(po(Permission.VIEW)));
        assertThat(checker.can("USER", "u1", ResourceType.DATA_SOURCE, "ds-1", Permission.EDIT)).isFalse();
    }
}
