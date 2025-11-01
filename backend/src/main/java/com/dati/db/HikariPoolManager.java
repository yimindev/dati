package com.dati.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class HikariPoolManager {

    // avoid instantiation
    private HikariPoolManager() {
    }

    private static final Map<JdbcConnector, HikariDataSource> POOLS = new ConcurrentHashMap<>();

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                closeAll();
            } catch (Exception e) {
                log.warn("Error closing all data sources on shutdown", e);
            }
        }, "HikariPoolManager-shutdown"));
    }


    private static HikariConfig buildConfig(JdbcConnector jdbcConnector) {
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(jdbcConnector.jdbcUrl());
        cfg.setUsername(jdbcConnector.username());
        cfg.setPassword(jdbcConnector.password());
        cfg.setMaximumPoolSize(10);
        cfg.setMinimumIdle(0);
        cfg.setConnectionTimeout(1000);
        cfg.setIdleTimeout(60000);
        cfg.setMaxLifetime(1800000);
        return cfg;
    }

    public static HikariDataSource getDataSource(JdbcConnector jdbcConnector) {
        return POOLS.computeIfAbsent(jdbcConnector, k -> new HikariDataSource(buildConfig(jdbcConnector)));
    }

    public static Connection getConnection(JdbcConnector jdbcConnector) throws SQLException {
        HikariDataSource hds = getDataSource(jdbcConnector);
        return hds.getConnection();
    }

    public static void close(JdbcConnector jdbcConnector) {
        HikariDataSource ds = POOLS.remove(jdbcConnector);
        if (ds != null) {
            try {
                ds.close();
            } catch (Exception e) {
                log.warn("Error closing data source for {}", jdbcConnector, e);
            }
        }
    }

    public static void closeAll() {
        // 拿快照，避免遍历中修改并发问题
        for (Map.Entry<JdbcConnector, HikariDataSource> entry : POOLS.entrySet()) {
            try {
                entry.getValue().close();
            } catch (Exception e) {
                log.warn("Error closing data source for {}", entry.getKey(), e);
            }
        }
        POOLS.clear();
    }


}
