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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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
    @Mock
    private DataSourceDAO dataSourceDAO;
    @Mock
    private SubjectDAO subjectDAO;
    @Mock
    private McpServiceDAO mcpServiceDAO;

    private PermissionService service;

    @BeforeEach
    void setUp() {
        service = new PermissionService(checker, dataSourceDAO, subjectDAO, mcpServiceDAO, "admin");
        User user = new User();
        user.setId("u-1");
        user.setName("u1");
        user.setDisplayName("User 1");
        RequestContext.setUser(user);
    }

    @AfterEach
    void tearDown() {
        RequestContext.clear();
    }

    @Test
    void adminCanDoAnything() {
        assertThat(service.can("u-1", "admin", ResourceType.DATA_SOURCE, "ds-1",
                Permission.EDIT, "someone-else")).isTrue();
        verify(checker, never()).can(anyString(), any(), anyString(), any());
    }

    @Test
    void ownerHasFullAccess() {
        assertThat(service.can("u-1", "u1", ResourceType.DATA_SOURCE, "ds-1",
                Permission.EDIT, "u-1")).isTrue();
        verify(checker, never()).can(anyString(), any(), anyString(), any());
    }

    @Test
    void ownerNullSkipsOwnerCheck() {
        when(checker.can(eq("u-1"), eq(ResourceType.DATA_SOURCE),
                eq("ds-1"), eq(Permission.VIEW))).thenReturn(true);
        assertThat(service.can("u-1", "u1", ResourceType.DATA_SOURCE, "ds-1",
                Permission.VIEW, null)).isTrue();
    }

    @Test
    void delegatesToCheckerWhenNotAdminNorOwner() {
        when(checker.can(eq("u-1"), eq(ResourceType.DATA_SOURCE),
                eq("ds-1"), eq(Permission.VIEW))).thenReturn(true);
        assertThat(service.can("u-1", "u1", ResourceType.DATA_SOURCE, "ds-1",
                Permission.VIEW, "u-2")).isTrue();
    }

    @Test
    void requireThrowsForbiddenWhenDenied() {
        when(checker.can(anyString(), any(), anyString(), any())).thenReturn(false);
        DatiException ex = assertThrows(DatiException.class, () ->
                service.require("u-1", "u1", ResourceType.DATA_SOURCE, "ds-1", Permission.EDIT, "u-2"));
        assertThat(ex.getCode()).isEqualTo(ErrorCode.PERMISSION_DENIED);
    }

    @Test
    void requireDataSource_successWhenOwner() {
        DataSourcePO po = new DataSourcePO();
        po.setId("ds-1");
        po.setCreatedBy("u-1");
        when(dataSourceDAO.findById("ds-1")).thenReturn(Optional.of(po));

        assertDoesNotThrow(() -> service.requireDataSource("ds-1", Permission.EDIT));
    }

    @Test
    void requireDataSource_throwsNotFoundWhenMissing() {
        when(dataSourceDAO.findById("ds-missing")).thenReturn(Optional.empty());

        DatiException ex = assertThrows(DatiException.class, () ->
                service.requireDataSource("ds-missing", Permission.VIEW));
        assertThat(ex.getCode()).isEqualTo(ErrorCode.DS_NOT_FOUND);
    }

    @Test
    void requireSubject_successWhenOwner() {
        SubjectPO po = new SubjectPO();
        po.setId("sub-1");
        po.setCreatedBy("u-1");
        when(subjectDAO.findById("sub-1")).thenReturn(Optional.of(po));

        assertDoesNotThrow(() -> service.requireSubject("sub-1", Permission.EDIT));
    }

    @Test
    void requireMcpService_successWhenOwner() {
        McpServicePO po = new McpServicePO();
        po.setId("ms-1");
        po.setCreatedBy("u-1");
        when(mcpServiceDAO.findById("ms-1")).thenReturn(Optional.of(po));

        assertDoesNotThrow(() -> service.requireMcpService("ms-1", Permission.EDIT));
    }
}
