package com.dati.datasource.server.assembler;

import com.dati.base.BaseAssembler;
import com.dati.datasource.domain.model.ColumnInfo;
import com.dati.datasource.server.pojo.ColumnInfoVO;
import org.springframework.stereotype.Component;

@Component
public class ColumnAssembler extends BaseAssembler {

    public ColumnInfoVO toColumnInfoVO(ColumnInfo columnInfo) {
        ColumnInfoVO columnInfoVO = new ColumnInfoVO();
        super.copyBaseInfo(columnInfo, columnInfoVO);
        columnInfoVO.setTableId(columnInfo.getTableId());
        columnInfoVO.setColumnType(columnInfo.getColumnType());
        columnInfoVO.setAliases(columnInfo.getAliases());
        return columnInfoVO;
    }

    public ColumnInfo toColumnInfo(ColumnInfoVO columnInfoVO) {
        ColumnInfo columnInfo = new ColumnInfo();
        super.copyBaseInfo(columnInfoVO, columnInfo);
        columnInfo.setTableId(columnInfoVO.getTableId());
        columnInfo.setColumnType(columnInfoVO.getColumnType());
        columnInfo.setAliases(columnInfoVO.getAliases());
        return columnInfo;
    }

}
