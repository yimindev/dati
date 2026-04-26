package com.dati.datasource.domain.service;

import com.dati.TestFixtures;
import com.dati.auth.authentication.User;
import com.dati.base.RequestContext;
import com.dati.base.pojo.PageReq;
import com.dati.datasource.domain.model.TableInfo;
import com.dati.datasource.repository.dao.ColumnInfoDAO;
import com.dati.datasource.repository.dao.TableInfoDAO;
import com.dati.datasource.repository.po.ColumnInfoPO;
import com.dati.datasource.repository.po.TableInfoPO;
import com.dati.datasource.server.pojo.AddTableRequest;
import com.dati.db.Column;
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

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TableService 单元测试")
class TableServiceTest {

    @Mock
    private TableInfoDAO tableInfoDAO;

    @Mock
    private ColumnInfoDAO columnInfoDAO;

    @Mock
    private JdbcMetaService jdbcMetaService;

    @Mock
    private SemanticIndexService semanticIndexService;

    @InjectMocks
    private TableService tableService;

    private TableInfoPO testTableInfoPO;

    @BeforeEach
    void setUp() {
        testTableInfoPO = TestFixtures.createTestTableInfoPO();
    }

    @Test
    @DisplayName("分页查询表 - 无关键词")
    void getTables_withoutKeyword() {
        // given
        PageReq pageReq = new PageReq();
        pageReq.setPage(1);
        pageReq.setSize(10);

        Page<TableInfoPO> page = new PageImpl<>(List.of(testTableInfoPO));
        when(tableInfoDAO.findByDataSourceId(eq(TestFixtures.TEST_DATASOURCE_ID), any()))
            .thenReturn(page);

        // when
        Page<TableInfo> result = tableService.getTables(pageReq, TestFixtures.TEST_DATASOURCE_ID, null);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getId()).isEqualTo(TestFixtures.TEST_TABLE_ID);
    }

    @Test
    @DisplayName("分页查询表 - 有关键词")
    void getTables_withKeyword() {
        // given
        PageReq pageReq = new PageReq();
        pageReq.setPage(1);
        pageReq.setSize(10);

        Page<TableInfoPO> page = new PageImpl<>(List.of(testTableInfoPO));
        when(tableInfoDAO.findByDataSourceIdAndNameContaining(
            eq(TestFixtures.TEST_DATASOURCE_ID), eq("test"), any()))
            .thenReturn(page);

        // when
        Page<TableInfo> result = tableService.getTables(pageReq, TestFixtures.TEST_DATASOURCE_ID, "test");

        // then
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("获取已添加的表名列表")
    void getAddedTableNames_shouldReturnTableNames() {
        // given
        when(tableInfoDAO.findByDataSourceId(TestFixtures.TEST_DATASOURCE_ID))
            .thenReturn(List.of(testTableInfoPO));

        // when
        List<String> result = tableService.getAddedTableNames(TestFixtures.TEST_DATASOURCE_ID);

        // then
        assertThat(result).containsExactly("test_table");
    }

    @Test
    @DisplayName("批量添加表 - 成功")
    void batchAddTables_shouldAddTablesWithColumns() throws SQLException {
        // given
        AddTableRequest request = new AddTableRequest();
        request.setName("new_table");
        request.setSchema("public");
        
        Column mockColumn = mock(Column.class);
        when(mockColumn.name()).thenReturn("col1");
        when(mockColumn.type()).thenReturn("VARCHAR");
        when(mockColumn.comment()).thenReturn("Column comment");
        
        when(jdbcMetaService.getColumns(TestFixtures.TEST_DATASOURCE_ID, null, "public", "new_table"))
            .thenReturn(List.of(mockColumn));
        when(tableInfoDAO.save(any(TableInfoPO.class))).thenReturn(testTableInfoPO);
        when(columnInfoDAO.save(any(ColumnInfoPO.class))).thenReturn(TestFixtures.createTestColumnInfoPO());

        User mockUser = mock(User.class);
        when(mockUser.getId()).thenReturn(TestFixtures.TEST_USER_ID);

        try (MockedStatic<RequestContext> mocked = mockStatic(RequestContext.class)) {
            mocked.when(RequestContext::getUser).thenReturn(mockUser);

            // when
            List<String> result = tableService.batchAddTables(TestFixtures.TEST_DATASOURCE_ID, List.of(request));

            // then
            assertThat(result).hasSize(1);
            verify(tableInfoDAO).save(any(TableInfoPO.class));
            verify(columnInfoDAO).save(any(ColumnInfoPO.class));
            verify(semanticIndexService).saveBatch(anyList());
        }
    }

    @Test
    @DisplayName("批量添加表 - 同步列失败时应抛出异常")
    void batchAddTables_shouldThrowWhenSyncColumnsFails() throws SQLException {
        // given
        AddTableRequest request = new AddTableRequest();
        request.setName("new_table");
        request.setSchema("public");
        
        when(jdbcMetaService.getTables(TestFixtures.TEST_DATASOURCE_ID, null, "public"))
            .thenReturn(List.of());
        when(jdbcMetaService.getColumns(TestFixtures.TEST_DATASOURCE_ID, null, "public", "new_table"))
            .thenThrow(new SQLException("Connection failed"));
        when(tableInfoDAO.save(any(TableInfoPO.class))).thenReturn(testTableInfoPO);

        User mockUser = mock(User.class);
        when(mockUser.getId()).thenReturn(TestFixtures.TEST_USER_ID);

        try (MockedStatic<RequestContext> mocked = mockStatic(RequestContext.class)) {
            mocked.when(RequestContext::getUser).thenReturn(mockUser);

            // when & then
            RuntimeException exception = assertThrows(RuntimeException.class, () ->
                tableService.batchAddTables(TestFixtures.TEST_DATASOURCE_ID, List.of(request))
            );
            assertThat(exception.getMessage()).contains("Failed to sync columns");
        }
    }

    @Test
    @DisplayName("批量添加表 - 空列表应返回空结果")
    void batchAddTables_withEmptyList_shouldReturnEmpty() {
        // when
        List<String> result = tableService.batchAddTables(TestFixtures.TEST_DATASOURCE_ID, Collections.emptyList());

        // then
        assertThat(result).isEmpty();
        verify(tableInfoDAO, never()).save(any());
    }

    @Test
    @DisplayName("删除表 - 成功")
    void deleteTable_shouldDeleteTableAndColumnsAndESDocuments() {
        // when
        tableService.deleteTable(TestFixtures.TEST_TABLE_ID);

        // then
        verify(columnInfoDAO).deleteByTableIdIn(List.of(TestFixtures.TEST_TABLE_ID));
        verify(tableInfoDAO).deleteAllById(List.of(TestFixtures.TEST_TABLE_ID));
        verify(semanticIndexService).deleteByEntityTableIds(List.of(TestFixtures.TEST_TABLE_ID));
    }

    @Test
    @DisplayName("批量删除表 - 成功")
    void deleteTables_shouldDeleteTablesAndColumnsAndESDocuments() {
        // given
        List<String> tableIds = List.of(TestFixtures.TEST_TABLE_ID, "table_2");

        // when
        tableService.deleteTables(tableIds);

        // then
        verify(columnInfoDAO).deleteByTableIdIn(tableIds);
        verify(tableInfoDAO).deleteAllById(tableIds);
        verify(semanticIndexService).deleteByEntityTableIds(tableIds);
    }

    @Test
    @DisplayName("批量删除表 - 空列表")
    void deleteTables_withEmptyList_shouldNotCallDAO() {
        // when
        tableService.deleteTables(Collections.emptyList());

        // then
        verify(columnInfoDAO).deleteByTableIdIn(Collections.emptyList());
        verify(tableInfoDAO).deleteAllById(Collections.emptyList());
        verify(semanticIndexService).deleteByEntityTableIds(Collections.emptyList());
    }
}
