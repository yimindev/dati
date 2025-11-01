package com.dati.datasource.domain.model;

import com.dati.base.pojo.BaseResource;
import com.dati.db.DbType;
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
