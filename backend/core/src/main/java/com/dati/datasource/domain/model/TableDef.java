package com.dati.datasource.domain.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/** Pure table metadata definition, shared by GET_TABLE_INFO and SEARCH_METADATA. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TableDef(String table, String schema, String description,
                       List<String> aliases, List<ColumnDef> columns) {}
