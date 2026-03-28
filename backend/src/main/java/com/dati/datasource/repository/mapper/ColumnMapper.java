package com.dati.datasource.repository.mapper;

import com.dati.base.MapperUtils;
import com.dati.datasource.domain.model.ColumnInfo;
import com.dati.datasource.repository.po.ColumnInfoPO;

public class ColumnMapper {

    public static ColumnInfo toColumnInfo(ColumnInfoPO columnInfoPO) {
        ColumnInfo columnInfo = new ColumnInfo();
        MapperUtils.copyBaseInfo(columnInfoPO, columnInfo);
        columnInfo.setTableId(columnInfoPO.getTableId());
        columnInfo.setColumnType(columnInfoPO.getColumnType());
        columnInfo.setDisplayName(columnInfoPO.getDisplayName());
        return columnInfo;
    }

    public static ColumnInfoPO toColumnInfoPO(ColumnInfo columnInfo) {
        ColumnInfoPO columnInfoPO = new ColumnInfoPO();
        MapperUtils.copyBaseInfo(columnInfo, columnInfoPO);
        columnInfoPO.setTableId(columnInfo.getTableId());
        columnInfoPO.setColumnType(columnInfo.getColumnType());
        columnInfoPO.setDisplayName(columnInfo.getDisplayName());
        return columnInfoPO;
    }

}
