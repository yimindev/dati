package com.dati.db.client;

import com.dati.db.DbType;

import java.util.HashMap;
import java.util.Map;

public class DbClientFactory {

    private static final Map<DbType, DbClient> dbClientMap = new HashMap<>();

    static {
        dbClientMap.put(DbType.MYSQL, new MysqlDbClient());
        dbClientMap.put(DbType.POSTGRESQL, new PostgresqlDbClient());
        dbClientMap.put(DbType.MARIADB, new MariaDbClient());
        dbClientMap.put(DbType.CLICKHOUSE, new ClickhouseDbClient());
        dbClientMap.put(DbType.DORIS, new DorisDbClient());
    }

    public static DbClient getDbClient(DbType dbType) {
        return dbClientMap.get(dbType);
    }

}
