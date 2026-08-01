package com.dati.mcp.domain.service;

import com.dati.base.exception.DatiException;
import com.dati.base.exception.ErrorCode;
import com.dati.common.StringUtils;
import com.dati.mcp.domain.model.McpService;
import com.dati.mcp.domain.model.McpServiceDataScope;
import com.dati.mcp.domain.model.McpServiceStatus;
import com.dati.mcp.repository.dao.McpServiceDAO;
import com.dati.mcp.repository.dao.McpServiceDataScopeDAO;
import com.dati.mcp.repository.mapper.McpServiceMapper;
import com.dati.mcp.repository.mapper.McpServiceDataScopeMapper;
import com.dati.mcp.repository.po.McpServicePO;
import com.dati.mcp.repository.po.McpServiceDataScopePO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.regex.Pattern;

@Service
public class McpServiceService {

    private static final Pattern CODE_PATTERN = Pattern.compile("^[a-z0-9]([a-z0-9_-]{0,62}[a-z0-9])?$");

    private final McpServiceDAO mcpServiceDAO;
    private final McpServiceDataScopeDAO dataScopeDAO;

    public McpServiceService(McpServiceDAO mcpServiceDAO,
                             McpServiceDataScopeDAO dataScopeDAO) {
        this.mcpServiceDAO = mcpServiceDAO;
        this.dataScopeDAO = dataScopeDAO;
    }

    @Transactional
    public String createMcpService(McpService service, List<McpServiceDataScope> scopes) {
        String code = service.getCode();
        if (code == null || code.isBlank()) {
            throw new DatiException(ErrorCode.MS_SERVICE_CODE_REQUIRED);
        }
        if (!CODE_PATTERN.matcher(code).matches()) {
            throw new DatiException(ErrorCode.MS_SERVICE_CODE_INVALID);
        }
        if (mcpServiceDAO.existsByCode(code)) {
            throw new DatiException(ErrorCode.MS_SERVICE_CODE_EXISTS, code);
        }
        if (scopes == null || scopes.isEmpty()) {
            throw new DatiException(ErrorCode.MS_SERVICE_DATA_SCOPE_REQUIRED);
        }
        service.setStatus(McpServiceStatus.DRAFT);
        McpServicePO po = McpServiceMapper.toPO(service);
        po = mcpServiceDAO.save(po);
        final String serviceId = po.getId();
        List<McpServiceDataScopePO> scopePos = scopes.stream().map(scope -> {
            scope.setServiceId(serviceId);
            return McpServiceDataScopeMapper.toPO(scope);
        }).toList();
        dataScopeDAO.saveAll(scopePos);
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
        if (service.getUpdatedBy() != null) {
            po.setUpdatedBy(service.getUpdatedBy());
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
        return mcpServiceDAO.searchByKeywordAndStatus(keyword, status, pageable)
                .map(McpServiceMapper::toModel);
    }

}
