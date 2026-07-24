package com.dati.semantic.server.pojo.vo;

import com.dati.base.pojo.BaseResourceVO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class TermVO extends BaseResourceVO {

    private String subjectId;

    private List<String> aliases = new ArrayList<>();

    private List<TermRelationVO> relations = new ArrayList<>();

}
