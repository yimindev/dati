package com.dati.semantic.domain.service;

import com.dati.base.exception.DatiException;
import com.dati.base.exception.ErrorCode;
import com.dati.datasource.repository.dao.ColumnInfoDAO;
import com.dati.datasource.repository.dao.TableInfoDAO;
import com.dati.datasource.repository.po.TableInfoPO;
import com.dati.semantic.domain.SemanticEntityType;
import com.dati.semantic.domain.TermRelationType;
import com.dati.semantic.domain.model.Term;
import com.dati.semantic.domain.model.TermRelation;
import com.dati.semantic.repository.dao.SubjectDAO;
import com.dati.semantic.repository.dao.SubjectTableDAO;
import com.dati.semantic.repository.dao.TermDAO;
import com.dati.semantic.repository.dao.TermRelationDAO;
import com.dati.semantic.repository.po.SemanticSearchDocument;
import com.dati.semantic.repository.po.TermPO;
import com.dati.semantic.repository.po.TermRelationPO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TermService unit tests")
class TermServiceTest {

    @Mock
    private TermDAO termDAO;

    @Mock
    private TermRelationDAO termRelationDAO;

    @Mock
    private SemanticIndexService semanticIndexService;

    @Mock
    private SubjectDAO subjectDAO;

    @Mock
    private SubjectTableDAO subjectTableDAO;

    @Mock
    private TableInfoDAO tableInfoDAO;

    @Mock
    private ColumnInfoDAO columnInfoDAO;

    @Mock
    private com.dati.permission.domain.service.PermissionService permissionService;

    @InjectMocks
    private TermService termService;

    @BeforeEach
    void stubExistingTables() {
        // linkEntity validates table existence before subject membership; default to an existing table
        lenient().when(tableInfoDAO.findById(anyString())).thenReturn(Optional.of(new TableInfoPO()));
    }

    @Test
    @DisplayName("createTerm - saves Term to DB and indexes to ES")
    void createTerm_shouldSaveToDbAndIndexToES() {
        String subjectId = "subject-001";
        String name = "客户";
        String description = "客户相关信息";

        when(termDAO.save(any(TermPO.class))).thenAnswer(invocation -> invocation.getArgument(0));

        com.dati.semantic.domain.model.Term model = new com.dati.semantic.domain.model.Term();
        model.setSubjectId(subjectId);
        model.setName(name);
        model.setDescription(description);
        termService.createTerm(model);

        ArgumentCaptor<TermPO> termCaptor = ArgumentCaptor.forClass(TermPO.class);
        verify(termDAO).save(termCaptor.capture());
        TermPO savedTerm = termCaptor.getValue();
        assertThat(savedTerm.getSubjectId()).isEqualTo(subjectId);
        assertThat(savedTerm.getName()).isEqualTo(name);
        assertThat(savedTerm.getDescription()).isEqualTo(description);

        ArgumentCaptor<SemanticSearchDocument> docCaptor = ArgumentCaptor.forClass(SemanticSearchDocument.class);
        verify(semanticIndexService).save(docCaptor.capture());
        SemanticSearchDocument savedDoc = docCaptor.getValue();
        assertThat(savedDoc.getId()).isEqualTo("term:" + savedTerm.getId());
        assertThat(savedDoc.getType()).isEqualTo(SemanticEntityType.TERM);
        assertThat(savedDoc.getKeywords()).contains(name);
        assertThat(savedDoc.getDescription()).isEqualTo(description);
        assertThat(savedDoc.getEntity().getSubjectId()).isEqualTo(subjectId);
    }

    @Test
    @DisplayName("linkEntity - throws when entityType=FIELD but fieldName is empty")
    void linkEntity_shouldThrowWhenFieldTypeWithoutFieldName() {
        String termId = "term-001";
        String tableId = "table-001";

        TermPO term = new TermPO();
        term.setId(termId);
        term.setSubjectId("subject-001");
        term.setName("客户");

        when(termDAO.findById(termId)).thenReturn(Optional.of(term));

        assertThatThrownBy(() -> termService.linkEntity(termId, TermRelationType.FIELD, tableId, null))
                .isInstanceOf(DatiException.class);
    }

    @Test
    @DisplayName("linkEntity - unknown table throws DS_TABLE_NOT_FOUND")
    void linkEntity_tableNotFound_throwsTableNotFound() {
        String termId = "term-001";
        String tableId = "table-missing";

        TermPO term = new TermPO();
        term.setId(termId);
        term.setSubjectId("subject-001");
        term.setName("客户");

        when(termDAO.findById(termId)).thenReturn(Optional.of(term));
        when(tableInfoDAO.findById(tableId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> termService.linkEntity(termId, TermRelationType.TABLE, tableId, null))
                .isInstanceOf(DatiException.class)
                .satisfies(e -> assertThat(((DatiException) e).getCode())
                        .isEqualTo(ErrorCode.DS_TABLE_NOT_FOUND));
        verify(subjectTableDAO, never()).existsBySubjectIdAndTableId(any(), any());
        verify(termRelationDAO, never()).save(any());
    }

    @Test
    @DisplayName("linkEntity - throws when duplicate relation exists")
    void linkEntity_duplicateRelation_throwsInvalidParameter() {
        String termId = "term-001";
        String tableId = "table-001";
        String fieldName = "customer_name";

        TermPO term = new TermPO();
        term.setId(termId);
        term.setSubjectId("subject-001");
        term.setName("客户");

        when(termDAO.findById(termId)).thenReturn(Optional.of(term));
        when(subjectTableDAO.existsBySubjectIdAndTableId("subject-001", tableId)).thenReturn(true);
        when(columnInfoDAO.existsByTableIdAndName(tableId, fieldName)).thenReturn(true);
        when(termRelationDAO.findByTermIdAndTableIdAndFieldName(termId, tableId, fieldName))
                .thenReturn(Optional.of(new TermRelationPO()));

        assertThatThrownBy(() -> termService.linkEntity(termId, TermRelationType.FIELD, tableId, fieldName))
                .isInstanceOf(DatiException.class)
                .satisfies(e -> assertThat(((DatiException) e).getCode()).isEqualTo(ErrorCode.INVALID_PARAMETER));
    }

    @Test
    @DisplayName("linkEntity - FIELD relation rejects a column that does not exist in the table")
    void linkEntity_fieldColumnNotFound_throwsInvalidRelation() {
        String termId = "term-001";
        String tableId = "table-001";
        String fieldName = "ghost_col";

        TermPO term = new TermPO();
        term.setId(termId);
        term.setSubjectId("subject-001");
        term.setName("客户");

        when(termDAO.findById(termId)).thenReturn(Optional.of(term));
        when(subjectTableDAO.existsBySubjectIdAndTableId("subject-001", tableId)).thenReturn(true);
        when(columnInfoDAO.existsByTableIdAndName(tableId, fieldName)).thenReturn(false);

        assertThatThrownBy(() -> termService.linkEntity(termId, TermRelationType.FIELD, tableId, fieldName))
                .isInstanceOf(DatiException.class)
                .satisfies(e -> assertThat(((DatiException) e).getCode())
                        .isEqualTo(ErrorCode.SM_TERM_RELATION_INVALID));
        verify(termRelationDAO, never()).save(any());
    }

    @Test
    @DisplayName("linkEntity - FIELD relation saves when the column exists")
    void linkEntity_fieldColumnExists_savesRelation() {
        String termId = "term-001";
        String tableId = "table-001";
        String fieldName = "customer_name";

        TermPO term = new TermPO();
        term.setId(termId);
        term.setSubjectId("subject-001");
        term.setName("客户");

        when(termDAO.findById(termId)).thenReturn(Optional.of(term));
        when(subjectTableDAO.existsBySubjectIdAndTableId("subject-001", tableId)).thenReturn(true);
        when(columnInfoDAO.existsByTableIdAndName(tableId, fieldName)).thenReturn(true);
        when(termRelationDAO.findByTermIdAndTableIdAndFieldName(termId, tableId, fieldName))
                .thenReturn(Optional.empty());

        TermRelation relation = termService.linkEntity(termId, TermRelationType.FIELD, tableId, fieldName);

        ArgumentCaptor<TermRelationPO> captor = ArgumentCaptor.forClass(TermRelationPO.class);
        verify(termRelationDAO).save(captor.capture());
        assertThat(captor.getValue().getEntityType()).isEqualTo(TermRelationType.FIELD);
        assertThat(captor.getValue().getFieldName()).isEqualTo(fieldName);
        assertThat(relation.getTermId()).isEqualTo(termId);
        assertThat(relation.getTableId()).isEqualTo(tableId);
        assertThat(relation.getFieldName()).isEqualTo(fieldName);
    }

    @Test
    @DisplayName("linkEntity - TABLE relation does not require a column")
    void linkEntity_tableRelation_skipsColumnValidation() {
        String termId = "term-001";
        String tableId = "table-001";

        TermPO term = new TermPO();
        term.setId(termId);
        term.setSubjectId("subject-001");
        term.setName("客户");

        when(termDAO.findById(termId)).thenReturn(Optional.of(term));
        when(subjectTableDAO.existsBySubjectIdAndTableId("subject-001", tableId)).thenReturn(true);
        when(termRelationDAO.findByTermIdAndTableIdAndFieldNameIsNull(termId, tableId))
                .thenReturn(Optional.empty());

        termService.linkEntity(termId, TermRelationType.TABLE, tableId, null);

        verify(columnInfoDAO, never()).existsByTableIdAndName(any(), any());
        verify(termRelationDAO).save(any(TermRelationPO.class));
    }

    @Test
    @DisplayName("linkEntity - duplicate TABLE-level relation throws INVALID_PARAMETER")
    void linkEntity_duplicateTableRelation_throwsInvalidParameter() {
        String termId = "term-001";
        String tableId = "table-001";

        TermPO term = new TermPO();
        term.setId(termId);
        term.setSubjectId("subject-001");
        term.setName("客户");

        when(termDAO.findById(termId)).thenReturn(Optional.of(term));
        when(subjectTableDAO.existsBySubjectIdAndTableId("subject-001", tableId)).thenReturn(true);
        when(termRelationDAO.findByTermIdAndTableIdAndFieldNameIsNull(termId, tableId))
                .thenReturn(Optional.of(new TermRelationPO()));

        assertThatThrownBy(() -> termService.linkEntity(termId, TermRelationType.TABLE, tableId, null))
                .isInstanceOf(DatiException.class)
                .satisfies(e -> assertThat(((DatiException) e).getCode()).isEqualTo(ErrorCode.INVALID_PARAMETER));
        verify(termRelationDAO, never()).save(any());
    }

    @Test
    @DisplayName("linkEntity - TABLE relation rejects fieldName")
    void linkEntity_tableRelationWithFieldName_throwsInvalidRelation() {
        String termId = "term-001";
        String tableId = "table-001";

        TermPO term = new TermPO();
        term.setId(termId);
        term.setSubjectId("subject-001");
        term.setName("客户");

        when(termDAO.findById(termId)).thenReturn(Optional.of(term));

        assertThatThrownBy(() -> termService.linkEntity(
                termId, TermRelationType.TABLE, tableId, "customer_name"))
                .isInstanceOf(DatiException.class)
                .satisfies(e -> assertThat(((DatiException) e).getCode())
                        .isEqualTo(ErrorCode.SM_TERM_RELATION_INVALID));
        verify(termRelationDAO, never()).save(any());
    }

    @Test
    @DisplayName("linkEntity - TABLE and FIELD relations on the same table can coexist")
    void linkEntity_tableAndFieldRelations_coexist() {
        String termId = "term-001";
        String tableId = "table-001";

        TermPO term = new TermPO();
        term.setId(termId);
        term.setSubjectId("subject-001");
        term.setName("客户");

        when(termDAO.findById(termId)).thenReturn(Optional.of(term));
        when(subjectTableDAO.existsBySubjectIdAndTableId("subject-001", tableId)).thenReturn(true);
        when(termRelationDAO.findByTermIdAndTableIdAndFieldNameIsNull(termId, tableId))
                .thenReturn(Optional.empty());

        termService.linkEntity(termId, TermRelationType.TABLE, tableId, null);

        ArgumentCaptor<TermRelationPO> captor = ArgumentCaptor.forClass(TermRelationPO.class);
        verify(termRelationDAO).save(captor.capture());
        assertThat(captor.getValue().getEntityType()).isEqualTo(TermRelationType.TABLE);
        assertThat(captor.getValue().getFieldName()).isNull();
    }

    @Test
    @DisplayName("unlinkRelation - deletes the addressed relation by id")
    void unlinkRelation_shouldDeleteById() {
        String termId = "term-001";
        TermPO termPO = new TermPO();
        termPO.setId(termId);
        termPO.setSubjectId("subject-001");

        TermRelationPO relation = new TermRelationPO();
        relation.setId("relation-001");
        relation.setTermId(termId);
        relation.setEntityType(TermRelationType.TABLE);
        relation.setTableId("table-001");

        when(termDAO.findById(termId)).thenReturn(Optional.of(termPO));
        when(termRelationDAO.findById("relation-001")).thenReturn(Optional.of(relation));

        termService.unlinkRelation(termId, "relation-001");

        verify(termRelationDAO).delete(relation);
    }

    @Test
    @DisplayName("unlinkRelation - relation belonging to another term throws NOT_FOUND")
    void unlinkRelation_otherTermRelation_throwsNotFound() {
        String termId = "term-001";
        TermPO termPO = new TermPO();
        termPO.setId(termId);
        termPO.setSubjectId("subject-001");

        TermRelationPO relation = new TermRelationPO();
        relation.setId("relation-001");
        relation.setTermId("term-002");

        when(termDAO.findById(termId)).thenReturn(Optional.of(termPO));
        when(termRelationDAO.findById("relation-001")).thenReturn(Optional.of(relation));

        assertThatThrownBy(() -> termService.unlinkRelation(termId, "relation-001"))
                .isInstanceOf(DatiException.class)
                .satisfies(e -> assertThat(((DatiException) e).getCode()).isEqualTo(ErrorCode.NOT_FOUND));
        verify(termRelationDAO, never()).delete(any());
    }

    @Test
    @DisplayName("unlinkRelation - nonexistent relation throws NOT_FOUND")
    void unlinkRelation_nonexistent_throwsNotFound() {
        String termId = "term-001";
        TermPO termPO = new TermPO();
        termPO.setId(termId);
        termPO.setSubjectId("subject-001");

        when(termDAO.findById(termId)).thenReturn(Optional.of(termPO));
        when(termRelationDAO.findById("relation-001")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> termService.unlinkRelation(termId, "relation-001"))
                .isInstanceOf(DatiException.class)
                .satisfies(e -> assertThat(((DatiException) e).getCode()).isEqualTo(ErrorCode.NOT_FOUND));
        verify(termRelationDAO, never()).delete(any());
    }

    @Test
    @DisplayName("getTermRelationsByTermIds - batch loads relations grouped by term with table info")
    void getTermRelationsByTermIds_shouldGroupByTermWithTableInfo() {
        TermRelationPO rel1 = new TermRelationPO();
        rel1.setId("rel-001");
        rel1.setTermId("term-001");
        rel1.setEntityType(TermRelationType.FIELD);
        rel1.setTableId("table-001");
        rel1.setFieldName("amount");

        TableInfoPO tableInfo = new TableInfoPO();
        tableInfo.setId("table-001");
        tableInfo.setName("orders");
        tableInfo.setSchema("sales");

        when(termRelationDAO.findByTermIdIn(Set.of("term-001", "term-002")))
                .thenReturn(List.of(rel1));
        when(tableInfoDAO.findAllById(Set.of("table-001"))).thenReturn(List.of(tableInfo));

        Map<String, List<TermRelation>> result =
                termService.getTermRelationsByTermIds(Set.of("term-001", "term-002"));

        assertThat(result).containsOnlyKeys("term-001");
        assertThat(result.get("term-001")).hasSize(1);
        TermRelation rel = result.get("term-001").getFirst();
        assertThat(rel.getTableName()).isEqualTo("orders");
        assertThat(rel.getSchema()).isEqualTo("sales");
        assertThat(rel.getFieldName()).isEqualTo("amount");
    }

    @Test
    @DisplayName("getTermRelationsByTermIds - empty input returns empty map")
    void getTermRelationsByTermIds_emptyInput_shouldReturnEmptyMap() {
        Map<String, List<TermRelation>> result = termService.getTermRelationsByTermIds(Set.of());

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getTermsBySubject - non-empty keyword matches by ID prefix or name")
    void getTermsBySubject_withKeyword_shouldReturnMatchingTerms() {
        String subjectId = "subject-001";
        String keyword = "客户";
        Pageable pageable = PageRequest.of(0, 10);

        TermPO termPO = new TermPO();
        termPO.setId("term-001");
        termPO.setSubjectId(subjectId);
        termPO.setName("客户分析");
        termPO.setDescription("客户相关数据");
        termPO.setCreatedAt(java.time.Instant.now());
        termPO.setUpdatedAt(java.time.Instant.now());

        Page<TermPO> poPage = new PageImpl<>(List.of(termPO), pageable, 1);
        when(termDAO.findBySubjectIdAndKeyword(subjectId, keyword, pageable)).thenReturn(poPage);
        when(termRelationDAO.findByTermIdIn(any())).thenReturn(List.of());

        Page<Term> result = termService.getTermsBySubject(subjectId, keyword, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().getFirst().getName()).isEqualTo("客户分析");
        assertThat(result.getContent().getFirst().getRelations()).isNotNull();
    }

    @Test
    @DisplayName("getTermsBySubject - nonexistent subject throws SM_SUBJECT_NOT_FOUND")
    void getTermsBySubject_nonexistentSubject_throwsSM001() {
        String subjectId = "bad-subject-id";
        Pageable pageable = PageRequest.of(0, 10);
        org.mockito.Mockito.doThrow(new DatiException(ErrorCode.SM_SUBJECT_NOT_FOUND))
                .when(permissionService).requireSubject(subjectId, com.dati.permission.domain.model.Permission.VIEW);

        assertThatThrownBy(() -> termService.getTermsBySubject(subjectId, null, pageable))
                .isInstanceOf(DatiException.class)
                .satisfies(e -> assertThat(((DatiException) e).getCode()).isEqualTo(ErrorCode.SM_SUBJECT_NOT_FOUND));
    }

    @Test
    @DisplayName("getTermsBySubject - empty keyword queries all")
    void getTermsBySubject_withNullKeyword_shouldReturnAll() {
        String subjectId = "subject-001";
        Pageable pageable = PageRequest.of(0, 10);

        TermPO termPO1 = new TermPO();
        termPO1.setId("term-001");
        termPO1.setSubjectId(subjectId);
        termPO1.setName("客户");
        termPO1.setCreatedAt(java.time.Instant.now());
        termPO1.setUpdatedAt(java.time.Instant.now());

        TermPO termPO2 = new TermPO();
        termPO2.setId("term-002");
        termPO2.setSubjectId(subjectId);
        termPO2.setName("营收");
        termPO2.setCreatedAt(java.time.Instant.now());
        termPO2.setUpdatedAt(java.time.Instant.now());

        Page<TermPO> poPage = new PageImpl<>(List.of(termPO1, termPO2), pageable, 2);
        when(termDAO.findBySubjectId(eq(subjectId), eq(pageable))).thenReturn(poPage);
        when(termRelationDAO.findByTermIdIn(any())).thenReturn(List.of());

        Page<Term> result = termService.getTermsBySubject(subjectId, null, pageable);

        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("getTermsWithSubject - batch query terms across subjects")
    void getTermsWithSubject_multipleSubjects() {
        TermPO t1 = new TermPO();
        t1.setId("t1"); t1.setSubjectId("s1"); t1.setName("活跃用户");
        t1.setDescription("30天内登录过的用户");
        TermPO t2 = new TermPO();
        t2.setId("t2"); t2.setSubjectId("s2"); t2.setName("高价值客户");
        t2.setDescription("累计消费超1000元");
        when(termDAO.findAllById(java.util.Set.of("t1", "t2"))).thenReturn(java.util.List.of(t1, t2));

        com.dati.semantic.repository.po.SubjectPO s1 = new com.dati.semantic.repository.po.SubjectPO();
        s1.setId("s1"); s1.setName("用户分析");
        com.dati.semantic.repository.po.SubjectPO s2 = new com.dati.semantic.repository.po.SubjectPO();
        s2.setId("s2"); s2.setName("客户价值");
        when(subjectDAO.findAllById(java.util.Set.of("s1", "s2"))).thenReturn(java.util.List.of(s1, s2));

        java.util.List<TermService.TermInfo> result = termService.getTermsWithSubject(java.util.Set.of("t1", "t2"));

        assertThat(result).hasSize(2);
        assertThat(result.get(0).name()).isEqualTo("活跃用户");
        assertThat(result.get(0).subjectName()).isEqualTo("用户分析");
        assertThat(result.get(1).name()).isEqualTo("高价值客户");
        assertThat(result.get(1).subjectName()).isEqualTo("客户价值");
    }

    @Test
    @DisplayName("getTermsBySubjectIds - empty subjectIds returns empty list")
    void getTermsBySubjectIds_emptyInput() {
        assertThat(termService.getTermsBySubjectIds(List.of())).isEmpty();
        assertThat(termService.getTermsBySubjectIds(null)).isEmpty();
    }

    @Test
    @DisplayName("getTermsBySubjectIds - resolves terms and subject names")
    void getTermsBySubjectIds_resolvesTermsAndSubjectNames() {
        TermPO t1 = new TermPO();
        t1.setId("t1"); t1.setSubjectId("s1"); t1.setName("活跃用户");
        t1.setDescription("30天内登录过的用户");
        when(termDAO.findBySubjectIdIn(List.of("s1"))).thenReturn(List.of(t1));

        com.dati.semantic.repository.po.SubjectPO s1 = new com.dati.semantic.repository.po.SubjectPO();
        s1.setId("s1"); s1.setName("用户分析");
        when(subjectDAO.findAllById(List.of("s1"))).thenReturn(List.of(s1));

        List<TermService.TermInfo> result = termService.getTermsBySubjectIds(List.of("s1"));

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().name()).isEqualTo("活跃用户");
        assertThat(result.getFirst().description()).isEqualTo("30天内登录过的用户");
        assertThat(result.getFirst().subjectName()).isEqualTo("用户分析");
    }
}