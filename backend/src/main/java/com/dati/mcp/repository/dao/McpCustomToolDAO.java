package com.dati.mcp.repository.dao;

import com.dati.mcp.repository.po.McpCustomToolPO;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface McpCustomToolDAO extends JpaRepository<McpCustomToolPO, String> {

    List<McpCustomToolPO> findAllByServiceIdOrderByCreatedAtDesc(String serviceId);

    Optional<McpCustomToolPO> findByServiceIdAndId(String serviceId, String id);

    boolean existsByServiceIdAndName(String serviceId, String name);

    boolean existsByServiceIdAndNameAndIdNot(String serviceId, String name, String id);

    long countByServiceId(String serviceId);
}
