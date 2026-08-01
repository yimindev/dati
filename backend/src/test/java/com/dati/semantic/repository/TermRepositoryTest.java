package com.dati.semantic.repository;

import com.dati.semantic.repository.dao.TermDAO;
import com.dati.semantic.repository.dao.TermRelationDAO;
import com.dati.semantic.repository.po.TermPO;
import com.dati.semantic.repository.po.TermRelationPO;
import com.dati.semantic.domain.SemanticEntityType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("TermRepository tests")
class TermRepositoryTest {

    @Autowired
    private TermDAO termDAO;

    @Autowired
    private TermRelationDAO termRelationDAO;

    @Test
    @DisplayName("Save TermPO - success")
    void save_shouldPersistTermPO() {
        TermPO termPO = new TermPO();
        termPO.setSubjectId("test-subject-id");
        termPO.setName("Test Term");
        termPO.setDescription("Test Description");

        TermPO saved = termDAO.save(termPO);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getSubjectId()).isEqualTo("test-subject-id");
        assertThat(saved.getName()).isEqualTo("Test Term");
        assertThat(saved.getDescription()).isEqualTo("Test Description");
    }

    @Test
    @DisplayName("Save TermRelationPO - success")
    void save_shouldPersistTermRelationPO() {
        TermRelationPO termRelationPO = new TermRelationPO();
        termRelationPO.setTermId("test-term-id");
        termRelationPO.setEntityType(SemanticEntityType.TABLE);
        termRelationPO.setTableId("test-table-id");

        TermRelationPO saved = termRelationDAO.save(termRelationPO);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getTermId()).isEqualTo("test-term-id");
        assertThat(saved.getEntityType()).isEqualTo(SemanticEntityType.TABLE);
        assertThat(saved.getTableId()).isEqualTo("test-table-id");
    }

    @Test
    @DisplayName("Query TermRelationPO by termId - success")
    void findByTermId_shouldReturnTermRelationPO() {
        TermRelationPO termRelationPO = new TermRelationPO();
        termRelationPO.setTermId("term-456");
        termRelationPO.setEntityType(SemanticEntityType.FIELD);
        termRelationPO.setTableId("test-table-id");
        termRelationPO.setFieldName("test_field");
        termRelationDAO.save(termRelationPO);

        List<TermRelationPO> foundList = termRelationDAO.findByTermId("term-456");

        assertThat(foundList).hasSize(1);
        assertThat(foundList.getFirst().getEntityType()).isEqualTo(SemanticEntityType.FIELD);
        assertThat(foundList.getFirst().getFieldName()).isEqualTo("test_field");
    }

    @Test
    @DisplayName("Query by termId and tableId and fieldName - success")
    void findByTermIdAndTableIdAndFieldName_shouldReturnTermRelationPO() {
        TermRelationPO termRelationPO = new TermRelationPO();
        termRelationPO.setTermId("term-789");
        termRelationPO.setEntityType(SemanticEntityType.FIELD);
        termRelationPO.setTableId("table-abc");
        termRelationPO.setFieldName("field_xyz");
        termRelationDAO.save(termRelationPO);

        Optional<TermRelationPO> found = termRelationDAO.findByTermIdAndTableIdAndFieldName(
                "term-789", "table-abc", "field_xyz");

        assertThat(found).isPresent();
        assertThat(found.get().getFieldName()).isEqualTo("field_xyz");
    }

    @Test
    @DisplayName("Query by termId and tableId - success")
    void findByTermIdAndTableId_shouldReturnTermRelationPO() {
        TermRelationPO termRelationPO = new TermRelationPO();
        termRelationPO.setTermId("term-999");
        termRelationPO.setEntityType(SemanticEntityType.TABLE);
        termRelationPO.setTableId("table-def");
        termRelationDAO.save(termRelationPO);

        List<TermRelationPO> foundList = termRelationDAO.findByTermIdAndTableId("term-999", "table-def");

        assertThat(foundList).hasSize(1);
        assertThat(foundList.getFirst().getEntityType()).isEqualTo(SemanticEntityType.TABLE);
    }

    @Test
    @DisplayName("Delete TermRelationPO by termId - success")
    void deleteByTermId_shouldRemoveTermRelationPO() {
        TermRelationPO termRelationPO = new TermRelationPO();
        termRelationPO.setTermId("term-to-delete");
        termRelationPO.setEntityType(SemanticEntityType.TABLE);
        termRelationPO.setTableId("table-ghi");
        termRelationDAO.save(termRelationPO);

        termRelationDAO.deleteByTermId("term-to-delete");

        List<TermRelationPO> foundList = termRelationDAO.findByTermId("term-to-delete");
        assertThat(foundList).isEmpty();
    }

    @Test
    @DisplayName("Delete TermRelationPO by termId and tableId - success")
    void deleteByTermIdAndTableId_shouldRemoveTermRelationPO() {
        TermRelationPO termRelationPO = new TermRelationPO();
        termRelationPO.setTermId("term-to-delete-2");
        termRelationPO.setEntityType(SemanticEntityType.TABLE);
        termRelationPO.setTableId("table-jkl");
        termRelationDAO.save(termRelationPO);

        termRelationDAO.deleteByTermIdAndTableId("term-to-delete-2", "table-jkl");

        List<TermRelationPO> foundList = termRelationDAO.findByTermIdAndTableId("term-to-delete-2", "table-jkl");
        assertThat(foundList).isEmpty();
    }
}