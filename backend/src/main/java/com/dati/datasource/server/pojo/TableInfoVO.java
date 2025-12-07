package com.dati.datasource.server.pojo;

import com.dati.base.pojo.BaseResourceVO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class TableInfoVO extends BaseResourceVO {
    private String schema;
    private String displayName;
    private String datasourceId;
}
