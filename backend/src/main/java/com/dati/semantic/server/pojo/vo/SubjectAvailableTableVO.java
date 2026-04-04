package com.dati.semantic.server.pojo.vo;

import lombok.Data;

@Data
public class SubjectAvailableTableVO {
    private String tableId;
    private String tableName;
    private String schema;
    private String description;
}
