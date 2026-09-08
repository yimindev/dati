package com.dati.mcp.server.assembler;

import com.dati.mcp.repository.po.McpInvocationLogPO;
import com.dati.mcp.server.pojo.McpInvocationLogVO;
import org.springframework.stereotype.Component;

@Component
public class McpUsageAssembler {

    public McpInvocationLogVO toVO(McpInvocationLogPO po) {
        if (po == null) {
            return null;
        }
        McpInvocationLogVO vo = new McpInvocationLogVO();
        vo.setId(po.getId());
        vo.setServiceId(po.getServiceId());
        vo.setMethod(po.getMethod());
        vo.setToolName(po.getToolName());
        vo.setCallerId(po.getCallerId());
        vo.setCallerName(po.getCallerName());
        vo.setClientIp(po.getClientIp());
        vo.setSuccess(po.getSuccess());
        vo.setDurationMs(po.getDurationMs());
        vo.setErrorMessage(po.getErrorMessage());
        vo.setStatDate(po.getStatDate());
        vo.setCreatedAt(po.getCreatedAt());
        return vo;
    }
}
