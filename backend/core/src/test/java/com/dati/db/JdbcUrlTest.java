package com.dati.db;

import com.dati.base.exception.DatiException;
import com.dati.base.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("JdbcUrl parsing and validation")
class JdbcUrlTest {

    @Test
    @DisplayName("parses host, port and database from a PostgreSQL URL")
    void shouldParsePostgresqlUrl() {
        JdbcUrl url = JdbcUrl.parse("jdbc:postgresql://db.internal:5433/app", DbType.POSTGRESQL);

        assertThat(url.host()).isEqualTo("db.internal");
        assertThat(url.port()).isEqualTo(5433);
        assertThat(url.database()).isEqualTo("app");
        assertThat(url.endpoint()).isEqualTo("db.internal:5433");
    }

    @Test
    @DisplayName("falls back to the default port when the URL omits it")
    void shouldUseDefaultPortWhenMissing() {
        JdbcUrl url = JdbcUrl.parse("jdbc:mysql://localhost/shop", DbType.MYSQL);

        assertThat(url.port()).isEqualTo(3306);
        assertThat(url.database()).isEqualTo("shop");
    }

    @Test
    @DisplayName("ignores connection parameters after the database name")
    void shouldIgnoreConnectionParameters() {
        JdbcUrl url = JdbcUrl.parse(
                "jdbc:postgresql://localhost:5432/shop?sslmode=require&currentSchema=public",
                DbType.POSTGRESQL);

        assertThat(url.database()).isEqualTo("shop");
        assertThat(url.host()).isEqualTo("localhost");
    }

    @Test
    @DisplayName("accepts ClickHouse HTTP and jdbc:ch URL forms")
    void shouldAcceptClickhouseUrlForms() {
        assertThat(JdbcUrl.parse("jdbc:clickhouse:http://localhost:8123/default", DbType.CLICKHOUSE).endpoint())
                .isEqualTo("localhost:8123");
        assertThat(JdbcUrl.parse("jdbc:ch://localhost:8123/default", DbType.CLICKHOUSE).database())
                .isEqualTo("default");
        assertThat(JdbcUrl.parse("jdbc:clickhouse:https://ch.example.com/db", DbType.CLICKHOUSE).port())
                .isEqualTo(8123);
    }

    @Test
    @DisplayName("strips credentials embedded in the URL and keeps the host")
    void shouldStripUserInfo() {
        JdbcUrl url = JdbcUrl.parse("jdbc:mysql://root:secret@db.internal:3307/shop", DbType.MYSQL);

        assertThat(url.host()).isEqualTo("db.internal");
        assertThat(url.port()).isEqualTo(3307);
        assertThat(url.display()).doesNotContain("secret");
        assertThat(url.toString()).doesNotContain("secret").contains("db.internal:3307");
    }

    @Test
    @DisplayName("parses IPv6 hosts without brackets")
    void shouldParseIpv6Host() {
        JdbcUrl url = JdbcUrl.parse("jdbc:postgresql://[::1]:5432/app", DbType.POSTGRESQL);

        assertThat(url.host()).isEqualTo("::1");
        assertThat(url.port()).isEqualTo(5432);
    }

    @Test
    @DisplayName("rejects a URL whose prefix does not match the selected database type")
    void shouldRejectTypeMismatch() {
        assertThatThrownBy(() -> JdbcUrl.parse("jdbc:postgresql://localhost:5432/app", DbType.MYSQL))
                .isInstanceOf(DatiException.class)
                .satisfies(e -> assertThat(((DatiException) e).getCode())
                        .isEqualTo(ErrorCode.DS_JDBC_TYPE_MISMATCH));
    }

    @Test
    @DisplayName("rejects a URL without a host")
    void shouldRejectMissingHost() {
        assertThatThrownBy(() -> JdbcUrl.parse("jdbc:postgresql://", DbType.POSTGRESQL))
                .isInstanceOf(DatiException.class)
                .satisfies(e -> assertThat(((DatiException) e).getCode())
                        .isEqualTo(ErrorCode.DS_INVALID_JDBC_URL));
        assertThatThrownBy(() -> JdbcUrl.parse("jdbc:postgresql:///app", DbType.POSTGRESQL))
                .isInstanceOf(DatiException.class)
                .satisfies(e -> assertThat(((DatiException) e).getCode())
                        .isEqualTo(ErrorCode.DS_INVALID_JDBC_URL));
    }

    @Test
    @DisplayName("rejects a URL without a database name")
    void shouldRejectMissingDatabase() {
        assertThatThrownBy(() -> JdbcUrl.parse("jdbc:postgresql://localhost:5432", DbType.POSTGRESQL))
                .isInstanceOf(DatiException.class)
                .satisfies(e -> assertThat(((DatiException) e).getCode())
                        .isEqualTo(ErrorCode.DS_DATABASE_NOT_SPECIFIED));
        assertThatThrownBy(() -> JdbcUrl.parse("jdbc:mysql://localhost:3306/", DbType.MYSQL))
                .isInstanceOf(DatiException.class)
                .satisfies(e -> assertThat(((DatiException) e).getCode())
                        .isEqualTo(ErrorCode.DS_DATABASE_NOT_SPECIFIED));
    }

    @Test
    @DisplayName("rejects an invalid or out-of-range port")
    void shouldRejectInvalidPort() {
        assertThatThrownBy(() -> JdbcUrl.parse("jdbc:postgresql://localhost:abc/app", DbType.POSTGRESQL))
                .isInstanceOf(DatiException.class)
                .satisfies(e -> assertThat(((DatiException) e).getCode())
                        .isEqualTo(ErrorCode.DS_INVALID_JDBC_URL));
        assertThatThrownBy(() -> JdbcUrl.parse("jdbc:postgresql://localhost:70000/app", DbType.POSTGRESQL))
                .isInstanceOf(DatiException.class)
                .satisfies(e -> assertThat(((DatiException) e).getCode())
                        .isEqualTo(ErrorCode.DS_INVALID_JDBC_URL));
    }

    @Test
    @DisplayName("rejects unsupported database types")
    void shouldRejectUnsupportedType() {
        assertThatThrownBy(() -> JdbcUrl.parse("jdbc:mysql://localhost:3306/app", DbType.UNKNOWN))
                .isInstanceOf(DatiException.class)
                .satisfies(e -> assertThat(((DatiException) e).getCode())
                        .isEqualTo(ErrorCode.DS_UNSUPPORTED_TYPE));
    }

    @Test
    @DisplayName("redact removes user info and password parameters")
    void redactShouldHideSecrets() {
        String redacted = JdbcUrl.redact(
                "jdbc:mysql://root:secret@db.internal:3307/shop?user=root&password=secret");

        assertThat(redacted)
                .doesNotContain("secret")
                .contains("db.internal:3307/shop");
    }

    @Test
    @DisplayName("invalid URLs are reported with the redacted URL and an example")
    void invalidUrlMessageShouldNotLeakPassword() {
        assertThatThrownBy(() -> JdbcUrl.parse("jdbc:postgresql://root:secret@", DbType.POSTGRESQL))
                .isInstanceOf(DatiException.class)
                .satisfies(e -> assertThat(e.getMessage()).doesNotContain("secret"));
    }
}
