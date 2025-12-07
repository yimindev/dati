package com.dati.datasource.domain.model;

import com.dati.base.pojo.BaseResource;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class TableInfo extends BaseResource {

    private String datasourceId;

    private String schema;

    private String displayName;

    private List<ColumnDef> columns;

}
