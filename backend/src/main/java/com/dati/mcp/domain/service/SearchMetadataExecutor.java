package com.dati.mcp.domain.service;

import com.dati.mcp.domain.model.McpToolType;
import com.dati.mcp.server.pojo.SearchHit;
import com.dati.mcp.server.pojo.ToolTestData;
import org.springframework.stereotype.Component;

@Component
public class SearchMetadataExecutor implements ToolExecutor {
    @Override
    public McpToolType getToolType() {
        return McpToolType.SEARCH_METADATA;
    }

    @Override
    public ToolTestData execute(ToolExecutionContext ctx) {
        return new SearchHit();
    }
}
