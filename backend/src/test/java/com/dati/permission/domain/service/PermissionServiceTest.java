package com.dati.permission.domain.service;

import com.dati.base.exception.DatiException;
import com.dati.base.exception.ErrorCode;
import com.dati.permission.domain.model.Permission;
import com.dati.permission.domain.model.ResourceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PermissionService unit tests")
class PermissionServiceTest {

    @Mock
    private PermissionChecker checker;

    private PermissionService service;

    @BeforeEach
    void setUp() {
        service = new PermissionService(checker, "admin");
    }

    @Test
    void adminCanDoAnything() {
        assertThat(service.can("u-1", "admin", ResourceType.DATA_SOURCE, "ds-1",
                Permission.EDIT, "someone-else")).isTrue();
        verify(checker, never()).can(anyString(), anyString(), any(), anyString(), any());
    }

    @Test
    void ownerHasFullAccess() {
        assertThat(service.can("u-1", "u1", ResourceType.DATA_SOURCE, "ds-1",
                Permission.EDIT, "u-1")).isTrue();
        verify(checker, never()).can(anyString(), anyString(), any(), anyString(), any());
    }

    @Test
    void ownerNullSkipsOwnerCheck() {
        when(checker.can(eq("USER"), eq("u-1"), eq(ResourceType.DATA_SOURCE),
                eq("ds-1"), eq(Permission.VIEW))).thenReturn(true);
        assertThat(service.can("u-1", "u1", ResourceType.DATA_SOURCE, "ds-1",
                Permission.VIEW, null)).isTrue();
    }

    @Test
    void delegatesToCheckerWhenNotAdminNorOwner() {
        when(checker.can(eq("USER"), eq("u-1"), eq(ResourceType.DATA_SOURCE),
                eq("ds-1"), eq(Permission.VIEW))).thenReturn(true);
        assertThat(service.can("u-1", "u1", ResourceType.DATA_SOURCE, "ds-1",
                Permission.VIEW, "u-2")).isTrue();
    }

    @Test
    void requireThrowsForbiddenWhenDenied() {
        when(checker.can(anyString(), anyString(), any(), anyString(), any())).thenReturn(false);
        DatiException ex = assertThrows(DatiException.class, () ->
                service.require("u-1", "u1", ResourceType.DATA_SOURCE, "ds-1", Permission.EDIT, "u-2"));
        assertThat(ex.getCode()).isEqualTo(ErrorCode.PERMISSION_DENIED);
    }
}
