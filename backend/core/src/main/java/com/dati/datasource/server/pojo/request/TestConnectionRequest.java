package com.dati.datasource.server.pojo.request;

import com.dati.db.DbType;
import lombok.Data;

@Data
public class TestConnectionRequest {

    private DbType type;

    private String jdbcUrl;

    private String username;

    private String password;

    private String defaultSchema;

}
