package com.dati.db;

import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

@Slf4j
public class JdbcUtils {

    public static boolean testConnection(String jdbcUrl, String username, String password) {
        DriverManager.setLoginTimeout(10);
        try (Connection ignored = DriverManager.getConnection(jdbcUrl, username, password)) {
            log.info("Database connection test successful");
            return true;
        } catch (SQLException e) {
            log.error("Failed to connect to database: {}", e.getMessage());
            return false;
        }
    }


}
