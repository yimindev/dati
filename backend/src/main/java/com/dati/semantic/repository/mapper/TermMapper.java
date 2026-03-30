package com.dati.semantic.repository.mapper;

import com.dati.semantic.repository.po.TermPO;

public class TermMapper {

    public static TermPO toPO(String subjectId, String name, String description) {
        TermPO po = new TermPO();
        po.setSubjectId(subjectId);
        po.setName(name);
        po.setDescription(description);
        return po;
    }
}
