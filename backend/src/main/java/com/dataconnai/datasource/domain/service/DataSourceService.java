package com.dataconnai.datasource.domain.service;

import com.dataconnai.db.Column;
import com.dataconnai.db.HikariPoolManager;
import com.dataconnai.db.JdbcConnector;
import com.dataconnai.datasource.repository.dao.DataSourceDAO;
import com.dataconnai.datasource.domain.model.DataSource;
import com.dataconnai.datasource.repository.mapper.DSMapper;
import com.dataconnai.datasource.repository.po.DataSourcePO;
import com.dataconnai.db.JdbcUtils;
import com.dataconnai.db.client.DbClient;
import com.dataconnai.db.client.DbClientFactory;
import jakarta.annotation.Nullable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class DataSourceService {

    private final DataSourceDAO dataSourceDAO;

    public DataSourceService(DataSourceDAO dataSourceDAO) {
        this.dataSourceDAO = dataSourceDAO;
    }

    public boolean testConnection(JdbcConnector jdbcConnector) {
        return JdbcUtils.testConnection(jdbcConnector.jdbcUrl(), jdbcConnector.username(), jdbcConnector.password());
    }

    public String addDataSource(DataSource dataSource) {
        DataSourcePO dataSourcePO = DSMapper.toDataSourcePO(dataSource);
        dataSourceDAO.save(dataSourcePO);
        return dataSourcePO.getId();
    }

    public void updateDataSource(String id, DataSource dataSource) {
        DataSourcePO dataSourcePO = dataSourceDAO.findById(id).orElseThrow();
        DSMapper.copyProperties(dataSource, dataSourcePO);
        dataSourceDAO.save(dataSourcePO);
    }

    public void deleteDataSource(String id) {
        Optional<DataSourcePO> dataSourcePOOptional = dataSourceDAO.findById(id);
        if (dataSourcePOOptional.isEmpty()) {
            return;
        }
        JdbcConnector jdbcConnector = new JdbcConnector(DSMapper.toDataSource(dataSourcePOOptional.get()));
        HikariPoolManager.close(jdbcConnector);
        dataSourceDAO.deleteById(id);
    }

    public List<DataSource> listDataSources() {
        return dataSourceDAO.findAll().stream().map(DSMapper::toDataSource).toList();
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

    public List<String> getTables(String dataSourceId, @Nullable String catalog, String schema) throws SQLException {
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
}
