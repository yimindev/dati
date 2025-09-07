package com.dataconnai.db.client;

import com.dataconnai.db.Column;
import com.dataconnai.db.HikariPoolManager;
import com.dataconnai.db.JdbcConnector;
import jakarta.annotation.Nullable;


import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractDbClient implements DbClient {

    private static final String CATALOG_COLUMN = "TABLE_CAT";
    private static final String SCHEMA_COLUMN = "TABLE_SCHEM";
    private static final String TABLE_COLUMN = "TABLE_NAME";


    @Override
    public List<String> getCatalogs(JdbcConnector jdbcConnector) throws SQLException {
        List<String> catalogs = new ArrayList<>();
        try (Connection connection = HikariPoolManager.getConnection(jdbcConnector);
             ResultSet resultSet = connection.getMetaData().getCatalogs()) {
            while (resultSet.next()) {
                catalogs.add(resultSet.getString(CATALOG_COLUMN));
            }
        }
        return catalogs;
    }

    @Override
    public List<String> getSchemas(JdbcConnector jdbcConnector, @Nullable String catalog) throws SQLException {
        List<String> schemas = new ArrayList<>();
        try (Connection connection = HikariPoolManager.getConnection(jdbcConnector);
             // default catalog is null
             ResultSet resultSet = connection.getMetaData().getSchemas()) {
            while (resultSet.next()) {
                schemas.add(resultSet.getString(SCHEMA_COLUMN));
            }
        }
        return schemas;
    }

    @Override
    public List<String> getTables(JdbcConnector jdbcConnector, @Nullable String catalog, String schema) throws SQLException {
        List<String> tables = new ArrayList<>();
        try (Connection connection = HikariPoolManager.getConnection(jdbcConnector);
             ResultSet resultSet = connection.getMetaData().getTables(catalog, schema, null, new String[] { "TABLE", "VIEW"})) {
            while (resultSet.next()) {
                tables.add(resultSet.getString(TABLE_COLUMN));
            }
        }
        return tables;
    }

    @Override
    public List<Column> getColumns(JdbcConnector jdbcConnector, @Nullable String catalog, String schema, String table) throws SQLException {
        List<Column> columns = new ArrayList<>();
        try (Connection connection = HikariPoolManager.getConnection(jdbcConnector);
        ResultSet resultSet = connection.getMetaData().getColumns(catalog, schema, table, null)) {
            while (resultSet.next()) {
                String columnName = resultSet.getString("COLUMN_NAME");
                String columnType = resultSet.getString("TYPE_NAME");
                String columnComment = resultSet.getString("REMARKS");
                columns.add(new Column(columnName, columnType, columnComment));
            }
        }
        return columns;
    }

    
}
