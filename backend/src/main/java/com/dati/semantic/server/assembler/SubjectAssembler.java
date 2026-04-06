package com.dati.semantic.server.assembler;

import com.dati.semantic.domain.model.Subject;
import com.dati.semantic.server.pojo.vo.SubjectVO;
import org.springframework.stereotype.Component;

@Component
public class SubjectAssembler {

    public SubjectVO toVO(Subject subject) {
        if (subject == null) {
            return null;
        }
        SubjectVO vo = new SubjectVO();
        vo.setId(subject.getId());
        vo.setName(subject.getName());
        vo.setDescription(subject.getDescription());
        vo.setDatasourceId(subject.getDatasourceId());
        vo.setCreatedAt(subject.getCreatedAt());
        vo.setUpdatedAt(subject.getUpdatedAt());
        return vo;
    }
}
