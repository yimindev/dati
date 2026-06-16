package com.dati.mcp.domain.service;

import com.dati.TestFixtures;
import com.dati.mcp.domain.model.McpDataScopeType;
import com.dati.mcp.domain.model.McpServiceDataScope;
import com.dati.mcp.repository.dao.McpServiceDataScopeDAO;
import com.dati.mcp.repository.mapper.McpServiceDataScopeMapper;
import com.dati.mcp.repository.po.McpServiceDataScopePO;
import com.dati.semantic.domain.model.Subject;
import com.dati.semantic.domain.service.SubjectService;
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
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("McpServiceDataScopeService 单元测试")
class McpServiceDataScopeServiceTest {

    @Mock
    private McpServiceDataScopeDAO dataScopeDAO;

    @Mock
    private SubjectService subjectService;

    @InjectMocks
    private McpServiceDataScopeService dataScopeService;

    @Captor
    ArgumentCaptor<List<McpServiceDataScopePO>> captor;

    private McpServiceDataScope testDataSourceScope;
    private McpServiceDataScope testSubjectScope;

    @BeforeEach
    void setUp() {
        testDataSourceScope = new McpServiceDataScope();
        testDataSourceScope.setServiceId(TestFixtures.TEST_MCP_SERVICE_ID);
        testDataSourceScope.setScopeType(McpDataScopeType.DATA_SOURCE);
        testDataSourceScope.setReferenceId(TestFixtures.TEST_DATASOURCE_ID);

        testSubjectScope = new McpServiceDataScope();
        testSubjectScope.setServiceId(TestFixtures.TEST_MCP_SERVICE_ID);
        testSubjectScope.setScopeType(McpDataScopeType.SUBJECT);
        testSubjectScope.setReferenceId("subject-001");
    }

    @Test
    @DisplayName("保存数据范围 - 全量替换，先删后插")
    void saveDataScope_shouldDeleteThenSave() {
        List<McpServiceDataScope> scopes = List.of(testDataSourceScope, testSubjectScope);
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
    @DisplayName("保存数据范围 - 空列表也应清空")
    void saveDataScope_empty_shouldDeleteAll() {
        dataScopeService.saveDataScope(TestFixtures.TEST_MCP_SERVICE_ID, List.of());

        verify(dataScopeDAO).deleteAllByServiceId(TestFixtures.TEST_MCP_SERVICE_ID);
        verify(dataScopeDAO, times(0)).saveAll(any());
    }

    @Test
    @DisplayName("查询数据范围 - 返回纯模型列表（无名称）")
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
    @DisplayName("查询数据范围 - 无记录返回空列表")
    void getDataScope_noRecords_shouldReturnEmpty() {
        when(dataScopeDAO.findAllByServiceId(TestFixtures.TEST_MCP_SERVICE_ID))
                .thenReturn(List.of());

        List<McpServiceDataScope> result = dataScopeService.getDataScope(TestFixtures.TEST_MCP_SERVICE_ID);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("解析数据源 ID - 只含 DATA_SOURCE 类型直接返回")
    void getResolvedDataSourceIds_onlyDataSource_shouldReturnDirectIds() {
        McpServiceDataScopePO po1 = McpServiceDataScopeMapper.toPO(testDataSourceScope);
        when(dataScopeDAO.findAllByServiceId(TestFixtures.TEST_MCP_SERVICE_ID))
                .thenReturn(List.of(po1));

        Set<String> result = dataScopeService.getResolvedDataSourceIds(TestFixtures.TEST_MCP_SERVICE_ID);

        assertThat(result).containsExactly(TestFixtures.TEST_DATASOURCE_ID);
        verify(subjectService, never()).getSubjectsByIds(any());
    }

    @Test
    @DisplayName("解析数据源 ID - 穿透 SUBJECT 获取 datasourceId")
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
    @DisplayName("解析数据源 ID - 直接引用和主题穿透的去重")
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
    @DisplayName("解析数据源 ID - 空 scope 返回空集合")
    void getResolvedDataSourceIds_empty_shouldReturnEmpty() {
        when(dataScopeDAO.findAllByServiceId(TestFixtures.TEST_MCP_SERVICE_ID))
                .thenReturn(List.of());

        Set<String> result = dataScopeService.getResolvedDataSourceIds(TestFixtures.TEST_MCP_SERVICE_ID);

        assertThat(result).isEmpty();
    }
}
