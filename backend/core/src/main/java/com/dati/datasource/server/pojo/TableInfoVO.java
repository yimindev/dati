package com.dati.datasource.server.pojo;

import com.dati.base.pojo.BaseResourceVO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class TableInfoVO extends BaseResourceVO {
    private String schema;
    private String datasourceId;
    private List<String> aliases = new ArrayList<>();
}
