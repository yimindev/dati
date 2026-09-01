package com.dati.mcp.repository.dao;

import com.dati.mcp.repository.po.McpServiceDataScopePO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface McpServiceDataScopeDAO extends JpaRepository<McpServiceDataScopePO, String> {

    List<McpServiceDataScopePO> findAllByServiceId(String serviceId);

    void deleteAllByServiceId(String serviceId);

}
