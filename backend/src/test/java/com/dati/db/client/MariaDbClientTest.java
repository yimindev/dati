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

@DisplayName("MariaDbClient unit tests")
class MariaDbClientTest {

    private final MariaDbClient mariaDbClient = new MariaDbClient();

    @Test
    @DisplayName("getDbType should return MARIADB")
    void getDbType_shouldReturnMariaDb() {
        assertThat(mariaDbClient.getDbType()).isEqualTo(DbType.MARIADB);
    }

    @Test
    @DisplayName("getSchemas should query catalogs from database metadata")
    void getSchemas_shouldReturnCatalogs() throws SQLException {
        JdbcConnector connector = new JdbcConnector("jdbc:mariadb://localhost:3306/test", "root", "pass");

        Connection connection = mock(Connection.class);
        DatabaseMetaData metaData = mock(DatabaseMetaData.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getCatalogs()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, true, false);
        when(resultSet.getString("TABLE_CAT")).thenReturn("test_db", "other_db");

        try (MockedStatic<HikariPoolManager> mockedHikari = mockStatic(HikariPoolManager.class)) {
            mockedHikari.when(() -> HikariPoolManager.getConnection(eq(connector))).thenReturn(connection);

            List<String> schemas = mariaDbClient.getSchemas(connector, null);

            assertThat(schemas).containsExactly("test_db", "other_db");
        }
    }

    @Test
    @DisplayName("getCurrentSchema should execute SELECT DATABASE()")
    void getCurrentSchema_shouldReturnCurrentDatabase() throws SQLException {
        JdbcConnector connector = new JdbcConnector("jdbc:mariadb://localhost:3306/test", "root", "pass");

        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(connection.createStatement()).thenReturn(statement);
        when(statement.executeQuery("SELECT DATABASE()")).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getString(1)).thenReturn("test_db");

        try (MockedStatic<HikariPoolManager> mockedHikari = mockStatic(HikariPoolManager.class)) {
            mockedHikari.when(() -> HikariPoolManager.getConnection(eq(connector))).thenReturn(connection);

            String schema = mariaDbClient.getCurrentSchema(connector);

            assertThat(schema).isEqualTo("test_db");
            verify(statement).executeQuery("SELECT DATABASE()");
        }
    }
}
