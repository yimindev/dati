package com.dati.semantic.repository.dao;

import com.dati.semantic.repository.po.TermPO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TermDAO extends JpaRepository<TermPO, String> {

    Optional<TermPO> findBySubjectIdAndName(String subjectId, String name);

    Page<TermPO> findBySubjectId(String subjectId, Pageable pageable);

    List<TermPO> findBySubjectIdIn(java.util.Collection<String> subjectIds);

    @Query("""
            SELECT t FROM TermPO t
            WHERE t.subjectId = :subjectId
            AND (locate(lower(:keyword), lower(t.name)) > 0
                 OR locate(lower(:keyword), lower(cast(t.aliases as String))) > 0)
            """)
    Page<TermPO> findBySubjectIdAndKeyword(@Param("subjectId") String subjectId,
                                           @Param("keyword") String keyword,
                                           Pageable pageable);
}