package com.dati.datasource.server.pojo;

import com.dati.base.pojo.BaseResourceVO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class ColumnInfoVO extends BaseResourceVO {

    private String datasourceId;

    private String tableId;

    private String columnType;

    private String comment;

}
