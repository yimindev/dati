package com.dati.db.client;

import com.dati.db.DbType;
import com.dati.db.HikariPoolManager;
import com.dati.db.JdbcConnector;
import jakarta.annotation.Nullable;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

public class DorisDbClient extends AbstractDbClient {

    @Override
    public DbType getDbType() {
        return DbType.DORIS;
    }

    @Override
    public List<String> getSchemas(JdbcConnector jdbcConnector, @Nullable String catalog) throws SQLException {
        return super.getCatalogs(jdbcConnector);
    }

    @Override
    public String getCurrentSchema(JdbcConnector jdbcConnector) throws SQLException {
        try (Connection connection = HikariPoolManager.getConnection(jdbcConnector);
             Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT DATABASE()")) {
            if (rs.next()) return rs.getString(1);
            return null;
        }
    }
}
