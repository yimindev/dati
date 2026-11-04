package com.dati.db.client;

import com.dati.db.DbType;

import java.sql.Connection;
import java.sql.SQLException;

public class MariaDbClient extends AbstractDbClient {

    @Override
    public DbType getDbType() {
        return DbType.MARIADB;
    }

    @Override
    protected boolean isCatalogAsSchema() {
        return true;
    }

    @Override
    public String getCurrentSchema(Connection connection) throws SQLException {
        return querySingleString(connection, "SELECT DATABASE()");
    }
}
