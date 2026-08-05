package com.dati.mcp.domain.service;

import com.dati.TestFixtures;
import com.dati.auth.authentication.User;
import com.dati.auth.domain.service.UserGroupService;
import com.dati.base.RequestContext;
import com.dati.base.exception.DatiException;
import com.dati.base.exception.ErrorCode;
import com.dati.mcp.domain.model.McpDataScopeType;
import com.dati.mcp.domain.model.McpService;
import com.dati.mcp.domain.model.McpServiceDataScope;
import com.dati.mcp.domain.model.McpServiceStatus;
import com.dati.mcp.repository.dao.McpCustomToolDAO;
import com.dati.mcp.repository.dao.McpPrebuiltToolConfigDAO;
import com.dati.mcp.repository.dao.McpPromptDAO;
import com.dati.mcp.repository.dao.McpServiceDAO;
import com.dati.mcp.repository.dao.McpServiceDataScopeDAO;
import com.dati.mcp.repository.dao.McpServiceSnapshotDAO;
import com.dati.mcp.repository.po.McpServiceDataScopePO;
import com.dati.mcp.repository.po.McpServicePO;
import com.dati.permission.domain.model.PrincipalType;
import com.dati.permission.domain.service.PermissionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("McpServiceService unit tests")
class McpServiceServiceTest {

    @Mock
    private McpServiceDAO mcpServiceDAO;

    @Mock
    private McpServiceDataScopeDAO dataScopeDAO;

    @Mock
    private McpServiceSnapshotDAO snapshotDAO;

    @Mock
    private McpPrebuiltToolConfigDAO prebuiltToolConfigDAO;

    @Mock
    private McpCustomToolDAO customToolDAO;

    @Mock
    private McpPromptDAO promptDAO;

    @Mock
    private PermissionService permissionService;

    @Mock
    private McpServiceDataScopeService dataScopeService;

    @Mock
    private UserGroupService userGroupService;

    @Captor
    ArgumentCaptor<List<McpServiceDataScopePO>> captor;

    @InjectMocks
    private McpServiceService mcpServiceService;

    private McpService testService;
    private McpServicePO testServicePO;

    @BeforeEach
    void setUp() {
        testService = TestFixtures.createTestMcpService();
        testServicePO = TestFixtures.createTestMcpServicePO();
        User user = new User();
        user.setId(TestFixtures.TEST_USER_ID);
        user.setName(TestFixtures.TEST_USER_ID);
        RequestContext.setUser(user);
        lenient().when(permissionService.isAdmin(anyString())).thenReturn(false);
        lenient().when(userGroupService.groupIdsOf(TestFixtures.TEST_USER_ID))
                .thenReturn(Set.of(PrincipalType.ALL_USERS));
        lenient().doAnswer(inv -> {
            String ownerId = inv.getArgument(3);
            if (!TestFixtures.TEST_USER_ID.equals(ownerId)) {
                throw new DatiException(ErrorCode.PERMISSION_DENIED);
            }
            return null;
        }).when(permissionService).requireCurrentUser(any(), anyString(), any(), anyString());
    }

    @AfterEach
    void tearDown() {
        RequestContext.getContext().clear();
    }

    @Test
    @DisplayName("Create MCP service - success with data scopes")
    void createMcpService_shouldReturnId() {
        // given
        McpServicePO savedPO = new McpServicePO();
        savedPO.setId(TestFixtures.TEST_MCP_SERVICE_ID);
        savedPO.setName(testService.getName());
        savedPO.setDescription(testService.getDescription());
        when(mcpServiceDAO.save(any(McpServicePO.class))).thenReturn(savedPO);

        // when
        String result = mcpServiceService.createMcpService(testService, List.of(TestFixtures.createTestDataScope()));

        // then
        assertThat(result).isEqualTo(TestFixtures.TEST_MCP_SERVICE_ID);
        verify(mcpServiceDAO).save(any(McpServicePO.class));
        verify(dataScopeDAO).saveAll(anyList());
    }

    @Test
    @DisplayName("Create MCP service - persists data scopes with service id")
    void createMcpService_withScopes_persistsScopes() {
        // given
        McpServicePO savedPO = new McpServicePO();
        savedPO.setId(TestFixtures.TEST_MCP_SERVICE_ID);
        when(mcpServiceDAO.save(any(McpServicePO.class))).thenReturn(savedPO);
        McpServiceDataScope scope = new McpServiceDataScope();
        scope.setScopeType(McpDataScopeType.DATA_SOURCE);
        scope.setReferenceId(TestFixtures.TEST_DATASOURCE_ID);

        // when
        mcpServiceService.createMcpService(testService, List.of(scope));

        // then
        assertThat(scope.getServiceId()).isEqualTo(TestFixtures.TEST_MCP_SERVICE_ID);
        verify(dataScopeDAO).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(1);
        assertThat(captor.getValue().getFirst().getServiceId()).isEqualTo(TestFixtures.TEST_MCP_SERVICE_ID);
        assertThat(captor.getValue().getFirst().getReferenceId()).isEqualTo(TestFixtures.TEST_DATASOURCE_ID);
    }

    @Test
    @DisplayName("Create MCP service - empty data scopes rejected")
    void createMcpService_emptyScopes_throws() {
        DatiException ex = assertThrows(DatiException.class,
            () -> mcpServiceService.createMcpService(testService, List.of()));
        assertThat(ex.getCode()).isEqualTo(ErrorCode.MS_SERVICE_DATA_SCOPE_REQUIRED);
    }

    @Test
    @DisplayName("Update MCP service - success")
    void updateMcpService_shouldUpdateSuccessfully() {
        // given
        when(mcpServiceDAO.findById(TestFixtures.TEST_MCP_SERVICE_ID)).thenReturn(Optional.of(testServicePO));
        when(mcpServiceDAO.save(any(McpServicePO.class))).thenReturn(testServicePO);

        // when
        mcpServiceService.updateMcpService(TestFixtures.TEST_MCP_SERVICE_ID, testService);

        // then
        verify(mcpServiceDAO).findById(TestFixtures.TEST_MCP_SERVICE_ID);
        verify(mcpServiceDAO).save(any(McpServicePO.class));
    }

    @Test
    @DisplayName("Update MCP service - throws when not found")
    void updateMcpService_shouldThrowWhenNotFound() {
        // given
        when(mcpServiceDAO.findById(TestFixtures.TEST_MCP_SERVICE_ID)).thenReturn(Optional.empty());

        // when & then
        DatiException exception = assertThrows(DatiException.class, () ->
            mcpServiceService.updateMcpService(TestFixtures.TEST_MCP_SERVICE_ID, testService)
        );
        assertThat(exception.getCode()).isEqualTo(ErrorCode.MS_SERVICE_NOT_FOUND);
        verify(mcpServiceDAO).findById(TestFixtures.TEST_MCP_SERVICE_ID);
        verify(mcpServiceDAO, never()).save(any());
    }

    @Test
    @DisplayName("Query MCP service detail - success")
    void getMcpService_shouldReturnService() {
        // given
        when(mcpServiceDAO.findById(TestFixtures.TEST_MCP_SERVICE_ID)).thenReturn(Optional.of(testServicePO));

        // when
        McpService result = mcpServiceService.getMcpService(TestFixtures.TEST_MCP_SERVICE_ID);

        // then
        assertThat(result.getId()).isEqualTo(TestFixtures.TEST_MCP_SERVICE_ID);
        assertThat(result.getName()).isEqualTo(testServicePO.getName());
    }

    @Test
    @DisplayName("Query MCP service detail - denies without VIEW permission")
    void getMcpService_requiresViewPermission() {
        McpServicePO other = TestFixtures.createTestMcpServicePO();
        other.setCreatedBy("other-user");
        when(mcpServiceDAO.findById(TestFixtures.TEST_MCP_SERVICE_ID)).thenReturn(Optional.of(other));

        DatiException ex = assertThrows(DatiException.class,
                () -> mcpServiceService.getMcpService(TestFixtures.TEST_MCP_SERVICE_ID));
        assertThat(ex.getCode()).isEqualTo(ErrorCode.PERMISSION_DENIED);
    }

    @Test
    @DisplayName("Update MCP service - denies without EDIT permission")
    void updateMcpService_requiresEditPermission() {
        McpServicePO other = TestFixtures.createTestMcpServicePO();
        other.setCreatedBy("other-user");
        when(mcpServiceDAO.findById(TestFixtures.TEST_MCP_SERVICE_ID)).thenReturn(Optional.of(other));

        DatiException ex = assertThrows(DatiException.class,
                () -> mcpServiceService.updateMcpService(TestFixtures.TEST_MCP_SERVICE_ID, testService));
        assertThat(ex.getCode()).isEqualTo(ErrorCode.PERMISSION_DENIED);
    }

    @Test
    @DisplayName("Query MCP service detail - throws when not found")
    void getMcpService_shouldThrowWhenNotFound() {
        // given
        when(mcpServiceDAO.findById(TestFixtures.TEST_MCP_SERVICE_ID)).thenReturn(Optional.empty());

        // when & then
        DatiException exception = assertThrows(DatiException.class, () ->
            mcpServiceService.getMcpService(TestFixtures.TEST_MCP_SERVICE_ID)
        );
        assertThat(exception.getCode()).isEqualTo(ErrorCode.MS_SERVICE_NOT_FOUND);
    }

    @Test
    @DisplayName("Paged query - no filters")
    void listMcpServices_withoutFilter() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        Page<McpServicePO> page = new PageImpl<>(List.of(testServicePO));
        when(mcpServiceDAO.findAllAccessible(TestFixtures.TEST_USER_ID,
                Set.of(PrincipalType.ALL_USERS), pageable)).thenReturn(page);

        // when
        Page<McpService> result = mcpServiceService.listMcpServices(null, null, pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getId()).isEqualTo(TestFixtures.TEST_MCP_SERVICE_ID);
        verify(mcpServiceDAO).findAllAccessible(TestFixtures.TEST_USER_ID,
                Set.of(PrincipalType.ALL_USERS), pageable);
    }

    @Test
    @DisplayName("Paged query - with keyword")
    void listMcpServices_withKeyword() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        Page<McpServicePO> page = new PageImpl<>(List.of(testServicePO));
        when(mcpServiceDAO.findAllByNameContainingOrIdAndAccessible("test", TestFixtures.TEST_USER_ID,
                Set.of(PrincipalType.ALL_USERS), pageable)).thenReturn(page);

        // when
        Page<McpService> result = mcpServiceService.listMcpServices("test", null, pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
        verify(mcpServiceDAO).findAllByNameContainingOrIdAndAccessible("test", TestFixtures.TEST_USER_ID,
                Set.of(PrincipalType.ALL_USERS), pageable);
    }

    @Test
    @DisplayName("Paged query - with status filter")
    void listMcpServices_withStatus() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        Page<McpServicePO> page = new PageImpl<>(List.of(testServicePO));
        when(mcpServiceDAO.findAllByStatusAndAccessible(McpServiceStatus.DRAFT, TestFixtures.TEST_USER_ID,
                Set.of(PrincipalType.ALL_USERS), pageable)).thenReturn(page);

        // when
        Page<McpService> result = mcpServiceService.listMcpServices(null, McpServiceStatus.DRAFT, pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
        verify(mcpServiceDAO).findAllByStatusAndAccessible(McpServiceStatus.DRAFT, TestFixtures.TEST_USER_ID,
                Set.of(PrincipalType.ALL_USERS), pageable);
    }

    @Test
    @DisplayName("Paged query - with keyword and status")
    void listMcpServices_withKeywordAndStatus() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        Page<McpServicePO> page = new PageImpl<>(List.of(testServicePO));
        when(mcpServiceDAO.searchByKeywordAndStatusAndAccessible("test", McpServiceStatus.DRAFT,
                TestFixtures.TEST_USER_ID, Set.of(PrincipalType.ALL_USERS), pageable)).thenReturn(page);

        // when
        Page<McpService> result = mcpServiceService.listMcpServices("test", McpServiceStatus.DRAFT, pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
        verify(mcpServiceDAO).searchByKeywordAndStatusAndAccessible("test", McpServiceStatus.DRAFT,
                TestFixtures.TEST_USER_ID, Set.of(PrincipalType.ALL_USERS), pageable);
    }

    @Nested
    @DisplayName("createMcpService - code validation")
    class CodeValidation {

        @Test
        @DisplayName("Blank code throws MS_SERVICE_CODE_REQUIRED")
        void shouldRejectNullCode() {
            McpService service = TestFixtures.createTestMcpService();
            service.setCode(null);

            DatiException ex = assertThrows(DatiException.class,
                () -> mcpServiceService.createMcpService(service, List.of()));
            assertThat(ex.getCode()).isEqualTo(ErrorCode.MS_SERVICE_CODE_REQUIRED);
        }

        @Test
        @DisplayName("Whitespace code throws MS_SERVICE_CODE_REQUIRED")
        void shouldRejectBlankCode() {
            McpService service = TestFixtures.createTestMcpService();
            service.setCode("   ");

            DatiException ex = assertThrows(DatiException.class,
                () -> mcpServiceService.createMcpService(service, List.of()));
            assertThat(ex.getCode()).isEqualTo(ErrorCode.MS_SERVICE_CODE_REQUIRED);
        }

        @Test
        @DisplayName("Code with uppercase letters throws MS_SERVICE_CODE_INVALID")
        void shouldRejectUpperCase() {
            McpService service = TestFixtures.createTestMcpService();
            service.setCode("TestService");

            DatiException ex = assertThrows(DatiException.class,
                () -> mcpServiceService.createMcpService(service, List.of()));
            assertThat(ex.getCode()).isEqualTo(ErrorCode.MS_SERVICE_CODE_INVALID);
        }

        @Test
        @DisplayName("Code with special characters throws MS_SERVICE_CODE_INVALID")
        void shouldRejectSpecialChars() {
            McpService service = TestFixtures.createTestMcpService();
            service.setCode("test service!");

            DatiException ex = assertThrows(DatiException.class,
                () -> mcpServiceService.createMcpService(service, List.of()));
            assertThat(ex.getCode()).isEqualTo(ErrorCode.MS_SERVICE_CODE_INVALID);
        }

        @Test
        @DisplayName("Code with Chinese characters throws MS_SERVICE_CODE_INVALID")
        void shouldRejectChineseChars() {
            McpService service = TestFixtures.createTestMcpService();
            service.setCode("我的服务");

            DatiException ex = assertThrows(DatiException.class,
                () -> mcpServiceService.createMcpService(service, List.of()));
            assertThat(ex.getCode()).isEqualTo(ErrorCode.MS_SERVICE_CODE_INVALID);
        }

        @Test
        @DisplayName("Code starting with hyphen throws MS_SERVICE_CODE_INVALID")
        void shouldRejectLeadingHyphen() {
            McpService service = TestFixtures.createTestMcpService();
            service.setCode("-test");

            DatiException ex = assertThrows(DatiException.class,
                () -> mcpServiceService.createMcpService(service, List.of()));
            assertThat(ex.getCode()).isEqualTo(ErrorCode.MS_SERVICE_CODE_INVALID);
        }

        @Test
        @DisplayName("Code ending with hyphen throws MS_SERVICE_CODE_INVALID")
        void shouldRejectTrailingHyphen() {
            McpService service = TestFixtures.createTestMcpService();
            service.setCode("test-");

            DatiException ex = assertThrows(DatiException.class,
                () -> mcpServiceService.createMcpService(service, List.of()));
            assertThat(ex.getCode()).isEqualTo(ErrorCode.MS_SERVICE_CODE_INVALID);
        }

        @Test
        @DisplayName("Code longer than 64 chars throws MS_SERVICE_CODE_INVALID")
        void shouldRejectTooLong() {
            McpService service = TestFixtures.createTestMcpService();
            service.setCode("a".repeat(65));

            DatiException ex = assertThrows(DatiException.class,
                () -> mcpServiceService.createMcpService(service, List.of()));
            assertThat(ex.getCode()).isEqualTo(ErrorCode.MS_SERVICE_CODE_INVALID);
        }

        @Test
        @DisplayName("Existing code throws MS_SERVICE_CODE_EXISTS")
        void shouldRejectDuplicateCode() {
            McpService service = TestFixtures.createTestMcpService();
            service.setCode("duplicate-code");
            when(mcpServiceDAO.existsByCode("duplicate-code")).thenReturn(true);

            DatiException ex = assertThrows(DatiException.class,
                () -> mcpServiceService.createMcpService(service, List.of()));
            assertThat(ex.getCode()).isEqualTo(ErrorCode.MS_SERVICE_CODE_EXISTS);
        }

        @Test
        @DisplayName("Valid code creates successfully")
        void shouldAcceptValidCode() {
            McpService service = TestFixtures.createTestMcpService();
            service.setCode("my-service-01");
            when(mcpServiceDAO.existsByCode("my-service-01")).thenReturn(false);
            when(mcpServiceDAO.save(any())).thenAnswer(inv -> {
                McpServicePO po = inv.getArgument(0);
                po.setId(TestFixtures.TEST_MCP_SERVICE_ID);
                return po;
            });

            String id = mcpServiceService.createMcpService(service, List.of(TestFixtures.createTestDataScope()));
            assertThat(id).isEqualTo(TestFixtures.TEST_MCP_SERVICE_ID);
        }
    }

    @Nested
    @DisplayName("deleteMcpService - cascade delete")
    class DeleteService {

        @Test
        @DisplayName("Deletes all child tables then the service itself")
        void delete_shouldCascadeAllChildren() {
            // given
            when(mcpServiceDAO.findById(TestFixtures.TEST_MCP_SERVICE_ID))
                    .thenReturn(Optional.of(testServicePO));

            // when
            mcpServiceService.deleteMcpService(TestFixtures.TEST_MCP_SERVICE_ID);

            // then
            verify(snapshotDAO).deleteAllByServiceId(TestFixtures.TEST_MCP_SERVICE_ID);
            verify(dataScopeDAO).deleteAllByServiceId(TestFixtures.TEST_MCP_SERVICE_ID);
            verify(prebuiltToolConfigDAO).deleteAllByServiceId(TestFixtures.TEST_MCP_SERVICE_ID);
            verify(customToolDAO).deleteAllByServiceId(TestFixtures.TEST_MCP_SERVICE_ID);
            verify(promptDAO).deleteAllByServiceId(TestFixtures.TEST_MCP_SERVICE_ID);
            verify(mcpServiceDAO).deleteById(TestFixtures.TEST_MCP_SERVICE_ID);
        }

        @Test
        @DisplayName("Service not found throws MS_SERVICE_NOT_FOUND")
        void delete_shouldThrowWhenServiceNotFound() {
            // given
            when(mcpServiceDAO.findById(TestFixtures.TEST_MCP_SERVICE_ID)).thenReturn(Optional.empty());

            // when & then
            DatiException ex = assertThrows(DatiException.class, () ->
                    mcpServiceService.deleteMcpService(TestFixtures.TEST_MCP_SERVICE_ID));
            assertThat(ex.getCode()).isEqualTo(ErrorCode.MS_SERVICE_NOT_FOUND);
            verify(mcpServiceDAO, never()).deleteById(any());
        }
    }

    @Nested
    @DisplayName("updateMcpService - code protection")
    class CodeProtection {

        @Test
        @DisplayName("Code field is ignored on update (not modified)")
        void shouldNotUpdateCode() {
            McpServicePO existingPO = TestFixtures.createTestMcpServicePO();
            existingPO.setCode("original-code");
            when(mcpServiceDAO.findById(TestFixtures.TEST_MCP_SERVICE_ID))
                .thenReturn(Optional.of(existingPO));

            McpService update = new McpService();
            update.setName("New Name");
            update.setCode("hacked-code");  // 尝试修改 code

            mcpServiceService.updateMcpService(TestFixtures.TEST_MCP_SERVICE_ID, update);

            // 确认 save 的 PO 中 code 未被修改
            verify(mcpServiceDAO).save(argThat(po -> "original-code".equals(po.getCode())));
        }
    }

    @Test
    @DisplayName("Create MCP service - rejects scope without permission (propagation)")
    void createMcpService_rejectsScopeWithoutPermission() {
        McpService service = TestFixtures.createTestMcpService();
        when(mcpServiceDAO.existsByCode(service.getCode())).thenReturn(false);
        org.mockito.Mockito.doThrow(new DatiException(ErrorCode.PERMISSION_DENIED))
                .when(dataScopeService).validateScopePermission(anyList());

        DatiException ex = assertThrows(DatiException.class,
                () -> mcpServiceService.createMcpService(service, List.of(TestFixtures.createTestDataScope())));
        assertThat(ex.getCode()).isEqualTo(ErrorCode.PERMISSION_DENIED);
    }
}

