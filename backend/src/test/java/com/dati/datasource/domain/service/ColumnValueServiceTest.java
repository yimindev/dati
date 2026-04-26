package com.dati.datasource.domain.service;

import com.dati.base.exception.DatiException;
import com.dati.base.pojo.PageReq;
import com.dati.config.ColumnValueConfig;
import com.dati.datasource.repository.dao.ColumnInfoDAO;
import com.dati.datasource.repository.dao.TableInfoDAO;
import com.dati.datasource.repository.po.ColumnInfoPO;
import com.dati.datasource.repository.po.TableInfoPO;
import com.dati.semantic.domain.SemanticEntityType;
import com.dati.semantic.domain.service.SemanticIndexService;
import com.dati.semantic.repository.po.EntityReference;
import com.dati.semantic.repository.po.SemanticSearchDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ColumnValueService 单元测试")
class ColumnValueServiceTest {

    @Mock
    private ColumnInfoDAO columnInfoDAO;

    @Mock
    private TableInfoDAO tableInfoDAO;

    @Mock
    private JdbcMetaService jdbcMetaService;

    @Mock
    private SemanticIndexService semanticIndexService;

    @Captor
    private ArgumentCaptor<List<SemanticSearchDocument>> captor;

    private ColumnValueService columnValueService;

    private ColumnInfoPO testColumnPO;

    private TableInfoPO testTablePO;

    @BeforeEach
    void setUp() {
        ColumnValueConfig columnValueConfig = new ColumnValueConfig();
        columnValueConfig.setColumnValueSampleLimit(1000);
        columnValueConfig.setColumnValueLengthLimit(256);

        columnValueService = new ColumnValueService(
                columnInfoDAO, tableInfoDAO, jdbcMetaService, semanticIndexService, columnValueConfig);

        testColumnPO = new ColumnInfoPO();
        testColumnPO.setId("col1");
        testColumnPO.setTableId("table1");
        testColumnPO.setName("status");

        testTablePO = new TableInfoPO();
        testTablePO.setId("table1");
        testTablePO.setName("users");
    }

    private SemanticSearchDocument createValueDoc(String id, List<String> keywords) {
        return SemanticSearchDocument.builder()
                .id(id)
                .type(SemanticEntityType.FIELD_VALUE)
                .keywords(keywords)
                .entity(EntityReference.builder()
                        .tableId("table1")
                        .field("status")
                        .build())
                .build();
    }

    @Nested
    @DisplayName("extractValues")
    class ExtractValuesTests {

        @Test
        @DisplayName("正常流程：提取多个不重复值，生成对应 ES 文档")
        void extractValues_normalCase() throws SQLException {
            when(columnInfoDAO.findById("col1")).thenReturn(Optional.of(testColumnPO));
            when(tableInfoDAO.findById("table1")).thenReturn(Optional.of(testTablePO));
            when(jdbcMetaService.executeSql(eq("ds1"), anyString()))
                    .thenReturn(List.of(
                            Map.of("status", "active"),
                            Map.of("status", "inactive")
                    ));

            columnValueService.extractValues("ds1", "col1", false);

            verify(semanticIndexService).saveBatch(captor.capture());
            List<SemanticSearchDocument> saved = captor.getValue();

            assertThat(saved).hasSize(2);
            assertThat(saved.get(0).getKeywords()).containsExactly("active");
            assertThat(saved.get(1).getKeywords()).containsExactly("inactive");
        }

        @Test
        @DisplayName("跳过 null 值")
        void extractValues_skipsNullValues() throws SQLException {
            when(columnInfoDAO.findById("col1")).thenReturn(Optional.of(testColumnPO));
            when(tableInfoDAO.findById("table1")).thenReturn(Optional.of(testTablePO));
            Map<String, Object> row1 = new HashMap<>();
            row1.put("status", "active");
            Map<String, Object> row2 = new HashMap<>();
            row2.put("status", null);
            when(jdbcMetaService.executeSql(eq("ds1"), anyString()))
                    .thenReturn(List.of(row1, row2));

            columnValueService.extractValues("ds1", "col1", false);

            verify(semanticIndexService).saveBatch(captor.capture());
            assertThat(captor.getValue()).hasSize(1);
        }

        @Test
        @DisplayName("长值截断：超过 256 字符的值被截断")
        void extractValues_truncatesLongValues() throws SQLException {
            when(columnInfoDAO.findById("col1")).thenReturn(Optional.of(testColumnPO));
            when(tableInfoDAO.findById("table1")).thenReturn(Optional.of(testTablePO));
            String longValue = "a".repeat(300);
            when(jdbcMetaService.executeSql(eq("ds1"), anyString()))
                    .thenReturn(List.of(Map.of("status", longValue)));

            columnValueService.extractValues("ds1", "col1", false);

            verify(semanticIndexService).saveBatch(captor.capture());
            assertThat(captor.getValue().getFirst().getKeywords().getFirst()).hasSize(256);
        }

        @Test
        @DisplayName("覆盖模式：先删除已有值再抽取")
        void extractValues_overwriteMode() throws SQLException {
            SemanticSearchDocument existingDoc = createValueDoc("existing_id", List.of("old_value"));
            when(columnInfoDAO.findById("col1")).thenReturn(Optional.of(testColumnPO));
            when(semanticIndexService.findByTableFieldAndType("table1", "status", SemanticEntityType.FIELD_VALUE))
                    .thenReturn(List.of(existingDoc));
            when(tableInfoDAO.findById("table1")).thenReturn(Optional.of(testTablePO));
            when(jdbcMetaService.executeSql(eq("ds1"), anyString()))
                    .thenReturn(List.of(Map.of("status", "new_value")));

            columnValueService.extractValues("ds1", "col1", true);

            verify(semanticIndexService).deleteById("existing_id");
            verify(semanticIndexService).saveBatch(anyList());
        }

        @Test
        @DisplayName("columnId 不存在时抛出 IllegalArgumentException")
        void extractValues_columnNotFound() {
            when(columnInfoDAO.findById("invalid")).thenReturn(Optional.empty());

            assertThrows(DatiException.class, () ->
                    columnValueService.extractValues("ds1", "invalid", false)
            );
        }
    }

    @Nested
    @DisplayName("saveValues")
    class SaveValuesTests {

        @Test
        @DisplayName("新增值：创建新 ES 文档，keywords 包含值和同义词")
        void saveValues_addNewValue() {
            when(columnInfoDAO.findById("col1")).thenReturn(Optional.of(testColumnPO));

            ColumnValueService.ValueItem newItem = new ColumnValueService.ValueItem();
            newItem.setId(null);
            newItem.setValue("北京");
            newItem.setSynonyms(List.of("帝都", "北漂之城"));

            columnValueService.saveValues("col1", List.of(newItem), null);

            verify(semanticIndexService).saveBatch(captor.capture());
            List<SemanticSearchDocument> saved = captor.getValue();

            assertThat(saved).hasSize(1);
            assertThat(saved.getFirst().getKeywords()).containsExactly("北京", "帝都", "北漂之城");
        }

        @Test
        @DisplayName("删除值：根据 deletedIds 调用 deleteById")
        void saveValues_deleteValues() {
            columnValueService.saveValues("col1", null, List.of("id1", "id2"));

            verify(semanticIndexService).deleteById("id1");
            verify(semanticIndexService).deleteById("id2");
            verify(semanticIndexService, never()).saveBatch(anyList());
        }

        @Test
        @DisplayName("混合场景：同时删除旧值和新增新值")
        void saveValues_mixedOperations() {
            when(columnInfoDAO.findById("col1")).thenReturn(Optional.of(testColumnPO));

            ColumnValueService.ValueItem newItem = new ColumnValueService.ValueItem();
            newItem.setId(null);
            newItem.setValue("广州");
            newItem.setSynonyms(List.of());

            columnValueService.saveValues("col1", List.of(newItem), List.of("id_to_delete"));

            verify(semanticIndexService).deleteById("id_to_delete");
            verify(semanticIndexService).saveBatch(anyList());
        }

        @Test
        @DisplayName("空操作：values 和 deletedIds 都为空时不做任何操作")
        void saveValues_emptyInput() {
            columnValueService.saveValues("col1", null, null);

            verify(semanticIndexService, never()).saveBatch(anyList());
            verify(semanticIndexService, never()).deleteById(anyString());
        }
    }

    @Nested
    @DisplayName("getValues")
    class GetValuesTests {

        @Test
        @DisplayName("正常返回：分页结构，解析 keywords 提取值和同义词")
        void getValues_normal() {
            SemanticSearchDocument doc1 = createValueDoc("doc1", List.of("北京", "帝都"));
            SemanticSearchDocument doc2 = createValueDoc("doc2", List.of("上海"));

            when(semanticIndexService.findByTableFieldAndTypePaginated(
                    eq("table1"), eq("status"), eq(SemanticEntityType.FIELD_VALUE), isNull(), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(doc1, doc2), PageRequest.of(0, 10), 2));
            when(columnInfoDAO.findById("col1")).thenReturn(Optional.of(testColumnPO));

            Page<ColumnValueService.ValueItem> result = columnValueService.getValues("col1", new PageReq(), null);

            assertThat(result.getTotalElements()).isEqualTo(2);
            assertThat(result.getContent()).hasSize(2);

            ColumnValueService.ValueItem first = result.getContent().getFirst();
            assertThat(first.getId()).isEqualTo("doc1");
            assertThat(first.getValue()).isEqualTo("北京");
            assertThat(first.getSynonyms()).containsExactly("帝都");

            ColumnValueService.ValueItem second = result.getContent().get(1);
            assertThat(second.getId()).isEqualTo("doc2");
            assertThat(second.getValue()).isEqualTo("上海");
            assertThat(second.getSynonyms()).isEmpty();
        }

        @Test
        @DisplayName("columnId 不存在时抛出 DatiException")
        void getValues_columnNotFound() {
            when(columnInfoDAO.findById("invalid")).thenReturn(Optional.empty());

            assertThrows(DatiException.class, () ->
                    columnValueService.getValues("invalid", new PageReq(), null)
            );
        }

        @Test
        @DisplayName("分页+搜索：验证参数正确传递")
        void getValues_withPaginationAndKeyword() {
            PageReq pageReq = new PageReq();
            pageReq.setPage(2);
            pageReq.setSize(20);

            when(semanticIndexService.findByTableFieldAndTypePaginated(
                    eq("table1"), eq("status"), eq(SemanticEntityType.FIELD_VALUE), eq("北京"), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(), PageRequest.of(1, 20), 0));
            when(columnInfoDAO.findById("col1")).thenReturn(Optional.of(testColumnPO));

            columnValueService.getValues("col1", pageReq, "北京");

            verify(semanticIndexService).findByTableFieldAndTypePaginated(
                    eq("table1"), eq("status"), eq(SemanticEntityType.FIELD_VALUE), eq("北京"),
                    argThat(pageable -> pageable.getPageNumber() == 1 && pageable.getPageSize() == 20)
            );
        }
    }
}
