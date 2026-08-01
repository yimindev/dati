package com.dati.db;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("HikariPoolManager unit tests")
class HikariPoolManagerTest {

    // 指向本机一个大概率无人监听的端口，连接会立即被拒绝，从而触发 Hikari 连接池初始化失败
    private static final JdbcConnector UNREACHABLE_CONNECTOR =
        new JdbcConnector("jdbc:mysql://127.0.0.1:1/test_db", "root", "wrong-password");

    @AfterEach
    void cleanup() {
        HikariPoolManager.close(UNREACHABLE_CONNECTOR);
    }

    @Test
    @DisplayName("getDataSource - converts pool init failure to SQLException instead of raw RuntimeException")
    void getDataSource_shouldWrapPoolInitializationExceptionAsSQLException() {
        assertThrows(SQLException.class, () -> HikariPoolManager.getDataSource(UNREACHABLE_CONNECTOR));
    }

    @Test
    @DisplayName("getConnection - converts pool init failure to SQLException")
    void getConnection_shouldWrapPoolInitializationExceptionAsSQLException() {
        assertThrows(SQLException.class, () -> HikariPoolManager.getConnection(UNREACHABLE_CONNECTOR));
    }
}
