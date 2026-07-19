package com.dati.mcp.server.pojo;

import com.dati.datasource.domain.model.TableDef;
import java.util.List;

public record TableMetadata(List<TableDef> tables) implements ToolTestData {}
