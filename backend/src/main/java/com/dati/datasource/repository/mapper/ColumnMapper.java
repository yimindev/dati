package com.dati.datasource.repository.mapper;

import com.dati.base.MapperUtils;
import com.dati.datasource.domain.model.ColumnInfo;
import com.dati.datasource.repository.po.ColumnInfoPO;

public class ColumnMapper {

    public static ColumnInfo toColumnInfo(ColumnInfoPO columnInfoPO) {
        ColumnInfo columnInfo = new ColumnInfo();
        MapperUtils.copyBaseInfo(columnInfoPO, columnInfo);
        columnInfo.setTableId(columnInfoPO.getTableId());
        columnInfo.setColumnName(columnInfoPO.getColumnName());
        columnInfo.setColumnType(columnInfoPO.getColumnType());
        columnInfo.setComment(columnInfoPO.getComment());
        return columnInfo;
    }

    public static ColumnInfoPO toColumnInfoPO(ColumnInfo columnInfo) {
        ColumnInfoPO columnInfoPO = new ColumnInfoPO();
        MapperUtils.copyBaseInfo(columnInfo, columnInfoPO);
        columnInfoPO.setTableId(columnInfo.getTableId());
        columnInfoPO.setColumnName(columnInfo.getColumnName());
        columnInfoPO.setColumnType(columnInfo.getColumnType());
        columnInfoPO.setComment(columnInfo.getComment());
        return columnInfoPO;
    }

}
