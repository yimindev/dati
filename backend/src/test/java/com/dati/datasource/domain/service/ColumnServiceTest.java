package com.dati.datasource.domain.service;

import com.dati.TestFixtures;
import com.dati.auth.authentication.User;
import com.dati.base.RequestContext;
import com.dati.base.pojo.PageReq;
import com.dati.datasource.domain.model.ColumnInfo;
import com.dati.datasource.repository.dao.ColumnInfoDAO;
import com.dati.datasource.repository.dao.TableInfoDAO;
import com.dati.datasource.repository.po.ColumnInfoPO;
import com.dati.datasource.repository.po.TableInfoPO;
import com.dati.db.Column;
import com.dati.semantic.domain.SemanticEntityType;
import com.dati.semantic.domain.service.SemanticIndexService;
import com.dati.semantic.repository.po.SemanticSearchDocument;
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

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ColumnService 单元测试")
class ColumnServiceTest {

    @Mock
    private ColumnInfoDAO columnInfoDAO;

    @Mock
    private TableInfoDAO tableInfoDAO;

    @Mock
    private JdbcMetaService jdbcMetaService;

    @Mock
    private SemanticIndexService semanticIndexService;

    @InjectMocks
    private ColumnService columnService;

    private ColumnInfo testColumnInfo;
    private ColumnInfoPO testColumnInfoPO;
    private TableInfoPO testTableInfoPO;

    @BeforeEach
    void setUp() {
        testColumnInfo = TestFixtures.createTestColumnInfo();
        testColumnInfoPO = TestFixtures.createTestColumnInfoPO();
        testTableInfoPO = TestFixtures.createTestTableInfoPO();
    }

    @Test
    @DisplayName("分页查询列 - 无关键词")
    void getColumns_withoutKeyword() {
        // given
        PageReq pageReq = new PageReq();
        pageReq.setPage(1);
        pageReq.setSize(10);

        Page<ColumnInfoPO> page = new PageImpl<>(List.of(testColumnInfoPO));
        when(columnInfoDAO.findByTableId(eq(TestFixtures.TEST_TABLE_ID), any()))
            .thenReturn(page);

        // when
        Page<ColumnInfo> result = columnService.getColumns(pageReq, TestFixtures.TEST_TABLE_ID, null);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getId()).isEqualTo(TestFixtures.TEST_COLUMN_ID);
    }

    @Test
    @DisplayName("分页查询列 - 有关键词")
    void getColumns_withKeyword() {
        // given
        PageReq pageReq = new PageReq();
        pageReq.setPage(1);
        pageReq.setSize(10);

        Page<ColumnInfoPO> page = new PageImpl<>(List.of(testColumnInfoPO));
        when(columnInfoDAO.findByTableIdAndNameContaining(eq(TestFixtures.TEST_TABLE_ID), eq("test"), any()))
            .thenReturn(page);

        // when
        Page<ColumnInfo> result = columnService.getColumns(pageReq, TestFixtures.TEST_TABLE_ID, "test");

        // then
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("更新列 - 成功")
    void updateColumn_shouldUpdateSuccessfully() {
        // given
        ColumnInfoPO existingPO = TestFixtures.createTestColumnInfoPO();
        when(columnInfoDAO.findById(TestFixtures.TEST_COLUMN_ID)).thenReturn(Optional.of(existingPO));
        when(columnInfoDAO.save(any(ColumnInfoPO.class))).thenReturn(existingPO);
        when(tableInfoDAO.findById(TestFixtures.TEST_TABLE_ID)).thenReturn(Optional.of(testTableInfoPO));

        ColumnInfo updateInfo = new ColumnInfo();
        updateInfo.setName("updated_column");
        updateInfo.setColumnType("INTEGER");
        updateInfo.setDescription("Updated description");

        // when
        columnService.updateColumn(TestFixtures.TEST_COLUMN_ID, updateInfo);

        // then
        verify(columnInfoDAO).findById(TestFixtures.TEST_COLUMN_ID);
        verify(columnInfoDAO).save(argThat(po -> 
            po.getName().equals("updated_column") &&
            po.getColumnType().equals("INTEGER") &&
            po.getDescription().equals("Updated description")
        ));

        ArgumentCaptor<SemanticSearchDocument> docCaptor = ArgumentCaptor.forClass(SemanticSearchDocument.class);
        verify(semanticIndexService).save(docCaptor.capture());
        SemanticSearchDocument savedDoc = docCaptor.getValue();
        assertThat(savedDoc.getId()).isEqualTo("field:" + TestFixtures.TEST_COLUMN_ID);
        assertThat(savedDoc.getType()).isEqualTo(SemanticEntityType.FIELD);
        assertThat(savedDoc.getKeywords()).containsExactly("updated_column");
        assertThat(savedDoc.getDescription()).isEqualTo("Updated description");
        assertThat(savedDoc.getEntity().getTableId()).isEqualTo(TestFixtures.TEST_TABLE_ID);
        assertThat(savedDoc.getEntity().getTableName()).isEqualTo("test_table");
        assertThat(savedDoc.getEntity().getField()).isEqualTo("updated_column");
    }

    @Test
    @DisplayName("更新列 - 列不存在时抛出异常")
    void updateColumn_shouldThrowWhenNotFound() {
        // given
        when(columnInfoDAO.findById(TestFixtures.TEST_COLUMN_ID)).thenReturn(Optional.empty());

        // when & then
        assertThrows(Exception.class, () ->
            columnService.updateColumn(TestFixtures.TEST_COLUMN_ID, testColumnInfo)
        );
        verify(columnInfoDAO, never()).save(any());
    }

    @Test
    @DisplayName("同步列 - 成功")
    void syncColumns_shouldSyncSuccessfully() throws SQLException {
        // given
        when(tableInfoDAO.findById(TestFixtures.TEST_TABLE_ID)).thenReturn(Optional.of(testTableInfoPO));
        
        Column mockColumn = mock(Column.class);
        when(mockColumn.name()).thenReturn("new_col");
        when(mockColumn.type()).thenReturn("VARCHAR");
        when(mockColumn.comment()).thenReturn("New comment");
        
        when(jdbcMetaService.getColumns(TestFixtures.TEST_DATASOURCE_ID, null, "public", "test_table"))
            .thenReturn(List.of(mockColumn));
        
        User mockUser = mock(User.class);
        when(mockUser.getId()).thenReturn(TestFixtures.TEST_USER_ID);
        
        ColumnInfoPO savedPO = TestFixtures.createTestColumnInfoPO();
        when(columnInfoDAO.saveAll(anyList())).thenReturn(List.of(savedPO));
        
        try (MockedStatic<RequestContext> mocked = mockStatic(RequestContext.class)) {
            mocked.when(RequestContext::getUser).thenReturn(mockUser);
            
            // when
            columnService.syncColumns(TestFixtures.TEST_DATASOURCE_ID, TestFixtures.TEST_TABLE_ID);

            // then
            verify(columnInfoDAO).deleteByTableId(TestFixtures.TEST_TABLE_ID);
            verify(columnInfoDAO).saveAll(anyList());
            verify(semanticIndexService).deleteByEntityTableId(TestFixtures.TEST_TABLE_ID);
            verify(semanticIndexService).saveBatch(anyList());
        }
    }

    @Test
    @DisplayName("同步列 - 表不存在时抛出异常")
    void syncColumns_shouldThrowWhenTableNotFound() {
        // given
        when(tableInfoDAO.findById(TestFixtures.TEST_TABLE_ID)).thenReturn(Optional.empty());

        // when & then
        assertThrows(Exception.class, () ->
            columnService.syncColumns(TestFixtures.TEST_DATASOURCE_ID, TestFixtures.TEST_TABLE_ID)
        );
        verify(columnInfoDAO, never()).deleteByTableId(any());
        verify(columnInfoDAO, never()).saveAll(any());
    }

    @Test
    @DisplayName("同步列 - 数据源获取列失败时抛出异常")
    void syncColumns_shouldThrowWhenDataSourceFails() throws SQLException {
        // given
        when(tableInfoDAO.findById(TestFixtures.TEST_TABLE_ID)).thenReturn(Optional.of(testTableInfoPO));
        when(jdbcMetaService.getColumns(TestFixtures.TEST_DATASOURCE_ID, null, "public", "test_table"))
            .thenThrow(new SQLException("Connection failed"));

        // when & then
        assertThrows(SQLException.class, () ->
            columnService.syncColumns(TestFixtures.TEST_DATASOURCE_ID, TestFixtures.TEST_TABLE_ID)
        );
    }

    @Test
    @DisplayName("同步列 - 无用户时应处理空用户")
    void syncColumns_shouldHandleNullUser() throws SQLException {
        // given
        when(tableInfoDAO.findById(TestFixtures.TEST_TABLE_ID)).thenReturn(Optional.of(testTableInfoPO));
        
        Column mockColumn = mock(Column.class);
        when(mockColumn.name()).thenReturn("col1");
        when(mockColumn.type()).thenReturn("INT");
        when(mockColumn.comment()).thenReturn(null);
        
        when(jdbcMetaService.getColumns(TestFixtures.TEST_DATASOURCE_ID, null, "public", "test_table"))
            .thenReturn(List.of(mockColumn));
        
        try (MockedStatic<RequestContext> mocked = mockStatic(RequestContext.class)) {
            mocked.when(RequestContext::getUser).thenReturn(null);
            
            // when
            columnService.syncColumns(TestFixtures.TEST_DATASOURCE_ID, TestFixtures.TEST_TABLE_ID);

            // then
            verify(columnInfoDAO).saveAll(argThat(list -> {
                List<ColumnInfoPO> columns = (List<ColumnInfoPO>) list;
                return columns.size() == 1 && columns.getFirst().getCreatedBy() == null;
            }));
        }
    }

    @Test
    @DisplayName("同步列 - 数据库 comment 为空时应保留旧的 displayName 和 description")
    void syncColumns_shouldPreserveOldDisplayNameAndDescriptionWhenDbCommentEmpty() throws SQLException {
        // given
        when(tableInfoDAO.findById(TestFixtures.TEST_TABLE_ID)).thenReturn(Optional.of(testTableInfoPO));
        
        ColumnInfoPO existingColumn = new ColumnInfoPO();
        existingColumn.setId("existing_col_id");
        existingColumn.setTableId(TestFixtures.TEST_TABLE_ID);
        existingColumn.setName("col1");
        existingColumn.setColumnType("VARCHAR");
        existingColumn.setDisplayName("Old Display Name");
        existingColumn.setDescription("User maintained description");
        
        when(columnInfoDAO.findByTableId(TestFixtures.TEST_TABLE_ID)).thenReturn(List.of(existingColumn));
        
        Column mockColumn = mock(Column.class);
        when(mockColumn.name()).thenReturn("col1");
        when(mockColumn.type()).thenReturn("VARCHAR");
        when(mockColumn.comment()).thenReturn(null);
        
        when(jdbcMetaService.getColumns(TestFixtures.TEST_DATASOURCE_ID, null, "public", "test_table"))
            .thenReturn(List.of(mockColumn));
        
        try (MockedStatic<RequestContext> mocked = mockStatic(RequestContext.class)) {
            mocked.when(RequestContext::getUser).thenReturn(null);
            
            // when
            columnService.syncColumns(TestFixtures.TEST_DATASOURCE_ID, TestFixtures.TEST_TABLE_ID);

            // then
            verify(columnInfoDAO).saveAll(argThat(list -> {
                List<ColumnInfoPO> columns = (List<ColumnInfoPO>) list;
                ColumnInfoPO savedCol = columns.getFirst();
                return savedCol.getDisplayName().equals("Old Display Name") &&
                       savedCol.getDescription().equals("User maintained description");
            }));
        }
    }

    @Test
    @DisplayName("同步列 - 数据库 comment 不为空时应覆盖 displayName")
    void syncColumns_shouldOverwriteDisplayNameWhenDbCommentNotEmpty() throws SQLException {
        // given
        when(tableInfoDAO.findById(TestFixtures.TEST_TABLE_ID)).thenReturn(Optional.of(testTableInfoPO));
        
        ColumnInfoPO existingColumn = new ColumnInfoPO();
        existingColumn.setId("existing_col_id");
        existingColumn.setTableId(TestFixtures.TEST_TABLE_ID);
        existingColumn.setName("col1");
        existingColumn.setColumnType("VARCHAR");
        existingColumn.setDisplayName("Old Display Name");
        existingColumn.setDescription("User maintained description");
        
        when(columnInfoDAO.findByTableId(TestFixtures.TEST_TABLE_ID)).thenReturn(List.of(existingColumn));
        
        Column mockColumn = mock(Column.class);
        when(mockColumn.name()).thenReturn("col1");
        when(mockColumn.type()).thenReturn("VARCHAR");
        when(mockColumn.comment()).thenReturn("New DB Comment");
        
        when(jdbcMetaService.getColumns(TestFixtures.TEST_DATASOURCE_ID, null, "public", "test_table"))
            .thenReturn(List.of(mockColumn));
        
        try (MockedStatic<RequestContext> mocked = mockStatic(RequestContext.class)) {
            mocked.when(RequestContext::getUser).thenReturn(null);
            
            // when
            columnService.syncColumns(TestFixtures.TEST_DATASOURCE_ID, TestFixtures.TEST_TABLE_ID);

            // then
            verify(columnInfoDAO).saveAll(argThat(list -> {
                List<ColumnInfoPO> columns = (List<ColumnInfoPO>) list;
                ColumnInfoPO savedCol = columns.getFirst();
                return savedCol.getDisplayName().equals("New DB Comment") &&
                       savedCol.getDescription().equals("User maintained description");
            }));
        }
    }
}
