package com.dati.semantic.repository.dao;

import com.dati.semantic.repository.po.SubjectPO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;

@Repository
public interface SubjectDAO extends JpaRepository<SubjectPO, String> {

    @Query("SELECT s FROM SubjectPO s WHERE " +
           "s.id LIKE CONCAT(:keyword, '%') OR LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<SubjectPO> findByKeyword(@Param("keyword") String keyword, Pageable pageable);

    @Query("""
            SELECT s FROM SubjectPO s
            WHERE (s.id LIKE CONCAT(:keyword, '%') OR LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (s.createdBy = :userId
                   OR EXISTS (SELECT 1 FROM ResourceAclPO a
                              WHERE a.resourceType = 'SUBJECT'
                                AND a.resourceId = s.id
                                AND ((a.principalType = 'USER' AND a.principalId = :userId)
                                     OR (a.principalType = 'GROUP' AND a.principalId IN :groupIds))))
            """)
    Page<SubjectPO> findByKeywordAndAccessible(@Param("keyword") String keyword,
                                               @Param("userId") String userId,
                                               @Param("groupIds") Collection<String> groupIds,
                                               Pageable pageable);

    @Query("""
            SELECT s FROM SubjectPO s
            WHERE s.createdBy = :userId
               OR EXISTS (SELECT 1 FROM ResourceAclPO a
                          WHERE a.resourceType = 'SUBJECT'
                            AND a.resourceId = s.id
                            AND ((a.principalType = 'USER' AND a.principalId = :userId)
                                 OR (a.principalType = 'GROUP' AND a.principalId IN :groupIds)))
            """)
    Page<SubjectPO> findAllAccessible(@Param("userId") String userId,
                                      @Param("groupIds") Collection<String> groupIds,
                                      Pageable pageable);
}
