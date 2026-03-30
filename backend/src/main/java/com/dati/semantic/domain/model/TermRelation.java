package com.dati.semantic.domain.model;

import com.dati.semantic.domain.SemanticEntityType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TermRelation {

    private String id;

    private String termId;

    private SemanticEntityType entityType;

    private String tableId;

    private String fieldName;

}