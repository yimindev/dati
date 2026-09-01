package com.dati.semantic.server.pojo.vo;

import com.dati.base.pojo.BaseResourceVO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class SubjectVO extends BaseResourceVO {

    private String datasourceId;

    private String datasourceName;

    private List<String> aliases = new ArrayList<>();

}
