package com.dati.semantic.domain.service;

import com.dati.semantic.domain.SemanticEntityType;
import com.dati.semantic.repository.dao.TermDAO;
import com.dati.semantic.repository.dao.TermRelationDAO;
import com.dati.semantic.repository.po.SemanticSearchDocument;
import com.dati.semantic.repository.po.TermPO;
import com.dati.semantic.repository.po.TermRelationPO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TermService 单元测试")
class TermServiceTest {

    @Mock
    private TermDAO termDAO;

    @Mock
    private TermRelationDAO termRelationDAO;

    @Mock
    private SemanticIndexService semanticIndexService;

    @InjectMocks
    private TermService termService;

    @Test
    @DisplayName("createTerm - 应保存Term到DB并索引到ES")
    void createTerm_shouldSaveToDbAndIndexToES() {
        String subjectId = "subject-001";
        String name = "客户";
        String description = "客户相关信息";

        when(termDAO.save(any(TermPO.class))).thenAnswer(invocation -> invocation.getArgument(0));

        termService.createTerm(subjectId, name, description, List.of());

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
    @DisplayName("linkEntity - entityType=FIELD但fieldName为空应抛出异常")
    void linkEntity_shouldThrowWhenFieldTypeWithoutFieldName() {
        String termId = "term-001";
        String tableId = "table-001";

        TermPO term = new TermPO();
        term.setId(termId);
        term.setSubjectId("subject-001");
        term.setName("客户");

        when(termDAO.findById(termId)).thenReturn(Optional.of(term));

        assertThatThrownBy(() -> termService.linkEntity(termId, SemanticEntityType.FIELD, tableId, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fieldName is required for FIELD entity type");
    }

    @Test
    @DisplayName("unlinkEntity - fieldName为null应删除TABLE级别关联")
    void unlinkEntity_shouldDeleteTableRelation() {
        String termId = "term-001";
        String tableId = "table-001";

        termService.unlinkEntity(termId, tableId, null);

        verify(termRelationDAO).deleteByTermIdAndTableId(termId, tableId);
    }

    @Test
    @DisplayName("unlinkEntity - fieldName非空应删除FIELD级别关联")
    void unlinkEntity_shouldDeleteFieldRelation() {
        String termId = "term-001";
        String tableId = "table-001";
        String fieldName = "customer_name";

        TermRelationPO relation = new TermRelationPO();
        relation.setId("relation-001");
        relation.setTermId(termId);
        relation.setEntityType(SemanticEntityType.FIELD);
        relation.setTableId(tableId);
        relation.setFieldName(fieldName);

        when(termRelationDAO.findByTermIdAndTableIdAndFieldName(termId, tableId, fieldName))
                .thenReturn(Optional.of(relation));

        termService.unlinkEntity(termId, tableId, fieldName);

        verify(termRelationDAO).delete(relation);
    }
}