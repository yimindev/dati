package com.dati.datasource.domain.service;

import com.dati.base.pojo.BasePO;
import com.dati.base.pojo.PageReq;
import com.dati.common.StringUtils;
import com.dati.datasource.domain.model.TableInfo;
import com.dati.datasource.repository.dao.TableInfoDAO;
import com.dati.datasource.repository.mapper.TableMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TableService {

    private final TableInfoDAO tableInfoDAO;

    public TableService(TableInfoDAO tableInfoDAO) {
        this.tableInfoDAO = tableInfoDAO;
    }

    public Page<TableInfo> getTables(PageReq pageReq, String datasourceId, String keyword) {
        Sort sortBy = Sort.by(Sort.Direction.DESC, BasePO.Fields.createdAt);
        if (StringUtils.isBlank(keyword)) {
            return tableInfoDAO.findByDataSourceId(datasourceId, pageReq.toPageRequest().withSort(sortBy)).map(TableMapper::toTableInfo);
        }
        return tableInfoDAO.findByDataSourceIdAndNameContaining(datasourceId, keyword, pageReq.toPageRequest().withSort(sortBy)).map(TableMapper::toTableInfo);
    }

    public void saveTables(List<TableInfo> tables) {
        tableInfoDAO.saveAll(tables.stream().map(TableMapper::toTableInfoPO).toList());
    }

}
