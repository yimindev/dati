package com.dati.semantic.domain.model;

import com.dati.base.pojo.BaseResource;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class Term extends BaseResource {

    private String subjectId;

    private List<TermRelation> relations;

}