package com.dati.semantic.repository.dao;

import com.dati.semantic.repository.po.TermPO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TermDAO extends JpaRepository<TermPO, String> {

    Optional<TermPO> findBySubjectIdAndName(String subjectId, String name);

    Page<TermPO> findBySubjectId(String subjectId, Pageable pageable);

    @Query("""
            SELECT t FROM TermPO t
            WHERE t.subjectId = :subjectId
            AND LOWER(t.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
            """)
    Page<TermPO> findBySubjectIdAndKeyword(@Param("subjectId") String subjectId,
                                           @Param("keyword") String keyword,
                                           Pageable pageable);
}