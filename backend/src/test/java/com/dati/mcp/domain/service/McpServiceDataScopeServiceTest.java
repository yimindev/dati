package com.dati.mcp.domain.service;

import com.dati.TestFixtures;
import com.dati.mcp.domain.model.McpDataScopeType;
import com.dati.mcp.domain.model.McpServiceDataScope;
import com.dati.mcp.repository.dao.McpServiceDataScopeDAO;
import com.dati.mcp.repository.mapper.McpServiceDataScopeMapper;
import com.dati.mcp.repository.po.McpServiceDataScopePO;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("McpServiceDataScopeService 单元测试")
class McpServiceDataScopeServiceTest {

    @Mock
    private McpServiceDataScopeDAO dataScopeDAO;

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
        testDataSourceScope.setReferenceName("Test MySQL");

        testSubjectScope = new McpServiceDataScope();
        testSubjectScope.setServiceId(TestFixtures.TEST_MCP_SERVICE_ID);
        testSubjectScope.setScopeType(McpDataScopeType.SUBJECT);
        testSubjectScope.setReferenceId("subject-001");
        testSubjectScope.setReferenceName("Sales Subject");
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
    @DisplayName("查询数据范围 - 返回列表")
    void getDataScope_shouldReturnList() {
        McpServiceDataScopePO po1 = McpServiceDataScopeMapper.toPO(testDataSourceScope);
        McpServiceDataScopePO po2 = McpServiceDataScopeMapper.toPO(testSubjectScope);
        when(dataScopeDAO.findAllByServiceId(TestFixtures.TEST_MCP_SERVICE_ID))
                .thenReturn(List.of(po1, po2));

        List<McpServiceDataScope> result = dataScopeService.getDataScope(TestFixtures.TEST_MCP_SERVICE_ID);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getScopeType()).isEqualTo(McpDataScopeType.DATA_SOURCE);
        assertThat(result.get(1).getScopeType()).isEqualTo(McpDataScopeType.SUBJECT);
    }

    @Test
    @DisplayName("查询数据范围 - 无记录返回空列表")
    void getDataScope_noRecords_shouldReturnEmpty() {
        when(dataScopeDAO.findAllByServiceId(TestFixtures.TEST_MCP_SERVICE_ID))
                .thenReturn(List.of());

        List<McpServiceDataScope> result = dataScopeService.getDataScope(TestFixtures.TEST_MCP_SERVICE_ID);

        assertThat(result).isEmpty();
    }
}
