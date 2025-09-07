package com.dataconnai.db;

import com.dataconnai.datasource.domain.model.DataSource;

public record JdbcConnector(String jdbcUrl, String username, String password) {

    public JdbcConnector(DataSource dataSource) {
        this(dataSource.getJdbcUrl(), dataSource.getUsername(), dataSource.getPassword());
    }

}
