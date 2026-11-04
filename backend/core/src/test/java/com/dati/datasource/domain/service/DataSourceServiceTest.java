package com.dati.datasource.domain.service;

import com.dati.TestFixtures;
import com.dati.auth.authentication.User;
import com.dati.auth.domain.service.UserGroupService;
import com.dati.base.RequestContext;
import com.dati.base.exception.DatiException;
import com.dati.base.exception.ErrorCode;
import com.dati.datasource.domain.model.DataSource;
import com.dati.datasource.repository.dao.DataSourceDAO;
import com.dati.datasource.repository.dao.TableInfoDAO;
import com.dati.datasource.repository.po.DataSourcePO;
import com.dati.datasource.repository.po.TableInfoPO;
import com.dati.db.DbType;
import com.dati.db.HikariPoolManager;
import com.dati.db.JdbcConnector;
import com.dati.permission.domain.model.Permission;
import com.dati.permission.domain.model.PrincipalType;
import com.dati.permission.domain.model.ResourceType;
import com.dati.permission.domain.service.PermissionService;
import org.junit.jupiter.api.AfterEach;
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
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DataSourceService unit tests")
class DataSourceServiceTest {

    @Mock
    private DataSourceDAO dataSourceDAO;

    @Mock
    private TableInfoDAO tableInfoDAO;

    @Mock
    private TableService tableService;

    @Mock
    private JdbcMetaService jdbcMetaService;

    @Mock
    private PermissionService permissionService;

    @Mock
    private UserGroupService userGroupService;

    @InjectMocks
    private DataSourceService dataSourceService;

    private DataSource testDataSource;
    private DataSourcePO testDataSourcePO;

    @BeforeEach
    void setUp() {
        testDataSource = TestFixtures.createTestDataSource();
        testDataSourcePO = TestFixtures.createTestDataSourcePO();
        User user = new User();
        user.setId(TestFixtures.TEST_USER_ID);
        user.setName(TestFixtures.TEST_USER_ID);
        RequestContext.setUser(user);
        lenient().when(permissionService.isAdmin(anyString())).thenReturn(false);
        lenient().when(userGroupService.groupIdsOf(TestFixtures.TEST_USER_ID))
                .thenReturn(Set.of(PrincipalType.ALL_USERS));
        // requireCurrentUser owner 感知：ownerId == 当前用户（TEST_USER_ID）放行；否则抛 403
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
    @DisplayName("Add data source - driver load failure converts to DS_DRIVER_MISSING, not saved")
    void addDataSource_shouldThrowDatiException_whenConnectionInitializationFails() throws SQLException {
        // given：HikariPoolManager 将无法解析的 URL/驱动加载失败转换为 SQLException
        when(jdbcMetaService.resolveCurrentSchema(any(JdbcConnector.class), eq(DbType.MYSQL)))
            .thenThrow(new SQLException("Failed to initialize connection pool: Failed to get driver instance"));

        // when & then
        assertThatThrownBy(() -> dataSourceService.addDataSource(testDataSource))
            .isInstanceOf(DatiException.class)
            .satisfies(e -> assertThat(((DatiException) e).getCode()).isEqualTo(ErrorCode.DS_DRIVER_MISSING));
        verify(dataSourceDAO, never()).save(any());
    }

    @Test
    @DisplayName("Add data source - unsupported DB type converts to DatiException, not saved")
    void addDataSource_shouldThrowDatiException_whenUnsupportedType() throws SQLException {
        // given
        when(jdbcMetaService.resolveCurrentSchema(any(JdbcConnector.class), eq(DbType.MYSQL)))
            .thenThrow(new DatiException(com.dati.base.exception.ErrorCode.DS_UNSUPPORTED_TYPE, DbType.MYSQL));

        // when & then
        assertThatThrownBy(() -> dataSourceService.addDataSource(testDataSource))
            .isInstanceOf(DatiException.class)
            .satisfies(e -> assertThat(((DatiException) e).getCode()).isEqualTo(ErrorCode.DS_UNSUPPORTED_TYPE));
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
            verify(permissionService).onResourceDeleted(
                    ResourceType.DATA_SOURCE, TestFixtures.TEST_DATASOURCE_ID);
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
            verify(tableService).deleteTables(List.of(TestFixtures.TEST_TABLE_ID));
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
        when(dataSourceDAO.findAllAccessible(TestFixtures.TEST_USER_ID,
                Set.of(PrincipalType.ALL_USERS), pageable)).thenReturn(page);

        // when
        Page<DataSource> result = dataSourceService.listDataSources(null, pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getId()).isEqualTo(TestFixtures.TEST_DATASOURCE_ID);
        verify(dataSourceDAO).findAllAccessible(TestFixtures.TEST_USER_ID,
                Set.of(PrincipalType.ALL_USERS), pageable);
    }

    @Test
    @DisplayName("Paged query data sources - with keyword")
    void listDataSources_withKeyword() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        Page<DataSourcePO> page = new PageImpl<>(List.of(testDataSourcePO));
        when(dataSourceDAO.findByNameContainingOrIdAndAccessible("test", TestFixtures.TEST_USER_ID,
                Set.of(PrincipalType.ALL_USERS), pageable))
                .thenReturn(page);

        // when
        Page<DataSource> result = dataSourceService.listDataSources("test", pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
        verify(dataSourceDAO).findByNameContainingOrIdAndAccessible("test", TestFixtures.TEST_USER_ID,
                Set.of(PrincipalType.ALL_USERS), pageable);
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
    @DisplayName("Get single data source - denies without VIEW permission")
    void getDataSource_requiresViewPermission() {
        DataSourcePO other = TestFixtures.createTestDataSourcePO();
        other.setCreatedBy("other-user");
        when(dataSourceDAO.findById(TestFixtures.TEST_DATASOURCE_ID)).thenReturn(Optional.of(other));

        DatiException ex = assertThrows(DatiException.class,
                () -> dataSourceService.getDataSource(TestFixtures.TEST_DATASOURCE_ID));
        assertThat(ex.getCode()).isEqualTo(ErrorCode.PERMISSION_DENIED);
    }

    @Test
    @DisplayName("Update data source - denies without EDIT permission")
    void updateDataSource_requiresEditPermission() {
        DataSourcePO other = TestFixtures.createTestDataSourcePO();
        other.setCreatedBy("other-user");
        when(dataSourceDAO.findById(TestFixtures.TEST_DATASOURCE_ID)).thenReturn(Optional.of(other));

        DatiException ex = assertThrows(DatiException.class,
                () -> dataSourceService.updateDataSource(TestFixtures.TEST_DATASOURCE_ID, testDataSource));
        assertThat(ex.getCode()).isEqualTo(ErrorCode.PERMISSION_DENIED);
    }

    @Test
    @DisplayName("Delete data source - denies without EDIT permission")
    void deleteDataSource_requiresEditPermission() {
        DataSourcePO other = TestFixtures.createTestDataSourcePO();
        other.setCreatedBy("other-user");
        when(dataSourceDAO.findById(TestFixtures.TEST_DATASOURCE_ID)).thenReturn(Optional.of(other));

        DatiException ex = assertThrows(DatiException.class,
                () -> dataSourceService.deleteDataSource(TestFixtures.TEST_DATASOURCE_ID));
        assertThat(ex.getCode()).isEqualTo(ErrorCode.PERMISSION_DENIED);
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
    @DisplayName("Test connection - success probes the same default schema as save")
    void testConnection_shouldReturnTrue_whenSchemaIsDetected() throws SQLException {
        // given
        when(jdbcMetaService.resolveCurrentSchema(any(JdbcConnector.class), eq(DbType.MYSQL)))
            .thenReturn("public");

        // when
        boolean result = dataSourceService.testConnection(testDataSource, null);

        // then
        assertThat(result).isTrue();
        verify(jdbcMetaService).resolveCurrentSchema(any(JdbcConnector.class), eq(DbType.MYSQL));
    }

    @Test
    @DisplayName("Test connection - connection failure names the refused endpoint (DS012)")
    void testConnection_shouldThrowConnectionFailed_whenSQLException() throws SQLException {
        // given
        when(jdbcMetaService.resolveCurrentSchema(any(JdbcConnector.class), eq(DbType.MYSQL)))
            .thenThrow(new SQLException("Connection refused", "08001"));

        // when & then
        assertThatThrownBy(() -> dataSourceService.testConnection(testDataSource, null))
            .isInstanceOf(DatiException.class)
            .satisfies(e -> assertThat(((DatiException) e).getCode()).isEqualTo(ErrorCode.DS_CONNECT_REFUSED));
    }

    @Test
    @DisplayName("Test connection - empty password with id probes the stored credentials")
    void testConnection_shouldUseStoredPassword_whenIdGivenAndPasswordEmpty() throws SQLException {
        ArgumentCaptor<JdbcConnector> captor = ArgumentCaptor.forClass(JdbcConnector.class);
        when(dataSourceDAO.findById(TestFixtures.TEST_DATASOURCE_ID)).thenReturn(Optional.of(testDataSourcePO));
        when(jdbcMetaService.resolveCurrentSchema(captor.capture(), eq(DbType.MYSQL))).thenReturn("test_db");
        DataSource request = storedConnectionRequest();
        request.setPassword("");

        boolean result = dataSourceService.testConnection(request, TestFixtures.TEST_DATASOURCE_ID);

        assertThat(result).isTrue();
        assertThat(captor.getValue().jdbcUrl()).isEqualTo("jdbc:mysql://localhost:3306/test_db");
        assertThat(captor.getValue().username()).isEqualTo("root");
        assertThat(captor.getValue().password()).isEqualTo("password123");
        verify(permissionService).requireDataSource(testDataSourcePO, Permission.VIEW);
    }

    @Test
    @DisplayName("Test connection - changed URL without password is rejected before probing")
    void testConnection_shouldRequirePassword_whenConnectionFieldsDiffer() throws SQLException {
        when(dataSourceDAO.findById(TestFixtures.TEST_DATASOURCE_ID)).thenReturn(Optional.of(testDataSourcePO));
        DataSource request = storedConnectionRequest();
        request.setJdbcUrl("jdbc:mysql://attacker.internal:3306/evil");
        request.setPassword("");

        assertThatThrownBy(() -> dataSourceService.testConnection(request, TestFixtures.TEST_DATASOURCE_ID))
            .isInstanceOf(DatiException.class)
            .satisfies(e -> assertThat(((DatiException) e).getCode())
                .isEqualTo(ErrorCode.DS_PASSWORD_REQUIRED_FOR_TEST));
        verify(jdbcMetaService, never()).resolveCurrentSchema(any(), any());
    }

    @Test
    @DisplayName("Test connection - unknown id with empty password returns DS_NOT_FOUND")
    void testConnection_shouldReturnNotFound_whenStoredDataSourceMissing() throws SQLException {
        when(dataSourceDAO.findById(TestFixtures.TEST_DATASOURCE_ID)).thenReturn(Optional.empty());
        DataSource request = storedConnectionRequest();
        request.setPassword("");

        assertThatThrownBy(() -> dataSourceService.testConnection(request, TestFixtures.TEST_DATASOURCE_ID))
            .isInstanceOf(DatiException.class)
            .satisfies(e -> assertThat(((DatiException) e).getCode()).isEqualTo(ErrorCode.DS_NOT_FOUND));
        verify(jdbcMetaService, never()).resolveCurrentSchema(any(), any());
    }

    @Test
    @DisplayName("Test connection - typed password ignores the stored connection")
    void testConnection_shouldProbeRequest_whenPasswordProvided() throws SQLException {
        testDataSource.setJdbcUrl("jdbc:mysql://new-host:3306/new_db");
        testDataSource.setPassword("typed-password");
        when(jdbcMetaService.resolveCurrentSchema(any(JdbcConnector.class), eq(DbType.MYSQL)))
            .thenReturn("new_db");

        dataSourceService.testConnection(testDataSource, TestFixtures.TEST_DATASOURCE_ID);

        verify(dataSourceDAO, never()).findById(anyString());
        verify(jdbcMetaService).resolveCurrentSchema(
            argThat(connector -> "typed-password".equals(connector.password())
                && "jdbc:mysql://new-host:3306/new_db".equals(connector.jdbcUrl())),
            eq(DbType.MYSQL));
    }

    @Test
    @DisplayName("Test connection - stored connection requires VIEW permission")
    void testConnection_shouldRequireViewPermission_whenUsingStoredConnection() throws SQLException {
        when(dataSourceDAO.findById(TestFixtures.TEST_DATASOURCE_ID)).thenReturn(Optional.of(testDataSourcePO));
        doThrow(new DatiException(ErrorCode.PERMISSION_DENIED))
            .when(permissionService).requireDataSource(testDataSourcePO, Permission.VIEW);
        DataSource request = storedConnectionRequest();
        request.setPassword("");

        assertThatThrownBy(() -> dataSourceService.testConnection(request, TestFixtures.TEST_DATASOURCE_ID))
            .isInstanceOf(DatiException.class)
            .satisfies(e -> assertThat(((DatiException) e).getCode()).isEqualTo(ErrorCode.PERMISSION_DENIED));
        verify(jdbcMetaService, never()).resolveCurrentSchema(any(), any());
    }

    private DataSource storedConnectionRequest() {
        DataSource request = new DataSource();
        request.setType(testDataSourcePO.getType());
        request.setJdbcUrl(testDataSourcePO.getJdbcUrl());
        request.setUsername(testDataSourcePO.getUserName());
        return request;
    }

    @Test
    @DisplayName("Test connection - missing default schema throws DS_SCHEMA_DETECTION_FAILED")
    void testConnection_shouldThrowSchemaDetectionFailed_whenSchemaIsNull() throws SQLException {
        // given
        when(jdbcMetaService.resolveCurrentSchema(any(JdbcConnector.class), eq(DbType.MYSQL)))
            .thenReturn(null);

        // when & then
        assertThatThrownBy(() -> dataSourceService.testConnection(testDataSource, null))
            .isInstanceOf(DatiException.class)
            .satisfies(e -> assertThat(((DatiException) e).getCode()).isEqualTo(ErrorCode.DS_SCHEMA_DETECTION_FAILED));
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
    @DisplayName("Update data source - URL change without password is rejected before probing")
    void updateDataSource_shouldRejectUrlChangeWithoutPassword() throws SQLException {
        testDataSource.setJdbcUrl("jdbc:mysql://attacker.internal:3306/evil");
        testDataSource.setPassword(null);
        when(dataSourceDAO.findById(TestFixtures.TEST_DATASOURCE_ID)).thenReturn(Optional.of(testDataSourcePO));

        assertThatThrownBy(() -> dataSourceService.updateDataSource(
                TestFixtures.TEST_DATASOURCE_ID, testDataSource))
            .isInstanceOf(DatiException.class)
            .satisfies(e -> assertThat(((DatiException) e).getCode())
                .isEqualTo(ErrorCode.DS_PASSWORD_REQUIRED_FOR_UPDATE));

        verify(jdbcMetaService, never()).resolveCurrentSchema(any(), any());
        verify(dataSourceDAO, never()).save(any());
    }

    @Test
    @DisplayName("Update data source - username change without password is rejected")
    void updateDataSource_shouldRejectUsernameChangeWithoutPassword() throws SQLException {
        testDataSource.setUsername("someone-else");
        testDataSource.setPassword(null);
        when(dataSourceDAO.findById(TestFixtures.TEST_DATASOURCE_ID)).thenReturn(Optional.of(testDataSourcePO));

        assertThatThrownBy(() -> dataSourceService.updateDataSource(
                TestFixtures.TEST_DATASOURCE_ID, testDataSource))
            .isInstanceOf(DatiException.class)
            .satisfies(e -> assertThat(((DatiException) e).getCode())
                .isEqualTo(ErrorCode.DS_PASSWORD_REQUIRED_FOR_UPDATE));
        verify(jdbcMetaService, never()).resolveCurrentSchema(any(), any());
    }

    @Test
    @DisplayName("Update data source - URL change with a new password probes the new secret")
    void updateDataSource_shouldProbeProvidedPassword_whenUrlChanges() throws SQLException {
        testDataSource.setJdbcUrl("jdbc:mysql://new-host:3306/db");
        testDataSource.setPassword("new-secret");
        when(dataSourceDAO.findById(TestFixtures.TEST_DATASOURCE_ID)).thenReturn(Optional.of(testDataSourcePO));
        when(dataSourceDAO.save(any(DataSourcePO.class))).thenReturn(testDataSourcePO);
        when(jdbcMetaService.resolveCurrentSchema(any(JdbcConnector.class), eq(DbType.MYSQL)))
            .thenReturn("newdb");

        try (MockedStatic<HikariPoolManager> ignored = mockStatic(HikariPoolManager.class)) {
            dataSourceService.updateDataSource(TestFixtures.TEST_DATASOURCE_ID, testDataSource);
            verify(jdbcMetaService).resolveCurrentSchema(
                argThat(connector -> "new-secret".equals(connector.password())
                    && "jdbc:mysql://new-host:3306/db".equals(connector.jdbcUrl())),
                eq(DbType.MYSQL));
        }
    }

    @Test
    @DisplayName("Update data source - name-only change works without a password")
    void updateDataSource_shouldAllowNameOnlyChangeWithoutPassword() throws SQLException {
        testDataSource.setName("Renamed Source");
        testDataSource.setPassword(null);
        when(dataSourceDAO.findById(TestFixtures.TEST_DATASOURCE_ID)).thenReturn(Optional.of(testDataSourcePO));
        when(dataSourceDAO.save(any(DataSourcePO.class))).thenReturn(testDataSourcePO);

        try (MockedStatic<HikariPoolManager> ignored = mockStatic(HikariPoolManager.class)) {
            dataSourceService.updateDataSource(TestFixtures.TEST_DATASOURCE_ID, testDataSource);
            verify(dataSourceDAO).save(any(DataSourcePO.class));
            verify(jdbcMetaService, never()).resolveCurrentSchema(any(), any());
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

        assertThatThrownBy(() -> dataSourceService.updateDataSource(
                TestFixtures.TEST_DATASOURCE_ID, testDataSource))
            .isInstanceOf(DatiException.class)
            .satisfies(e -> assertThat(((DatiException) e).getCode()).isEqualTo(ErrorCode.DS_CONNECT_REFUSED));

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
