package com.dati.db.client;

import com.dati.db.DbType;
import com.dati.db.HikariPoolManager;
import com.dati.db.JdbcConnector;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class PostgresqlDbClient extends AbstractDbClient {

    @Override
    public DbType getDbType() {
        return DbType.POSTGRESQL;
    }

    @Override
    public String getCurrentSchema(JdbcConnector jdbcConnector) throws SQLException {
        try (Connection connection = HikariPoolManager.getConnection(jdbcConnector);
             Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT current_schema()")) {
            if (rs.next()) return rs.getString(1);
            return null;
        }
    }
}
