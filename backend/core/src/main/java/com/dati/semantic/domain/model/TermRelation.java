package com.dati.semantic.domain.model;

import com.dati.semantic.domain.TermRelationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TermRelation {

    private String id;

    private String termId;

    private TermRelationType entityType;

    private String tableId;

    private String fieldName;

    private String tableName;

    private String schema;

}