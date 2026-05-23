package com.dati.mcp.server.pojo;

import lombok.Data;

import java.util.List;

@Data
public class ToolsResponse {
    private List<McpToolVO> prebuilt;
    private List<McpToolVO> custom;
}
