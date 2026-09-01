package com.dati.db.client;

import com.dati.db.DbType;
import com.dati.db.HikariPoolManager;
import com.dati.db.JdbcConnector;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("MysqlDbClient unit tests")
class MysqlDbClientTest {

    private final MysqlDbClient mysqlDbClient = new MysqlDbClient();

    @Test
    @DisplayName("getDbType should return MYSQL")
    void getDbType_shouldReturnMysql() {
        assertThat(mysqlDbClient.getDbType()).isEqualTo(DbType.MYSQL);
    }

    @Test
    @DisplayName("getSchemas should query catalogs from database metadata")
    void getSchemas_shouldReturnCatalogs() throws SQLException {
        JdbcConnector connector = new JdbcConnector("jdbc:mysql://localhost:3306/test", "root", "pass");

        Connection connection = mock(Connection.class);
        DatabaseMetaData metaData = mock(DatabaseMetaData.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getCatalogs()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, true, false);
        when(resultSet.getString("TABLE_CAT")).thenReturn("db1", "db2");

        try (MockedStatic<HikariPoolManager> mockedHikari = mockStatic(HikariPoolManager.class)) {
            mockedHikari.when(() -> HikariPoolManager.getConnection(eq(connector))).thenReturn(connection);

            List<String> schemas = mysqlDbClient.getSchemas(connector);

            assertThat(schemas).containsExactly("db1", "db2");
        }
    }

    @Test
    @DisplayName("getTables should map schema to catalog in database metadata")
    void getTables_shouldMapSchemaToCatalog() throws SQLException {
        JdbcConnector connector = new JdbcConnector("jdbc:mysql://localhost:3306/test", "root", "pass");

        Connection connection = mock(Connection.class);
        DatabaseMetaData metaData = mock(DatabaseMetaData.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getTables(eq("test_db"), eq(null), eq(null), eq(new String[]{"TABLE", "VIEW"})))
                .thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, false);
        when(resultSet.getString("TABLE_NAME")).thenReturn("users");
        when(resultSet.getString("REMARKS")).thenReturn("User table");

        try (MockedStatic<HikariPoolManager> mockedHikari = mockStatic(HikariPoolManager.class)) {
            mockedHikari.when(() -> HikariPoolManager.getConnection(eq(connector))).thenReturn(connection);

            var tables = mysqlDbClient.getTables(connector, "test_db");

            assertThat(tables).hasSize(1);
            assertThat(tables.getFirst().name()).isEqualTo("users");
            assertThat(tables.getFirst().comment()).isEqualTo("User table");
        }
    }

    @Test
    @DisplayName("getColumns should map schema to catalog in database metadata")
    void getColumns_shouldMapSchemaToCatalog() throws SQLException {
        JdbcConnector connector = new JdbcConnector("jdbc:mysql://localhost:3306/test", "root", "pass");

        Connection connection = mock(Connection.class);
        DatabaseMetaData metaData = mock(DatabaseMetaData.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getColumns(eq("test_db"), eq(null), eq("users"), eq(null)))
                .thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, false);
        when(resultSet.getString("COLUMN_NAME")).thenReturn("id");
        when(resultSet.getString("TYPE_NAME")).thenReturn("INT");
        when(resultSet.getString("REMARKS")).thenReturn("Primary key");

        try (MockedStatic<HikariPoolManager> mockedHikari = mockStatic(HikariPoolManager.class)) {
            mockedHikari.when(() -> HikariPoolManager.getConnection(eq(connector))).thenReturn(connection);

            var columns = mysqlDbClient.getColumns(connector, "test_db", "users");

            assertThat(columns).hasSize(1);
            assertThat(columns.getFirst().name()).isEqualTo("id");
            assertThat(columns.getFirst().type()).isEqualTo("INT");
            assertThat(columns.getFirst().comment()).isEqualTo("Primary key");
        }
    }

    @Test
    @DisplayName("getCurrentSchema should execute SELECT DATABASE()")
    void getCurrentSchema_shouldReturnCurrentDatabase() throws SQLException {
        JdbcConnector connector = new JdbcConnector("jdbc:mysql://localhost:3306/test", "root", "pass");

        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(connection.createStatement()).thenReturn(statement);
        when(statement.executeQuery("SELECT DATABASE()")).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getString(1)).thenReturn("test_db");

        try (MockedStatic<HikariPoolManager> mockedHikari = mockStatic(HikariPoolManager.class)) {
            mockedHikari.when(() -> HikariPoolManager.getConnection(eq(connector))).thenReturn(connection);

            String schema = mysqlDbClient.getCurrentSchema(connector);

            assertThat(schema).isEqualTo("test_db");
            verify(statement).executeQuery("SELECT DATABASE()");
        }
    }
}
