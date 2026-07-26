package com.dati.mcp.repository.dao;

import com.dati.mcp.domain.model.McpServiceStatus;
import com.dati.mcp.repository.po.McpServicePO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface McpServiceDAO extends JpaRepository<McpServicePO, String> {

    Page<McpServicePO> findAllByNameContainingOrId(String name, String id, Pageable pageable);

    Page<McpServicePO> findAllByStatus(McpServiceStatus status, Pageable pageable);

    @Query("SELECT m FROM McpServicePO m WHERE (m.name LIKE %:keyword% AND m.status = :status) OR (m.id = :keyword AND m.status = :status)")
    Page<McpServicePO> searchByKeywordAndStatus(@Param("keyword") String keyword, @Param("status") McpServiceStatus status, Pageable pageable);

    boolean existsByCode(String code);
}
