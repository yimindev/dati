package com.dati.datasource.repository.mapper;

import com.dati.base.MapperUtils;
import com.dati.datasource.domain.model.TableInfo;
import com.dati.datasource.repository.po.TableInfoPO;

public class TableMapper {

    public static TableInfo toTableInfo(TableInfoPO tableInfoPO) {
        TableInfo tableInfo = new TableInfo();
        MapperUtils.copyBaseInfo(tableInfoPO, tableInfo);
        tableInfo.setDatasourceId(tableInfoPO.getDataSourceId());
        tableInfo.setSchema(tableInfoPO.getSchema());
        return tableInfo;
    }

    public static TableInfoPO toTableInfoPO(TableInfo tableInfo) {
        TableInfoPO tableInfoPO = new TableInfoPO();
        MapperUtils.copyBaseInfo(tableInfo, tableInfoPO);
        tableInfoPO.setDataSourceId(tableInfo.getDatasourceId());
        tableInfoPO.setSchema(tableInfo.getSchema());
        return tableInfoPO;
    }

}
