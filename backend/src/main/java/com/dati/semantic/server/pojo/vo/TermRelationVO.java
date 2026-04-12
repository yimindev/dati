package com.dati.semantic.server.pojo.vo;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TermRelationVO {
    private String id;
    private String termId;
    private String entityType;
    private String tableId;
    private String tableName;
    private String schema;
    private String fieldName;
}
