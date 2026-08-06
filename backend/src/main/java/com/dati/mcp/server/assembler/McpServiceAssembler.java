package com.dati.mcp.server.assembler;

import com.dati.base.BaseAssembler;
import com.dati.base.pojo.PageResponse;
import com.dati.mcp.domain.model.McpService;
import com.dati.mcp.domain.model.McpServiceSnapshot;
import com.dati.mcp.domain.service.McpToolService;
import com.dati.mcp.server.pojo.McpServiceSnapshotVO;
import com.dati.mcp.server.pojo.McpServiceVO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class McpServiceAssembler extends BaseAssembler {

    private final McpToolService mcpToolService;

    /** MCP endpoint 基础 URL（如 https://mcp.example.com），空则返回相对路径 /{code}/mcp */
    @Value("${dati.mcp.endpoint-base-url:}")
    private String endpointBaseUrl;

    public McpServiceAssembler(McpToolService mcpToolService) {
        this.mcpToolService = mcpToolService;
    }

    /** 快照列表 VO：仅暴露元信息（版本号/Release Note/时间），不携带快照全文 content */
    public McpServiceSnapshotVO toSnapshotVO(McpServiceSnapshot snapshot) {
        McpServiceSnapshotVO vo = new McpServiceSnapshotVO();
        super.copyBaseInfo(snapshot, vo);
        vo.setServiceId(snapshot.getServiceId());
        vo.setVersionNumber(snapshot.getVersionNumber());
        vo.setReleaseNote(snapshot.getReleaseNote());
        return vo;
    }

    public McpServiceVO toMcpServiceVO(McpService service) {
        McpServiceVO vo = mapFields(service);
        super.fillUserInfo(List.of(vo));
        return vo;
    }

    public PageResponse<McpServiceVO> toPageResponse(Page<McpService> page) {
        List<McpServiceVO> vos = page.getContent().stream()
                .map(this::mapFields)
                .collect(Collectors.toList());
        super.fillUserInfo(vos);
        return PageResponse.of(new PageImpl<>(vos, page.getPageable(), page.getTotalElements()));
    }

    private McpServiceVO mapFields(McpService service) {
        McpServiceVO vo = new McpServiceVO();
        super.copyBaseInfo(service, vo);
        vo.setCode(service.getCode());
        vo.setStatus(service.getStatus() != null ? service.getStatus().name() : null);
        vo.setEndpointPath(buildEndpointPath(service.getCode()));
        vo.setToolCount((int) mcpToolService.countToolsByServiceId(service.getId()));
        vo.setActiveVersionNumber(service.getActiveVersionNumber());
        return vo;
    }

    /** 拼接完整 endpoint URL：baseUrl 去除尾部斜杠 + /{code}/mcp；baseUrl 未配置时保持相对路径 */
    private String buildEndpointPath(String code) {
        String base = endpointBaseUrl == null ? "" : endpointBaseUrl.trim();
        while (!base.isEmpty() && base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + "/" + code + "/mcp";
    }

}
