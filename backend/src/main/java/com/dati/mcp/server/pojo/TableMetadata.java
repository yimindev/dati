package com.dati.mcp.server.pojo;

import java.util.List;

public record TableMetadata(List<TableEntry> tables) implements ToolTestData {}
