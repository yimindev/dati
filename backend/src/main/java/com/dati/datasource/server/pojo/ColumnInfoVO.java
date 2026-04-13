package com.dati.datasource.server.pojo;

import com.dati.base.pojo.BaseResourceVO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class ColumnInfoVO extends BaseResourceVO {

    private String datasourceId;

    private String tableId;

    private String columnType;

    private List<String> aliases = new ArrayList<>();

}
