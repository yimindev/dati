package com.dati.semantic.server.assembler;

import com.dati.base.BaseAssembler;
import com.dati.base.pojo.PageResponse;
import com.dati.semantic.domain.model.Term;
import com.dati.semantic.domain.model.TermRelation;
import com.dati.semantic.server.pojo.vo.TermRelationVO;
import com.dati.semantic.server.pojo.vo.TermVO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class TermAssembler extends BaseAssembler {

    public TermVO toVO(Term term) {
        if (term == null) {
            return null;
        }
        TermVO vo = mapFields(term);
        super.fillUserInfo(List.of(vo));
        return vo;
    }

    public TermVO toVO(Term term, List<TermRelation> relations) {
        TermVO vo = toVO(term);
        if (relations != null) {
            vo.setRelations(relations.stream()
                    .map(this::toRelationVO)
                    .toList());
        }
        return vo;
    }

    public PageResponse<TermVO> toPageResponse(Page<Term> page) {
        List<TermVO> vos = page.getContent().stream()
                .map(term -> {
                    TermVO vo = mapFields(term);
                    vo.setRelations(term.getRelations() != null
                            ? term.getRelations().stream().map(this::toRelationVO).toList()
                            : new ArrayList<>());
                    return vo;
                })
                .collect(Collectors.toList());
        super.fillUserInfo(vos);
        return PageResponse.of(new PageImpl<>(vos, page.getPageable(), page.getTotalElements()));
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

    private TermVO mapFields(Term term) {
        TermVO vo = new TermVO();
        super.copyBaseInfo(term, vo);
        vo.setSubjectId(term.getSubjectId());
        vo.setAliases(term.getAliases() != null ? term.getAliases() : new ArrayList<>());
        return vo;
    }
}
