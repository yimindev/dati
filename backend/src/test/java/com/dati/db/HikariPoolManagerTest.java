package com.dati.db;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("HikariPoolManager 单元测试")
class HikariPoolManagerTest {

    // 指向本机一个大概率无人监听的端口，连接会立即被拒绝，从而触发 Hikari 连接池初始化失败
    private static final JdbcConnector UNREACHABLE_CONNECTOR =
        new JdbcConnector("jdbc:mysql://127.0.0.1:1/test_db", "root", "wrong-password");

    @AfterEach
    void cleanup() {
        HikariPoolManager.close(UNREACHABLE_CONNECTOR);
    }

    @Test
    @DisplayName("getDataSource - 连接池初始化失败时转换为 SQLException，而不是抛出未处理的 RuntimeException")
    void getDataSource_shouldWrapPoolInitializationExceptionAsSQLException() {
        assertThrows(SQLException.class, () -> HikariPoolManager.getDataSource(UNREACHABLE_CONNECTOR));
    }

    @Test
    @DisplayName("getConnection - 连接池初始化失败时转换为 SQLException")
    void getConnection_shouldWrapPoolInitializationExceptionAsSQLException() {
        assertThrows(SQLException.class, () -> HikariPoolManager.getConnection(UNREACHABLE_CONNECTOR));
    }
}
