package com.dati.mcp.server.pojo;

import java.util.List;

/** METADATA_UPDATE tool result: one entry per processed item (partial failure allowed). */
public record MetadataUpdateData(List<MetadataUpdateResult> results) implements ToolTestData {}
