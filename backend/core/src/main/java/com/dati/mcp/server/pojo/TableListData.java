package com.dati.mcp.server.pojo;

import com.dati.datasource.domain.model.DataSourceDef;
import java.util.List;

/** TABLE_LIST result: table-level inventory (schema/name/description/aliases) grouped by data source. */
public record TableListData(List<DataSourceDef> dataSources) implements ToolTestData {}
