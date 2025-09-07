package com.dataconnai.db.client;

import com.dataconnai.db.Column;
import com.dataconnai.db.JdbcConnector;
import com.dataconnai.db.DbType;
import jakarta.annotation.Nullable;

import java.sql.SQLException;
import java.util.List;

public interface DbClient {
    DbType getDbType();

    List<String> getCatalogs(JdbcConnector connection) throws SQLException;

    List<String> getSchemas(JdbcConnector connection, @Nullable String catalog) throws SQLException;

    List<String> getTables(JdbcConnector connection, @Nullable String catalog, String schema) throws SQLException;

    List<Column> getColumns(JdbcConnector connection, @Nullable String catalog, String schema, String table) throws SQLException;
}
