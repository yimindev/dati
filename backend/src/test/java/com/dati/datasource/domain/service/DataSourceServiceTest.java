package com.dati.datasource.domain.service;

import com.dati.TestFixtures;
import com.dati.datasource.domain.model.DataSource;
import com.dati.datasource.repository.dao.ColumnInfoDAO;
import com.dati.datasource.repository.dao.DataSourceDAO;
import com.dati.datasource.repository.dao.TableInfoDAO;
import com.dati.datasource.repository.po.DataSourcePO;
import com.dati.datasource.repository.po.TableInfoPO;
import com.dati.db.HikariPoolManager;
import com.dati.db.JdbcConnector;
import com.dati.db.JdbcUtils;
import com.dati.semantic.domain.service.SemanticIndexService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DataSourceService 单元测试")
class DataSourceServiceTest {

    @Mock
    private DataSourceDAO dataSourceDAO;

    @Mock
    private TableInfoDAO tableInfoDAO;

    @Mock
    private ColumnInfoDAO columnInfoDAO;

    @Mock
    private SemanticIndexService semanticIndexService;

    @InjectMocks
    private DataSourceService dataSourceService;

    private DataSource testDataSource;
    private DataSourcePO testDataSourcePO;

    @BeforeEach
    void setUp() {
        testDataSource = TestFixtures.createTestDataSource();
        testDataSourcePO = TestFixtures.createTestDataSourcePO();
    }

    @Test
    @DisplayName("添加数据源 - 成功")
    void addDataSource_shouldReturnId() {
        // given
        when(dataSourceDAO.save(any(DataSourcePO.class))).thenReturn(testDataSourcePO);

        // when
        String result = dataSourceService.addDataSource(testDataSource);

        // then
        assertThat(result).isEqualTo(TestFixtures.TEST_DATASOURCE_ID);
        verify(dataSourceDAO).save(any(DataSourcePO.class));
    }

    @Test
    @DisplayName("更新数据源 - 成功")
    void updateDataSource_shouldUpdateSuccessfully() {
        // given
        when(dataSourceDAO.findById(TestFixtures.TEST_DATASOURCE_ID)).thenReturn(Optional.of(testDataSourcePO));
        when(dataSourceDAO.save(any(DataSourcePO.class))).thenReturn(testDataSourcePO);

        // when
        dataSourceService.updateDataSource(TestFixtures.TEST_DATASOURCE_ID, testDataSource);

        // then
        verify(dataSourceDAO).findById(TestFixtures.TEST_DATASOURCE_ID);
        verify(dataSourceDAO).save(any(DataSourcePO.class));
    }

    @Test
    @DisplayName("更新数据源 - 数据源不存在时抛出异常")
    void updateDataSource_shouldThrowWhenNotFound() {
        // given
        when(dataSourceDAO.findById(TestFixtures.TEST_DATASOURCE_ID)).thenReturn(Optional.empty());

        // when & then
        assertThrows(Exception.class, () -> 
            dataSourceService.updateDataSource(TestFixtures.TEST_DATASOURCE_ID, testDataSource)
        );
        verify(dataSourceDAO).findById(TestFixtures.TEST_DATASOURCE_ID);
        verify(dataSourceDAO, never()).save(any());
    }

    @Test
    @DisplayName("删除数据源 - 成功，无关联表")
    void deleteDataSource_shouldDeleteSuccessfully_withNoTables() {
        // given
        when(dataSourceDAO.findById(TestFixtures.TEST_DATASOURCE_ID)).thenReturn(Optional.of(testDataSourcePO));
        when(tableInfoDAO.findByDataSourceId(TestFixtures.TEST_DATASOURCE_ID)).thenReturn(Collections.emptyList());

        try (MockedStatic<HikariPoolManager> mockedHikari = mockStatic(HikariPoolManager.class)) {
            // when
            dataSourceService.deleteDataSource(TestFixtures.TEST_DATASOURCE_ID);

            // then
            verify(dataSourceDAO).findById(TestFixtures.TEST_DATASOURCE_ID);
            verify(dataSourceDAO).deleteById(TestFixtures.TEST_DATASOURCE_ID);
            mockedHikari.verify(() -> HikariPoolManager.close(any(JdbcConnector.class)));
        }
    }

    @Test
    @DisplayName("删除数据源 - 成功，有关联表")
    void deleteDataSource_shouldDeleteSuccessfully_withTables() {
        // given
        TableInfoPO tableInfoPO = TestFixtures.createTestTableInfoPO();
        when(dataSourceDAO.findById(TestFixtures.TEST_DATASOURCE_ID)).thenReturn(Optional.of(testDataSourcePO));
        when(tableInfoDAO.findByDataSourceId(TestFixtures.TEST_DATASOURCE_ID)).thenReturn(List.of(tableInfoPO));

        try (MockedStatic<HikariPoolManager> ignored = mockStatic(HikariPoolManager.class)) {
            // when
            dataSourceService.deleteDataSource(TestFixtures.TEST_DATASOURCE_ID);

            // then
            verify(columnInfoDAO).deleteByTableIdIn(List.of(TestFixtures.TEST_TABLE_ID));
            verify(tableInfoDAO).deleteAllById(List.of(TestFixtures.TEST_TABLE_ID));
            verify(semanticIndexService).deleteByEntityTableIds(List.of(TestFixtures.TEST_TABLE_ID));
            verify(dataSourceDAO).deleteById(TestFixtures.TEST_DATASOURCE_ID);
        }
    }

    @Test
    @DisplayName("删除数据源 - 不存在时静默处理")
    void deleteDataSource_shouldDoNothingWhenNotFound() {
        // given
        when(dataSourceDAO.findById(TestFixtures.TEST_DATASOURCE_ID)).thenReturn(Optional.empty());

        // when
        dataSourceService.deleteDataSource(TestFixtures.TEST_DATASOURCE_ID);

        // then
        verify(dataSourceDAO).findById(TestFixtures.TEST_DATASOURCE_ID);
        verify(dataSourceDAO, never()).deleteById(any());
    }

    @Test
    @DisplayName("分页查询数据源 - 无关键词")
    void listDataSources_withoutKeyword() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        Page<DataSourcePO> page = new PageImpl<>(List.of(testDataSourcePO));
        when(dataSourceDAO.findAll(pageable)).thenReturn(page);

        // when
        Page<DataSource> result = dataSourceService.listDataSources(null, pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getId()).isEqualTo(TestFixtures.TEST_DATASOURCE_ID);
        verify(dataSourceDAO).findAll(pageable);
    }

    @Test
    @DisplayName("分页查询数据源 - 有关键词")
    void listDataSources_withKeyword() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        Page<DataSourcePO> page = new PageImpl<>(List.of(testDataSourcePO));
        when(dataSourceDAO.findAllByNameContainingOrId("test", "test", pageable)).thenReturn(page);

        // when
        Page<DataSource> result = dataSourceService.listDataSources("test", pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
        verify(dataSourceDAO).findAllByNameContainingOrId("test", "test", pageable);
    }

    @Test
    @DisplayName("获取单个数据源 - 成功返回带 defaultSchema")
    void getDataSource_shouldReturnWithDefaultSchema() {
        // given
        testDataSourcePO.setDefaultSchema("public");
        when(dataSourceDAO.findById(TestFixtures.TEST_DATASOURCE_ID)).thenReturn(Optional.of(testDataSourcePO));

        // when
        DataSource result = dataSourceService.getDataSource(TestFixtures.TEST_DATASOURCE_ID);

        // then
        assertEquals("public", result.getDefaultSchema());
        verify(dataSourceDAO).findById(TestFixtures.TEST_DATASOURCE_ID);
    }

    @Test
    @DisplayName("获取单个数据源 - 不存在时抛出异常")
    void getDataSource_shouldThrowWhenNotFound() {
        // given
        when(dataSourceDAO.findById(TestFixtures.TEST_DATASOURCE_ID)).thenReturn(Optional.empty());

        // when & then
        assertThrows(Exception.class, () ->
            dataSourceService.getDataSource(TestFixtures.TEST_DATASOURCE_ID)
        );
        verify(dataSourceDAO).findById(TestFixtures.TEST_DATASOURCE_ID);
    }

    @Test
    @DisplayName("批量获取数据源名称映射 - 按 ID 查询")
    void getDataSourceNameMap_shouldReturnIdToNameMap() {
        DataSourcePO po1 = new DataSourcePO();
        po1.setId("ds-1");
        po1.setName("MySQL Source");

        DataSourcePO po2 = new DataSourcePO();
        po2.setId("ds-2");
        po2.setName("PG Source");

        when(dataSourceDAO.findAllById(List.of("ds-1", "ds-2"))).thenReturn(List.of(po1, po2));

        Map<String, String> result = dataSourceService.getDataSourceNameMap(List.of("ds-1", "ds-2"));

        assertThat(result).hasSize(2);
        assertThat(result.get("ds-1")).isEqualTo("MySQL Source");
        assertThat(result.get("ds-2")).isEqualTo("PG Source");
    }

    @Test
    @DisplayName("批量获取数据源名称映射 - 空集合返回空 Map")
    void getDataSourceNameMap_withEmptyIds_shouldReturnEmptyMap() {
        Map<String, String> result = dataSourceService.getDataSourceNameMap(List.of());
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("测试连接 - 成功")
    void testConnection_shouldReturnTrue() {
        // given
        JdbcConnector connector = new JdbcConnector(testDataSource);
        try (MockedStatic<JdbcUtils> mockedJdbc = mockStatic(JdbcUtils.class)) {
            mockedJdbc.when(() -> JdbcUtils.testConnection(any(), any(), any())).thenReturn(true);

            // when
            boolean result = dataSourceService.testConnection(connector);

            // then
            assertThat(result).isTrue();
            mockedJdbc.verify(() -> JdbcUtils.testConnection(eq(testDataSource.getJdbcUrl()), eq(testDataSource.getUsername()), eq(testDataSource.getPassword())));
        }
    }
}
