package com.dati.mcp.domain.service;

import com.dati.TestFixtures;
import com.dati.auth.authentication.User;
import com.dati.base.RequestContext;
import com.dati.base.exception.DatiException;
import com.dati.base.exception.ErrorCode;
import com.dati.mcp.domain.model.McpDataScopeType;
import com.dati.mcp.domain.model.McpServiceDataScope;
import com.dati.mcp.repository.dao.McpServiceDataScopeDAO;
import com.dati.mcp.repository.mapper.McpServiceDataScopeMapper;
import com.dati.datasource.repository.dao.DataSourceDAO;
import com.dati.datasource.repository.po.DataSourcePO;
import com.dati.mcp.repository.dao.McpServiceDAO;
import com.dati.mcp.repository.po.McpServiceDataScopePO;
import com.dati.mcp.repository.po.McpServicePO;
import com.dati.permission.domain.service.PermissionService;
import com.dati.semantic.repository.dao.SubjectDAO;
import com.dati.semantic.repository.po.SubjectPO;
import com.dati.semantic.domain.model.Subject;
import com.dati.semantic.domain.service.SubjectService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("McpServiceDataScopeService unit tests")
class McpServiceDataScopeServiceTest {

    @Mock
    private McpServiceDataScopeDAO dataScopeDAO;

    @Mock
    private SubjectService subjectService;

    @Mock
    private McpServiceDAO mcpServiceDAO;

    @Mock
    private DataSourceDAO dataSourceDAO;

    @Mock
    private SubjectDAO subjectDAO;

    @Mock
    private PermissionService permissionService;

    @InjectMocks
    private McpServiceDataScopeService dataScopeService;

    @Captor
    ArgumentCaptor<List<McpServiceDataScopePO>> captor;

    private McpServiceDataScope testDataSourceScope;
    private McpServiceDataScope testSubjectScope;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setId(TestFixtures.TEST_USER_ID);
        user.setName("qa-user");
        RequestContext.setUser(user);
        lenient().doAnswer(inv -> {
            String ownerId = inv.getArgument(3);
            if (!TestFixtures.TEST_USER_ID.equals(ownerId)) {
                throw new DatiException(ErrorCode.PERMISSION_DENIED);
            }
            return null;
        }).when(permissionService).requireCurrentUser(any(), anyString(), any(), anyString());
        lenient().doAnswer(inv -> {
            String ownerId = inv.getArgument(5);
            if (!TestFixtures.TEST_USER_ID.equals(ownerId)) {
                throw new DatiException(ErrorCode.PERMISSION_DENIED);
            }
            return null;
        }).when(permissionService).require(anyString(), anyString(), any(), anyString(), any(), anyString());
        // 默认服务存在；不存在的场景由具体测试覆盖
        lenient().when(mcpServiceDAO.existsById(anyString())).thenReturn(true);
        testDataSourceScope = new McpServiceDataScope();
        testDataSourceScope.setServiceId(TestFixtures.TEST_MCP_SERVICE_ID);
        testDataSourceScope.setScopeType(McpDataScopeType.DATA_SOURCE);
        testDataSourceScope.setReferenceId(TestFixtures.TEST_DATASOURCE_ID);

        testSubjectScope = new McpServiceDataScope();
        testSubjectScope.setServiceId(TestFixtures.TEST_MCP_SERVICE_ID);
        testSubjectScope.setScopeType(McpDataScopeType.SUBJECT);
        testSubjectScope.setReferenceId("subject-001");
    }

    @AfterEach
    void tearDown() {
        RequestContext.getContext().clear();
    }


    /** stub 默认 scope（ds-001 数据源 + subject-001 主题）的 owner 查询，owner = 当前用户。 */
    private void stubScopeOwners() {
        DataSourcePO ds = new DataSourcePO();
        ds.setId(TestFixtures.TEST_DATASOURCE_ID);
        ds.setCreatedBy(TestFixtures.TEST_USER_ID);
        when(dataSourceDAO.findById(TestFixtures.TEST_DATASOURCE_ID)).thenReturn(Optional.of(ds));
        SubjectPO subject = new SubjectPO();
        subject.setId("subject-001");
        subject.setCreatedBy(TestFixtures.TEST_USER_ID);
        when(subjectDAO.findById("subject-001")).thenReturn(Optional.of(subject));
    }

    @Test
    @DisplayName("Save data scope - full replace, delete then insert")
    void saveDataScope_shouldDeleteThenSave() {
        List<McpServiceDataScope> scopes = List.of(testDataSourceScope, testSubjectScope);
        when(mcpServiceDAO.findById(TestFixtures.TEST_MCP_SERVICE_ID))
                .thenReturn(Optional.of(TestFixtures.createTestMcpServicePO()));
        stubScopeOwners();
        when(dataScopeDAO.saveAll(any())).thenReturn(List.of());

        dataScopeService.saveDataScope(TestFixtures.TEST_MCP_SERVICE_ID, scopes);

        verify(dataScopeDAO).deleteAllByServiceId(TestFixtures.TEST_MCP_SERVICE_ID);
        verify(dataScopeDAO).saveAll(captor.capture());
        List<McpServiceDataScopePO> saved = captor.getValue();
        assertThat(saved).hasSize(2);
        assertThat(saved.get(0).getScopeType()).isEqualTo(McpDataScopeType.DATA_SOURCE);
        assertThat(saved.get(1).getScopeType()).isEqualTo(McpDataScopeType.SUBJECT);
    }


    @Test
    @DisplayName("Save data scope - empty list clears existing")
    void saveDataScope_empty_shouldDeleteAll() {
        when(mcpServiceDAO.findById(TestFixtures.TEST_MCP_SERVICE_ID))
                .thenReturn(Optional.of(TestFixtures.createTestMcpServicePO()));
        dataScopeService.saveDataScope(TestFixtures.TEST_MCP_SERVICE_ID, List.of());

        verify(dataScopeDAO).deleteAllByServiceId(TestFixtures.TEST_MCP_SERVICE_ID);
        verify(dataScopeDAO, times(0)).saveAll(any());
    }

    @Test
    @DisplayName("Query data scope - throws MS_SERVICE_NOT_FOUND when service does not exist")
    void getDataScope_serviceNotFound_shouldThrow() {
        when(mcpServiceDAO.existsById(TestFixtures.TEST_MCP_SERVICE_ID)).thenReturn(false);

        DatiException e = assertThrows(DatiException.class,
            () -> dataScopeService.getDataScope(TestFixtures.TEST_MCP_SERVICE_ID));
        assertThat(e.getCode()).isEqualTo(ErrorCode.MS_SERVICE_NOT_FOUND);
    }

    @Test
    @DisplayName("Query data scope - returns plain model list (no names)")
    void getDataScope_shouldReturnModels() {
        McpServiceDataScopePO po1 = McpServiceDataScopeMapper.toPO(testDataSourceScope);
        when(dataScopeDAO.findAllByServiceId(TestFixtures.TEST_MCP_SERVICE_ID))
                .thenReturn(List.of(po1));

        List<McpServiceDataScope> result = dataScopeService.getDataScope(TestFixtures.TEST_MCP_SERVICE_ID);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getScopeType()).isEqualTo(McpDataScopeType.DATA_SOURCE);
        assertThat(result.getFirst().getReferenceId()).isEqualTo(TestFixtures.TEST_DATASOURCE_ID);
    }

    @Test
    @DisplayName("Query data scope - returns empty list when no records")
    void getDataScope_noRecords_shouldReturnEmpty() {
        when(dataScopeDAO.findAllByServiceId(TestFixtures.TEST_MCP_SERVICE_ID))
                .thenReturn(List.of());

        List<McpServiceDataScope> result = dataScopeService.getDataScope(TestFixtures.TEST_MCP_SERVICE_ID);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Resolve data source IDs - returns directly when only DATA_SOURCE")
    void getResolvedDataSourceIds_onlyDataSource_shouldReturnDirectIds() {
        McpServiceDataScopePO po1 = McpServiceDataScopeMapper.toPO(testDataSourceScope);
        when(dataScopeDAO.findAllByServiceId(TestFixtures.TEST_MCP_SERVICE_ID))
                .thenReturn(List.of(po1));

        Set<String> result = dataScopeService.getResolvedDataSourceIds(TestFixtures.TEST_MCP_SERVICE_ID);

        assertThat(result).containsExactly(TestFixtures.TEST_DATASOURCE_ID);
        verify(subjectService, never()).getSubjectsByIds(any());
    }

    @Test
    @DisplayName("Resolve data source IDs - resolves datasourceId through SUBJECT")
    void getResolvedDataSourceIds_throughSubject_shouldResolve() {
        McpServiceDataScopePO subjectScopePO = McpServiceDataScopeMapper.toPO(testSubjectScope);
        when(dataScopeDAO.findAllByServiceId(TestFixtures.TEST_MCP_SERVICE_ID))
                .thenReturn(List.of(subjectScopePO));

        Subject subject = new Subject();
        subject.setId("subject-001");
        subject.setName("Sales Subject");
        subject.setDatasourceId(TestFixtures.TEST_DATASOURCE_ID);
        when(subjectService.getSubjectsByIds(List.of("subject-001"))).thenReturn(List.of(subject));

        Set<String> result = dataScopeService.getResolvedDataSourceIds(TestFixtures.TEST_MCP_SERVICE_ID);

        assertThat(result).containsExactly(TestFixtures.TEST_DATASOURCE_ID);
    }

    @Test
    @DisplayName("Resolve data source IDs - deduplicates direct refs and subject resolution")
    void getResolvedDataSourceIds_shouldDeduplicate() {
        McpServiceDataScopePO dsScopePO = McpServiceDataScopeMapper.toPO(testDataSourceScope);
        McpServiceDataScopePO subjectScopePO = McpServiceDataScopeMapper.toPO(testSubjectScope);

        when(dataScopeDAO.findAllByServiceId(TestFixtures.TEST_MCP_SERVICE_ID))
                .thenReturn(List.of(dsScopePO, subjectScopePO));

        Subject subject = new Subject();
        subject.setId("subject-001");
        subject.setName("Sales Subject");
        subject.setDatasourceId(TestFixtures.TEST_DATASOURCE_ID);
        when(subjectService.getSubjectsByIds(List.of("subject-001"))).thenReturn(List.of(subject));

        Set<String> result = dataScopeService.getResolvedDataSourceIds(TestFixtures.TEST_MCP_SERVICE_ID);

        assertThat(result).hasSize(1);
        assertThat(result).contains(TestFixtures.TEST_DATASOURCE_ID);
    }

    @Test
    @DisplayName("Resolve data source IDs - empty scope returns empty set")
    void getResolvedDataSourceIds_empty_shouldReturnEmpty() {
        when(dataScopeDAO.findAllByServiceId(TestFixtures.TEST_MCP_SERVICE_ID))
                .thenReturn(List.of());

        Set<String> result = dataScopeService.getResolvedDataSourceIds(TestFixtures.TEST_MCP_SERVICE_ID);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Save data scope - denies without service EDIT permission")
    void saveDataScope_requiresServiceEdit() {
        McpServicePO other = TestFixtures.createTestMcpServicePO();
        other.setCreatedBy("other-user");
        when(mcpServiceDAO.findById(TestFixtures.TEST_MCP_SERVICE_ID)).thenReturn(Optional.of(other));

        DatiException ex = assertThrows(DatiException.class,
                () -> dataScopeService.saveDataScope(TestFixtures.TEST_MCP_SERVICE_ID, List.of(testDataSourceScope)));
        assertThat(ex.getCode()).isEqualTo(ErrorCode.PERMISSION_DENIED);
    }

    @Test
    @DisplayName("Validate scope - rejects data source without VIEW")
    void validateScopePermission_rejectsDataSourceWithoutView() {
        DataSourcePO ds = new DataSourcePO();
        ds.setId(TestFixtures.TEST_DATASOURCE_ID);
        ds.setCreatedBy("other-user");
        when(dataSourceDAO.findById(TestFixtures.TEST_DATASOURCE_ID)).thenReturn(Optional.of(ds));

        DatiException ex = assertThrows(DatiException.class,
                () -> dataScopeService.validateScopePermission(List.of(testDataSourceScope)));
        assertThat(ex.getCode()).isEqualTo(ErrorCode.PERMISSION_DENIED);
    }

    @Test
    @DisplayName("Validate scope - accepts owned data source")
    void validateScopePermission_acceptsOwnedDataSource() {
        DataSourcePO ds = new DataSourcePO();
        ds.setId(TestFixtures.TEST_DATASOURCE_ID);
        ds.setCreatedBy(TestFixtures.TEST_USER_ID);
        when(dataSourceDAO.findById(TestFixtures.TEST_DATASOURCE_ID)).thenReturn(Optional.of(ds));

        assertThatCode(() -> dataScopeService.validateScopePermission(List.of(testDataSourceScope)))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Validate scope - rejects subject without VIEW")
    void validateScopePermission_rejectsSubjectWithoutView() {
        SubjectPO subject = new SubjectPO();
        subject.setId("subject-001");
        subject.setCreatedBy("other-user");
        when(subjectDAO.findById("subject-001")).thenReturn(Optional.of(subject));

        DatiException ex = assertThrows(DatiException.class,
                () -> dataScopeService.validateScopePermission(List.of(testSubjectScope)));
        assertThat(ex.getCode()).isEqualTo(ErrorCode.PERMISSION_DENIED);
    }
}

