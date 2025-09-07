package com.dataconnai.db.client;

import com.dataconnai.db.DbType;
import com.dataconnai.db.JdbcConnector;
import jakarta.annotation.Nullable;

import java.sql.SQLException;
import java.util.List;

public class MysqlDbClient extends AbstractDbClient {
    @Override
    public DbType getDbType() {
        return DbType.MYSQL;
    }

    @Override
    public List<String> getSchemas(JdbcConnector jdbcConnector, @Nullable String catalog) throws SQLException {
        return super.getCatalogs(jdbcConnector);
    }
}
