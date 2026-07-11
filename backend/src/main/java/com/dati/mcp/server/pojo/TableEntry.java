package com.dati.mcp.server.pojo;

import com.dati.db.Column;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TableEntry(boolean success, String table, String schema,
                         List<Column> columns, String errorMessage) {}
