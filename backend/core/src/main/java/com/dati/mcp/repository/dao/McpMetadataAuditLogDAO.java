package com.dati.mcp.repository.dao;

import com.dati.mcp.repository.po.McpMetadataAuditLogPO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface McpMetadataAuditLogDAO extends JpaRepository<McpMetadataAuditLogPO, String> {
}
