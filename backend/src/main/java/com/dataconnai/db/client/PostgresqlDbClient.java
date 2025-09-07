package com.dataconnai.db.client;

import com.dataconnai.db.DbType;

public class PostgresqlDbClient extends AbstractDbClient {

    @Override
    public DbType getDbType() {
        return DbType.POSTGRESQL;
    }


}
