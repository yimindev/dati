package com.dati.semantic.repository.mapper;

import com.dati.base.MapperUtils;
import com.dati.semantic.domain.model.Term;
import com.dati.semantic.repository.po.TermPO;

import java.util.ArrayList;
import java.util.List;

public class TermMapper {

    public static TermPO toPO(String subjectId, String name, String description, List<String> aliases) {
        TermPO po = new TermPO();
        po.setSubjectId(subjectId);
        po.setName(name);
        po.setDescription(description);
        po.setAliases(aliases != null ? aliases : new ArrayList<>());
        return po;
    }

    public static Term toTerm(TermPO po) {
        Term term = new Term();
        MapperUtils.copyBaseResourceInfo(po, term);
        term.setSubjectId(po.getSubjectId());
        term.setAliases(po.getAliases() != null ? po.getAliases() : new ArrayList<>());
        return term;
    }
}
