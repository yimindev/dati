package com.dati.db.client;

import com.dati.db.DbType;

import java.sql.Connection;
import java.sql.SQLException;

public class PostgresqlDbClient extends AbstractDbClient {

    @Override
    public DbType getDbType() {
        return DbType.POSTGRESQL;
    }

    @Override
    public String getCurrentSchema(Connection connection) throws SQLException {
        return querySingleString(connection, "SELECT current_schema()");
    }
}
