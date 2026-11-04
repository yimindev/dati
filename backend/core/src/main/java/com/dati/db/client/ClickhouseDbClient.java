package com.dati.db.client;

import com.dati.db.DbType;

import java.sql.Connection;
import java.sql.SQLException;

public class ClickhouseDbClient extends AbstractDbClient {

    @Override
    public DbType getDbType() {
        return DbType.CLICKHOUSE;
    }

    @Override
    public String getCurrentSchema(Connection connection) throws SQLException {
        return querySingleString(connection, "SELECT currentDatabase()");
    }
}
