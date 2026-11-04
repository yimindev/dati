package com.dati.db;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.ConnectException;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("JdbcConnections probe helpers")
class JdbcConnectionsTest {

    // Port 1 on loopback is never listening, so the TCP pre-check fails immediately.
    private static final JdbcConnector REFUSED =
            new JdbcConnector("jdbc:mysql://127.0.0.1:1/test_db", "root", "wrong-password");

    @Test
    @DisplayName("refused TCP connection fails before the driver runs")
    void shouldFailFastWhenPortIsClosed() {
        JdbcUrl url = JdbcUrl.parse(REFUSED.jdbcUrl(), DbType.MYSQL);

        assertThatThrownBy(() -> JdbcConnections.withConnection(REFUSED, url, connection -> {
            throw new AssertionError("task must not run when the endpoint is unreachable");
        }))
                .isInstanceOf(SQLException.class)
                .hasRootCauseInstanceOf(ConnectException.class);
    }

    @Test
    @DisplayName("unresolvable hosts fail before the driver with an UnknownHostException cause")
    void shouldFailOnUnresolvableHost() {
        // Host names longer than 255 characters are invalid by definition, so resolution fails
        // deterministically even on networks whose DNS resolves wildcard names.
        String host = "a".repeat(300);
        JdbcConnector connector = new JdbcConnector("jdbc:postgresql://" + host + ":5432/app", "postgres", "x");
        JdbcUrl url = JdbcUrl.parse(connector.jdbcUrl(), DbType.POSTGRESQL);

        assertThatThrownBy(() -> JdbcConnections.withConnection(connector, url, connection -> null))
                .isInstanceOf(JdbcConnections.UnresolvedHostException.class)
                .hasRootCauseInstanceOf(UnknownHostException.class);
    }

    @Test
    @DisplayName("a stuck task is cancelled and reported as a timeout")
    void shouldTimeOutStuckTasks() {
        assertThatThrownBy(() -> JdbcConnections.runBounded("db.internal:5432", Duration.ofMillis(200), () -> {
            Thread.sleep(5_000);
            return "never";
        }))
                .isInstanceOf(SQLTimeoutException.class)
                .hasMessageContaining("db.internal:5432");
    }

    @Test
    @DisplayName("SQLExceptions from the task keep their type and message")
    void shouldPropagateSqlException() {
        assertThatThrownBy(() -> JdbcConnections.runBounded("db.internal:5432", Duration.ofSeconds(1), () -> {
            throw new SQLException("driver said no", "08000");
        }))
                .isInstanceOf(SQLException.class)
                .hasMessage("driver said no");
    }

    @Test
    @DisplayName("runtime exceptions from the task propagate unchanged")
    void shouldPropagateRuntimeException() {
        assertThatThrownBy(() -> JdbcConnections.runBounded("db.internal:5432", Duration.ofSeconds(1), () -> {
            throw new IllegalStateException("boom");
        }))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("boom");
    }
}
