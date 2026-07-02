package com.dati.mcp.domain.service;

import com.dati.base.exception.DatiException;
import com.dati.base.exception.ErrorCode;
import com.dati.mcp.domain.model.McpServiceDataScope;
import com.dati.mcp.domain.model.McpToolType;
import com.dati.mcp.repository.dao.McpServiceDataScopeDAO;
import com.dati.mcp.repository.mapper.McpServiceDataScopeMapper;
import com.dati.mcp.server.pojo.ToolTestData;
import com.dati.mcp.server.pojo.ToolTestError;
import com.dati.mcp.server.pojo.ToolTestRequest;
import com.dati.mcp.server.pojo.ToolTestResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class McpToolTestService {

    private final ToolResolver toolResolver;
    private final McpServiceDataScopeDAO scopeDAO;
    private final Map<McpToolType, ToolExecutor> executorMap;

    public McpToolTestService(ToolResolver toolResolver, McpServiceDataScopeDAO scopeDAO,
                              List<ToolExecutor> toolExecutorList) {
        this.toolResolver = toolResolver;
        this.scopeDAO = scopeDAO;
        this.executorMap = toolExecutorList.stream().collect(Collectors.toMap(ToolExecutor::getToolType, Function.identity()));
    }

    public ToolTestResponse test(String serviceId, String toolId, ToolTestRequest request) {
        long start = System.currentTimeMillis();
        try {
            ToolResolver.ResolvedTool tool = toolResolver.resolve(serviceId, toolId);
            List<McpServiceDataScope> scopeItems = scopeDAO.findAllByServiceId(serviceId)
                .stream().map(McpServiceDataScopeMapper::toModel).toList();
            ToolExecutionContext ctx = new ToolExecutionContext(
                serviceId, tool.toolType(), tool.config(), request.arguments(), scopeItems);
            ToolExecutor executor = executorMap.get(tool.toolType());
            if (executor == null) {
                throw new DatiException(ErrorCode.MS_TOOL_NOT_FOUND,
                    "No executor registered for tool type: " + tool.toolType());
            }
            ToolTestData data = executor.execute(ctx);
            long elapsed = System.currentTimeMillis() - start;
            return new ToolTestResponse(true, elapsed, data, null);
        } catch (DatiException e) {
            long elapsed = System.currentTimeMillis() - start;
            log.error("Tool execution error: ", e);
            return new ToolTestResponse(false, elapsed, null,
                new ToolTestError(mapErrorCategory(e.getCode()), e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error during tool test", e);
            long elapsed = System.currentTimeMillis() - start;
            return new ToolTestResponse(false, elapsed, null,
                new ToolTestError("INTERNAL_ERROR", e.getMessage()));
        }
    }

    private String mapErrorCategory(ErrorCode code) {
        return switch (code) {
            case MS_TOOL_DISABLED, MS_TOOL_NOT_FOUND, INVALID_PARAMETER, DS_NOT_FOUND -> "PARAM_ERROR";
            case MS_SCOPE_ERROR -> "SCOPE_ERROR";
            case MS_SQL_POLICY_VIOLATION -> "PERMISSION_DENIED";
            case DS_CONNECTION_FAILED, DS_SQL_ERROR -> "SQL_ERROR";
            default -> "INTERNAL_ERROR";
        };
    }
}
