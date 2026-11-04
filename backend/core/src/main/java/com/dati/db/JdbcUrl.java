package com.dati.db;

import com.dati.base.exception.DatiException;
import com.dati.base.exception.ErrorCode;

/**
 * JDBC URL parsed into the endpoint pieces needed for pre-flight checks and user-facing messages.
 * Instances only exist for URLs that satisfy the selected {@link DbType}; parsing throws a
 * {@link DatiException} describing exactly what is wrong otherwise.
 */
public record JdbcUrl(String raw, DbType type, String host, int port, String database) {

    private static final String SCHEME_SEPARATOR = "://";

    /**
     * Parses and validates {@code jdbcUrl} for {@code type}.
     *
     * @throws DatiException {@code DS_UNSUPPORTED_TYPE} for unknown types,
     *                       {@code DS_JDBC_TYPE_MISMATCH} when the prefix belongs to another database type,
     *                       {@code DS_INVALID_JDBC_URL} when the host or port is unusable,
     *                       {@code DS_DATABASE_NOT_SPECIFIED} when the URL has no database name
     */
    public static JdbcUrl parse(String jdbcUrl, DbType type) {
        if (type == null || !type.isSupported()) {
            throw new DatiException(ErrorCode.DS_UNSUPPORTED_TYPE, type);
        }
        String raw = jdbcUrl == null ? "" : jdbcUrl.trim();
        if (!type.acceptsUrl(raw)) {
            throw new DatiException(ErrorCode.DS_JDBC_TYPE_MISMATCH,
                    type.getLabel(), type.primaryUrlPrefix(), redact(raw));
        }

        String normalized = normalize(raw);
        String remainder = normalized.substring(normalized.indexOf(SCHEME_SEPARATOR) + SCHEME_SEPARATOR.length());
        int authorityEnd = indexOfAny(remainder, '/', '?', ';');
        String authority = authorityEnd < 0 ? remainder : remainder.substring(0, authorityEnd);

        int userInfoEnd = authority.lastIndexOf('@');
        if (userInfoEnd >= 0) {
            authority = authority.substring(userInfoEnd + 1);
        }

        Endpoint endpoint = endpointOf(authority, raw, type);
        String database = databaseOf(authorityEnd < 0 ? "" : remainder.substring(authorityEnd));
        if (database.isEmpty()) {
            throw new DatiException(ErrorCode.DS_DATABASE_NOT_SPECIFIED, type.getJdbcUrlTemplate());
        }
        return new JdbcUrl(raw, type, endpoint.host(), endpoint.port(), database);
    }

    /** Parses a URL without throwing; returns {@code null} when it is not a valid URL for the type. */
    public static JdbcUrl parseOrNull(String jdbcUrl, DbType type) {
        try {
            return parse(jdbcUrl, type);
        } catch (DatiException e) {
            return null;
        }
    }

    public String endpoint() {
        return host + ":" + port;
    }

    /** Never exposes the raw URL, which may contain embedded credentials. */
    @Override
    public String toString() {
        return "JdbcUrl[type=" + type + ", endpoint=" + endpoint() + ", database=" + database + "]";
    }

    /** URL safe for logs and error messages: user info and password parameters are masked. */
    public String display() {
        return redact(raw);
    }

    public static String redact(String jdbcUrl) {
        if (jdbcUrl == null) {
            return "";
        }
        return jdbcUrl
                .replaceAll("(?i)(://[^/@:]+:)[^/@]*@", "$1***@")
                .replaceAll("(?i)([?;&](?:password|passwd|pwd|secret|token)=)[^;&]*", "$1***");
    }

    private static String normalize(String raw) {
        return raw.replaceFirst("(?i)^(jdbc:clickhouse|jdbc:ch):https?://", "$1://");
    }

    private static Endpoint endpointOf(String authority, String raw, DbType type) {
        if (authority.isBlank()) {
            throw invalidUrl(raw, type);
        }
        if (authority.charAt(0) == '[') {
            int closingBracket = authority.indexOf(']');
            if (closingBracket < 0) {
                throw invalidUrl(raw, type);
            }
            String host = authority.substring(1, closingBracket);
            String rest = authority.substring(closingBracket + 1);
            int port = rest.startsWith(":") ? portOf(rest.substring(1), raw, type) : type.getDefaultPort();
            requireHost(host, raw, type);
            return new Endpoint(host, port);
        }

        int colon = authority.lastIndexOf(':');
        if (colon < 0) {
            requireHost(authority, raw, type);
            return new Endpoint(authority, type.getDefaultPort());
        }
        String host = authority.substring(0, colon);
        requireHost(host, raw, type);
        String portText = authority.substring(colon + 1);
        return new Endpoint(host, portText.isEmpty() ? type.getDefaultPort() : portOf(portText, raw, type));
    }

    private static void requireHost(String host, String raw, DbType type) {
        if (host.isBlank()) {
            throw invalidUrl(raw, type);
        }
    }

    private static int portOf(String portText, String raw, DbType type) {
        if (!portText.chars().allMatch(Character::isDigit)) {
            throw invalidUrl(raw, type);
        }
        int port = Integer.parseInt(portText);
        if (port < 1 || port > 65535) {
            throw invalidUrl(raw, type);
        }
        return port;
    }

    private static String databaseOf(String path) {
        String database = path;
        int parameterStart = indexOfAny(database, '?', ';');
        if (parameterStart >= 0) {
            database = database.substring(0, parameterStart);
        }
        database = database.startsWith("/") ? database.substring(1) : database;
        return database.trim();
    }

    private static int indexOfAny(String text, char... candidates) {
        int result = -1;
        for (char candidate : candidates) {
            int index = text.indexOf(candidate);
            if (index >= 0 && (result < 0 || index < result)) {
                result = index;
            }
        }
        return result;
    }

    private static DatiException invalidUrl(String raw, DbType type) {
        return new DatiException(ErrorCode.DS_INVALID_JDBC_URL, redact(raw), type.getJdbcUrlTemplate());
    }

    private record Endpoint(String host, int port) {
    }
}
