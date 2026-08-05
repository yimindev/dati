package com.dati.mcp.repository.dao;

import com.dati.mcp.domain.model.McpServiceStatus;
import com.dati.mcp.repository.po.McpServicePO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;

@Repository
public interface McpServiceDAO extends JpaRepository<McpServicePO, String> {

    Page<McpServicePO> findAllByNameContainingOrId(String name, String id, Pageable pageable);

    Page<McpServicePO> findAllByStatus(McpServiceStatus status, Pageable pageable);

    @Query("SELECT m FROM McpServicePO m WHERE (m.name LIKE %:keyword% AND m.status = :status) OR (m.id = :keyword AND m.status = :status)")
    Page<McpServicePO> searchByKeywordAndStatus(@Param("keyword") String keyword, @Param("status") McpServiceStatus status, Pageable pageable);

    boolean existsByCode(String code);

    @Query("""
            SELECT m FROM McpServicePO m
            WHERE (m.name LIKE %:keyword% OR m.id = :keyword)
              AND (m.createdBy = :userId
                   OR EXISTS (SELECT 1 FROM ResourceAclPO a
                              WHERE a.resourceType = 'MCP_SERVICE'
                                AND a.resourceId = m.id
                                AND ((a.principalType = 'USER' AND a.principalId = :userId)
                                     OR (a.principalType = 'GROUP' AND a.principalId IN :groupIds))))
            """)
    Page<McpServicePO> findAllByNameContainingOrIdAndAccessible(@Param("keyword") String keyword,
                                                                @Param("userId") String userId,
                                                                @Param("groupIds") Collection<String> groupIds,
                                                                Pageable pageable);

    @Query("""
            SELECT m FROM McpServicePO m
            WHERE m.status = :status
              AND (m.createdBy = :userId
                   OR EXISTS (SELECT 1 FROM ResourceAclPO a
                              WHERE a.resourceType = 'MCP_SERVICE'
                                AND a.resourceId = m.id
                                AND ((a.principalType = 'USER' AND a.principalId = :userId)
                                     OR (a.principalType = 'GROUP' AND a.principalId IN :groupIds))))
            """)
    Page<McpServicePO> findAllByStatusAndAccessible(@Param("status") McpServiceStatus status,
                                                    @Param("userId") String userId,
                                                    @Param("groupIds") Collection<String> groupIds,
                                                    Pageable pageable);

    @Query("""
            SELECT m FROM McpServicePO m
            WHERE ((m.name LIKE %:keyword% AND m.status = :status)
                   OR (m.id = :keyword AND m.status = :status))
              AND (m.createdBy = :userId
                   OR EXISTS (SELECT 1 FROM ResourceAclPO a
                              WHERE a.resourceType = 'MCP_SERVICE'
                                AND a.resourceId = m.id
                                AND ((a.principalType = 'USER' AND a.principalId = :userId)
                                     OR (a.principalType = 'GROUP' AND a.principalId IN :groupIds))))
            """)
    Page<McpServicePO> searchByKeywordAndStatusAndAccessible(@Param("keyword") String keyword,
                                                             @Param("status") McpServiceStatus status,
                                                             @Param("userId") String userId,
                                                             @Param("groupIds") Collection<String> groupIds,
                                                             Pageable pageable);

    @Query("""
            SELECT m FROM McpServicePO m
            WHERE m.createdBy = :userId
               OR EXISTS (SELECT 1 FROM ResourceAclPO a
                          WHERE a.resourceType = 'MCP_SERVICE'
                            AND a.resourceId = m.id
                            AND ((a.principalType = 'USER' AND a.principalId = :userId)
                                 OR (a.principalType = 'GROUP' AND a.principalId IN :groupIds)))
            """)
    Page<McpServicePO> findAllAccessible(@Param("userId") String userId,
                                         @Param("groupIds") Collection<String> groupIds,
                                         Pageable pageable);
}
