package com.dati.mcp.domain.service;

import com.dati.mcp.domain.model.McpServiceDataScope;
import com.dati.mcp.repository.dao.McpServiceDataScopeDAO;
import com.dati.mcp.repository.mapper.McpServiceDataScopeMapper;
import com.dati.mcp.repository.po.McpServiceDataScopePO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class McpServiceDataScopeService {

    private final McpServiceDataScopeDAO dataScopeDAO;

    public McpServiceDataScopeService(McpServiceDataScopeDAO dataScopeDAO) {
        this.dataScopeDAO = dataScopeDAO;
    }

    @Transactional
    public void saveDataScope(String serviceId, List<McpServiceDataScope> scopes) {
        dataScopeDAO.deleteAllByServiceId(serviceId);
        if (scopes != null && !scopes.isEmpty()) {
            List<McpServiceDataScopePO> pos = scopes.stream()
                    .map(McpServiceDataScopeMapper::toPO)
                    .toList();
            dataScopeDAO.saveAll(pos);
        }
    }

    public List<McpServiceDataScope> getDataScope(String serviceId) {
        return dataScopeDAO.findAllByServiceId(serviceId).stream()
                .map(McpServiceDataScopeMapper::toModel)
                .toList();
    }

}
