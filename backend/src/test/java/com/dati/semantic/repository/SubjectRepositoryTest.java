package com.dati.semantic.repository;

import com.dati.semantic.repository.dao.SubjectDAO;
import com.dati.semantic.repository.dao.SubjectTableDAO;
import com.dati.semantic.repository.po.SubjectPO;
import com.dati.semantic.repository.po.SubjectTablePO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class SubjectRepositoryTest {

    @Autowired
    private SubjectDAO subjectDAO;

    @Autowired
    private SubjectTableDAO subjectTableDAO;

    @Test
    void shouldSaveAndFindSubject() {
        SubjectPO subject = new SubjectPO();
        subject.setName("Test Subject");
        subject.setDescription("Test Description");
        subject.setDatasourceId("ds-001");
        
        SubjectPO saved = subjectDAO.save(subject);
        
        assertNotNull(saved.getId());
        assertEquals("Test Subject", saved.getName());
        assertEquals("ds-001", saved.getDatasourceId());
    }

    @Test
    void shouldFindByDatasourceId() {
        SubjectPO subject = new SubjectPO();
        subject.setName("DS Subject");
        subject.setDatasourceId("ds-002");
        subjectDAO.save(subject);
        
        Page<SubjectPO> results = subjectDAO.findByDatasourceId("ds-002", PageRequest.of(0, 10));
        
        assertEquals(1, results.getTotalElements());
        assertEquals("DS Subject", results.getContent().getFirst().getName());
    }

    @Test
    void shouldSaveAndFindSubjectTable() {
        String subjectId = UUID.randomUUID().toString();
        String tableId = UUID.randomUUID().toString();
        
        SubjectTablePO subjectTable = new SubjectTablePO();
        subjectTable.setSubjectId(subjectId);
        subjectTable.setTableId(tableId);
        
        subjectTableDAO.save(subjectTable);
        
        Optional<SubjectTablePO> found = subjectTableDAO.findBySubjectIdAndTableId(subjectId, tableId);
        assertTrue(found.isPresent());
        assertEquals(subjectId, found.get().getSubjectId());
    }

    @Test
    void shouldFindBySubjectId() {
        String subjectId = UUID.randomUUID().toString();
        
        SubjectTablePO st1 = new SubjectTablePO();
        st1.setSubjectId(subjectId);
        st1.setTableId(UUID.randomUUID().toString());
        
        SubjectTablePO st2 = new SubjectTablePO();
        st2.setSubjectId(subjectId);
        st2.setTableId(UUID.randomUUID().toString());
        
        subjectTableDAO.saveAll(List.of(st1, st2));
        
        List<SubjectTablePO> results = subjectTableDAO.findBySubjectId(subjectId);
        
        assertEquals(2, results.size());
    }

    @Test
    void shouldDeleteBySubjectIdAndTableId() {
        String subjectId = UUID.randomUUID().toString();
        String tableId = UUID.randomUUID().toString();
        
        SubjectTablePO subjectTable = new SubjectTablePO();
        subjectTable.setSubjectId(subjectId);
        subjectTable.setTableId(tableId);
        subjectTableDAO.save(subjectTable);
        
        subjectTableDAO.deleteBySubjectIdAndTableId(subjectId, tableId);
        
        assertFalse(subjectTableDAO.existsBySubjectIdAndTableId(subjectId, tableId));
    }

    @Test
    void shouldCheckExistsBySubjectIdAndTableId() {
        String subjectId = UUID.randomUUID().toString();
        String tableId = UUID.randomUUID().toString();
        
        assertFalse(subjectTableDAO.existsBySubjectIdAndTableId(subjectId, tableId));
        
        SubjectTablePO subjectTable = new SubjectTablePO();
        subjectTable.setSubjectId(subjectId);
        subjectTable.setTableId(tableId);
        subjectTableDAO.save(subjectTable);
        
        assertTrue(subjectTableDAO.existsBySubjectIdAndTableId(subjectId, tableId));
    }
}