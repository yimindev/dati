package com.dati;

import com.dati.datasource.domain.model.ColumnInfo;
import com.dati.datasource.domain.model.DataSource;
import com.dati.datasource.domain.model.TableInfo;
import com.dati.datasource.repository.po.ColumnInfoPO;
import com.dati.datasource.repository.po.DataSourcePO;
import com.dati.datasource.repository.po.TableInfoPO;
import com.dati.db.DbType;

import java.time.Instant;

public class TestFixtures {

    public static final String TEST_USER_ID = "test-user-001";
    public static final String TEST_DATASOURCE_ID = "ds-001";
    public static final String TEST_TABLE_ID = "table-001";
    public static final String TEST_COLUMN_ID = "col-001";

    public static DataSource createTestDataSource() {
        DataSource ds = new DataSource();
        ds.setId(TEST_DATASOURCE_ID);
        ds.setName("Test MySQL DataSource");
        ds.setDescription("Test data source for unit tests");
        ds.setType(DbType.MYSQL);
        ds.setJdbcUrl("jdbc:mysql://localhost:3306/test_db");
        ds.setUsername("root");
        ds.setPassword("password123");
        ds.setCreatedBy(TEST_USER_ID);
        ds.setCreatedAt(Instant.now());
        ds.setUpdatedBy(TEST_USER_ID);
        ds.setUpdatedAt(Instant.now());
        return ds;
    }

    public static DataSourcePO createTestDataSourcePO() {
        DataSourcePO po = new DataSourcePO();
        po.setId(TEST_DATASOURCE_ID);
        po.setName("Test MySQL DataSource");
        po.setDescription("Test data source for unit tests");
        po.setType(DbType.MYSQL);
        po.setJdbcUrl("jdbc:mysql://localhost:3306/test_db");
        po.setUserName("root");
        po.setEncryptedPassword("password123");
        po.setCreatedBy(TEST_USER_ID);
        po.setCreatedAt(Instant.now());
        po.setUpdatedBy(TEST_USER_ID);
        po.setUpdatedAt(Instant.now());
        return po;
    }

    public static TableInfo createTestTableInfo() {
        TableInfo table = new TableInfo();
        table.setId(TEST_TABLE_ID);
        table.setName("test_table");
        table.setDescription("Test table");
        table.setDatasourceId(TEST_DATASOURCE_ID);
        table.setSchema("public");
        table.setCreatedBy(TEST_USER_ID);
        table.setCreatedAt(Instant.now());
        table.setUpdatedBy(TEST_USER_ID);
        table.setUpdatedAt(Instant.now());
        return table;
    }

    public static TableInfoPO createTestTableInfoPO() {
        TableInfoPO po = new TableInfoPO();
        po.setId(TEST_TABLE_ID);
        po.setName("test_table");
        po.setDescription("Test table");
        po.setDataSourceId(TEST_DATASOURCE_ID);
        po.setSchema("public");
        po.setCreatedBy(TEST_USER_ID);
        po.setCreatedAt(Instant.now());
        po.setUpdatedBy(TEST_USER_ID);
        po.setUpdatedAt(Instant.now());
        return po;
    }

    public static ColumnInfo createTestColumnInfo() {
        ColumnInfo column = new ColumnInfo();
        column.setId(TEST_COLUMN_ID);
        column.setName("test_column");
        column.setDescription("Test column description");
        column.setTableId(TEST_TABLE_ID);
        column.setColumnType("VARCHAR(255)");
        column.setCreatedBy(TEST_USER_ID);
        column.setCreatedAt(Instant.now());
        column.setUpdatedBy(TEST_USER_ID);
        column.setUpdatedAt(Instant.now());
        return column;
    }

    public static ColumnInfoPO createTestColumnInfoPO() {
        ColumnInfoPO po = new ColumnInfoPO();
        po.setId(TEST_COLUMN_ID);
        po.setName("test_column");
        po.setDescription("Test column description");
        po.setTableId(TEST_TABLE_ID);
        po.setColumnType("VARCHAR(255)");
        po.setCreatedBy(TEST_USER_ID);
        po.setCreatedAt(Instant.now());
        po.setUpdatedBy(TEST_USER_ID);
        po.setUpdatedAt(Instant.now());
        return po;
    }

}
