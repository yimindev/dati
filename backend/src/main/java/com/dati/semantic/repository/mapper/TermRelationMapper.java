package com.dati.semantic.repository.mapper;

import com.dati.semantic.domain.SemanticEntityType;
import com.dati.semantic.repository.po.TermRelationPO;

public class TermRelationMapper {

    public static TermRelationPO toPO(String termId, SemanticEntityType entityType,
                                       String tableId, String fieldName) {
        TermRelationPO po = new TermRelationPO();
        po.setTermId(termId);
        po.setEntityType(entityType);
        po.setTableId(tableId);
        po.setFieldName(fieldName);
        return po;
    }
}
