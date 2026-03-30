package com.dati.semantic.repository.dao;

import com.dati.semantic.repository.po.SubjectTablePO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface SubjectTableDAO extends JpaRepository<SubjectTablePO, String> {
    List<SubjectTablePO> findBySubjectId(String subjectId);
    
    Optional<SubjectTablePO> findBySubjectIdAndTableId(String subjectId, String tableId);
    
    void deleteBySubjectIdAndTableId(String subjectId, String tableId);
    
    boolean existsBySubjectIdAndTableId(String subjectId, String tableId);
}
