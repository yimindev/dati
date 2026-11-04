package com.dati.db;

import com.dati.base.exception.DatiException;
import com.dati.base.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Translates raw JDBC/driver failures into business errors whose messages tell the user what to
 * check. Classification looks at SQLState, vendor error codes and the cause chain because every
 * driver reports the same problem differently (and sometimes with a misleading cause type).
 */
@Slf4j
public final class JdbcFailures {

    private static final int MYSQL_ACCESS_DENIED_TO_DATABASE = 1044;
    private static final int MYSQL_ACCESS_DENIED = 1045;
    private static final int MYSQL_UNKNOWN_DATABASE = 1049;

    private JdbcFailures() {
    }

    public static DatiException translate(SQLException exception, JdbcConnector connector, DbType type) {
        JdbcUrl url = JdbcUrl.parseOrNull(connector.jdbcUrl(), type);
        String host = url == null ? "" : url.host();
        int port = url == null ? 0 : url.port();
        String database = url == null ? "" : url.database();
        String username = connector.username() == null ? "" : connector.username();
        String text = combinedMessages(exception);
        int vendorCode = vendorCode(exception);
        String sqlState = sqlState(exception);

        if (isDriverMissing(text)) {
            return new DatiException(ErrorCode.DS_DRIVER_MISSING);
        }
        if (text.contains("unable to parse url") || text.contains("invalid url")) {
            return new DatiException(ErrorCode.DS_INVALID_JDBC_URL,
                    url == null ? "" : url.display(), type.getJdbcUrlTemplate());
        }
        if (isTimeout(exception, text)) {
            return endpointError(ErrorCode.DS_CONNECT_TIMEOUT, host, port, exception);
        }
        if (hasCause(exception, ConnectException.class) || text.contains("connection refused")) {
            return endpointError(ErrorCode.DS_CONNECT_REFUSED, host, port, exception);
        }
        if (exception instanceof JdbcConnections.UnresolvedHostException) {
            return hostError(host, exception);
        }
        if (isTlsFailure(text)) {
            return endpointError(ErrorCode.DS_TLS_FAILED, host, port, exception);
        }
        if (isAccessDenied(text, sqlState, vendorCode) && !database.isBlank()) {
            return new DatiException(ErrorCode.DS_ACCESS_DENIED, username, database);
        }
        if (isDatabaseMissing(text, sqlState, vendorCode) && !database.isBlank() && !host.isBlank()) {
            return new DatiException(ErrorCode.DS_DATABASE_NOT_FOUND, database, host, port);
        }
        if (isAuthenticationFailure(text, sqlState, vendorCode) && !username.isBlank()) {
            return new DatiException(ErrorCode.DS_AUTH_FAILED, username);
        }
        if (text.contains("the connection attempt failed")) {
            return new DatiException(ErrorCode.DS_SESSION_FAILED);
        }
        // Unclassified failures keep the driver's own explanation (cleaned and masked) for the user,
        // while the full stack goes to the log so uncovered cases stay diagnosable.
        log.warn("Unclassified JDBC failure for {}", JdbcUrl.redact(connector.jdbcUrl()), exception);
        return new DatiException(ErrorCode.DS_CONNECTION_FAILED, conciseMessage(exception));
    }

    private static boolean isDriverMissing(String text) {
        return text.contains("no suitable driver") || text.contains("failed to get driver instance");
    }

    private static boolean isTimeout(SQLException exception, String text) {
        return exception instanceof SQLTimeoutException
                || hasCause(exception, SocketTimeoutException.class)
                || text.contains("timed out")
                || text.contains("timeout expired");
    }

    private static boolean isTlsFailure(String text) {
        boolean tlsMentioned = text.contains("ssl") || text.contains("tls") || text.contains("certificate");
        boolean failed = text.contains("handshake") || text.contains("negotiat")
                || text.contains("does not support") || text.contains("certificate");
        return tlsMentioned && failed;
    }

    private static boolean isAccessDenied(String text, String sqlState, int vendorCode) {
        return vendorCode == MYSQL_ACCESS_DENIED_TO_DATABASE
                || "42501".equals(sqlState)
                || text.contains("permission denied for database")
                || (text.contains("access denied for user") && text.contains("to database"));
    }

    private static boolean isDatabaseMissing(String text, String sqlState, int vendorCode) {
        return vendorCode == MYSQL_UNKNOWN_DATABASE
                || "3D000".equals(sqlState)
                || text.contains("unknown database")
                || (text.contains("database") && text.contains("does not exist"));
    }

    private static boolean isAuthenticationFailure(String text, String sqlState, int vendorCode) {
        return vendorCode == MYSQL_ACCESS_DENIED
                || (sqlState != null && sqlState.startsWith("28"))
                || text.contains("password authentication failed")
                || text.contains("authentication failed")
                || text.contains("access denied for user")
                || text.contains("no postgresql user name")
                || text.contains("scram")
                || text.contains("login failed");
    }

    private static DatiException endpointError(ErrorCode code, String host, int port, SQLException exception) {
        if (host.isBlank()) {
            return new DatiException(ErrorCode.DS_CONNECTION_FAILED, conciseMessage(exception));
        }
        return new DatiException(code, host, port);
    }

    private static DatiException hostError(String host, SQLException exception) {
        if (host.isBlank()) {
            return new DatiException(ErrorCode.DS_CONNECTION_FAILED, conciseMessage(exception));
        }
        return new DatiException(ErrorCode.DS_HOST_UNRESOLVED, host);
    }

    private static boolean hasCause(Throwable throwable, Class<? extends Throwable> type) {
        for (Throwable current = throwable; current != null; current = current.getCause()) {
            if (type.isInstance(current)) {
                return true;
            }
        }
        return false;
    }

    private static String sqlState(SQLException exception) {
        for (Throwable current = exception; current != null; current = current.getCause()) {
            if (current instanceof SQLException sqlException && sqlException.getSQLState() != null) {
                return sqlException.getSQLState();
            }
        }
        return null;
    }

    private static int vendorCode(SQLException exception) {
        for (Throwable current = exception; current != null; current = current.getCause()) {
            if (current instanceof SQLException sqlException && sqlException.getErrorCode() != 0) {
                return sqlException.getErrorCode();
            }
        }
        return 0;
    }

    private static String combinedMessages(SQLException exception) {
        List<String> messages = new ArrayList<>();
        for (Throwable current = exception; current != null; current = current.getCause()) {
            if (current.getMessage() != null && !current.getMessage().isBlank()) {
                messages.add(current.getMessage().toLowerCase(Locale.ROOT));
            }
        }
        return String.join(" | ", messages);
    }

    /** User-facing driver text: pool wrappers and line breaks removed, credentials masked, length capped. */
    public static String conciseMessage(SQLException exception) {
        String message = null;
        for (Throwable current = exception; current != null; current = current.getCause()) {
            if (current instanceof SQLException && current.getMessage() != null && !current.getMessage().isBlank()) {
                message = current.getMessage();
                break;
            }
        }
        if (message == null) {
            for (Throwable current = exception; current != null && message == null; current = current.getCause()) {
                if (current.getMessage() != null && !current.getMessage().isBlank()) {
                    message = current.getMessage();
                }
            }
        }
        if (message == null) {
            return exception.getClass().getSimpleName();
        }
        String cleaned = JdbcUrl.redact(message
                .replaceAll("(?i)^(failed to initialize (connection )?pool: )+", "")
                .replaceAll("\\s+", " ")
                .trim());
        return cleaned.length() <= 300 ? cleaned : cleaned.substring(0, 300) + "...";
    }
}
