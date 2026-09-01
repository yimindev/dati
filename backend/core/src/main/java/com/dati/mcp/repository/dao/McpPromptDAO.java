package com.dati.mcp.repository.dao;

import com.dati.mcp.repository.po.McpPromptPO;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface McpPromptDAO extends JpaRepository<McpPromptPO, String> {
    List<McpPromptPO> findAllByServiceIdOrderByCreatedAtDesc(String serviceId);
    Optional<McpPromptPO> findByServiceIdAndId(String serviceId, String id);
    boolean existsByServiceIdAndName(String serviceId, String name);
    boolean existsByServiceIdAndNameAndIdNot(String serviceId, String name, String id);

    void deleteAllByServiceId(String serviceId);
}
