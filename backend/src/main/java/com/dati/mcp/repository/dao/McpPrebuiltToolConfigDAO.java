package com.dati.mcp.repository.dao;

import com.dati.mcp.domain.model.McpToolType;
import com.dati.mcp.repository.po.McpPrebuiltToolConfigPO;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface McpPrebuiltToolConfigDAO extends JpaRepository<McpPrebuiltToolConfigPO, String> {

    List<McpPrebuiltToolConfigPO> findAllByServiceId(String serviceId);

    Optional<McpPrebuiltToolConfigPO> findByServiceIdAndToolType(String serviceId, McpToolType toolType);
}
