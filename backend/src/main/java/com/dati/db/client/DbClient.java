package com.dati.db.client;

import com.dati.db.Column;
import com.dati.db.JdbcConnector;
import com.dati.db.DbType;
import com.dati.db.Table;
import jakarta.annotation.Nullable;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public interface DbClient {
    DbType getDbType();

    List<String> getCatalogs(JdbcConnector jdbcConnector) throws SQLException;

    List<String> getSchemas(JdbcConnector jdbcConnector, @Nullable String catalog) throws SQLException;

    List<Table> getTables(JdbcConnector jdbcConnector, @Nullable String catalog, String schema) throws SQLException;

    List<Column> getColumns(JdbcConnector jdbcConnector, @Nullable String catalog, String schema, String table) throws SQLException;

    /** 使用已有连接查询列信息，由调用方管理连接生命周期。 */
    List<Column> getColumns(Connection connection, @Nullable String catalog, String schema, String table) throws SQLException;

    String getCurrentSchema(JdbcConnector jdbcConnector) throws SQLException;
}
