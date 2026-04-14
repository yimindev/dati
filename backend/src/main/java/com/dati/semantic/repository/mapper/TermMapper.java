package com.dati.semantic.repository.mapper;

import com.dati.semantic.repository.po.TermPO;

import java.util.List;

public class TermMapper {

    public static TermPO toPO(String subjectId, String name, String description, List<String> aliases) {
        TermPO po = new TermPO();
        po.setSubjectId(subjectId);
        po.setName(name);
        po.setDescription(description);
        po.setAliases(aliases != null ? aliases : new java.util.ArrayList<>());
        return po;
    }
}
