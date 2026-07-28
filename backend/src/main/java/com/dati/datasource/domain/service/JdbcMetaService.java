package com.dati.datasource.domain.service;

import com.dati.base.exception.DatiException;
import com.dati.base.exception.ErrorCode;
import com.dati.db.Column;
import com.dati.db.DbType;
import com.dati.db.HikariPoolManager;
import com.dati.db.JdbcConnector;
import com.dati.db.Table;
import com.dati.db.client.DbClient;
import com.dati.db.client.DbClientFactory;
import com.dati.datasource.domain.model.DataSource;
import com.dati.datasource.repository.dao.DataSourceDAO;
import com.dati.datasource.repository.mapper.DSMapper;
import com.dati.datasource.repository.po.DataSourcePO;
import jakarta.annotation.Nullable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

@Service
public class JdbcMetaService {

    private final DataSourceDAO dataSourceDAO;

    public JdbcMetaService(DataSourceDAO dataSourceDAO) {
        this.dataSourceDAO = dataSourceDAO;
    }

    public List<String> getCatalogs(String dataSourceId) throws SQLException {
        DataSourcePO dataSourcePO = dataSourceDAO.findById(dataSourceId).orElseThrow();
        DbClient dbClient = DbClientFactory.getDbClient(dataSourcePO.getType());
        DataSource dataSource = DSMapper.toDataSource(dataSourcePO);
        return dbClient.getCatalogs(new JdbcConnector(dataSource));
    }

    public List<String> getSchemas(String dataSourceId, @Nullable String catalog) throws SQLException {
        DataSourcePO dataSourcePO = dataSourceDAO.findById(dataSourceId).orElseThrow();
        DbClient dbClient = DbClientFactory.getDbClient(dataSourcePO.getType());
        DataSource dataSource = DSMapper.toDataSource(dataSourcePO);
        return dbClient.getSchemas(new JdbcConnector(dataSource), catalog);
    }

    public List<Table> getTables(String dataSourceId, @Nullable String catalog, String schema) throws SQLException {
        DataSourcePO dataSourcePO = dataSourceDAO.findById(dataSourceId).orElseThrow();
        DbClient dbClient = DbClientFactory.getDbClient(dataSourcePO.getType());
        DataSource dataSource = DSMapper.toDataSource(dataSourcePO);
        return dbClient.getTables(new JdbcConnector(dataSource), catalog, schema);
    }

    public List<Column> getColumns(String dataSourceId, @Nullable String catalog, String schema, String table) throws SQLException {
        DataSourcePO dataSourcePO = dataSourceDAO.findById(dataSourceId).orElseThrow();
        DbClient dbClient = DbClientFactory.getDbClient(dataSourcePO.getType());
        DataSource dataSource = DSMapper.toDataSource(dataSourcePO);
        return dbClient.getColumns(new JdbcConnector(dataSource), catalog, schema, table);
    }

    public List<Map<String, Object>> executeSql(String dataSourceId, String sql) throws SQLException {
        DataSourcePO dataSourcePO = dataSourceDAO.findById(dataSourceId).orElseThrow();
        JdbcConnector jdbcConnector = new JdbcConnector(DSMapper.toDataSource(dataSourcePO));
        JdbcTemplate jdbcTemplate = new JdbcTemplate(HikariPoolManager.getDataSource(jdbcConnector));
        return jdbcTemplate.queryForList(sql);
    }

    @Nullable
    public String resolveCurrentSchema(JdbcConnector connector, DbType dbType) throws SQLException {
        DbClient dbClient = DbClientFactory.getDbClient(dbType);
        if (dbClient == null) {
            throw new DatiException(ErrorCode.DS_UNSUPPORTED_TYPE, dbType);
        }
        return dbClient.getCurrentSchema(connector);
    }
}
