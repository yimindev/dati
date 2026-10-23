package com.dati.semantic.repository.mapper;

import com.dati.semantic.domain.TermRelationType;
import com.dati.semantic.domain.model.TermRelation;
import com.dati.semantic.repository.po.TermRelationPO;

public class TermRelationMapper {

    public static TermRelationPO toPO(String termId, TermRelationType entityType,
                                       String tableId, String fieldName) {
        TermRelationPO po = new TermRelationPO();
        po.setTermId(termId);
        po.setEntityType(entityType);
        po.setTableId(tableId);
        po.setFieldName(fieldName);
        return po;
    }

    public static TermRelation toRelation(TermRelationPO po) {
        return TermRelation.builder()
                .id(po.getId())
                .termId(po.getTermId())
                .entityType(po.getEntityType())
                .tableId(po.getTableId())
                .fieldName(po.getFieldName())
                .build();
    }
}
