package com.dati.semantic.domain.service;

import com.dati.semantic.domain.SemanticEntityType;
import com.dati.semantic.repository.dao.SemanticSearchDAO;
import com.dati.semantic.repository.po.EntityReference;
import com.dati.semantic.repository.po.SemanticSearchDocument;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SemanticIndexService unit tests")
class SemanticIndexServiceTest {

    @Mock
    private SemanticSearchDAO semanticSearchDAO;

    @InjectMocks
    private SemanticIndexService semanticIndexService;

    @Captor
    ArgumentCaptor<List<SemanticSearchDocument>> captor;

    @Test
    @DisplayName("Save document - sets createdTime and updatedTime on first save")
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
    @DisplayName("Save document - keeps createdTime, only updates updatedTime on update")
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
    @DisplayName("Batch save documents - sets timestamps on all documents")
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
    @DisplayName("Delete documents by tableId - calls DAO batch delete method")
    void deleteByEntityTableId_shouldCallDaoDeleteMethod() {
        // given
        String tableId = "table-001";

        // when
        semanticIndexService.deleteByEntityTableId(tableId);

        // then
        verify(semanticSearchDAO).deleteByEntity_TableIdIn(List.of(tableId));
    }

    @Test
    @DisplayName("Batch delete documents by tableIds - calls DAO batch delete method")
    void deleteByEntityTableIds_shouldCallDaoBatchDeleteMethod() {
        // given
        List<String> tableIds = List.of("table-001", "table-002");

        // when
        semanticIndexService.deleteByEntityTableIds(tableIds);

        // then
        verify(semanticSearchDAO).deleteByEntity_TableIdIn(tableIds);
    }

    @Test
    @DisplayName("Delete documents by subjectId - calls DAO delete method")
    void deleteByEntity_SubjectId_shouldCallDaoDeleteMethod() {
        // given
        String subjectId = "subject-001";

        // when
        semanticIndexService.deleteByEntity_SubjectId(subjectId);

        // then
        verify(semanticSearchDAO).deleteByEntity_SubjectId(subjectId);
    }

    @Test
    @DisplayName("Delete documents by ID - calls DAO delete method")
    void deleteById_shouldCallDaoDeleteMethod() {
        // given
        String id = "term:123";

        // when
        semanticIndexService.deleteById(id);

        // then
        verify(semanticSearchDAO).deleteById(id);
    }

    @Test
    @DisplayName("Paged query documents - null keyword calls no-keyword query")
    void findByTableFieldAndTypePaginated_withNullKeyword_shouldCallQueryWithoutKeyword() {
        // given
        String tableId = "table1";
        String field = "status";
        Pageable pageable = PageRequest.of(0, 10);
        SemanticSearchDocument doc = SemanticSearchDocument.builder()
                .id("doc1")
                .keywords(List.of("active"))
                .type(SemanticEntityType.FIELD_VALUE)
                .build();
        when(semanticSearchDAO.findByEntity_TableIdAndEntity_FieldAndType(tableId, field, SemanticEntityType.FIELD_VALUE))
                .thenReturn(List.of(doc));

        // when
        Page<SemanticSearchDocument> result = semanticIndexService.findByTableFieldAndTypePaginated(
                tableId, field, SemanticEntityType.FIELD_VALUE, null, pageable);

        // then
        verify(semanticSearchDAO).findByEntity_TableIdAndEntity_FieldAndType(tableId, field, SemanticEntityType.FIELD_VALUE);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getKeywords()).containsExactly("active");
    }

    @Test
    @DisplayName("Paged query documents - empty string keyword calls no-keyword query")
    void findByTableFieldAndTypePaginated_withEmptyKeyword_shouldCallQueryWithoutKeyword() {
        // given
        String tableId = "table1";
        String field = "status";
        Pageable pageable = PageRequest.of(0, 10);
        SemanticSearchDocument doc = SemanticSearchDocument.builder()
                .id("doc1")
                .keywords(List.of("active"))
                .type(SemanticEntityType.FIELD_VALUE)
                .build();
        when(semanticSearchDAO.findByEntity_TableIdAndEntity_FieldAndType(tableId, field, SemanticEntityType.FIELD_VALUE))
                .thenReturn(List.of(doc));

        // when
        Page<SemanticSearchDocument> result = semanticIndexService.findByTableFieldAndTypePaginated(
                tableId, field, SemanticEntityType.FIELD_VALUE, "", pageable);

        // then
        verify(semanticSearchDAO).findByEntity_TableIdAndEntity_FieldAndType(tableId, field, SemanticEntityType.FIELD_VALUE);
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("Paged query documents - non-empty keyword calls keyword query")
    void findByTableFieldAndTypePaginated_withKeyword_shouldCallQueryWithKeyword() {
        // given
        String tableId = "table1";
        String field = "status";
        String keyword = "active";
        Pageable pageable = PageRequest.of(0, 10);
        SemanticSearchDocument doc = SemanticSearchDocument.builder()
                .id("doc1")
                .keywords(List.of("active"))
                .type(SemanticEntityType.FIELD_VALUE)
                .build();
        when(semanticSearchDAO.searchByTableFieldAndKeyword(
                tableId, field, keyword, SemanticEntityType.FIELD_VALUE.name(), pageable))
                .thenReturn(new PageImpl<>(List.of(doc), pageable, 1));

        // when
        Page<SemanticSearchDocument> result = semanticIndexService.findByTableFieldAndTypePaginated(
                tableId, field, SemanticEntityType.FIELD_VALUE, keyword, pageable);

        // then
        verify(semanticSearchDAO).searchByTableFieldAndKeyword(tableId, field, keyword, SemanticEntityType.FIELD_VALUE.name(), pageable);
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("Delete by table id and type - delegates to DAO")
    void deleteByTableIdAndType_shouldDelegateToDAO() {
        // when
        semanticIndexService.deleteByTableIdAndType("table-001", SemanticEntityType.FIELD);

        // then
        verify(semanticSearchDAO).deleteByEntity_TableIdAndType("table-001", SemanticEntityType.FIELD);
    }
}
