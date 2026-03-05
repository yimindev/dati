package com.dati.datasource.domain.service;

import com.dati.base.pojo.BasePO;
import com.dati.base.pojo.PageReq;
import com.dati.common.StringUtils;
import com.dati.db.Column;
import com.dati.datasource.domain.model.ColumnInfo;
import com.dati.datasource.domain.model.TableInfo;
import com.dati.datasource.repository.mapper.TableMapper;
import com.dati.datasource.repository.dao.ColumnInfoDAO;
import com.dati.datasource.repository.dao.TableInfoDAO;
import com.dati.datasource.repository.mapper.ColumnMapper;
import com.dati.datasource.repository.po.ColumnInfoPO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.List;

@Service
public class ColumnService {

    private final ColumnInfoDAO columnInfoDAO;

    private final TableInfoDAO tableInfoDAO;

    private final DataSourceService dataSourceService;

    public ColumnService(ColumnInfoDAO columnInfoDAO, TableInfoDAO tableInfoDAO, DataSourceService dataSourceService) {
        this.columnInfoDAO = columnInfoDAO;
        this.tableInfoDAO = tableInfoDAO;
        this.dataSourceService = dataSourceService;
    }

    public Page<ColumnInfo> getColumns(PageReq pageReq, String datasourceId, String tableId, String keyword) {
        Sort sortBy = Sort.by(Sort.Direction.ASC, BasePO.Fields.createdAt);
        if (StringUtils.isBlank(keyword)) {
            return columnInfoDAO.findByTableId(tableId, pageReq.toPageRequest().withSort(sortBy)).map(ColumnMapper::toColumnInfo);
        }
        return columnInfoDAO.findByTableIdAndColumnNameContaining(tableId, keyword, pageReq.toPageRequest().withSort(sortBy)).map(ColumnMapper::toColumnInfo);
    }

    public void updateColumn(String id, ColumnInfo columnInfo) {
        ColumnInfoPO columnInfoPO = columnInfoDAO.findById(id).orElseThrow();
        columnInfoPO.setColumnName(columnInfo.getColumnName());
        columnInfoPO.setColumnType(columnInfo.getColumnType());
        columnInfoPO.setComment(columnInfo.getComment());
        columnInfoPO.setName(columnInfo.getName());
        columnInfoPO.setDescription(columnInfo.getDescription());
        columnInfoDAO.save(columnInfoPO);
    }

    public void syncColumns(String datasourceId, String tableId) throws SQLException {
        TableInfo tableInfo = TableMapper.toTableInfo(tableInfoDAO.findById(tableId).orElseThrow());
        List<Column> columns = dataSourceService.getColumns(datasourceId, null, tableInfo.getSchema(), tableInfo.getName());
        
        columnInfoDAO.deleteByTableId(tableId);
        
        List<ColumnInfoPO> columnInfoPOList = columns.stream().map(column -> {
            ColumnInfoPO columnInfoPO = new ColumnInfoPO();
            columnInfoPO.setTableId(tableId);
            columnInfoPO.setColumnName(column.name());
            columnInfoPO.setColumnType(column.type());
            columnInfoPO.setComment(column.comment());
            return columnInfoPO;
        }).toList();
        
        columnInfoDAO.saveAll(columnInfoPOList);
    }

}
