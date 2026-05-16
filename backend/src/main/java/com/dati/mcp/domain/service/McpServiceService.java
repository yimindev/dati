package com.dati.mcp.domain.service;

import com.dati.base.exception.DatiException;
import com.dati.base.exception.ErrorCode;
import com.dati.common.StringUtils;
import com.dati.mcp.domain.model.McpService;
import com.dati.mcp.domain.model.McpServiceStatus;
import com.dati.mcp.repository.dao.McpServiceDAO;
import com.dati.mcp.repository.mapper.McpServiceMapper;
import com.dati.mcp.repository.po.McpServicePO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class McpServiceService {

    private final McpServiceDAO mcpServiceDAO;

    public McpServiceService(McpServiceDAO mcpServiceDAO) {
        this.mcpServiceDAO = mcpServiceDAO;
    }

    public String createMcpService(McpService service) {
        service.setStatus(McpServiceStatus.DRAFT);
        McpServicePO po = McpServiceMapper.toPO(service);
        po = mcpServiceDAO.save(po);
        return po.getId();
    }

    public void updateMcpService(String id, McpService service) {
        McpServicePO po = mcpServiceDAO.findById(id)
                .orElseThrow(() -> new DatiException(ErrorCode.MS_SERVICE_NOT_FOUND, id));
        if (service.getName() != null) {
            po.setName(service.getName());
        }
        if (service.getDescription() != null) {
            po.setDescription(service.getDescription());
        }
        mcpServiceDAO.save(po);
    }

    public McpService getMcpService(String id) {
        return mcpServiceDAO.findById(id)
                .map(McpServiceMapper::toModel)
                .orElseThrow(() -> new DatiException(ErrorCode.MS_SERVICE_NOT_FOUND, id));
    }

    public Page<McpService> listMcpServices(String keyword, McpServiceStatus status, Pageable pageable) {
        if (StringUtils.isEmpty(keyword) && status == null) {
            return mcpServiceDAO.findAll(pageable).map(McpServiceMapper::toModel);
        }
        if (StringUtils.isEmpty(keyword)) {
            return mcpServiceDAO.findAllByStatus(status, pageable).map(McpServiceMapper::toModel);
        }
        if (status == null) {
            return mcpServiceDAO.findAllByNameContainingOrId(keyword, keyword, pageable)
                    .map(McpServiceMapper::toModel);
        }
        return mcpServiceDAO.findAllByNameContainingOrIdAndStatus(keyword, keyword, status, pageable)
                .map(McpServiceMapper::toModel);
    }

}
