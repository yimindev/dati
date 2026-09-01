package com.dati.db;

import com.dati.datasource.domain.model.DataSource;

public record JdbcConnector(String jdbcUrl, String username, String password) {

    public JdbcConnector(DataSource dataSource) {
        this(dataSource.getJdbcUrl(), dataSource.getUsername(), dataSource.getPassword());
    }

}
