package com.dati.semantic.server.assembler;

import com.dati.semantic.domain.model.Term;
import com.dati.semantic.server.pojo.vo.TermVO;
import org.springframework.stereotype.Component;

@Component
public class TermAssembler {

    public TermVO toVO(Term term) {
        if (term == null) {
            return null;
        }
        TermVO vo = new TermVO();
        vo.setId(term.getId());
        vo.setSubjectId(term.getSubjectId());
        vo.setName(term.getName());
        vo.setDescription(term.getDescription());
        vo.setCreatedAt(term.getCreatedAt());
        vo.setUpdatedAt(term.getUpdatedAt());
        return vo;
    }
}
