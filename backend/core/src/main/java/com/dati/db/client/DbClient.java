package com.dati.db.client;

import com.dati.db.Column;
import com.dati.db.DbType;
import com.dati.db.JdbcConnector;
import com.dati.db.Table;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public interface DbClient {
    DbType getDbType();

    List<String> getSchemas(JdbcConnector jdbcConnector) throws SQLException;

    List<Table> getTables(JdbcConnector jdbcConnector, String schema) throws SQLException;

    List<Column> getColumns(JdbcConnector jdbcConnector, String schema, String table) throws SQLException;

    /** 使用已有连接查询列信息，由调用方管理连接生命周期。 */
    List<Column> getColumns(Connection connection, String schema, String table) throws SQLException;

    String getCurrentSchema(JdbcConnector jdbcConnector) throws SQLException;
}
