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
import com.dati.permission.domain.service.PermissionService;
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

import com.dati.base.exception.DatiException;
import com.dati.base.exception.ErrorCode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ColumnService unit tests")
class ColumnServiceTest {

    @Mock
    private ColumnInfoDAO columnInfoDAO;

    @Mock
    private TableInfoDAO tableInfoDAO;

    @Mock
    private JdbcMetaService jdbcMetaService;

    @Mock
    private SemanticIndexService semanticIndexService;

    @Mock
    private PermissionService permissionService;

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
        lenient().when(tableInfoDAO.findById(anyString())).thenReturn(Optional.of(testTableInfoPO));
    }

    @Test
    @DisplayName("Paged query columns - without keyword")
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
    @DisplayName("Paged query columns - with keyword")
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
    @DisplayName("Update column - success")
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
        updateInfo.setExtractValueEnabled(true);

        // when
        columnService.updateColumn(TestFixtures.TEST_COLUMN_ID, updateInfo);

        // then
        verify(columnInfoDAO).findById(TestFixtures.TEST_COLUMN_ID);
        verify(columnInfoDAO).save(argThat(po -> 
            po.getName().equals("updated_column") &&
            po.getColumnType().equals("INTEGER") &&
            po.getDescription().equals("Updated description") &&
            po.isExtractValueEnabled()
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
    @DisplayName("Update column - clears FIELD_VALUE data when value matching disabled")
    void updateColumn_disableValueMatching_shouldClearFieldValues() {
        // given
        ColumnInfoPO existingPO = TestFixtures.createTestColumnInfoPO();
        existingPO.setExtractValueEnabled(true);
        when(columnInfoDAO.findById(TestFixtures.TEST_COLUMN_ID)).thenReturn(Optional.of(existingPO));
        when(columnInfoDAO.save(any())).thenReturn(existingPO);
        when(tableInfoDAO.findById(TestFixtures.TEST_TABLE_ID)).thenReturn(Optional.of(testTableInfoPO));

        ColumnInfo updateInfo = new ColumnInfo();
        updateInfo.setName("test_column");
        updateInfo.setColumnType("VARCHAR");
        updateInfo.setExtractValueEnabled(false);

        // when
        columnService.updateColumn(TestFixtures.TEST_COLUMN_ID, updateInfo);

        // then
        verify(columnInfoDAO).save(argThat(po -> !po.isExtractValueEnabled()));
        verify(semanticIndexService).deleteByTableFieldAndType(TestFixtures.TEST_TABLE_ID, "test_column", SemanticEntityType.FIELD_VALUE);
    }

    @Test
    @DisplayName("Update column - keeps data when staying disabled")
    void updateColumn_keepDisabled_shouldNotClearValues() {
        // given
        ColumnInfoPO existingPO = TestFixtures.createTestColumnInfoPO();
        existingPO.setExtractValueEnabled(false);
        when(columnInfoDAO.findById(TestFixtures.TEST_COLUMN_ID)).thenReturn(Optional.of(existingPO));
        when(columnInfoDAO.save(any())).thenReturn(existingPO);
        when(tableInfoDAO.findById(TestFixtures.TEST_TABLE_ID)).thenReturn(Optional.of(testTableInfoPO));

        ColumnInfo updateInfo = new ColumnInfo();
        updateInfo.setName("test_column");
        updateInfo.setColumnType("VARCHAR");
        updateInfo.setExtractValueEnabled(false);

        // when
        columnService.updateColumn(TestFixtures.TEST_COLUMN_ID, updateInfo);

        // then
        verify(columnInfoDAO).save(any());
        verify(semanticIndexService, never()).deleteByTableFieldAndType(any(), any(), any());
    }

    @Test
    @DisplayName("Update column - keeps extractValueEnabled and FIELD_VALUE data when not specified")
    void updateColumn_withoutExtractValueEnabled_shouldPreserveStatusAndValues() {
        // given
        ColumnInfoPO existingPO = TestFixtures.createTestColumnInfoPO();
        existingPO.setExtractValueEnabled(true);
        existingPO.setDescription("Original description");
        when(columnInfoDAO.findById(TestFixtures.TEST_COLUMN_ID)).thenReturn(Optional.of(existingPO));
        when(columnInfoDAO.save(any())).thenReturn(existingPO);
        when(tableInfoDAO.findById(TestFixtures.TEST_TABLE_ID)).thenReturn(Optional.of(testTableInfoPO));

        ColumnInfo updateInfo = new ColumnInfo();
        updateInfo.setDescription("New description");
        // extractValueEnabled is left null (omitted)

        // when
        columnService.updateColumn(TestFixtures.TEST_COLUMN_ID, updateInfo);

        // then
        verify(columnInfoDAO).save(argThat(ColumnInfoPO::isExtractValueEnabled));
        verify(semanticIndexService, never()).deleteByTableFieldAndType(any(), any(), any());
    }

    @Test
    @DisplayName("Update column - preserves existing description in semantic index when description is null")
    void updateColumn_withoutDescription_shouldPreserveExistingDescriptionInSemanticIndex() {
        // given
        ColumnInfoPO existingPO = TestFixtures.createTestColumnInfoPO();
        existingPO.setDescription("Original description");
        when(columnInfoDAO.findById(TestFixtures.TEST_COLUMN_ID)).thenReturn(Optional.of(existingPO));
        when(columnInfoDAO.save(any())).thenReturn(existingPO);
        when(tableInfoDAO.findById(TestFixtures.TEST_TABLE_ID)).thenReturn(Optional.of(testTableInfoPO));

        ColumnInfo updateInfo = new ColumnInfo();
        updateInfo.setAliases(List.of("alias1"));
        // description is left null

        // when
        columnService.updateColumn(TestFixtures.TEST_COLUMN_ID, updateInfo);

        // then
        ArgumentCaptor<SemanticSearchDocument> docCaptor = ArgumentCaptor.forClass(SemanticSearchDocument.class);
        verify(semanticIndexService).save(docCaptor.capture());
        assertThat(docCaptor.getValue().getDescription()).isEqualTo("Original description");
    }

    @Test
    @DisplayName("Update column - keeps existing aliases when aliases not specified")
    void updateColumn_withoutAliases_shouldPreserveExistingAliases() {
        // given
        ColumnInfoPO existingPO = TestFixtures.createTestColumnInfoPO();
        existingPO.setAliases(List.of("genre", "流派"));
        existingPO.setDescription("Original description");
        when(columnInfoDAO.findById(TestFixtures.TEST_COLUMN_ID)).thenReturn(Optional.of(existingPO));
        when(columnInfoDAO.save(any())).thenReturn(existingPO);
        when(tableInfoDAO.findById(TestFixtures.TEST_TABLE_ID)).thenReturn(Optional.of(testTableInfoPO));

        ColumnInfo updateInfo = new ColumnInfo();
        updateInfo.setDescription("New description");
        // aliases is left null (omitted)

        // when
        columnService.updateColumn(TestFixtures.TEST_COLUMN_ID, updateInfo);

        // then
        verify(columnInfoDAO).save(argThat(po -> po.getAliases() != null
            && po.getAliases().equals(List.of("genre", "流派"))));
    }

    @Test
    @DisplayName("Update column - throws when column not found")
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
    @DisplayName("Sync columns - success")
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
            columnService.syncColumns(TestFixtures.TEST_DATASOURCE_ID, TestFixtures.TEST_TABLE_ID, true);

            // then
            verify(columnInfoDAO).deleteByTableId(TestFixtures.TEST_TABLE_ID);
            verify(columnInfoDAO).saveAll(anyList());
            verify(semanticIndexService).deleteByTableIdAndType(TestFixtures.TEST_TABLE_ID, SemanticEntityType.FIELD);
            verify(semanticIndexService).saveBatch(anyList());
        }
    }

    @Test
    @DisplayName("Sync columns - preserves extractValueEnabled and FIELD_VALUE of existing columns, cleans dropped columns")
    void syncColumns_shouldPreserveExtractValueEnabledAndKeepFieldValuesOfExistingColumns() throws SQLException {
        // given
        when(tableInfoDAO.findById(TestFixtures.TEST_TABLE_ID)).thenReturn(Optional.of(testTableInfoPO));

        ColumnInfoPO existingGenre = new ColumnInfoPO();
        existingGenre.setId("existing_genre_id");
        existingGenre.setTableId(TestFixtures.TEST_TABLE_ID);
        existingGenre.setName("genre");
        existingGenre.setColumnType("VARCHAR");
        existingGenre.setAliases(List.of("流派"));
        existingGenre.setDescription("User maintained description");
        existingGenre.setExtractValueEnabled(true);

        ColumnInfoPO existingDropped = new ColumnInfoPO();
        existingDropped.setId("existing_dropped_id");
        existingDropped.setTableId(TestFixtures.TEST_TABLE_ID);
        existingDropped.setName("dropped_col");
        existingDropped.setColumnType("VARCHAR");
        existingDropped.setExtractValueEnabled(true);

        when(columnInfoDAO.findByTableId(TestFixtures.TEST_TABLE_ID))
            .thenReturn(List.of(existingGenre, existingDropped));

        Column mockColumn = mock(Column.class);
        when(mockColumn.name()).thenReturn("genre");
        when(mockColumn.type()).thenReturn("VARCHAR");
        when(mockColumn.comment()).thenReturn("DB comment");

        when(jdbcMetaService.getColumns(TestFixtures.TEST_DATASOURCE_ID, null, "public", "test_table"))
            .thenReturn(List.of(mockColumn));

        ColumnInfoPO savedPO = TestFixtures.createTestColumnInfoPO();
        when(columnInfoDAO.saveAll(anyList())).thenReturn(List.of(savedPO));

        try (MockedStatic<RequestContext> mocked = mockStatic(RequestContext.class)) {
            mocked.when(RequestContext::getUser).thenReturn(null);

            // when
            columnService.syncColumns(TestFixtures.TEST_DATASOURCE_ID, TestFixtures.TEST_TABLE_ID, true);

            // then - new PO keeps the user's extractValueEnabled and aliases config
            verify(columnInfoDAO).saveAll(argThat(list -> {
                List<ColumnInfoPO> columns = (List<ColumnInfoPO>) list;
                ColumnInfoPO savedCol = columns.getFirst();
                return savedCol.isExtractValueEnabled()
                    && savedCol.getAliases().equals(List.of("流派"));
            }));

            // then - structure index rebuilt per FIELD type, no full-table delete
            verify(semanticIndexService).deleteByTableIdAndType(
                TestFixtures.TEST_TABLE_ID, SemanticEntityType.FIELD);
            verify(semanticIndexService, never()).deleteByEntityTableId(any());

            // then - FIELD_VALUE cleaned only for dropped columns, kept for existing ones
            verify(semanticIndexService).deleteByTableFieldAndType(
                TestFixtures.TEST_TABLE_ID, "dropped_col", SemanticEntityType.FIELD_VALUE);
            verify(semanticIndexService, never()).deleteByTableFieldAndType(
                TestFixtures.TEST_TABLE_ID, "genre", SemanticEntityType.FIELD_VALUE);
        }
    }

    @Test
    @DisplayName("Sync columns - throws when table not found")
    void syncColumns_shouldThrowWhenTableNotFound() {
        // given
        when(tableInfoDAO.findById(TestFixtures.TEST_TABLE_ID)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() ->
            columnService.syncColumns(TestFixtures.TEST_DATASOURCE_ID, TestFixtures.TEST_TABLE_ID, true)
        ).isInstanceOf(DatiException.class)
         .satisfies(e -> assertThat(((DatiException) e).getCode()).isEqualTo(ErrorCode.DS_TABLE_NOT_FOUND));

        verify(columnInfoDAO, never()).deleteByTableId(any());
        verify(columnInfoDAO, never()).saveAll(any());
    }

    @Test
    @DisplayName("Sync columns - throws when datasource column fetch fails")
    void syncColumns_shouldThrowWhenDataSourceFails() throws SQLException {
        // given
        when(tableInfoDAO.findById(TestFixtures.TEST_TABLE_ID)).thenReturn(Optional.of(testTableInfoPO));
        when(jdbcMetaService.getColumns(TestFixtures.TEST_DATASOURCE_ID, null, "public", "test_table"))
            .thenThrow(new SQLException("Connection failed"));

        // when & then
        assertThrows(SQLException.class, () ->
            columnService.syncColumns(TestFixtures.TEST_DATASOURCE_ID, TestFixtures.TEST_TABLE_ID, true)
        );
    }

    @Test
    @DisplayName("Sync columns - handles empty user")
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
            columnService.syncColumns(TestFixtures.TEST_DATASOURCE_ID, TestFixtures.TEST_TABLE_ID, true);

            // then
            verify(columnInfoDAO).saveAll(argThat(list -> {
                List<ColumnInfoPO> columns = (List<ColumnInfoPO>) list;
                return columns.size() == 1 && columns.getFirst().getCreatedBy() == null;
            }));
        }
    }

    @Test
    @DisplayName("Sync columns - keeps old description when DB comment empty (flag=true)")
    void syncColumns_shouldPreserveOldDescriptionWhenDbCommentEmptyEvenIfFlagTrue() throws SQLException {
        // given
        when(tableInfoDAO.findById(TestFixtures.TEST_TABLE_ID)).thenReturn(Optional.of(testTableInfoPO));
        
        ColumnInfoPO existingColumn = new ColumnInfoPO();
        existingColumn.setId("existing_col_id");
        existingColumn.setTableId(TestFixtures.TEST_TABLE_ID);
        existingColumn.setName("col1");
        existingColumn.setColumnType("VARCHAR");
        existingColumn.setAliases(List.of("order_no", "订单号"));
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
            
            // when - flag true but DB comment is empty, should preserve
            columnService.syncColumns(TestFixtures.TEST_DATASOURCE_ID, TestFixtures.TEST_TABLE_ID, true);

            // then
            verify(columnInfoDAO).saveAll(argThat(list -> {
                List<ColumnInfoPO> columns = (List<ColumnInfoPO>) list;
                ColumnInfoPO savedCol = columns.getFirst();
                return savedCol.getAliases().equals(List.of("order_no", "订单号")) &&
                       savedCol.getDescription().equals("User maintained description");
            }));
        }
    }

    @Test
    @DisplayName("Sync columns - existing column + flag=true + DB comment present applies DB comment")
    void syncColumns_shouldOverwriteWhenFlagTrueAndDbCommentExists() throws SQLException {
        // given
        when(tableInfoDAO.findById(TestFixtures.TEST_TABLE_ID)).thenReturn(Optional.of(testTableInfoPO));
        
        ColumnInfoPO existingColumn = new ColumnInfoPO();
        existingColumn.setId("existing_col_id");
        existingColumn.setTableId(TestFixtures.TEST_TABLE_ID);
        existingColumn.setName("col1");
        existingColumn.setColumnType("VARCHAR");
        existingColumn.setAliases(List.of("order_no", "订单号"));
        existingColumn.setDescription("Old user description");
        
        when(columnInfoDAO.findByTableId(TestFixtures.TEST_TABLE_ID)).thenReturn(List.of(existingColumn));
        
        Column mockColumn = mock(Column.class);
        when(mockColumn.name()).thenReturn("col1");
        when(mockColumn.type()).thenReturn("VARCHAR");
        when(mockColumn.comment()).thenReturn("New DB Comment");
        
        when(jdbcMetaService.getColumns(TestFixtures.TEST_DATASOURCE_ID, null, "public", "test_table"))
            .thenReturn(List.of(mockColumn));
        
        try (MockedStatic<RequestContext> mocked = mockStatic(RequestContext.class)) {
            mocked.when(RequestContext::getUser).thenReturn(null);
            
            // when - flag true and DB comment exists, should overwrite
            columnService.syncColumns(TestFixtures.TEST_DATASOURCE_ID, TestFixtures.TEST_TABLE_ID, true);

            // then
            verify(columnInfoDAO).saveAll(argThat(list -> {
                List<ColumnInfoPO> columns = (List<ColumnInfoPO>) list;
                ColumnInfoPO savedCol = columns.getFirst();
                return savedCol.getAliases().equals(List.of("order_no", "订单号")) &&
                       savedCol.getDescription().equals("New DB Comment");
            }));
        }
    }

    @Test
    @DisplayName("Sync columns - existing column + flag=false keeps old description")
    void syncColumns_shouldPreserveWhenFlagFalse() throws SQLException {
        // given
        when(tableInfoDAO.findById(TestFixtures.TEST_TABLE_ID)).thenReturn(Optional.of(testTableInfoPO));
        
        ColumnInfoPO existingColumn = new ColumnInfoPO();
        existingColumn.setId("existing_col_id");
        existingColumn.setTableId(TestFixtures.TEST_TABLE_ID);
        existingColumn.setName("col1");
        existingColumn.setColumnType("VARCHAR");
        existingColumn.setAliases(List.of("order_no", "订单号"));
        existingColumn.setDescription("Old user description");
        
        when(columnInfoDAO.findByTableId(TestFixtures.TEST_TABLE_ID)).thenReturn(List.of(existingColumn));
        
        Column mockColumn = mock(Column.class);
        when(mockColumn.name()).thenReturn("col1");
        when(mockColumn.type()).thenReturn("VARCHAR");
        when(mockColumn.comment()).thenReturn("New DB Comment");
        
        when(jdbcMetaService.getColumns(TestFixtures.TEST_DATASOURCE_ID, null, "public", "test_table"))
            .thenReturn(List.of(mockColumn));
        
        try (MockedStatic<RequestContext> mocked = mockStatic(RequestContext.class)) {
            mocked.when(RequestContext::getUser).thenReturn(null);
            
            // when - flag false, should preserve old
            columnService.syncColumns(TestFixtures.TEST_DATASOURCE_ID, TestFixtures.TEST_TABLE_ID, false);

            // then
            verify(columnInfoDAO).saveAll(argThat(list -> {
                List<ColumnInfoPO> columns = (List<ColumnInfoPO>) list;
                ColumnInfoPO savedCol = columns.getFirst();
                return savedCol.getAliases().equals(List.of("order_no", "订单号")) &&
                       savedCol.getDescription().equals("Old user description");
            }));
        }
    }

    @Test
    @DisplayName("Sync columns - new column always uses DB comment regardless of flag")
    void syncColumns_newColumn_shouldUseDbCommentRegardlessOfFlag() throws SQLException {
        // given
        when(tableInfoDAO.findById(TestFixtures.TEST_TABLE_ID)).thenReturn(Optional.of(testTableInfoPO));
        
        when(columnInfoDAO.findByTableId(TestFixtures.TEST_TABLE_ID)).thenReturn(List.of());
        
        Column mockColumn = mock(Column.class);
        when(mockColumn.name()).thenReturn("new_col");
        when(mockColumn.type()).thenReturn("VARCHAR");
        when(mockColumn.comment()).thenReturn("New column comment");
        
        when(jdbcMetaService.getColumns(TestFixtures.TEST_DATASOURCE_ID, null, "public", "test_table"))
            .thenReturn(List.of(mockColumn));
        
        try (MockedStatic<RequestContext> mocked = mockStatic(RequestContext.class)) {
            mocked.when(RequestContext::getUser).thenReturn(null);
            
            // when - new column, flag false, should use DB comment
            columnService.syncColumns(TestFixtures.TEST_DATASOURCE_ID, TestFixtures.TEST_TABLE_ID, false);

            // then
            verify(columnInfoDAO).saveAll(argThat(list -> {
                List<ColumnInfoPO> columns = (List<ColumnInfoPO>) list;
                ColumnInfoPO savedCol = columns.getFirst();
                return savedCol.getDescription().equals("New column comment");
            }));
        }
    }
}
