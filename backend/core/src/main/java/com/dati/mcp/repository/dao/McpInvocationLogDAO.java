package com.dati.mcp.repository.dao;

import com.dati.mcp.repository.po.McpInvocationLogPO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Repository
public interface McpInvocationLogDAO extends JpaRepository<McpInvocationLogPO, String>, JpaSpecificationExecutor<McpInvocationLogPO> {

    @Query("""
        SELECT l FROM McpInvocationLogPO l
        WHERE l.serviceId = :serviceId
          AND (:toolName IS NULL OR :toolName = '' OR l.toolName = :toolName)
          AND (:success IS NULL OR l.success = :success)
        ORDER BY l.createdAt DESC
    """)
    Page<McpInvocationLogPO> findLogs(@Param("serviceId") String serviceId,
                                      @Param("toolName") String toolName,
                                      @Param("success") Boolean success,
                                      Pageable pageable);

    @Modifying
    @Transactional
    @Query("DELETE FROM McpInvocationLogPO l WHERE l.createdAt < :before")
    int deleteByCreatedAtBefore(@Param("before") Instant before);
}
