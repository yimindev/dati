package com.dati.datasource.domain.service;

import com.dati.TestFixtures;
import com.dati.base.exception.DatiException;
import com.dati.datasource.domain.model.DataSource;
import com.dati.datasource.repository.dao.ColumnInfoDAO;
import com.dati.datasource.repository.dao.DataSourceDAO;
import com.dati.datasource.repository.dao.TableInfoDAO;
import com.dati.datasource.repository.po.DataSourcePO;
import com.dati.datasource.repository.po.TableInfoPO;
import com.dati.db.DbType;
import com.dati.db.HikariPoolManager;
import com.dati.db.JdbcConnector;
import com.dati.db.JdbcUtils;
import com.dati.semantic.domain.service.SemanticIndexService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DataSourceService unit tests")
class DataSourceServiceTest {

    @Mock
    private DataSourceDAO dataSourceDAO;

    @Mock
    private TableInfoDAO tableInfoDAO;

    @Mock
    private ColumnInfoDAO columnInfoDAO;

    @Mock
    private SemanticIndexService semanticIndexService;

    @Mock
    private JdbcMetaService jdbcMetaService;

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
    @DisplayName("Add data source - success, probes real schema and saves once")
    void addDataSource_shouldReturnId() throws SQLException {
        // given
        ArgumentCaptor<DataSourcePO> captor = ArgumentCaptor.forClass(DataSourcePO.class);
        when(jdbcMetaService.resolveCurrentSchema(any(JdbcConnector.class), eq(DbType.MYSQL)))
            .thenReturn("real_schema");
        when(dataSourceDAO.save(captor.capture())).thenReturn(testDataSourcePO);

        // when
        String result = dataSourceService.addDataSource(testDataSource);

        // then
        assertThat(result).isEqualTo(TestFixtures.TEST_DATASOURCE_ID);
        verify(dataSourceDAO, org.mockito.Mockito.times(1)).save(any(DataSourcePO.class));
        assertThat(captor.getValue().getDefaultSchema()).isEqualTo("real_schema");
    }

    @Test
    @DisplayName("Add data source - ignores forged defaultSchema, uses real probe result")
    void addDataSource_shouldIgnoreClientSuppliedFakeSchema() throws SQLException {
        // given：TestFixtures 中的 testDataSource 自带 defaultSchema="public"，属于客户端伪造值
        ArgumentCaptor<DataSourcePO> captor = ArgumentCaptor.forClass(DataSourcePO.class);
        when(jdbcMetaService.resolveCurrentSchema(any(JdbcConnector.class), eq(DbType.MYSQL)))
            .thenReturn("real_schema");
        when(dataSourceDAO.save(captor.capture())).thenReturn(testDataSourcePO);

        // when
        dataSourceService.addDataSource(testDataSource);

        // then
        assertThat(testDataSource.getDefaultSchema()).isEqualTo("public");
        assertThat(captor.getValue().getDefaultSchema()).isEqualTo("real_schema");
    }

    @Test
    @DisplayName("Add data source - SQLException converts to DatiException, not saved")
    void addDataSource_shouldThrowDatiException_whenSQLException() throws SQLException {
        // given
        when(jdbcMetaService.resolveCurrentSchema(any(JdbcConnector.class), eq(DbType.MYSQL)))
            .thenThrow(new SQLException("connection refused"));

        // when & then
        assertThrows(DatiException.class, () -> dataSourceService.addDataSource(testDataSource));
        verify(dataSourceDAO, never()).save(any());
    }

    @Test
    @DisplayName("Add data source - Hikari init failure path converts to DatiException, not saved")
    void addDataSource_shouldThrowDatiException_whenConnectionInitializationFails() throws SQLException {
        // given：等价于 HikariPoolManager 将连接池初始化失败转换后的 SQLException
        when(jdbcMetaService.resolveCurrentSchema(any(JdbcConnector.class), eq(DbType.MYSQL)))
            .thenThrow(new SQLException("Failed to initialize connection pool: Connection is not available"));

        // when & then
        assertThrows(DatiException.class, () -> dataSourceService.addDataSource(testDataSource));
        verify(dataSourceDAO, never()).save(any());
    }

    @Test
    @DisplayName("Add data source - unsupported DB type converts to DatiException, not saved")
    void addDataSource_shouldThrowDatiException_whenUnsupportedType() throws SQLException {
        // given
        when(jdbcMetaService.resolveCurrentSchema(any(JdbcConnector.class), eq(DbType.MYSQL)))
            .thenThrow(new DatiException(com.dati.base.exception.ErrorCode.DS_UNSUPPORTED_TYPE, DbType.MYSQL));

        // when & then
        assertThrows(DatiException.class, () -> dataSourceService.addDataSource(testDataSource));
        verify(dataSourceDAO, never()).save(any());
    }

    @Test
    @DisplayName("Add data source - null probe result converts to DatiException, not saved")
    void addDataSource_shouldThrowDatiException_whenSchemaIsNull() throws SQLException {
        // given
        when(jdbcMetaService.resolveCurrentSchema(any(JdbcConnector.class), eq(DbType.MYSQL)))
            .thenReturn(null);

        // when & then
        assertThrows(DatiException.class, () -> dataSourceService.addDataSource(testDataSource));
        verify(dataSourceDAO, never()).save(any());
    }

    @Test
    @DisplayName("Add data source - blank probe result converts to DatiException, not saved")
    void addDataSource_shouldThrowDatiException_whenSchemaIsEmpty() throws SQLException {
        // given
        when(jdbcMetaService.resolveCurrentSchema(any(JdbcConnector.class), eq(DbType.MYSQL)))
            .thenReturn("   ");

        // when & then
        assertThrows(DatiException.class, () -> dataSourceService.addDataSource(testDataSource));
        verify(dataSourceDAO, never()).save(any());
    }

    @Test
    @DisplayName("Update data source - success")
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
    @DisplayName("Update data source - throws DS_NOT_FOUND when not found")
    void updateDataSource_shouldThrowWhenNotFound() {
        // given
        when(dataSourceDAO.findById(TestFixtures.TEST_DATASOURCE_ID)).thenReturn(Optional.empty());

        // when & then
        assertThrows(DatiException.class, () ->
            dataSourceService.updateDataSource(TestFixtures.TEST_DATASOURCE_ID, testDataSource)
        );
        verify(dataSourceDAO).findById(TestFixtures.TEST_DATASOURCE_ID);
        verify(dataSourceDAO, never()).save(any());
    }

    @Test
    @DisplayName("Delete data source - success, no linked tables")
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
    @DisplayName("Delete data source - success, with linked tables")
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
    @DisplayName("Delete data source - throws DS_NOT_FOUND when not found")
    void deleteDataSource_shouldThrowWhenNotFound() {
        // given
        when(dataSourceDAO.findById(TestFixtures.TEST_DATASOURCE_ID)).thenReturn(Optional.empty());

        // when & then
        assertThrows(DatiException.class, () ->
                dataSourceService.deleteDataSource(TestFixtures.TEST_DATASOURCE_ID));

        verify(dataSourceDAO).findById(TestFixtures.TEST_DATASOURCE_ID);
        verify(dataSourceDAO, never()).deleteById(any());
    }

    @Test
    @DisplayName("Paged query data sources - without keyword")
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
    @DisplayName("Paged query data sources - with keyword")
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
    @DisplayName("Get single data source - success returns defaultSchema")
    void getDataSource_shouldReturnWithDefaultSchema() {
        testDataSourcePO.setDefaultSchema("public");
        when(dataSourceDAO.findById(TestFixtures.TEST_DATASOURCE_ID)).thenReturn(Optional.of(testDataSourcePO));

        assertThat(dataSourceService.getDataSource(TestFixtures.TEST_DATASOURCE_ID))
            .hasValueSatisfying(ds -> assertEquals("public", ds.getDefaultSchema()));
        verify(dataSourceDAO).findById(TestFixtures.TEST_DATASOURCE_ID);
    }

    @Test
    @DisplayName("Get single data source - returns empty Optional when not found")
    void getDataSource_shouldReturnEmptyWhenNotFound() {
        when(dataSourceDAO.findById(TestFixtures.TEST_DATASOURCE_ID)).thenReturn(Optional.empty());

        assertThat(dataSourceService.getDataSource(TestFixtures.TEST_DATASOURCE_ID)).isEmpty();
        verify(dataSourceDAO).findById(TestFixtures.TEST_DATASOURCE_ID);
    }

    @Test
    @DisplayName("Batch get datasource name map - queries by IDs")
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
    @DisplayName("Batch get datasource name map - empty set returns empty map")
    void getDataSourceNameMap_withEmptyIds_shouldReturnEmptyMap() {
        Map<String, String> result = dataSourceService.getDataSourceNameMap(List.of());
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Test connection - success")
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

    @Test
    @DisplayName("Update data source - re-probes defaultSchema on URL change, saves once")
    void updateDataSource_reDetectsWhenUrlChanges() throws SQLException {
        testDataSourcePO.setJdbcUrl("jdbc:mysql://old-host:3306/db");
        testDataSource.setJdbcUrl("jdbc:mysql://new-host:3306/db");
        ArgumentCaptor<DataSourcePO> captor = ArgumentCaptor.forClass(DataSourcePO.class);
        when(dataSourceDAO.findById(TestFixtures.TEST_DATASOURCE_ID)).thenReturn(Optional.of(testDataSourcePO));
        when(dataSourceDAO.save(captor.capture())).thenReturn(testDataSourcePO);
        when(jdbcMetaService.resolveCurrentSchema(any(JdbcConnector.class), eq(DbType.MYSQL)))
            .thenReturn("newdb");

        try (MockedStatic<HikariPoolManager> mockedHikari = mockStatic(HikariPoolManager.class)) {
            dataSourceService.updateDataSource(TestFixtures.TEST_DATASOURCE_ID, testDataSource);

            verify(jdbcMetaService).resolveCurrentSchema(any(JdbcConnector.class), eq(DbType.MYSQL));
            verify(dataSourceDAO, org.mockito.Mockito.times(1)).save(any(DataSourcePO.class));
            assertThat(captor.getValue().getDefaultSchema()).isEqualTo("newdb");
            mockedHikari.verify(() -> HikariPoolManager.close(any(JdbcConnector.class)));
        }
    }

    @Test
    @DisplayName("Update data source - re-probes defaultSchema on username change")
    void updateDataSource_reDetectsWhenUsernameChanges() throws SQLException {
        testDataSource.setUsername("new_user");
        when(dataSourceDAO.findById(TestFixtures.TEST_DATASOURCE_ID)).thenReturn(Optional.of(testDataSourcePO));
        when(dataSourceDAO.save(any(DataSourcePO.class))).thenReturn(testDataSourcePO);
        when(jdbcMetaService.resolveCurrentSchema(any(JdbcConnector.class), eq(DbType.MYSQL)))
            .thenReturn("newdb");

        try (MockedStatic<HikariPoolManager> mockedHikari = mockStatic(HikariPoolManager.class)) {
            dataSourceService.updateDataSource(TestFixtures.TEST_DATASOURCE_ID, testDataSource);

            verify(jdbcMetaService).resolveCurrentSchema(any(JdbcConnector.class), eq(DbType.MYSQL));
            mockedHikari.verify(() -> HikariPoolManager.close(any(JdbcConnector.class)));
        }
    }

    @Test
    @DisplayName("Update data source - re-probes defaultSchema on password change")
    void updateDataSource_reDetectsWhenPasswordChanges() throws SQLException {
        testDataSource.setPassword("new_password");
        when(dataSourceDAO.findById(TestFixtures.TEST_DATASOURCE_ID)).thenReturn(Optional.of(testDataSourcePO));
        when(dataSourceDAO.save(any(DataSourcePO.class))).thenReturn(testDataSourcePO);
        when(jdbcMetaService.resolveCurrentSchema(any(JdbcConnector.class), eq(DbType.MYSQL)))
            .thenReturn("newdb");

        try (MockedStatic<HikariPoolManager> mockedHikari = mockStatic(HikariPoolManager.class)) {
            dataSourceService.updateDataSource(TestFixtures.TEST_DATASOURCE_ID, testDataSource);

            verify(jdbcMetaService).resolveCurrentSchema(any(JdbcConnector.class), eq(DbType.MYSQL));
            mockedHikari.verify(() -> HikariPoolManager.close(any(JdbcConnector.class)));
        }
    }

    @Test
    @DisplayName("Update data source - re-probes defaultSchema on type change")
    void updateDataSource_reDetectsWhenTypeChanges() throws SQLException {
        testDataSource.setType(DbType.POSTGRESQL);
        when(dataSourceDAO.findById(TestFixtures.TEST_DATASOURCE_ID)).thenReturn(Optional.of(testDataSourcePO));
        when(dataSourceDAO.save(any(DataSourcePO.class))).thenReturn(testDataSourcePO);
        when(jdbcMetaService.resolveCurrentSchema(any(JdbcConnector.class), eq(DbType.POSTGRESQL)))
            .thenReturn("newdb");

        try (MockedStatic<HikariPoolManager> mockedHikari = mockStatic(HikariPoolManager.class)) {
            dataSourceService.updateDataSource(TestFixtures.TEST_DATASOURCE_ID, testDataSource);

            verify(jdbcMetaService).resolveCurrentSchema(any(JdbcConnector.class), eq(DbType.POSTGRESQL));
            mockedHikari.verify(() -> HikariPoolManager.close(any(JdbcConnector.class)));
        }
    }

    @Test
    @DisplayName("Update data source - non-connection field change: no probe, pool kept")
    void updateDataSource_doesNotDetect_whenOnlyNonConnectionFieldsChange() {
        testDataSource.setName("Renamed Source");
        testDataSource.setDescription("new description");
        when(dataSourceDAO.findById(TestFixtures.TEST_DATASOURCE_ID)).thenReturn(Optional.of(testDataSourcePO));
        when(dataSourceDAO.save(any(DataSourcePO.class))).thenReturn(testDataSourcePO);

        try (MockedStatic<HikariPoolManager> mockedHikari = mockStatic(HikariPoolManager.class)) {
            dataSourceService.updateDataSource(TestFixtures.TEST_DATASOURCE_ID, testDataSource);

            verify(dataSourceDAO, org.mockito.Mockito.times(1)).save(any(DataSourcePO.class));
            mockedHikari.verifyNoInteractions();
        }
    }

    @Test
    @DisplayName("Update data source - probe failure (SQLException): no modify, no save")
    void updateDataSource_shouldNotSave_whenDetectionThrowsSQLException() throws SQLException {
        testDataSource.setJdbcUrl("jdbc:mysql://new-host:3306/db");
        String originalJdbcUrl = testDataSourcePO.getJdbcUrl();
        when(dataSourceDAO.findById(TestFixtures.TEST_DATASOURCE_ID)).thenReturn(Optional.of(testDataSourcePO));
        when(jdbcMetaService.resolveCurrentSchema(any(JdbcConnector.class), eq(DbType.MYSQL)))
            .thenThrow(new SQLException("connection refused"));

        assertThrows(DatiException.class, () ->
            dataSourceService.updateDataSource(TestFixtures.TEST_DATASOURCE_ID, testDataSource));

        assertThat(testDataSourcePO.getJdbcUrl()).isEqualTo(originalJdbcUrl);
        verify(dataSourceDAO, never()).save(any());
    }

    @Test
    @DisplayName("Update data source - unsupported DB type: no modify, no save")
    void updateDataSource_shouldNotSave_whenDetectionThrowsUnsupportedType() throws SQLException {
        testDataSource.setType(DbType.POSTGRESQL);
        when(dataSourceDAO.findById(TestFixtures.TEST_DATASOURCE_ID)).thenReturn(Optional.of(testDataSourcePO));
        when(jdbcMetaService.resolveCurrentSchema(any(JdbcConnector.class), eq(DbType.POSTGRESQL)))
            .thenThrow(new DatiException(com.dati.base.exception.ErrorCode.DS_UNSUPPORTED_TYPE, DbType.POSTGRESQL));

        assertThrows(DatiException.class, () ->
            dataSourceService.updateDataSource(TestFixtures.TEST_DATASOURCE_ID, testDataSource));

        verify(dataSourceDAO, never()).save(any());
    }

    @Test
    @DisplayName("Update data source - empty probe result: no modify, no save")
    void updateDataSource_shouldNotSave_whenSchemaIsBlank() throws SQLException {
        testDataSource.setJdbcUrl("jdbc:mysql://new-host:3306/db");
        when(dataSourceDAO.findById(TestFixtures.TEST_DATASOURCE_ID)).thenReturn(Optional.of(testDataSourcePO));
        when(jdbcMetaService.resolveCurrentSchema(any(JdbcConnector.class), eq(DbType.MYSQL)))
            .thenReturn("");

        assertThrows(DatiException.class, () ->
            dataSourceService.updateDataSource(TestFixtures.TEST_DATASOURCE_ID, testDataSource));

        verify(dataSourceDAO, never()).save(any());
    }

    @Test
    @DisplayName("Update data source - closes old pool with pre-change credentials after probe")
    void updateDataSource_closesOldPool_withOldCredentials() throws SQLException {
        JdbcConnector expectedOldConnector = new JdbcConnector(
            testDataSourcePO.getJdbcUrl(), testDataSourcePO.getUserName(), testDataSourcePO.getEncryptedPassword());
        testDataSource.setJdbcUrl("jdbc:mysql://new-host:3306/db");
        when(dataSourceDAO.findById(TestFixtures.TEST_DATASOURCE_ID)).thenReturn(Optional.of(testDataSourcePO));
        when(dataSourceDAO.save(any(DataSourcePO.class))).thenReturn(testDataSourcePO);
        when(jdbcMetaService.resolveCurrentSchema(any(JdbcConnector.class), eq(DbType.MYSQL)))
            .thenReturn("newdb");

        try (MockedStatic<HikariPoolManager> mockedHikari = mockStatic(HikariPoolManager.class)) {
            dataSourceService.updateDataSource(TestFixtures.TEST_DATASOURCE_ID, testDataSource);

            mockedHikari.verify(() -> HikariPoolManager.close(eq(expectedOldConnector)));
        }
    }

    @Test
    @DisplayName("Update data source - keeps old pool open when save fails")
    void updateDataSource_doesNotClosePool_whenSaveFails() throws SQLException {
        testDataSource.setJdbcUrl("jdbc:mysql://new-host:3306/db");
        when(dataSourceDAO.findById(TestFixtures.TEST_DATASOURCE_ID)).thenReturn(Optional.of(testDataSourcePO));
        when(jdbcMetaService.resolveCurrentSchema(any(JdbcConnector.class), eq(DbType.MYSQL)))
            .thenReturn("newdb");
        when(dataSourceDAO.save(any(DataSourcePO.class))).thenThrow(new RuntimeException("db down"));

        try (MockedStatic<HikariPoolManager> mockedHikari = mockStatic(HikariPoolManager.class)) {
            assertThrows(RuntimeException.class, () ->
                dataSourceService.updateDataSource(TestFixtures.TEST_DATASOURCE_ID, testDataSource));

            mockedHikari.verify(() -> HikariPoolManager.close(any(JdbcConnector.class)), never());
        }
    }
}
