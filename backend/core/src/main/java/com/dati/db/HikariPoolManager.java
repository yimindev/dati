package com.dati.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.pool.HikariPool;
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

    /** 预期的 Hikari 连接池初始化失败（如认证失败、网络不通等）会被转换为 SQLException，供调用方统一处理为业务异常。 */
    public static HikariDataSource getDataSource(JdbcConnector jdbcConnector) throws SQLException {
        HikariDataSource existing = POOLS.get(jdbcConnector);
        if (existing != null) {
            return existing;
        }
        HikariDataSource created;
        try {
            created = new HikariDataSource(buildConfig(jdbcConnector));
        } catch (HikariPool.PoolInitializationException e) {
            throw new SQLException("Failed to initialize connection pool: " + e.getMessage(), e);
        }
        HikariDataSource prior = POOLS.putIfAbsent(jdbcConnector, created);
        if (prior != null) {
            created.close();
            return prior;
        }
        return created;
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
