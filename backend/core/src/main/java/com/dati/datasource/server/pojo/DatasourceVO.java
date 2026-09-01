package com.dati.datasource.server.pojo;

import com.dati.base.pojo.BaseResourceVO;
import com.dati.db.DbType;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class DatasourceVO extends BaseResourceVO {

    private String jdbcUrl;

    private String username;

    private DbType type;

    private String defaultSchema;

}
