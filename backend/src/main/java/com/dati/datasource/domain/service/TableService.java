package com.dati.datasource.domain.service;

import com.dati.base.pojo.BasePO;
import com.dati.base.pojo.BaseResourcePO;
import com.dati.base.pojo.PageReq;
import com.dati.common.StringUtils;
import com.dati.datasource.domain.model.ColumnInfo;
import com.dati.datasource.domain.model.TableInfo;
import com.dati.datasource.repository.dao.ColumnInfoDAO;
import com.dati.datasource.repository.dao.TableInfoDAO;
import com.dati.datasource.repository.mapper.ColumnMapper;
import com.dati.datasource.repository.mapper.TableMapper;
import com.dati.datasource.repository.po.TableInfoPO;
import com.dati.datasource.server.assembler.TableAssembler;
import com.dati.datasource.server.pojo.AddTableRequest;
import com.dati.db.Column;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TableService {

    private final TableInfoDAO tableInfoDAO;
    private final ColumnInfoDAO columnInfoDAO;
    private final DataSourceService dataSourceService;
    private final TableAssembler tableAssembler;

    public TableService(TableInfoDAO tableInfoDAO, ColumnInfoDAO columnInfoDAO, DataSourceService dataSourceService, TableAssembler tableAssembler) {
        this.tableInfoDAO = tableInfoDAO;
        this.columnInfoDAO = columnInfoDAO;
        this.dataSourceService = dataSourceService;
        this.tableAssembler = tableAssembler;
    }

    public Page<TableInfo> getTables(PageReq pageReq, String datasourceId, String keyword) {
        Sort sortBy = Sort.by(Sort.Direction.DESC, BasePO.Fields.createdAt);
        if (StringUtils.isBlank(keyword)) {
            return tableInfoDAO.findByDataSourceId(datasourceId, pageReq.toPageRequest().withSort(sortBy)).map(TableMapper::toTableInfo);
        }
        return tableInfoDAO.findByDataSourceIdAndNameContaining(datasourceId, keyword, pageReq.toPageRequest().withSort(sortBy)).map(TableMapper::toTableInfo);
    }

    public List<String> getAddedTableNames(String datasourceId) {
        return tableInfoDAO.findByDataSourceId(datasourceId).stream()
                .map(BaseResourcePO::getName)
                .collect(Collectors.toList());
    }

    @Transactional
    public List<String> batchAddTables(String datasourceId, List<AddTableRequest> tables) {
        List<String> tableIds = new ArrayList<>();
        for (AddTableRequest request : tables) {
            TableInfo tableInfo = new TableInfo();
            tableInfo.setDatasourceId(datasourceId);
            tableInfo.setName(request.getName());
            tableInfo.setSchema(request.getSchema());
            tableAssembler.fillUsersFromRequest(tableInfo);

            TableInfoPO savedPO = tableInfoDAO.save(TableMapper.toTableInfoPO(tableInfo));
            String tableId = savedPO.getId();
            tableIds.add(tableId);

            try {
                List<Column> columns = dataSourceService.getColumns(datasourceId, null, request.getSchema(), request.getName());
                for (Column column : columns) {
                    ColumnInfo columnInfo = new ColumnInfo();
                    columnInfo.setTableId(tableId);
                    columnInfo.setName(column.name());
                    columnInfo.setColumnType(column.type());
                    String columnComment = column.comment();
                    columnInfo.setDescription(StringUtils.isNotBlank(columnComment) ? columnComment : column.name());
                    tableAssembler.fillUsersFromRequest(columnInfo);
                    columnInfoDAO.save(ColumnMapper.toColumnInfoPO(columnInfo));
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to sync columns for table " + request.getName(), e);
            }
        }
        return tableIds;
    }

    @Transactional
    public void deleteTable(String tableId) {
        columnInfoDAO.deleteByTableId(tableId);
        tableInfoDAO.deleteById(tableId);
    }
}
