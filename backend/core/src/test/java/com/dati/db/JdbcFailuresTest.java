package com.dati.db;

import com.dati.base.exception.DatiException;
import com.dati.base.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.ConnectException;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.sql.SQLTimeoutException;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JdbcFailures classification")
class JdbcFailuresTest {

    private static final JdbcConnector MYSQL =
            new JdbcConnector("jdbc:mysql://db.internal:3306/shop", "root", "secret");
    private static final JdbcConnector POSTGRES =
            new JdbcConnector("jdbc:postgresql://pg.internal:5432/app", "postgres", "secret");

    @Test
    @DisplayName("wrong credentials become DS014 naming the user")
    void shouldClassifyAuthenticationFailure() {
        DatiException error = JdbcFailures.translate(
                new SQLException("FATAL: password authentication failed for user \"postgres\"", "28P01"),
                POSTGRES, DbType.POSTGRESQL);

        assertThat(error.getCode()).isEqualTo(ErrorCode.DS_AUTH_FAILED);
        assertThat(error.getMessage()).contains("postgres");
        assertThat(error.getArgs()).containsExactly("postgres");
    }

    @Test
    @DisplayName("MySQL access denied (1045) becomes DS014")
    void shouldClassifyMysqlAccessDenied() {
        DatiException error = JdbcFailures.translate(
                new SQLException("Access denied for user 'root'@'10.0.0.1' (using password: YES)", "28000", 1045),
                MYSQL, DbType.MYSQL);

        assertThat(error.getCode()).isEqualTo(ErrorCode.DS_AUTH_FAILED);
    }

    @Test
    @DisplayName("unknown database becomes DS015 with host and port")
    void shouldClassifyUnknownDatabase() {
        DatiException error = JdbcFailures.translate(
                new SQLException("FATAL: database \"shop\" does not exist", "3D000"),
                POSTGRES, DbType.POSTGRESQL);

        assertThat(error.getCode()).isEqualTo(ErrorCode.DS_DATABASE_NOT_FOUND);
        assertThat(error.getMessage()).contains("app", "pg.internal", "5432");
    }

    @Test
    @DisplayName("MySQL unknown database (1049) becomes DS015")
    void shouldClassifyMysqlUnknownDatabase() {
        DatiException error = JdbcFailures.translate(
                new SQLException("Unknown database 'shop'", "42000", 1049),
                MYSQL, DbType.MYSQL);

        assertThat(error.getCode()).isEqualTo(ErrorCode.DS_DATABASE_NOT_FOUND);
    }

    @Test
    @DisplayName("driver URL parse failures become DS009 with the expected URL shape")
    void shouldClassifyUrlParseFailure() {
        DatiException error = JdbcFailures.translate(
                new SQLException("Unable to parse URL jdbc:postgresql://pg.internal:5432", "08001"),
                POSTGRES, DbType.POSTGRESQL);

        assertThat(error.getCode()).isEqualTo(ErrorCode.DS_INVALID_JDBC_URL);
        assertThat(error.getMessage()).contains("jdbc:postgresql://localhost:5432/database_name");
    }

    @Test
    @DisplayName("database level access denial becomes DS016")
    void shouldClassifyDatabaseAccessDenied() {
        DatiException error = JdbcFailures.translate(
                new SQLException("Access denied for user 'root'@'%' to database 'shop'", "28000", 1044),
                MYSQL, DbType.MYSQL);

        assertThat(error.getCode()).isEqualTo(ErrorCode.DS_ACCESS_DENIED);
        assertThat(error.getArgs()).containsExactly("root", "shop");
    }

    @Test
    @DisplayName("refused connections become DS012")
    void shouldClassifyRefusedConnection() {
        DatiException error = JdbcFailures.translate(
                new SQLException("Connection refused", "08001", new ConnectException("Connection refused")),
                MYSQL, DbType.MYSQL);

        assertThat(error.getCode()).isEqualTo(ErrorCode.DS_CONNECT_REFUSED);
        assertThat(error.getMessage()).contains("db.internal", "3306");
    }

    @Test
    @DisplayName("timeouts become DS013")
    void shouldClassifyTimeout() {
        DatiException error = JdbcFailures.translate(
                new SQLTimeoutException("connect timed out", "08001"),
                MYSQL, DbType.MYSQL);

        assertThat(error.getCode()).isEqualTo(ErrorCode.DS_CONNECT_TIMEOUT);
    }

    @Test
    @DisplayName("only the probe's own DNS failure becomes DS011")
    void shouldClassifyUnknownHost() {
        DatiException error = JdbcFailures.translate(
                new JdbcConnections.UnresolvedHostException("db.internal", new UnknownHostException("nope")),
                MYSQL, DbType.MYSQL);

        assertThat(error.getCode()).isEqualTo(ErrorCode.DS_HOST_UNRESOLVED);
        assertThat(error.getArgs()).containsExactly("db.internal");
    }

    @Test
    @DisplayName("driver-level UnknownHostException (PostgreSQL quirk) becomes DS022, not a DNS failure")
    void shouldReportSessionFailure_whenDriverRejectsAfterReachableTcp() {
        // The TCP pre-check already succeeded when the driver runs, so PostgreSQL's generic
        // "The connection attempt failed." means the session was rejected, not that the host is unknown.
        DatiException error = JdbcFailures.translate(
                new SQLException("The connection attempt failed.", "08001", new UnknownHostException("127.0.0.1")),
                MYSQL, DbType.MYSQL);

        assertThat(error.getCode()).isEqualTo(ErrorCode.DS_SESSION_FAILED);
    }

    @Test
    @DisplayName("TLS negotiation problems become DS017")
    void shouldClassifyTlsFailure() {
        DatiException error = JdbcFailures.translate(
                new SQLException("The server does not support SSL.", "08004"),
                POSTGRES, DbType.POSTGRESQL);

        assertThat(error.getCode()).isEqualTo(ErrorCode.DS_TLS_FAILED);
    }

    @Test
    @DisplayName("missing driver becomes DS018")
    void shouldClassifyMissingDriver() {
        DatiException error = JdbcFailures.translate(
                new SQLException("No suitable driver", "08001"),
                MYSQL, DbType.MYSQL);

        assertThat(error.getCode()).isEqualTo(ErrorCode.DS_DRIVER_MISSING);
    }

    @Test
    @DisplayName("unknown failures fall back to DS001 with a cleaned driver message")
    void shouldFallBackToConnectionFailed() {
        DatiException error = JdbcFailures.translate(
                new SQLException("Failed to initialize connection pool: Failed to initialize pool:   something\nunexpected  ", "08000"),
                MYSQL, DbType.MYSQL);

        assertThat(error.getCode()).isEqualTo(ErrorCode.DS_CONNECTION_FAILED);
        assertThat(error.getArgs()).containsExactly("something unexpected");
    }

    @Test
    @DisplayName("unclassified failures keep the driver message but mask embedded credentials")
    void shouldRedactCredentialsInFallbackMessage() {
        DatiException error = JdbcFailures.translate(
                new SQLException(
                        "Failed to connect to jdbc:mysql://root:secret@db.internal:3306/shop?password=secret",
                        "08000"),
                MYSQL, DbType.MYSQL);

        assertThat(error.getCode()).isEqualTo(ErrorCode.DS_CONNECTION_FAILED);
        assertThat(error.getMessage())
                .doesNotContain("secret")
                .contains("db.internal:3306/shop");
    }

    @Test
    @DisplayName("classification falls back when the URL cannot be parsed")
    void shouldFallBackWithoutParsableUrl() {
        DatiException error = JdbcFailures.translate(
                new SQLException("weird", "08000"),
                new JdbcConnector("not-a-url", "root", "secret"), DbType.MYSQL);

        assertThat(error.getCode()).isEqualTo(ErrorCode.DS_CONNECTION_FAILED);
    }
}
