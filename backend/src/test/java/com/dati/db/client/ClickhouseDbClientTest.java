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

@DisplayName("ClickhouseDbClient unit tests")
class ClickhouseDbClientTest {

    private final ClickhouseDbClient clickhouseDbClient = new ClickhouseDbClient();

    @Test
    @DisplayName("getDbType should return CLICKHOUSE")
    void getDbType_shouldReturnClickHouse() {
        assertThat(clickhouseDbClient.getDbType()).isEqualTo(DbType.CLICKHOUSE);
    }

    @Test
    @DisplayName("getSchemas should query schemas from database metadata")
    void getSchemas_shouldReturnSchemas() throws SQLException {
        JdbcConnector connector = new JdbcConnector("jdbc:clickhouse://localhost:8123/default", "default", "");

        Connection connection = mock(Connection.class);
        DatabaseMetaData metaData = mock(DatabaseMetaData.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getSchemas()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, true, false);
        when(resultSet.getString("TABLE_SCHEM")).thenReturn("default", "system");

        try (MockedStatic<HikariPoolManager> mockedHikari = mockStatic(HikariPoolManager.class)) {
            mockedHikari.when(() -> HikariPoolManager.getConnection(eq(connector))).thenReturn(connection);

            List<String> schemas = clickhouseDbClient.getSchemas(connector);

            assertThat(schemas).containsExactly("default", "system");
        }
    }

    @Test
    @DisplayName("getCurrentSchema should execute SELECT currentDatabase()")
    void getCurrentSchema_shouldReturnCurrentDatabase() throws SQLException {
        JdbcConnector connector = new JdbcConnector("jdbc:clickhouse://localhost:8123/default", "default", "");

        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(connection.createStatement()).thenReturn(statement);
        when(statement.executeQuery("SELECT currentDatabase()")).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getString(1)).thenReturn("default");

        try (MockedStatic<HikariPoolManager> mockedHikari = mockStatic(HikariPoolManager.class)) {
            mockedHikari.when(() -> HikariPoolManager.getConnection(eq(connector))).thenReturn(connection);

            String schema = clickhouseDbClient.getCurrentSchema(connector);

            assertThat(schema).isEqualTo("default");
            verify(statement).executeQuery("SELECT currentDatabase()");
        }
    }
}
