package com.dati.db.client;

import com.dati.db.Column;
import com.dati.db.JdbcConnector;
import com.dati.db.DbType;
import com.dati.db.Table;
import jakarta.annotation.Nullable;

import java.sql.SQLException;
import java.util.List;

public interface DbClient {
    DbType getDbType();

    List<String> getCatalogs(JdbcConnector connection) throws SQLException;

    List<String> getSchemas(JdbcConnector connection, @Nullable String catalog) throws SQLException;

    List<Table> getTables(JdbcConnector connection, @Nullable String catalog, String schema) throws SQLException;

    List<Column> getColumns(JdbcConnector connection, @Nullable String catalog, String schema, String table) throws SQLException;
}
