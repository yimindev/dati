package com.dati.db.client;

import com.dati.db.DbType;
import com.dati.db.JdbcConnector;

import java.sql.SQLException;

public class PostgresqlDbClient extends AbstractDbClient {

    @Override
    public DbType getDbType() {
        return DbType.POSTGRESQL;
    }

    @Override
    public String getCurrentSchema(JdbcConnector jdbcConnector) throws SQLException {
        return querySingleString(jdbcConnector, "SELECT current_schema()");
    }
}
