package com.dati.db.client;

import com.dati.db.DbType;

import java.util.HashMap;
import java.util.Map;

public class DbClientFactory {

    private static final Map<DbType, DbClient> dbClientMap = new HashMap<>();

    static {
        dbClientMap.put(DbType.MYSQL, new MysqlDbClient());
        dbClientMap.put(DbType.POSTGRESQL, new PostgresqlDbClient());
    }

    public static DbClient getDbClient(DbType dbType) {
        return dbClientMap.get(dbType);
    }

}
