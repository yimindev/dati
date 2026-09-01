package com.dati.db.client;

import com.dati.db.Column;
import com.dati.db.HikariPoolManager;
import com.dati.db.JdbcConnector;
import com.dati.db.Table;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractDbClient implements DbClient {

    private static final String CATALOG_COLUMN = "TABLE_CAT";
    private static final String SCHEMA_COLUMN = "TABLE_SCHEM";
    private static final String TABLE_COLUMN = "TABLE_NAME";

    /**
     * Indicates whether the database uses JDBC Catalog as the user-facing Schema concept.
     * MySQL-family databases (MySQL, MariaDB, Doris) return true;
     * standard schema databases (PostgreSQL, ClickHouse, Oracle) return false (default).
     */
    protected boolean isCatalogAsSchema() {
        return false;
    }

    @Override
    public List<String> getSchemas(JdbcConnector jdbcConnector) throws SQLException {
        return isCatalogAsSchema() ? getCatalogs(jdbcConnector) : getStandardSchemas(jdbcConnector);
    }

    @Override
    public List<Table> getTables(JdbcConnector jdbcConnector, String schema) throws SQLException {
        String actualCatalog = isCatalogAsSchema() ? schema : null;
        String actualSchema = isCatalogAsSchema() ? null : schema;

        List<Table> tables = new ArrayList<>();
        try (Connection connection = HikariPoolManager.getConnection(jdbcConnector);
             ResultSet resultSet = connection.getMetaData().getTables(actualCatalog, actualSchema, null, new String[]{"TABLE", "VIEW"})) {
            while (resultSet.next()) {
                String tableName = resultSet.getString(TABLE_COLUMN);
                String tableComment = resultSet.getString("REMARKS");
                tables.add(new Table(tableName, tableComment));
            }
        }
        return tables;
    }

    @Override
    public List<Column> getColumns(JdbcConnector jdbcConnector, String schema, String table) throws SQLException {
        try (Connection connection = HikariPoolManager.getConnection(jdbcConnector)) {
            return getColumns(connection, schema, table);
        }
    }

    @Override
    public List<Column> getColumns(Connection connection, String schema, String table) throws SQLException {
        String actualCatalog = isCatalogAsSchema() ? schema : null;
        String actualSchema = isCatalogAsSchema() ? null : schema;

        List<Column> columns = new ArrayList<>();
        try (ResultSet resultSet = connection.getMetaData().getColumns(actualCatalog, actualSchema, table, null)) {
            while (resultSet.next()) {
                columns.add(new Column(
                    resultSet.getString("COLUMN_NAME"),
                    resultSet.getString("TYPE_NAME"),
                    resultSet.getString("REMARKS")));
            }
        }
        return columns;
    }

    protected List<String> getCatalogs(JdbcConnector jdbcConnector) throws SQLException {
        List<String> catalogs = new ArrayList<>();
        try (Connection connection = HikariPoolManager.getConnection(jdbcConnector);
             ResultSet resultSet = connection.getMetaData().getCatalogs()) {
            while (resultSet.next()) {
                catalogs.add(resultSet.getString(CATALOG_COLUMN));
            }
        }
        return catalogs;
    }

    protected List<String> getStandardSchemas(JdbcConnector jdbcConnector) throws SQLException {
        List<String> schemas = new ArrayList<>();
        try (Connection connection = HikariPoolManager.getConnection(jdbcConnector);
             ResultSet resultSet = connection.getMetaData().getSchemas()) {
            while (resultSet.next()) {
                schemas.add(resultSet.getString(SCHEMA_COLUMN));
            }
        }
        return schemas;
    }

    /**
     * Helper to execute a query returning a single String result (e.g. SELECT DATABASE(), SELECT current_schema()).
     */
    protected String querySingleString(JdbcConnector jdbcConnector, String sql) throws SQLException {
        try (Connection connection = HikariPoolManager.getConnection(jdbcConnector);
             Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getString(1);
            }
            return null;
        }
    }
}
