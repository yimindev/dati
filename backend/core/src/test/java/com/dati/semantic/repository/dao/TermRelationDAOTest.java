package com.dati.semantic.repository.dao;

import com.dati.semantic.domain.TermRelationType;
import com.dati.semantic.repository.mapper.TermRelationMapper;
import com.dati.semantic.repository.po.TermRelationPO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("TermRelationDAO tests")
class TermRelationDAOTest {

    @Autowired
    private TermRelationDAO termRelationDAO;

    @Test
    @DisplayName("deleteByTableIdAndFieldNameIn deletes only matching FIELD relations")
    void deleteByTableIdAndFieldNameIn_shouldDeleteOnlyMatchingFieldRelations() {
        TermRelationPO dropped = termRelationDAO.save(TermRelationMapper.toPO(
                "term-1", TermRelationType.FIELD, "table-1", "dropped"));
        TermRelationPO kept = termRelationDAO.save(TermRelationMapper.toPO(
                "term-2", TermRelationType.FIELD, "table-1", "kept"));
        TermRelationPO tableRelation = termRelationDAO.save(TermRelationMapper.toPO(
                "term-3", TermRelationType.TABLE, "table-1", null));
        TermRelationPO otherTable = termRelationDAO.save(TermRelationMapper.toPO(
                "term-4", TermRelationType.FIELD, "table-2", "dropped"));

        int deleted = termRelationDAO.deleteByTableIdAndFieldNameIn("table-1", List.of("dropped"));

        assertThat(deleted).isEqualTo(1);
        assertThat(termRelationDAO.findById(dropped.getId())).isEmpty();
        assertThat(termRelationDAO.findById(kept.getId())).isPresent();
        assertThat(termRelationDAO.findById(tableRelation.getId())).isPresent();
        assertThat(termRelationDAO.findById(otherTable.getId())).isPresent();
    }
}
