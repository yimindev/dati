package com.dataconnai.datasource.server.pojo;

import com.dataconnai.base.pojo.BaseResourceVO;
import com.dataconnai.db.DbType;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class DatasourceVO extends BaseResourceVO {

    private String jdbcUrl;

    private String username;

    private DbType type;

}
