package com.dati.db;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.time.Duration;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Opens short-lived JDBC connections for connection probes. A probe resolves the host, checks TCP
 * reachability and runs the caller's work against a fresh connection; the whole sequence is bounded
 * so a black-holed server cannot hang the HTTP request. Pooled connections for runtime metadata
 * queries are managed by {@link HikariPoolManager}.
 */
public final class JdbcConnections {

    static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    static final Duration PROBE_TIMEOUT = Duration.ofSeconds(10);

    private JdbcConnections() {
    }

    @FunctionalInterface
    public interface JdbcTask<T> {
        T execute(Connection connection) throws SQLException;
    }

    /** Raised only when the host name cannot be resolved; the sole signal that maps to DS011. */
    public static final class UnresolvedHostException extends SQLException {
        UnresolvedHostException(String host, Throwable cause) {
            super("Unknown host " + host, "08001", cause);
        }
    }

    /** Runs {@code task} on a fresh connection, bounded by {@link #PROBE_TIMEOUT}. */
    public static <T> T withConnection(JdbcConnector connector, JdbcUrl url, JdbcTask<T> task) throws SQLException {
        return withConnection(connector, url, task, PROBE_TIMEOUT);
    }

    static <T> T withConnection(JdbcConnector connector, JdbcUrl url, JdbcTask<T> task, Duration timeout)
            throws SQLException {
        return runBounded(url.endpoint(), timeout, () -> {
            ensureReachable(url);
            try (Connection connection = open(connector, url)) {
                return task.execute(connection);
            }
        });
    }

    /** Runs {@code task} on a virtual thread and fails with {@link SQLTimeoutException} once {@code timeout} elapses. */
    static <T> T runBounded(String endpoint, Duration timeout, Callable<T> task) throws SQLException {
        FutureTask<T> future = new FutureTask<>(task);
        Thread.ofVirtual().name("jdbc-probe-" + endpoint).start(future);
        try {
            return future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new SQLTimeoutException("Connection to " + endpoint + " timed out", "08001");
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof SQLException sqlException) {
                throw sqlException;
            }
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new SQLException(String.valueOf(cause != null ? cause.getMessage() : e.getMessage()), "08001", cause);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SQLException("Interrupted while connecting to " + endpoint, "08001", e);
        }
    }

    private static void ensureReachable(JdbcUrl url) throws SQLException {
        InetAddress address;
        try {
            address = InetAddress.getByName(url.host());
        } catch (UnknownHostException e) {
            throw new UnresolvedHostException(url.host(), e);
        }
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(address, url.port()), (int) CONNECT_TIMEOUT.toMillis());
        } catch (SocketTimeoutException e) {
            throw new SQLException("Connection to " + url.endpoint() + " timed out", "08001", e);
        } catch (ConnectException e) {
            throw new SQLException("Connection to " + url.endpoint() + " was refused", "08001", e);
        } catch (IOException e) {
            throw new SQLException("Cannot reach " + url.endpoint() + ": " + e.getMessage(), "08001", e);
        }
    }

    private static Connection open(JdbcConnector connector, JdbcUrl url) throws SQLException {
        DriverManager.setLoginTimeout((int) CONNECT_TIMEOUT.toSeconds());
        Properties properties = new Properties();
        properties.setProperty("user", connector.username() == null ? "" : connector.username());
        properties.setProperty("password", connector.password() == null ? "" : connector.password());
        applyConnectTimeout(properties, connector.jdbcUrl(), url.type());
        return DriverManager.getConnection(connector.jdbcUrl(), properties);
    }

    /** Drivers express the connect timeout in different units; a timeout already in the URL wins. */
    private static void applyConnectTimeout(Properties properties, String jdbcUrl, DbType type) {
        if (jdbcUrl.toLowerCase().contains("connecttimeout")) {
            return;
        }
        switch (type) {
            case POSTGRESQL -> properties.setProperty("connectTimeout", String.valueOf(CONNECT_TIMEOUT.toSeconds()));
            case MYSQL, MARIADB, DORIS ->
                    properties.setProperty("connectTimeout", String.valueOf(CONNECT_TIMEOUT.toMillis()));
            default -> {
                // ClickHouse configures HTTP timeouts inside its own URL.
            }
        }
    }
}
