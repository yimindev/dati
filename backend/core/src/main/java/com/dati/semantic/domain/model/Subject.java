package com.dati.semantic.domain.model;

import com.dati.base.pojo.BaseResource;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class Subject extends BaseResource {

    private String datasourceId;

    private List<String> aliases = new ArrayList<>();

}
