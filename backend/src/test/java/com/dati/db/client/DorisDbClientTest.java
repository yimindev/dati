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

@DisplayName("DorisDbClient unit tests")
class DorisDbClientTest {

    private final DorisDbClient dorisDbClient = new DorisDbClient();

    @Test
    @DisplayName("getDbType should return DORIS")
    void getDbType_shouldReturnDoris() {
        assertThat(dorisDbClient.getDbType()).isEqualTo(DbType.DORIS);
    }

    @Test
    @DisplayName("getSchemas should query catalogs from database metadata")
    void getSchemas_shouldReturnCatalogs() throws SQLException {
        JdbcConnector connector = new JdbcConnector("jdbc:mysql://localhost:9030/test", "root", "pass");

        Connection connection = mock(Connection.class);
        DatabaseMetaData metaData = mock(DatabaseMetaData.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getCatalogs()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, true, false);
        when(resultSet.getString("TABLE_CAT")).thenReturn("doris_db1", "doris_db2");

        try (MockedStatic<HikariPoolManager> mockedHikari = mockStatic(HikariPoolManager.class)) {
            mockedHikari.when(() -> HikariPoolManager.getConnection(eq(connector))).thenReturn(connection);

            List<String> schemas = dorisDbClient.getSchemas(connector, null);

            assertThat(schemas).containsExactly("doris_db1", "doris_db2");
        }
    }

    @Test
    @DisplayName("getCurrentSchema should execute SELECT DATABASE()")
    void getCurrentSchema_shouldReturnCurrentDatabase() throws SQLException {
        JdbcConnector connector = new JdbcConnector("jdbc:mysql://localhost:9030/test", "root", "pass");

        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(connection.createStatement()).thenReturn(statement);
        when(statement.executeQuery("SELECT DATABASE()")).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getString(1)).thenReturn("doris_db");

        try (MockedStatic<HikariPoolManager> mockedHikari = mockStatic(HikariPoolManager.class)) {
            mockedHikari.when(() -> HikariPoolManager.getConnection(eq(connector))).thenReturn(connection);

            String schema = dorisDbClient.getCurrentSchema(connector);

            assertThat(schema).isEqualTo("doris_db");
            verify(statement).executeQuery("SELECT DATABASE()");
        }
    }
}
