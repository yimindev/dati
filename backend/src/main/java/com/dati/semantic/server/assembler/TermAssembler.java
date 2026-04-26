package com.dati.semantic.server.assembler;

import com.dati.base.BaseAssembler;
import com.dati.semantic.domain.model.Term;
import com.dati.semantic.domain.model.TermRelation;
import com.dati.semantic.server.pojo.vo.TermRelationVO;
import com.dati.semantic.server.pojo.vo.TermVO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class TermAssembler extends BaseAssembler {

    public TermVO toVO(Term term) {
        if (term == null) {
            return null;
        }
        TermVO vo = new TermVO();
        super.copyBaseInfo(term, vo);
        vo.setSubjectId(term.getSubjectId());
        vo.setAliases(term.getAliases() != null ? term.getAliases() : new ArrayList<>());
        return vo;
    }

    public TermVO toVO(Term term, List<TermRelation> relations) {
        TermVO vo = toVO(term);
        if (relations != null && !relations.isEmpty()) {
            vo.setRelations(relations.stream()
                    .map(this::toRelationVO)
                    .toList());
        }
        return vo;
    }

    public TermRelationVO toRelationVO(TermRelation relation) {
        if (relation == null) {
            return null;
        }
        return TermRelationVO.builder()
                .id(relation.getId())
                .termId(relation.getTermId())
                .entityType(relation.getEntityType().name())
                .tableId(relation.getTableId())
                .tableName(relation.getTableName())
                .schema(relation.getSchema())
                .fieldName(relation.getFieldName())
                .build();
    }
}
