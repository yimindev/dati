package com.dati.db.client;

import com.dati.db.DbType;
import com.dati.db.JdbcConnector;

import java.sql.SQLException;

public class MysqlDbClient extends AbstractDbClient {

    @Override
    public DbType getDbType() {
        return DbType.MYSQL;
    }

    @Override
    protected boolean isCatalogAsSchema() {
        return true;
    }

    @Override
    public String getCurrentSchema(JdbcConnector jdbcConnector) throws SQLException {
        return querySingleString(jdbcConnector, "SELECT DATABASE()");
    }
}
