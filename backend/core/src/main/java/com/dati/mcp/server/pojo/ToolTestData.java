package com.dati.mcp.server.pojo;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = SqlExecution.class, name = "SQL_EXECUTION"),
    @JsonSubTypes.Type(value = TableMetadata.class, name = "TABLE_METADATA"),
    @JsonSubTypes.Type(value = SearchHit.class, name = "SEARCH_HIT"),
    @JsonSubTypes.Type(value = MetadataUpdateData.class, name = "METADATA_UPDATE"),
    @JsonSubTypes.Type(value = TableListData.class, name = "TABLE_LIST"),
})
public interface ToolTestData {}
