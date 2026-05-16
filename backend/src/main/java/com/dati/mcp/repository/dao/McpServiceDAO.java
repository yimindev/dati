package com.dati.mcp.repository.dao;

import com.dati.mcp.domain.model.McpServiceStatus;
import com.dati.mcp.repository.po.McpServicePO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface McpServiceDAO extends JpaRepository<McpServicePO, String> {

    Page<McpServicePO> findAllByNameContainingOrId(String name, String id, Pageable pageable);

    Page<McpServicePO> findAllByStatus(McpServiceStatus status, Pageable pageable);

    Page<McpServicePO> findAllByNameContainingOrIdAndStatus(String name, String id, McpServiceStatus status, Pageable pageable);

}
