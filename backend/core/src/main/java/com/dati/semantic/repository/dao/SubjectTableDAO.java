package com.dati.semantic.repository.dao;

import com.dati.datasource.repository.po.TableInfoPO;
import com.dati.semantic.repository.po.SubjectTablePO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubjectTableDAO extends JpaRepository<SubjectTablePO, String> {
    List<SubjectTablePO> findBySubjectId(String subjectId);

    Optional<SubjectTablePO> findBySubjectIdAndTableId(String subjectId, String tableId);

    void deleteBySubjectIdAndTableId(String subjectId, String tableId);

    boolean existsBySubjectIdAndTableId(String subjectId, String tableId);

    @Query("""
            SELECT ti FROM TableInfoPO ti
            WHERE ti.id IN (
                SELECT st.tableId FROM SubjectTablePO st WHERE st.subjectId = :subjectId
            )
            """)
    Page<TableInfoPO> findTablesBySubjectId(@Param("subjectId") String subjectId, Pageable pageable);

    @Query("""
            SELECT ti FROM TableInfoPO ti
            WHERE ti.id IN (
                SELECT st.tableId FROM SubjectTablePO st WHERE st.subjectId = :subjectId
            )
            AND LOWER(ti.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
            """)
    Page<TableInfoPO> findTablesBySubjectIdAndNameContaining(
            @Param("subjectId") String subjectId,
            @Param("keyword") @Nullable String keyword,
            Pageable pageable);
}
