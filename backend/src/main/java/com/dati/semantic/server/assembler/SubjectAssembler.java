package com.dati.semantic.server.assembler;

import com.dati.semantic.domain.model.Subject;
import com.dati.semantic.domain.model.SubjectTable;
import com.dati.semantic.server.pojo.vo.SubjectVO;
import com.dati.semantic.server.pojo.vo.SubjectTableVO;
import com.dati.semantic.server.pojo.vo.SubjectDetailVO;
import org.springframework.stereotype.Component;

import java.util.List;

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

    public SubjectTableVO toSubjectTableVO(SubjectTable subjectTable) {
        if (subjectTable == null) {
            return null;
        }
        SubjectTableVO vo = new SubjectTableVO();
        vo.setId(subjectTable.getId());
        vo.setSubjectId(subjectTable.getSubjectId());
        vo.setTableId(subjectTable.getTableId());
        vo.setCreatedAt(subjectTable.getCreatedAt());
        return vo;
    }

    public SubjectDetailVO toDetailVO(com.dati.semantic.domain.model.SubjectDetailVO domainDetail) {
        if (domainDetail == null) {
            return null;
        }
        SubjectDetailVO vo = new SubjectDetailVO();
        Subject subject = domainDetail.getSubject();
        if (subject != null) {
            vo.setId(subject.getId());
            vo.setName(subject.getName());
            vo.setDescription(subject.getDescription());
            vo.setDatasourceId(subject.getDatasourceId());
            vo.setCreatedAt(subject.getCreatedAt());
            vo.setUpdatedAt(subject.getUpdatedAt());
        }
        List<SubjectTable> tables = domainDetail.getTables();
        if (tables != null) {
            vo.setTables(tables.stream().map(this::toSubjectTableVO).toList());
        }
        return vo;
    }
}
