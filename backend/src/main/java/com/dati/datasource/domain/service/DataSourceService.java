package com.dati.datasource.domain.service;

import com.dati.common.StringUtils;
import com.dati.db.Column;
import com.dati.db.HikariPoolManager;
import com.dati.db.JdbcConnector;
import com.dati.datasource.repository.dao.DataSourceDAO;
import com.dati.datasource.domain.model.DataSource;
import com.dati.datasource.repository.mapper.DSMapper;
import com.dati.datasource.repository.po.DataSourcePO;
import com.dati.db.JdbcUtils;
import com.dati.db.client.DbClient;
import com.dati.db.client.DbClientFactory;
import jakarta.annotation.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    public Page<DataSource> listDataSources(String keyword, Pageable pageable) {
        if (StringUtils.isBlank(keyword)) {
            return dataSourceDAO.findAll(pageable).map(DSMapper::toDataSource);
        }
        return dataSourceDAO.findAllByNameContainingOrId(keyword, keyword, pageable).map(DSMapper::toDataSource);
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
