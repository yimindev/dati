package com.dati.semantic.domain.service;

import com.dati.semantic.domain.SemanticEntityType;
import com.dati.semantic.repository.dao.SemanticSearchDAO;
import com.dati.semantic.repository.po.EntityReference;
import com.dati.semantic.repository.po.SemanticSearchDocument;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("SemanticIndexService 单元测试")
class SemanticIndexServiceTest {

    @Mock
    private SemanticSearchDAO semanticSearchDAO;

    @InjectMocks
    private SemanticIndexService semanticIndexService;

    @Test
    @DisplayName("保存文档 - 首次保存应设置 createdTime 和 updatedTime")
    void save_shouldSetCreatedTimeAndUpdatedTimeWhenFirstSave() {
        // given
        SemanticSearchDocument doc = SemanticSearchDocument.builder()
                .id("field:test-id")
                .type(SemanticEntityType.FIELD)
                .keywords(List.of("test_column"))
                .description("Test description")
                .entity(EntityReference.builder()
                        .tableId("table-001")
                        .tableName("test_table")
                        .field("test_column")
                        .build())
                .build();

        // when
        semanticIndexService.save(doc);

        // then
        ArgumentCaptor<SemanticSearchDocument> captor = ArgumentCaptor.forClass(SemanticSearchDocument.class);
        verify(semanticSearchDAO).save(captor.capture());

        SemanticSearchDocument savedDoc = captor.getValue();
        assertThat(savedDoc.getCreatedTime()).isNotNull();
        assertThat(savedDoc.getUpdatedTime()).isNotNull();
        assertThat(savedDoc.getCreatedTime()).isEqualTo(savedDoc.getUpdatedTime());
    }

    @Test
    @DisplayName("保存文档 - 更新时应保持 createdTime 不变，仅更新 updatedTime")
    void save_shouldKeepCreatedTimeWhenUpdating() {
        // given
        LocalDateTime originalCreatedTime = LocalDateTime.now().minusDays(1);
        SemanticSearchDocument doc = SemanticSearchDocument.builder()
                .id("field:test-id")
                .type(SemanticEntityType.FIELD)
                .keywords(List.of("test_column"))
                .description("Updated description")
                .createdTime(originalCreatedTime)
                .entity(EntityReference.builder()
                        .tableId("table-001")
                        .tableName("test_table")
                        .field("test_column")
                        .build())
                .build();

        // when
        semanticIndexService.save(doc);

        // then
        ArgumentCaptor<SemanticSearchDocument> captor = ArgumentCaptor.forClass(SemanticSearchDocument.class);
        verify(semanticSearchDAO).save(captor.capture());

        SemanticSearchDocument savedDoc = captor.getValue();
        assertThat(savedDoc.getCreatedTime()).isEqualTo(originalCreatedTime);
        assertThat(savedDoc.getUpdatedTime()).isNotNull();
        assertThat(savedDoc.getUpdatedTime()).isAfter(originalCreatedTime);
    }

    @Test
    @DisplayName("批量保存文档 - 应正确设置所有文档的时间戳")
    void saveBatch_shouldSetTimeForAllDocuments() {
        // given
        LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
        
        SemanticSearchDocument doc1 = SemanticSearchDocument.builder()
                .id("field:test-id-1")
                .type(SemanticEntityType.FIELD)
                .keywords(List.of("col1"))
                .description("Description 1")
                .build();

        SemanticSearchDocument doc2 = SemanticSearchDocument.builder()
                .id("field:test-id-2")
                .type(SemanticEntityType.FIELD)
                .keywords(List.of("col2"))
                .description("Description 2")
                .createdTime(yesterday)
                .build();

        // when
        semanticIndexService.saveBatch(List.of(doc1, doc2));

        // then
        verify(semanticSearchDAO).saveAll(anyList());

        ArgumentCaptor<List<SemanticSearchDocument>> captor = ArgumentCaptor.forClass(List.class);
        verify(semanticSearchDAO).saveAll(captor.capture());

        List<SemanticSearchDocument> savedDocs = captor.getValue();
        assertThat(savedDocs).hasSize(2);
        assertThat(savedDocs.getFirst().getCreatedTime()).isNotNull();
        assertThat(savedDocs.get(0).getUpdatedTime()).isNotNull();
        assertThat(savedDocs.get(0).getCreatedTime()).isEqualTo(savedDocs.get(0).getUpdatedTime());
        assertThat(savedDocs.get(1).getUpdatedTime()).isNotNull();
        assertThat(savedDocs.get(1).getCreatedTime()).isEqualTo(yesterday);
    }

    @Test
    @DisplayName("根据 tableId 删除文档 - 应调用 DAO 批量删除方法")
    void deleteByEntityTableId_shouldCallDaoDeleteMethod() {
        // given
        String tableId = "table-001";

        // when
        semanticIndexService.deleteByEntityTableId(tableId);

        // then
        verify(semanticSearchDAO).deleteByEntity_TableIdIn(List.of(tableId));
    }

    @Test
    @DisplayName("根据 tableIds 批量删除文档 - 应调用 DAO 批量删除方法")
    void deleteByEntityTableIds_shouldCallDaoBatchDeleteMethod() {
        // given
        List<String> tableIds = List.of("table-001", "table-002");

        // when
        semanticIndexService.deleteByEntityTableIds(tableIds);

        // then
        verify(semanticSearchDAO).deleteByEntity_TableIdIn(tableIds);
    }

    @Test
    @DisplayName("根据 subjectId 删除文档 - 应调用 DAO 删除方法")
    void deleteByEntity_SubjectId_shouldCallDaoDeleteMethod() {
        // given
        String subjectId = "subject-001";

        // when
        semanticIndexService.deleteByEntity_SubjectId(subjectId);

        // then
        verify(semanticSearchDAO).deleteByEntity_SubjectId(subjectId);
    }

    @Test
    @DisplayName("根据 ID 删除文档 - 应调用 DAO 删除方法")
    void deleteById_shouldCallDaoDeleteMethod() {
        // given
        String id = "term:123";

        // when
        semanticIndexService.deleteById(id);

        // then
        verify(semanticSearchDAO).deleteById(id);
    }
}