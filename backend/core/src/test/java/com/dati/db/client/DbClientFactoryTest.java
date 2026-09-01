package com.dati.db.client;

import com.dati.db.DbType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DbClientFactory unit tests")
class DbClientFactoryTest {

    @Test
    @DisplayName("Should return correct DbClient implementation for supported DbTypes")
    void getDbClient_shouldReturnCorrectClientForSupportedTypes() {
        assertThat(DbClientFactory.getDbClient(DbType.MYSQL)).isInstanceOf(MysqlDbClient.class);
        assertThat(DbClientFactory.getDbClient(DbType.POSTGRESQL)).isInstanceOf(PostgresqlDbClient.class);
        assertThat(DbClientFactory.getDbClient(DbType.MARIADB)).isInstanceOf(MariaDbClient.class);
        assertThat(DbClientFactory.getDbClient(DbType.CLICKHOUSE)).isInstanceOf(ClickhouseDbClient.class);
        assertThat(DbClientFactory.getDbClient(DbType.DORIS)).isInstanceOf(DorisDbClient.class);
    }

    @Test
    @DisplayName("Should return null for UNKNOWN DbType")
    void getDbClient_shouldReturnNullForUnknown() {
        assertThat(DbClientFactory.getDbClient(DbType.UNKNOWN)).isNull();
    }
}

