package com.dati.db;

import com.dati.datasource.domain.model.DataSource;
import org.springframework.lang.NonNull;

public record JdbcConnector(String jdbcUrl, String username, String password) {

    public JdbcConnector(DataSource dataSource) {
        this(dataSource.getJdbcUrl(), dataSource.getUsername(), dataSource.getPassword());
    }

    /** Password never appears in logs; credentials embedded in the URL are masked as well. */
    @NonNull
    @Override
    public String toString() {
        return "JdbcConnector[jdbcUrl=" + JdbcUrl.redact(jdbcUrl) + ", username=" + username + "]";
    }
}
