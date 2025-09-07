package com.dataconnai.datasource.domain.model;

import com.dataconnai.base.pojo.BaseResource;
import com.dataconnai.db.DbType;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class DataSource extends BaseResource {

    private DbType type;

    private String jdbcUrl;

    private String username;

    private String password;

}
