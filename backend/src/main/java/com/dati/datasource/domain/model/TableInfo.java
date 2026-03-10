package com.dati.datasource.domain.model;

import com.dati.base.pojo.BaseResource;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class TableInfo extends BaseResource {

    private String datasourceId;

    private String schema;

}
