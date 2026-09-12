package com.dati.mcp.server.pojo;

import com.dati.datasource.domain.model.DataSourceDef;
import com.dati.semantic.domain.model.TermDef;
import java.util.List;

/** TABLE_LIST result: table-level inventory (schema/name/description/aliases) grouped by data source, and terms under subjects in data scope. */
public record TableListData(List<DataSourceDef> dataSources, List<TermDef> terms) implements ToolTestData {}

