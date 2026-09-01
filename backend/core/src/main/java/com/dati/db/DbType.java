package com.dati.db;

import lombok.Getter;

@Getter
public enum DbType {
    MYSQL("MySQL", 3306, "jdbc:mysql://localhost:3306/database_name", true),
    POSTGRESQL("PostgreSQL", 5432, "jdbc:postgresql://localhost:5432/database_name", true),
    MARIADB("MariaDB", 3306, "jdbc:mariadb://localhost:3306/database_name", true),
    CLICKHOUSE("ClickHouse", 8123, "jdbc:clickhouse://localhost:8123/database_name", true),
    DORIS("Apache Doris", 9030, "jdbc:mysql://localhost:9030/database_name", true),
    UNKNOWN("Unknown", 0, "", false);

    private final String label;
    private final int defaultPort;
    private final String jdbcUrlTemplate;
    private final boolean supported;

    DbType(String label, int defaultPort, String jdbcUrlTemplate, boolean supported) {
        this.label = label;
        this.defaultPort = defaultPort;
        this.jdbcUrlTemplate = jdbcUrlTemplate;
        this.supported = supported;
    }
}

