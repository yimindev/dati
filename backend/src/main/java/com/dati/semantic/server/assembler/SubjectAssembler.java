package com.dati.semantic.server.assembler;

import com.dati.base.BaseAssembler;
import com.dati.semantic.domain.model.Subject;
import com.dati.semantic.server.pojo.vo.SubjectVO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Component
public class SubjectAssembler extends BaseAssembler {

    public SubjectVO toVO(Subject subject) {
        if (subject == null) {
            return null;
        }
        SubjectVO vo = new SubjectVO();
        super.copyBaseInfo(subject, vo);
        vo.setDatasourceId(subject.getDatasourceId());
        vo.setAliases(subject.getAliases() != null ? subject.getAliases() : new ArrayList<>());
        return vo;
    }
}
