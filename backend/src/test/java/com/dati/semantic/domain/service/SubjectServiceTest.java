package com.dati.semantic.domain.service;

import com.dati.base.exception.DatiException;
import com.dati.datasource.repository.dao.TableInfoDAO;
import com.dati.datasource.repository.po.TableInfoPO;
import com.dati.semantic.domain.SemanticEntityType;
import com.dati.semantic.domain.model.Subject;
import com.dati.datasource.domain.model.TableInfo;
import com.dati.semantic.repository.dao.SubjectDAO;
import com.dati.semantic.repository.dao.SubjectTableDAO;
import com.dati.semantic.repository.po.SemanticSearchDocument;
import com.dati.semantic.repository.po.SubjectPO;
import com.dati.semantic.repository.po.SubjectTablePO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SubjectService 单元测试")
class SubjectServiceTest {

    @Mock
    private SubjectDAO subjectDAO;

    @Mock
    private SubjectTableDAO subjectTableDAO;

    @Mock
    private TableInfoDAO tableInfoDAO;

    @Mock
    private SemanticIndexService semanticIndexService;

    @InjectMocks
    private SubjectService subjectService;

    private SubjectPO sampleSubjectPO;
    private TableInfoPO sampleTableInfoPO;

    @BeforeEach
    void setUp() {
        sampleSubjectPO = new SubjectPO();
        sampleSubjectPO.setId("subject-001");
        sampleSubjectPO.setName("Test Subject");
        sampleSubjectPO.setDescription("Test Description");
        sampleSubjectPO.setDatasourceId("datasource-001");
        sampleSubjectPO.setCreatedAt(Instant.now());
        sampleSubjectPO.setUpdatedAt(Instant.now());

        sampleTableInfoPO = new TableInfoPO();
        sampleTableInfoPO.setId("table-001");
        sampleTableInfoPO.setName("test_table");
        sampleTableInfoPO.setDataSourceId("datasource-001");
    }

    @Test
    @DisplayName("createSubject - 应保存 SubjectPO 并索引 ES 文档")
    void createSubject_shouldSaveAndIndex() {
        String name = "New Subject";
        String description = "New Description";
        String datasourceId = "datasource-001";

        when(subjectDAO.save(any(SubjectPO.class))).thenAnswer(invocation -> {
            SubjectPO po = invocation.getArgument(0);
            if (po.getId() == null) {
                po.setId("generated-subject-id");
            }
            return po;
        });

        Subject result = subjectService.createSubject(name, description, datasourceId, List.of());

        assertThat(result).isNotNull();
        assertThat(result.getId()).isNotNull();
        assertThat(result.getName()).isEqualTo(name);
        assertThat(result.getDescription()).isEqualTo(description);
        assertThat(result.getDatasourceId()).isEqualTo(datasourceId);

        verify(subjectDAO).save(any(SubjectPO.class));

        ArgumentCaptor<SemanticSearchDocument> docCaptor = ArgumentCaptor.forClass(SemanticSearchDocument.class);
        verify(semanticIndexService).save(docCaptor.capture());

        SemanticSearchDocument savedDoc = docCaptor.getValue();
        assertThat(savedDoc.getType()).isEqualTo(SemanticEntityType.SUBJECT);
        assertThat(savedDoc.getKeywords()).contains(name);
        assertThat(savedDoc.getDescription()).isEqualTo(description);
        assertThat(savedDoc.getEntity().getSubjectId()).isNotNull();
    }

    @Test
    @DisplayName("updateSubject - 应更新 SubjectPO 并重新索引 ES 文档")
    void updateSubject_shouldUpdateAndReindex() {
        String id = "subject-001";
        String newName = "Updated Subject";
        String newDescription = "Updated Description";

        when(subjectDAO.findById(id)).thenReturn(Optional.of(sampleSubjectPO));
        when(subjectDAO.save(any(SubjectPO.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Subject result = subjectService.updateSubject(id, newName, newDescription, List.of());

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo(newName);
        assertThat(result.getDescription()).isEqualTo(newDescription);

        verify(subjectDAO).save(any(SubjectPO.class));

        ArgumentCaptor<SemanticSearchDocument> docCaptor = ArgumentCaptor.forClass(SemanticSearchDocument.class);
        verify(semanticIndexService).save(docCaptor.capture());

        SemanticSearchDocument savedDoc = docCaptor.getValue();
        assertThat(savedDoc.getId()).isEqualTo("subject:" + id);
        assertThat(savedDoc.getKeywords()).contains(newName);
    }

    @Test
    @DisplayName("updateSubject - Subject 不存在时应抛出异常")
    void updateSubject_shouldThrowWhenNotFound() {
        String id = "non-existent";
        when(subjectDAO.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> subjectService.updateSubject(id, "name", "desc", List.of()))
                .isInstanceOf(DatiException.class)
                .hasMessageContaining("Subject not found");
    }

    @Test
    @DisplayName("deleteSubject - 应删除 SubjectPO 和 ES 文档")
    void deleteSubject_shouldDeleteSubjectAndEsDocs() {
        String id = "subject-001";
        when(subjectDAO.existsById(id)).thenReturn(true);

        subjectService.deleteSubject(id);

        verify(subjectDAO).deleteById(id);
        verify(semanticIndexService).deleteByEntity_SubjectId(id);
    }

    @Test
    @DisplayName("deleteSubject - Subject 不存在时应抛出异常")
    void deleteSubject_shouldThrowWhenNotFound() {
        String id = "non-existent";
        when(subjectDAO.existsById(id)).thenReturn(false);

        assertThatThrownBy(() -> subjectService.deleteSubject(id))
                .isInstanceOf(DatiException.class)
                .hasMessageContaining("Subject not found");
    }

    @Test
    @DisplayName("addTableToSubject - 应验证 table 属于同一 datasource")
    void addTableToSubject_shouldValidateDatasource() {
        String subjectId = "subject-001";
        String tableId = "table-001";

        TableInfoPO otherDatasourceTable = new TableInfoPO();
        otherDatasourceTable.setId(tableId);
        otherDatasourceTable.setDataSourceId("other-datasource");

        when(subjectDAO.findById(subjectId)).thenReturn(Optional.of(sampleSubjectPO));
        when(tableInfoDAO.findById(tableId)).thenReturn(Optional.of(otherDatasourceTable));

        assertThatThrownBy(() -> subjectService.addTableToSubject(subjectId, tableId))
                .isInstanceOf(DatiException.class)
                .hasMessageContaining("does not belong to subject");

        verify(semanticIndexService, never()).save(any());
    }

    @Test
    @DisplayName("addTableToSubject - 关联已存在时应抛出异常")
    void addTableToSubject_shouldThrowWhenAlreadyAssociated() {
        String subjectId = "subject-001";
        String tableId = "table-001";

        when(subjectDAO.findById(subjectId)).thenReturn(Optional.of(sampleSubjectPO));
        when(tableInfoDAO.findById(tableId)).thenReturn(Optional.of(sampleTableInfoPO));
        when(subjectTableDAO.existsBySubjectIdAndTableId(subjectId, tableId)).thenReturn(true);

        assertThatThrownBy(() -> subjectService.addTableToSubject(subjectId, tableId))
                .isInstanceOf(DatiException.class)
                .hasMessageContaining("already associated");

        verify(semanticIndexService, never()).save(any());
    }

    @Test
    @DisplayName("addTableToSubject - 有效添加应保存关联")
    void addTableToSubject_shouldSaveAssociation() {
        String subjectId = "subject-001";
        String tableId = "table-001";

        when(subjectDAO.findById(subjectId)).thenReturn(Optional.of(sampleSubjectPO));
        when(tableInfoDAO.findById(tableId)).thenReturn(Optional.of(sampleTableInfoPO));
        when(subjectTableDAO.existsBySubjectIdAndTableId(subjectId, tableId)).thenReturn(false);
        when(subjectTableDAO.save(any(SubjectTablePO.class))).thenAnswer(invocation -> invocation.getArgument(0));

        subjectService.addTableToSubject(subjectId, tableId);

        verify(subjectTableDAO).save(any(SubjectTablePO.class));
        verify(semanticIndexService, never()).save(any());
    }

    @Test
    @DisplayName("removeTableFromSubject - 应删除关联")
    void removeTableFromSubject_shouldDeleteAssociation() {
        String subjectId = "subject-001";
        String tableId = "table-001";

        SubjectTablePO subjectTablePO = new SubjectTablePO();
        subjectTablePO.setId("st-001");
        subjectTablePO.setSubjectId(subjectId);
        subjectTablePO.setTableId(tableId);

        when(subjectTableDAO.findBySubjectIdAndTableId(subjectId, tableId))
                .thenReturn(Optional.of(subjectTablePO));

        subjectService.removeTableFromSubject(subjectId, tableId);

        verify(subjectTableDAO).deleteBySubjectIdAndTableId(subjectId, tableId);
    }

    @Test
    @DisplayName("removeTableFromSubject - 关联不存在时应抛出异常")
    void removeTableFromSubject_shouldThrowWhenNotFound() {
        String subjectId = "subject-001";
        String tableId = "table-001";

        when(subjectTableDAO.findBySubjectIdAndTableId(subjectId, tableId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> subjectService.removeTableFromSubject(subjectId, tableId))
                .isInstanceOf(DatiException.class)
                .hasMessageContaining("Association between subject");
    }

    @Test
    @DisplayName("getTablesBySubjectId - 应返回 Subject 关联的 tables")
    void getTablesBySubjectId_shouldReturnTables() {
        String subjectId = "subject-001";

        SubjectTablePO subjectTablePO = new SubjectTablePO();
        subjectTablePO.setId("st-001");
        subjectTablePO.setSubjectId(subjectId);
        subjectTablePO.setTableId("table-001");
        subjectTablePO.setCreatedAt(Instant.now());

        when(subjectDAO.existsById(subjectId)).thenReturn(true);
        when(subjectTableDAO.findBySubjectId(subjectId)).thenReturn(List.of(subjectTablePO));
        when(tableInfoDAO.findAllById(List.of("table-001"))).thenReturn(List.of(sampleTableInfoPO));

        List<TableInfo> result = subjectService.getTablesBySubjectId(subjectId);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getId()).isEqualTo("table-001");
        assertThat(result.getFirst().getName()).isEqualTo("test_table");
    }

    @Test
    @DisplayName("getSubjectsByDatasource - 应返回该 datasource 的所有 subjects")
    void getSubjectsByDatasource_shouldReturnSubjects() {
        String datasourceId = "datasource-001";
        Pageable pageable = PageRequest.of(0, 10);

        Page<SubjectPO> subjectPOPage = new org.springframework.data.domain.PageImpl<>(List.of(sampleSubjectPO), pageable, 1);
        when(subjectDAO.findByDatasourceId(datasourceId, pageable)).thenReturn(subjectPOPage);

        Page<Subject> result = subjectService.getSubjectsByDatasource(datasourceId, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().getFirst().getName()).isEqualTo("Test Subject");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("getSubjectsByDatasource - datasourceId 为 null 或空时应查所有")
    void getSubjectsByDatasource_nullOrEmpty_shouldReturnAll(String datasourceId) {
        Pageable pageable = PageRequest.of(0, 10);

        Page<SubjectPO> subjectPOPage = new org.springframework.data.domain.PageImpl<>(List.of(sampleSubjectPO), pageable, 1);
        when(subjectDAO.findAll(pageable)).thenReturn(subjectPOPage);

        Page<Subject> result = subjectService.getSubjectsByDatasource(datasourceId, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().getFirst().getName()).isEqualTo("Test Subject");
        verify(subjectDAO, never()).findByDatasourceId(any(), any());
    }
}
