package com.dati.db.client;

import com.dati.db.Column;
import com.dati.db.DbType;
import com.dati.db.HikariPoolManager;
import com.dati.db.JdbcConnector;
import com.dati.db.JdbcUtils;
import com.dati.db.Table;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ClickHouse and Doris Live Docker Integration Tests")
class ClickhouseAndDorisIntegrationTest {

    private static boolean isReachable(int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("localhost", port), 500);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @AfterAll
    static void cleanup() {
        HikariPoolManager.closeAll();
    }

    @Test
    @DisplayName("Verify ClickHouse real connection, metadata extraction, and SQL query")
    void testClickhouseRealConnection() throws SQLException {
        Assumptions.assumeTrue(
                isReachable(8123),
                "ClickHouse service is not running on localhost:8123, skipping integration test."
        );

        JdbcConnector ckConnector = new JdbcConnector(
                "jdbc:clickhouse://localhost:8123/default",
                "default",
                "password123"
        );

        // 1. Test connection
        boolean connected = JdbcUtils.testConnection(ckConnector.jdbcUrl(), ckConnector.username(), ckConnector.password());
        assertThat(connected).isTrue();

        // 2. DbClient from Factory
        DbClient dbClient = DbClientFactory.getDbClient(DbType.CLICKHOUSE);
        assertThat(dbClient).isInstanceOf(ClickhouseDbClient.class);

        // 3. Current schema / database
        String currentSchema = dbClient.getCurrentSchema(ckConnector);
        assertThat(currentSchema).isEqualTo("default");

        // 4. Schema list
        List<String> schemas = dbClient.getSchemas(ckConnector, null);
        assertThat(schemas).contains("default", "system");

        // 5. Table list in default schema
        List<Table> tables = dbClient.getTables(ckConnector, null, "default");
        assertThat(tables.stream().map(Table::name)).contains("test_events");

        // 6. Column list for test_events
        List<Column> columns = dbClient.getColumns(ckConnector, null, "default", "test_events");
        assertThat(columns.stream().map(Column::name)).contains("id", "title", "event_type", "created_at");

        // 7. Execute query via Hikari pool & JdbcTemplate
        JdbcTemplate jdbcTemplate = new JdbcTemplate(HikariPoolManager.getDataSource(ckConnector));
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT id, title, event_type FROM default.test_events ORDER BY id");
        assertThat(rows).hasSize(3);
        assertThat(rows.get(0).get("title")).isEqualTo("Login event");
        assertThat(rows.get(1).get("title")).isEqualTo("Purchase event");
        assertThat(rows.get(2).get("title")).isEqualTo("Logout event");
    }

    @Test
    @DisplayName("Verify Apache Doris real connection, metadata extraction, and SQL query")
    void testDorisRealConnection() throws SQLException {
        Assumptions.assumeTrue(
                isReachable(9030),
                "Apache Doris service is not running on localhost:9030, skipping integration test."
        );

        JdbcConnector dorisConnector = new JdbcConnector(
                "jdbc:mysql://localhost:9030/test_db",
                "root",
                ""
        );

        // 1. Test connection
        boolean connected = JdbcUtils.testConnection(dorisConnector.jdbcUrl(), dorisConnector.username(), dorisConnector.password());
        assertThat(connected).isTrue();

        // 2. DbClient from Factory
        DbClient dbClient = DbClientFactory.getDbClient(DbType.DORIS);
        assertThat(dbClient).isInstanceOf(DorisDbClient.class);

        // 3. Current schema / database
        String currentSchema = dbClient.getCurrentSchema(dorisConnector);
        assertThat(currentSchema).isEqualTo("test_db");

        // 4. Schema list
        List<String> schemas = dbClient.getSchemas(dorisConnector, null);
        assertThat(schemas).contains("test_db", "information_schema");

        // 5. Table list in test_db
        List<Table> tables = dbClient.getTables(dorisConnector, null, "test_db");
        assertThat(tables.stream().map(Table::name)).contains("users");

        // 6. Column list for users table
        List<Column> columns = dbClient.getColumns(dorisConnector, null, "test_db", "users");
        assertThat(columns.stream().map(Column::name)).contains("id", "name", "age", "user_role");

        // 7. Execute query via Hikari pool & JdbcTemplate
        JdbcTemplate jdbcTemplate = new JdbcTemplate(HikariPoolManager.getDataSource(dorisConnector));
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT id, name, age, user_role FROM test_db.users ORDER BY id");
        assertThat(rows).hasSize(3);
        assertThat(rows.get(0).get("name")).isEqualTo("Alice");
        assertThat(rows.get(1).get("name")).isEqualTo("Bob");
        assertThat(rows.get(2).get("name")).isEqualTo("Charlie");
    }
}
