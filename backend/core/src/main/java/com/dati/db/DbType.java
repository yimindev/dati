package com.dati.db;

import lombok.Getter;

import java.util.List;
import java.util.Locale;

@Getter
public enum DbType {
    MYSQL("MySQL", 3306, "jdbc:mysql://localhost:3306/database_name", true,
            "jdbc:mysql://"),
    POSTGRESQL("PostgreSQL", 5432, "jdbc:postgresql://localhost:5432/database_name", true,
            "jdbc:postgresql://"),
    MARIADB("MariaDB", 3306, "jdbc:mariadb://localhost:3306/database_name", true,
            "jdbc:mariadb://"),
    CLICKHOUSE("ClickHouse", 8123, "jdbc:clickhouse://localhost:8123/database_name", true,
            "jdbc:clickhouse://", "jdbc:clickhouse:http://", "jdbc:clickhouse:https://",
            "jdbc:ch://", "jdbc:ch:http://", "jdbc:ch:https://"),
    DORIS("Apache Doris", 9030, "jdbc:mysql://localhost:9030/database_name", true,
            "jdbc:mysql://"),
    UNKNOWN("Unknown", 0, "", false);

    private final String label;
    private final int defaultPort;
    private final String jdbcUrlTemplate;
    private final boolean supported;
    private final List<String> urlPrefixes;

    DbType(String label, int defaultPort, String jdbcUrlTemplate, boolean supported, String... urlPrefixes) {
        this.label = label;
        this.defaultPort = defaultPort;
        this.jdbcUrlTemplate = jdbcUrlTemplate;
        this.supported = supported;
        this.urlPrefixes = List.of(urlPrefixes);
    }

    /** Canonical prefix shown to users when their JDBC URL does not match the selected type. */
    public String primaryUrlPrefix() {
        return urlPrefixes.isEmpty() ? "" : urlPrefixes.getFirst();
    }

    public boolean acceptsUrl(String jdbcUrl) {
        String lowerCaseUrl = jdbcUrl.toLowerCase(Locale.ROOT);
        return urlPrefixes.stream().anyMatch(lowerCaseUrl::startsWith);
    }
}
