package com.dati.datasource.domain.service;

import com.dati.common.StringUtils;
import com.dati.db.HikariPoolManager;
import com.dati.db.JdbcConnector;
import com.dati.db.JdbcUtils;
import com.dati.datasource.repository.dao.ColumnInfoDAO;
import com.dati.datasource.repository.dao.DataSourceDAO;
import com.dati.datasource.repository.dao.TableInfoDAO;
import com.dati.datasource.domain.model.DataSource;
import com.dati.datasource.repository.mapper.DSMapper;
import com.dati.datasource.repository.po.DataSourcePO;
import com.dati.datasource.repository.po.TableInfoPO;
import com.dati.semantic.domain.service.SemanticIndexService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class DataSourceService {

    private final DataSourceDAO dataSourceDAO;
    private final TableInfoDAO tableInfoDAO;
    private final ColumnInfoDAO columnInfoDAO;
    private final SemanticIndexService semanticIndexService;

    public DataSourceService(DataSourceDAO dataSourceDAO, TableInfoDAO tableInfoDAO, ColumnInfoDAO columnInfoDAO, SemanticIndexService semanticIndexService) {
        this.dataSourceDAO = dataSourceDAO;
        this.tableInfoDAO = tableInfoDAO;
        this.columnInfoDAO = columnInfoDAO;
        this.semanticIndexService = semanticIndexService;
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

        List<String> tableIds = tableInfoDAO.findByDataSourceId(id)
                .stream().map(TableInfoPO::getId).toList();
        columnInfoDAO.deleteByTableIdIn(tableIds);
        tableInfoDAO.deleteAllById(tableIds);
        semanticIndexService.deleteByEntityTableIds(tableIds);

        dataSourceDAO.deleteById(id);
    }

    public Page<DataSource> listDataSources(String keyword, Pageable pageable) {
        if (StringUtils.isBlank(keyword)) {
            return dataSourceDAO.findAll(pageable).map(DSMapper::toDataSource);
        }
        return dataSourceDAO.findAllByNameContainingOrId(keyword, keyword, pageable).map(DSMapper::toDataSource);
    }
}
