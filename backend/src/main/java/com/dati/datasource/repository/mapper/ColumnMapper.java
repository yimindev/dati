package com.dati.datasource.repository.mapper;

import com.dati.base.MapperUtils;
import com.dati.datasource.domain.model.ColumnInfo;
import com.dati.datasource.repository.po.ColumnInfoPO;

import java.util.ArrayList;

public class ColumnMapper {

    public static ColumnInfo toColumnInfo(ColumnInfoPO columnInfoPO) {
        ColumnInfo columnInfo = new ColumnInfo();
        MapperUtils.copyBaseResourceInfo(columnInfoPO, columnInfo);
        columnInfo.setTableId(columnInfoPO.getTableId());
        columnInfo.setColumnType(columnInfoPO.getColumnType());
        columnInfo.setAliases(columnInfoPO.getAliases() != null ? columnInfoPO.getAliases() : new ArrayList<>());
        columnInfo.setExtractValueEnabled(columnInfoPO.isExtractValueEnabled());
        return columnInfo;
    }

    public static ColumnInfoPO toColumnInfoPO(ColumnInfo columnInfo) {
        ColumnInfoPO columnInfoPO = new ColumnInfoPO();
        MapperUtils.copyBaseResourceInfo(columnInfo, columnInfoPO);
        columnInfoPO.setTableId(columnInfo.getTableId());
        columnInfoPO.setColumnType(columnInfo.getColumnType());
        columnInfoPO.setAliases(columnInfo.getAliases() != null ? columnInfo.getAliases() : new ArrayList<>());
        columnInfoPO.setExtractValueEnabled(Boolean.TRUE.equals(columnInfo.getExtractValueEnabled()));
        return columnInfoPO;
    }

}
