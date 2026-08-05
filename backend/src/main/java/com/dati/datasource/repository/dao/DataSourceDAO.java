package com.dati.datasource.repository.dao;

import com.dati.datasource.repository.po.DataSourcePO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;

public interface DataSourceDAO extends JpaRepository<DataSourcePO, String> {

    Page<DataSourcePO> findAllByNameContainingOrId(String name, String id, Pageable pageable);

    @Query("""
            SELECT d FROM DataSourcePO d
            WHERE (d.name LIKE %:keyword% OR d.id = :keyword)
              AND (d.createdBy = :userId
                   OR EXISTS (SELECT 1 FROM ResourceAclPO a
                              WHERE a.resourceType = 'DATA_SOURCE'
                                AND a.resourceId = d.id
                                AND ((a.principalType = 'USER' AND a.principalId = :userId)
                                     OR (a.principalType = 'GROUP' AND a.principalId IN :groupIds))))
            """)
    Page<DataSourcePO> findByNameContainingOrIdAndAccessible(@Param("keyword") String keyword,
                                                             @Param("userId") String userId,
                                                             @Param("groupIds") Collection<String> groupIds,
                                                             Pageable pageable);

    @Query("""
            SELECT d FROM DataSourcePO d
            WHERE d.createdBy = :userId
               OR EXISTS (SELECT 1 FROM ResourceAclPO a
                          WHERE a.resourceType = 'DATA_SOURCE'
                            AND a.resourceId = d.id
                            AND ((a.principalType = 'USER' AND a.principalId = :userId)
                                 OR (a.principalType = 'GROUP' AND a.principalId IN :groupIds)))
            """)
    Page<DataSourcePO> findAllAccessible(@Param("userId") String userId,
                                         @Param("groupIds") Collection<String> groupIds,
                                         Pageable pageable);
}
