package com.dati.db.client;

import com.dati.db.DbType;

public class PostgresqlDbClient extends AbstractDbClient {

    @Override
    public DbType getDbType() {
        return DbType.POSTGRESQL;
    }


}
