package com.dati.semantic.repository.mapper;

import com.dati.base.MapperUtils;
import com.dati.semantic.domain.model.Subject;
import com.dati.semantic.repository.po.SubjectPO;

import java.util.ArrayList;

public class SubjectMapper {

    public static SubjectPO toPO(Subject subject) {
        SubjectPO po = new SubjectPO();
        MapperUtils.copyBaseResourceInfo(subject, po);
        po.setDatasourceId(subject.getDatasourceId());
        po.setAliases(subject.getAliases() != null ? subject.getAliases() : new ArrayList<>());
        return po;
    }

    public static Subject toSubject(SubjectPO po) {
        Subject subject = new Subject();
        MapperUtils.copyBaseResourceInfo(po, subject);
        subject.setDatasourceId(po.getDatasourceId());
        subject.setAliases(po.getAliases() != null ? po.getAliases() : new ArrayList<>());
        return subject;
    }
}
