package com.dati.semantic.repository.mapper;

import com.dati.base.MapperUtils;
import com.dati.semantic.domain.model.Subject;
import com.dati.semantic.repository.po.SubjectPO;

import java.util.ArrayList;

public class SubjectMapper {

    public static Subject toSubject(SubjectPO po) {
        Subject subject = new Subject();
        MapperUtils.copyBaseInfo(po, subject);
        subject.setDatasourceId(po.getDatasourceId());
        subject.setAliases(po.getAliases() != null ? po.getAliases() : new ArrayList<>());
        return subject;
    }
}
